package com.example.cinerec.service;

import com.example.cinerec.entity.Genre;
import com.example.cinerec.entity.Mood;
import com.example.cinerec.entity.Movie;
import com.example.cinerec.entity.MovieType;
import com.example.cinerec.repository.MovieRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Проверка правил подбора без базы данных:
 * репозиторий подменяется заглушкой (mock) с заранее заданными фильмами.
 */
class RecommendationServiceTest {

    private static final long FUNNY = 1L, SAD = 2L;     // настроения
    private static final long COMEDY = 10L, DRAMA = 11L; // жанры
    private static final long FILM = 20L, SERIES = 21L;  // типы

    private final MovieRepository repository = mock(MovieRepository.class);
    private final RecommendationService service = new RecommendationService(repository);

    @Test
    void scoresAreSummedAndSortedByScore() {
        when(repository.findAllWithDetails()).thenReturn(List.of(
                movie("Только тип", 9.0, FILM, DRAMA, SAD),       // 1 балл
                movie("Всё совпало", 7.0, FILM, COMEDY, FUNNY),   // 3 + 2 + 1 = 6
                movie("Настроение", 8.0, SERIES, DRAMA, FUNNY),   // 3
                movie("Ничего", 9.9, SERIES, DRAMA, SAD)));       // 0 — не попадает

        List<Recommendation> result = service.recommend(FUNNY, COMEDY, FILM);

        assertEquals(List.of("Всё совпало", "Настроение", "Только тип"), titles(result));
        assertEquals(List.of(6, 3, 1), result.stream().map(Recommendation::score).toList());
    }

    @Test
    void equalScoresAreSortedByRating() {
        when(repository.findAllWithDetails()).thenReturn(List.of(
                movie("Средний", 7.5, FILM, COMEDY, FUNNY),
                movie("Лучший", 9.0, FILM, COMEDY, FUNNY),
                movie("Слабый", 6.0, FILM, COMEDY, FUNNY)));

        List<Recommendation> result = service.recommend(FUNNY, null, null);

        assertEquals(List.of("Лучший", "Средний", "Слабый"), titles(result));
    }

    @Test
    void returnsAtMostTenMovies() {
        List<Movie> movies = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            movies.add(movie("Фильм " + i, 5.0 + i * 0.1, FILM, COMEDY, FUNNY));
        }
        when(repository.findAllWithDetails()).thenReturn(movies);

        assertEquals(10, service.recommend(null, COMEDY, null).size());
        // Для чата («ещё») — все подходящие фильмы
        assertEquals(15, service.rank(null, COMEDY, null, Set.of(), Set.of()).size());
    }

    @Test
    void excludedGenresAndTypesAreSkipped() {
        when(repository.findAllWithDetails()).thenReturn(List.of(
                movie("Весёлая драма", 9.0, FILM, DRAMA, FUNNY),      // исключён жанр
                movie("Весёлый сериал", 8.0, SERIES, COMEDY, FUNNY),  // исключён тип
                movie("Весёлая комедия", 7.0, FILM, COMEDY, FUNNY)));

        List<Recommendation> result = service.rank(FUNNY, null, null, Set.of(DRAMA), Set.of(SERIES));

        assertEquals(List.of("Весёлая комедия"), titles(result));
    }

    @Test
    void nothingSelectedGivesEmptyResult() {
        when(repository.findAllWithDetails()).thenReturn(List.of(movie("Фильм", 8.0, FILM, COMEDY, FUNNY)));

        assertTrue(service.recommend(null, null, null).isEmpty());
    }

    private static Movie movie(String title, double rating, long typeId, long genreId, long moodId) {
        MovieType type = new MovieType();
        type.setId(typeId);
        Genre genre = new Genre();
        genre.setId(genreId);
        Mood mood = new Mood();
        mood.setId(moodId);

        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setRating(rating);
        movie.setType(type);
        movie.setGenres(Set.of(genre));
        movie.setMoods(Set.of(mood));
        return movie;
    }

    private static List<String> titles(List<Recommendation> result) {
        return result.stream().map(r -> r.movie().getTitle()).toList();
    }
}
