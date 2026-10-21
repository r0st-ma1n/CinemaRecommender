package com.example.cinerec.service;

import com.example.cinerec.entity.Movie;
import com.example.cinerec.form.MovieForm;
import com.example.cinerec.repository.GenreRepository;
import com.example.cinerec.repository.MoodRepository;
import com.example.cinerec.repository.MovieRepository;
import com.example.cinerec.repository.MovieTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;

/**
 * Добавление, изменение и удаление фильмов. Общее для админки на Thymeleaf и для REST API,
 * поэтому данные приходят в одном виде — MovieForm, уже проверенной валидацией.
 */
@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final MovieTypeRepository typeRepository;
    private final GenreRepository genreRepository;
    private final MoodRepository moodRepository;

    public MovieService(MovieRepository movieRepository,
                        MovieTypeRepository typeRepository,
                        GenreRepository genreRepository,
                        MoodRepository moodRepository) {
        this.movieRepository = movieRepository;
        this.typeRepository = typeRepository;
        this.genreRepository = genreRepository;
        this.moodRepository = moodRepository;
    }

    public Movie find(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден"));
    }

    public Movie create(MovieForm form) {
        Movie movie = new Movie();
        copyFormToMovie(form, movie);
        movieRepository.save(movie);
        return movie;
    }

    public Movie update(Long id, MovieForm form) {
        Movie movie = find(id);
        copyFormToMovie(form, movie);
        movieRepository.save(movie);
        return movie;
    }

    public Movie delete(Long id) {
        Movie movie = find(id);
        movieRepository.delete(movie);
        return movie;
    }

    /** Перенести данные из формы в сущность; тип, жанры и настроения загружаются по id. */
    private void copyFormToMovie(MovieForm form, Movie movie) {
        movie.setTitle(form.getTitle().trim());
        movie.setYear(form.getYear());
        movie.setDescription(form.getDescription().trim());
        // Пустая ссылка на постер хранится как NULL — тогда показывается заглушка
        String poster = form.getPosterUrl();
        movie.setPosterUrl(poster == null || poster.isBlank() ? null : poster.trim());
        movie.setRating(form.getRating());
        movie.setType(typeRepository.findById(form.getTypeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нет такого типа")));
        movie.setGenres(new HashSet<>(genreRepository.findAllById(form.getGenreIds())));
        movie.setMoods(new HashSet<>(moodRepository.findAllById(form.getMoodIds())));
    }
}
