package com.example.cinerec.controller.admin;

import com.example.cinerec.entity.Mood;
import com.example.cinerec.repository.MoodRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Админка: настроения. Добавление, переименование и удаление.
 * При удалении настроения его связи с фильмами удаляет сама база (ON DELETE CASCADE).
 */
@Controller
@RequestMapping("/admin/moods")
public class AdminMoodController {

    private final MoodRepository moodRepository;

    public AdminMoodController(MoodRepository moodRepository) {
        this.moodRepository = moodRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", moodRepository.findAll(Sort.by("id")));
        model.addAttribute("section", "moods");
        model.addAttribute("pageTitle", "Настроения");
        model.addAttribute("baseUrl", "/admin/moods");
        return "admin/dictionary";
    }

    @PostMapping
    public String create(@RequestParam String name, RedirectAttributes redirect) {
        String error = DictionaryNames.check(name);
        if (error == null && moodRepository.existsByNameIgnoreCase(name.trim())) {
            error = "Настроение «" + name.trim() + "» уже есть";
        }
        if (error != null) {
            redirect.addFlashAttribute("error", error);
        } else {
            Mood mood = new Mood();
            mood.setName(name.trim());
            moodRepository.save(mood);
            redirect.addFlashAttribute("message", "Настроение «" + mood.getName() + "» добавлено");
        }
        return "redirect:/admin/moods";
    }

    @PostMapping("/{id}")
    public String rename(@PathVariable Long id, @RequestParam String name, RedirectAttributes redirect) {
        String error = DictionaryNames.check(name);
        if (error == null && moodRepository.existsByNameIgnoreCaseAndIdNot(name.trim(), id)) {
            error = "Настроение «" + name.trim() + "» уже есть";
        }
        if (error != null) {
            redirect.addFlashAttribute("error", error);
        } else {
            moodRepository.findById(id).ifPresent(mood -> {
                mood.setName(name.trim());
                moodRepository.save(mood);
                redirect.addFlashAttribute("message", "Настроение переименовано в «" + mood.getName() + "»");
            });
        }
        return "redirect:/admin/moods";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        moodRepository.findById(id).ifPresent(mood -> {
            moodRepository.delete(mood);
            redirect.addFlashAttribute("message", "Настроение «" + mood.getName() + "» удалено");
        });
        return "redirect:/admin/moods";
    }
}
