package com.example.cinerec;

import com.example.cinerec.entity.Genre;
import com.example.cinerec.entity.Mood;
import com.example.cinerec.entity.Movie;
import com.example.cinerec.repository.GenreRepository;
import com.example.cinerec.repository.MoodRepository;
import com.example.cinerec.repository.MovieRepository;
import com.example.cinerec.repository.MovieTypeRepository;
import com.example.cinerec.service.Recommendation;
import com.example.cinerec.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Всё приложение на настоящем PostgreSQL 16, который Testcontainers поднимает в Docker.
 * Проверяет то, чего не видят тесты с заглушками: миграции Flyway, соответствие сущностей схеме
 * (ddl-auto=validate — иначе приложение просто не запустится), начальные данные, каскадное удаление
 * и страницы на реальных данных.
 * Без Docker тесты пропускаются. Каждый тест в транзакции, которая откатывается, — база остаётся исходной.
 * Модели не загружаются (несуществующий путь), чтобы тесты не зависели от того, обучены ли они.
 */
@SpringBootTest(properties = {
        "cinerec.model.path=target/no-such-model",
        "cinerec.chat-model.path=target/no-such-model",
        "cinerec.admin.password=test"})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class DatabaseIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private MoodRepository moodRepository;
    @Autowired
    private MovieTypeRepository typeRepository;
    @Autowired
    private RecommendationService recommendationService;

    @Test
    void migrationsCreateSchemaAndInitialData() {
        assertEquals(2, jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success", Integer.class));
        assertEquals(60, movieRepository.count());
        assertEquals(16, genreRepository.count());
        assertEquals(6, moodRepository.count());
        assertEquals(3, typeRepository.count());
        assertEquals(2020, movieRepository.findNewestYear());
        // У каждого фильма есть хотя бы один жанр и одно настроение
        assertEquals(0, jdbc.queryForObject("""
                select count(*) from movie m
                where not exists (select 1 from movie_genre g where g.movie_id = m.id)
                   or not exists (select 1 from movie_mood d where d.movie_id = m.id)""", Integer.class));
    }

    @Test
    void recommendationOnRealData() {
        Mood funny = mood("весёлое");
        Genre comedy = genre("комедия");

        List<Recommendation> result = recommendationService.recommend(funny.getId(), comedy.getId(), null);

        assertFalse(result.isEmpty());
        assertTrue(result.size() <= RecommendationService.LIMIT);
        Recommendation best = result.get(0);
        assertTrue(best.moodMatched() && best.genreMatched());
        assertTrue(best.movie().getMoods().contains(funny));
    }

    @Test
    void deletingGenreKeepsMoviesAndRemovesTheirLinks() {
        Genre comedy = genre("комедия");
        int linksBefore = linksOf(comedy);
        assertTrue(linksBefore > 0);

        genreRepository.delete(comedy);
        genreRepository.flush();

        assertEquals(0, linksOf(comedy)); // ON DELETE CASCADE в movie_genre
        assertEquals(60, movieRepository.count());
    }

    @Test
    void catalogAndMoviePageRenderRealData() throws Exception {
        Movie shawshank = movieRepository.findAll().stream()
                .filter(m -> m.getTitle().equals("Побег из Шоушенка"))
                .findFirst()
                .orElseThrow();

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Побег из Шоушенка")));
        mvc.perform(get("/movies/" + shawshank.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Энди Дюфрейна")));
        mvc.perform(get("/movies/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void recommendFormRendersResults() throws Exception {
        mvc.perform(get("/recommend")
                        .param("moodId", mood("грустное").getId().toString())
                        .param("submitted", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("results", hasSize(RecommendationService.LIMIT)));
    }

    @Test
    void chatMoreWorksOnRealDataWithoutModel() throws Exception {
        // «ещё» по контексту не обращается к модели, поэтому работает и без обученной модели
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("""
                        {"message": "ещё",
                         "context": {"criteria": {"yearFrom": 1990, "yearTo": 1999}, "shownMovieIds": []}}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movies.length()").value(5))
                .andExpect(jsonPath("$.movies[*].year", everyItem(allOf(greaterThanOrEqualTo(1990), lessThanOrEqualTo(1999)))))
                .andExpect(jsonPath("$.context.shownMovieIds.length()").value(5));
    }

    @Test
    void apiDocsDescribeOnlyApiWithLoginAndProtectedAdmin() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/movies'].get.summary").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/admin/movies'].post.security[0].session").exists())
                .andExpect(jsonPath("$.paths['/api/movies'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/recommend']").doesNotExist());
    }

    private int linksOf(Genre genre) {
        return jdbc.queryForObject("select count(*) from movie_genre where genre_id = ?", Integer.class, genre.getId());
    }

    private Mood mood(String name) {
        return moodRepository.findAll().stream().filter(m -> m.getName().equals(name)).findFirst().orElseThrow();
    }

    private Genre genre(String name) {
        return genreRepository.findAll().stream().filter(g -> g.getName().equals(name)).findFirst().orElseThrow();
    }
}
