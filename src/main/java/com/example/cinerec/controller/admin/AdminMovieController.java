package com.example.cinerec.controller.admin;

import com.example.cinerec.entity.Movie;
import com.example.cinerec.form.MovieForm;
import com.example.cinerec.repository.GenreRepository;
import com.example.cinerec.repository.MoodRepository;
import com.example.cinerec.repository.MovieRepository;
import com.example.cinerec.repository.MovieTypeRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;

/**
 * Админка: список, добавление, изменение и удаление фильмов.
 * После успешного сохранения — редирект на список (шаблон Post/Redirect/Get),
 * чтобы обновление страницы не отправляло форму повторно.
 */
@Controller
@RequestMapping("/admin/movies")
public class AdminMovieController {

    private final MovieRepository movieRepository;
    private final MovieTypeRepository typeRepository;
    private final GenreRepository genreRepository;
    private final MoodRepository moodRepository;

    public AdminMovieController(MovieRepository movieRepository,
                                MovieTypeRepository typeRepository,
                                GenreRepository genreRepository,
                                MoodRepository moodRepository) {
        this.movieRepository = movieRepository;
        this.typeRepository = typeRepository;
        this.genreRepository = genreRepository;
        this.moodRepository = moodRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("movies", movieRepository.findAllByOrderByTitleAsc());
        return "admin/movies";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        return showForm(new MovieForm(), model);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("movieForm") MovieForm form, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            return showForm(form, model);
        }
        Movie movie = new Movie();
        copyFormToMovie(form, movie);
        movieRepository.save(movie);
        redirect.addFlashAttribute("message", "Фильм «" + movie.getTitle() + "» добавлен");
        return "redirect:/admin/movies";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return showForm(MovieForm.fromMovie(findMovie(id)), model);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("movieForm") MovieForm form,
                         BindingResult errors, Model model, RedirectAttributes redirect) {
        form.setId(id);
        if (errors.hasErrors()) {
            return showForm(form, model);
        }
        Movie movie = findMovie(id);
        copyFormToMovie(form, movie);
        movieRepository.save(movie);
        redirect.addFlashAttribute("message", "Фильм «" + movie.getTitle() + "» сохранён");
        return "redirect:/admin/movies";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        Movie movie = findMovie(id);
        movieRepository.delete(movie);
        redirect.addFlashAttribute("message", "Фильм «" + movie.getTitle() + "» удалён");
        return "redirect:/admin/movies";
    }

    /** Показать форму фильма вместе со списками для выбора типа, жанров и настроений. */
    private String showForm(MovieForm form, Model model) {
        model.addAttribute("movieForm", form);
        model.addAttribute("types", typeRepository.findAll(Sort.by("id")));
        model.addAttribute("genres", genreRepository.findAll(Sort.by("name")));
        model.addAttribute("moods", moodRepository.findAll(Sort.by("id")));
        return "admin/movie-form";
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

    private Movie findMovie(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден"));
    }
}
