package com.example.cinerec.controller.admin;

import com.example.cinerec.entity.Genre;
import com.example.cinerec.repository.GenreRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Админка: жанры. Добавление, переименование и удаление.
 * При удалении жанра его связи с фильмами удаляет сама база (ON DELETE CASCADE).
 */
@Controller
@RequestMapping("/admin/genres")
public class AdminGenreController {

    private final GenreRepository genreRepository;

    public AdminGenreController(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", genreRepository.findAll(Sort.by("name")));
        model.addAttribute("section", "genres");
        model.addAttribute("pageTitle", "Жанры");
        model.addAttribute("baseUrl", "/admin/genres");
        return "admin/dictionary";
    }

    @PostMapping
    public String create(@RequestParam String name, RedirectAttributes redirect) {
        String error = DictionaryNames.check(name);
        if (error == null && genreRepository.existsByNameIgnoreCase(name.trim())) {
            error = "Жанр «" + name.trim() + "» уже есть";
        }
        if (error != null) {
            redirect.addFlashAttribute("error", error);
        } else {
            Genre genre = new Genre();
            genre.setName(name.trim());
            genreRepository.save(genre);
            redirect.addFlashAttribute("message", "Жанр «" + genre.getName() + "» добавлен");
        }
        return "redirect:/admin/genres";
    }

    @PostMapping("/{id}")
    public String rename(@PathVariable Long id, @RequestParam String name, RedirectAttributes redirect) {
        String error = DictionaryNames.check(name);
        if (error == null && genreRepository.existsByNameIgnoreCaseAndIdNot(name.trim(), id)) {
            error = "Жанр «" + name.trim() + "» уже есть";
        }
        if (error != null) {
            redirect.addFlashAttribute("error", error);
        } else {
            genreRepository.findById(id).ifPresent(genre -> {
                genre.setName(name.trim());
                genreRepository.save(genre);
                redirect.addFlashAttribute("message", "Жанр переименован в «" + genre.getName() + "»");
            });
        }
        return "redirect:/admin/genres";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        genreRepository.findById(id).ifPresent(genre -> {
            genreRepository.delete(genre);
            redirect.addFlashAttribute("message", "Жанр «" + genre.getName() + "» удалён");
        });
        return "redirect:/admin/genres";
    }
}
