package com.example.cinerec.controller.api;

import com.example.cinerec.entity.Genre;
import com.example.cinerec.entity.Mood;
import com.example.cinerec.entity.Movie;
import com.example.cinerec.entity.MovieType;
import com.example.cinerec.repository.GenreRepository;
import com.example.cinerec.repository.MoodRepository;
import com.example.cinerec.repository.MovieRepository;
import com.example.cinerec.repository.MovieTypeRepository;
import com.example.cinerec.security.SecurityConfig;
import com.example.cinerec.service.Criteria;
import com.example.cinerec.service.MovieService;
import com.example.cinerec.service.Recommendation;
import com.example.cinerec.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Публичный API: формат JSON, 404 с текстом ошибки, разбор условий подбора из адреса. */
@WebMvcTest(CatalogApiController.class)
@Import({MovieService.class, SecurityConfig.class})
class CatalogApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MovieRepository movieRepository;
    @MockitoBean
    private MovieTypeRepository typeRepository;
    @MockitoBean
    private GenreRepository genreRepository;
    @MockitoBean
    private MoodRepository moodRepository;
    @MockitoBean
    private RecommendationService recommendationService;

    @Test
    void movieComesWithTypeGenresAndMoodsAsObjects() throws Exception {
        when(movieRepository.findAllWithDetailsForCatalog()).thenReturn(List.of(amelie()));

        mvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].title").value("Амели"))
                .andExpect(jsonPath("$[0].posterUrl").isEmpty())
                .andExpect(jsonPath("$[0].type.name").value("фильм"))
                .andExpect(jsonPath("$[0].genres[0].name").value("комедия"))
                .andExpect(jsonPath("$[0].moods[0].id").value(3));
    }

    @Test
    void unknownMovieGives404WithMessage() throws Exception {
        when(movieRepository.findById(999L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/movies/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Фильм не найден"));
    }

    @Test
    void recommendReadsCommaSeparatedIds() throws Exception {
        Criteria expected = new Criteria(3L, Set.of(2L, 5L), Set.of(1L), Set.of(), Set.of(), 1990, null);
        when(recommendationService.rank(expected))
                .thenReturn(List.of(new Recommendation(amelie(), 5, true, true, false)));

        mvc.perform(get("/api/recommend?moodId=3&genreIds=2,5&typeIds=1&yearFrom=1990"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movie.title").value("Амели"))
                .andExpect(jsonPath("$[0].score").value(5))
                .andExpect(jsonPath("$[0].typeMatched").value(false));
    }

    @Test
    void recommendWithoutWishesIsRejected() throws Exception {
        mvc.perform(get("/api/recommend?yearFrom=1990"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Выберите хотя бы что-нибудь: настроение, жанр или тип."));

        verify(recommendationService, never()).rank(any());
    }

    private static Movie amelie() {
        MovieType film = new MovieType();
        film.setId(1L);
        film.setName("фильм");
        Genre comedy = new Genre();
        comedy.setId(2L);
        comedy.setName("комедия");
        Mood light = new Mood();
        light.setId(3L);
        light.setName("лёгкое");

        Movie movie = new Movie();
        movie.setId(7L);
        movie.setTitle("Амели");
        movie.setYear(2001);
        movie.setDescription("Описание");
        movie.setRating(8.3);
        movie.setType(film);
        movie.setGenres(Set.of(comedy));
        movie.setMoods(Set.of(light));
        return movie;
    }
}
