package com.example.cinerec.controller.api;

import com.example.cinerec.entity.Genre;
import com.example.cinerec.form.MovieForm;
import com.example.cinerec.ml.MoodPredictor;
import com.example.cinerec.ml.MoodSuggestion;
import com.example.cinerec.repository.GenreRepository;
import com.example.cinerec.service.MovieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Админка фильмов через API: те же проверки, что у формы на Thymeleaf (MovieForm). */
@RestController
@RequestMapping("/api/admin/movies")
public class AdminMovieApiController {

    private final MovieService movieService;
    private final GenreRepository genreRepository;
    private final MoodPredictor moodPredictor;

    public AdminMovieApiController(MovieService movieService, GenreRepository genreRepository,
                                   MoodPredictor moodPredictor) {
        this.movieService = movieService;
        this.genreRepository = genreRepository;
        this.moodPredictor = moodPredictor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovieDto create(@Valid @RequestBody MovieForm form) {
        return MovieDto.from(movieService.create(form));
    }

    @PutMapping("/{id}")
    public MovieDto update(@PathVariable Long id, @Valid @RequestBody MovieForm form) {
        return MovieDto.from(movieService.update(id, form));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        movieService.delete(id);
    }

    public record SuggestMoodsRequest(String description, List<Long> genreIds) {
    }

    /** Подсказка настроений по описанию и жанрам; фильм при этом не сохраняется. */
    @PostMapping("/suggest-moods")
    public List<MoodSuggestion> suggestMoods(@RequestBody SuggestMoodsRequest request) {
        if (request.description() == null || request.description().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Чтобы получить подсказку, сначала заполните описание");
        }
        if (!moodPredictor.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Модель настроений не загружена");
        }
        List<Long> genreIds = request.genreIds() == null ? List.of() : request.genreIds();
        List<String> genreNames = genreRepository.findAllById(genreIds).stream().map(Genre::getName).toList();
        return moodPredictor.suggest(request.description(), genreNames);
    }
}
