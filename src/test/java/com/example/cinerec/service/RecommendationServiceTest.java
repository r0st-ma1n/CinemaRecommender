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
        assertEquals(15, service.rank(new Criteria(null, Set.of(COMEDY), Set.of(), Set.of(), Set.of(), null, null)).size());
    }

    @Test
    void excludedGenresAndTypesAreSkipped() {
        when(repository.findAllWithDetails()).thenReturn(List.of(
                movie("Весёлая драма", 9.0, FILM, DRAMA, FUNNY),      // исключён жанр
                movie("Весёлый сериал", 8.0, SERIES, COMEDY, FUNNY),  // исключён тип
                movie("Весёлая комедия", 7.0, FILM, COMEDY, FUNNY)));

        List<Recommendation> result = service.rank(new Criteria(FUNNY, Set.of(), Set.of(), Set.of(DRAMA), Set.of(SERIES), null, null));

        assertEquals(List.of("Весёлая комедия"), titles(result));
    }

    @Test
    void anyOfSeveralGenresAndTypesGivesPoints() {
        when(repository.findAllWithDetails()).thenReturn(List.of(
                movie("Комедия-сериал", 7.0, SERIES, COMEDY, SAD), // жанр: 2
                movie("Драма-фильм", 8.0, FILM, DRAMA, SAD),       // жанр и тип: 2 + 1 = 3
                movie("Драма-сериал", 9.0, SERIES, DRAMA, SAD)));  // жанр: 2

        // «комедию или драму, лучше фильм»
        List<Recommendation> result = service.rank(new Criteria(null, Set.of(COMEDY, DRAMA), Set.of(FILM), Set.of(), Set.of(), null, null));
        assertEquals(List.of("Драма-фильм", "Драма-сериал", "Комедия-сериал"), titles(result));
        assertEquals(List.of(3, 2, 2), result.stream().map(Recommendation::score).toList());

        // «фильм или сериал» — тип любого из них даёт балл
        assertEquals(3, service.rank(new Criteria(null, Set.of(), Set.of(FILM, SERIES), Set.of(), Set.of(), null, null)).size());
    }

    @Test
    void nothingSelectedGivesEmptyResult() {
        when(repository.findAllWithDetails()).thenReturn(List.of(movie("Фильм", 8.0, FILM, COMEDY, FUNNY)));

        assertTrue(service.recommend(null, null, null).isEmpty());
    }

    @Test
    void yearsFilterMoviesAndAloneGiveAllMoviesOfThoseYears() {
        when(repository.findAllWithDetails()).thenReturn(List.of(
                movie("Комедия 1985", 9.0, FILM, COMEDY, FUNNY, 1985),
                movie("Драма 1994", 8.0, FILM, DRAMA, SAD, 1994),
                movie("Комедия 1997", 7.0, FILM, COMEDY, FUNNY, 1997)));

        // «весёлое из 90-х»: комедия 1985 года не подходит по годам
        List<Recommendation> funny90s = service.rank(new Criteria(FUNNY, Set.of(), Set.of(), Set.of(), Set.of(), 1990, 1999));
        assertEquals(List.of("Комедия 1997"), titles(funny90s));

        // «что-нибудь из 90-х»: только годы — все фильмы этих лет по рейтингу
        List<Recommendation> any90s = service.rank(new Criteria(null, Set.of(), Set.of(), Set.of(), Set.of(), 1990, 1999));
        assertEquals(List.of("Драма 1994", "Комедия 1997"), titles(any90s));
    }

    private static Movie movie(String title, double rating, long typeId, long genreId, long moodId) {
        return movie(title, rating, typeId, genreId, moodId, 2000);
    }

    private static Movie movie(String title, double rating, long typeId, long genreId, long moodId, int year) {
        MovieType type = new MovieType();
        type.setId(typeId);
        Genre genre = new Genre();
        genre.setId(genreId);
        Mood mood = new Mood();
        mood.setId(moodId);

        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setYear(year);
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
