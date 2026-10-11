package com.example.cinerec.controller;

import com.example.cinerec.entity.Movie;
import com.example.cinerec.repository.MovieRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/**
 * Публичная часть сайта: каталог и страница фильма.
 * Данные каждый раз читаются из базы, поэтому правки из админки видны сразу.
 */
@Controller
public class MovieController {

    private final MovieRepository movieRepository;

    public MovieController(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @GetMapping("/")
    public String catalog(Model model) {
        model.addAttribute("movies", movieRepository.findAllByOrderByRatingDescTitleAsc());
        return "movies/catalog";
    }

    @GetMapping("/movies/{id}")
    public String movie(@PathVariable Long id, Model model) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден"));
        model.addAttribute("movie", movie);
        return "movies/details";
    }
}
