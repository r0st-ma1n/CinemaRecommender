package com.example.cinerec.controller.admin;

import com.example.cinerec.entity.Genre;
import com.example.cinerec.entity.Mood;
import com.example.cinerec.entity.Movie;
import com.example.cinerec.form.MovieForm;
import com.example.cinerec.ml.MoodPredictor;
import com.example.cinerec.ml.MoodSuggestion;
import com.example.cinerec.repository.GenreRepository;
import com.example.cinerec.repository.MoodRepository;
import com.example.cinerec.repository.MovieRepository;
import com.example.cinerec.repository.MovieTypeRepository;
import com.example.cinerec.service.MovieService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Админка: список, добавление, изменение и удаление фильмов.
 * После успешного сохранения — редирект на список (шаблон Post/Redirect/Get),
 * чтобы обновление страницы не отправляло форму повторно.
 */
@Controller
@RequestMapping("/admin/movies")
public class AdminMovieController {

    private final MovieService movieService;
    private final MovieRepository movieRepository;
    private final MovieTypeRepository typeRepository;
    private final GenreRepository genreRepository;
    private final MoodRepository moodRepository;
    private final MoodPredictor moodPredictor;

    public AdminMovieController(MovieService movieService,
                                MovieRepository movieRepository,
                                MovieTypeRepository typeRepository,
                                GenreRepository genreRepository,
                                MoodRepository moodRepository,
                                MoodPredictor moodPredictor) {
        this.movieService = movieService;
        this.movieRepository = movieRepository;
        this.typeRepository = typeRepository;
        this.genreRepository = genreRepository;
        this.moodRepository = moodRepository;
        this.moodPredictor = moodPredictor;
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
        Movie movie = movieService.create(form);
        redirect.addFlashAttribute("message", "Фильм «" + movie.getTitle() + "» добавлен");
        return "redirect:/admin/movies";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return showForm(MovieForm.fromMovie(movieService.find(id)), model);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("movieForm") MovieForm form,
                         BindingResult errors, Model model, RedirectAttributes redirect) {
        form.setId(id);
        if (errors.hasErrors()) {
            return showForm(form, model);
        }
        Movie movie = movieService.update(id, form);
        redirect.addFlashAttribute("message", "Фильм «" + movie.getTitle() + "» сохранён");
        return "redirect:/admin/movies";
    }

    /**
     * Кнопка «Подсказать настроение»: модель по описанию и жанрам из формы
     * предлагает одно-два настроения и сразу отмечает их галочками.
     * Фильм при этом НЕ сохраняется — админ проверяет, при необходимости
     * исправляет отметки и только потом нажимает «Сохранить».
     */
    @PostMapping("/suggest-moods")
    public String suggestMoods(@ModelAttribute("movieForm") MovieForm form, Model model) {
        if (form.getDescription() == null || form.getDescription().isBlank()) {
            model.addAttribute("error", "Чтобы получить подсказку, сначала заполните описание");
            return showForm(form, model);
        }

        List<String> genreNames = genreRepository.findAllById(form.getGenreIds()).stream()
                .map(Genre::getName).toList();
        List<MoodSuggestion> suggestions = moodPredictor.suggest(form.getDescription(), genreNames);

        // Отметить предложенные настроения: находим их в базе по названию.
        // Настроение могли переименовать или удалить в админке — такие запоминаем отдельно.
        List<Mood> allMoods = moodRepository.findAll();
        List<Long> foundIds = new ArrayList<>();
        List<String> missingMoods = new ArrayList<>();
        for (MoodSuggestion suggestion : suggestions) {
            allMoods.stream()
                    .filter(mood -> mood.getName().equalsIgnoreCase(suggestion.mood()))
                    .findFirst()
                    .ifPresentOrElse(mood -> foundIds.add(mood.getId()),
                            () -> missingMoods.add(suggestion.mood()));
        }
        form.setMoodIds(foundIds);

        model.addAttribute("suggestions", suggestions);
        model.addAttribute("missingMoods", missingMoods);
        model.addAttribute("checkedCount", foundIds.size());
        return showForm(form, model);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        Movie movie = movieService.delete(id);
        redirect.addFlashAttribute("message", "Фильм «" + movie.getTitle() + "» удалён");
        return "redirect:/admin/movies";
    }

    /** Показать форму фильма вместе со списками для выбора типа, жанров и настроений. */
    private String showForm(MovieForm form, Model model) {
        model.addAttribute("movieForm", form);
        model.addAttribute("types", typeRepository.findAll(Sort.by("id")));
        model.addAttribute("genres", genreRepository.findAll(Sort.by("name")));
        model.addAttribute("moods", moodRepository.findAll(Sort.by("id")));
        model.addAttribute("modelAvailable", moodPredictor.isAvailable());
        return "admin/movie-form";
    }
}
