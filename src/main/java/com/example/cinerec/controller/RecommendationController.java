package com.example.cinerec.controller;

import com.example.cinerec.repository.GenreRepository;
import com.example.cinerec.repository.MoodRepository;
import com.example.cinerec.repository.MovieTypeRepository;
import com.example.cinerec.service.RecommendationService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Форма подбора. Параметры передаются в адресе (GET),
 * поэтому страницу с результатом можно обновить или отправить ссылкой.
 */
@Controller
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final MoodRepository moodRepository;
    private final GenreRepository genreRepository;
    private final MovieTypeRepository typeRepository;

    public RecommendationController(RecommendationService recommendationService,
                                    MoodRepository moodRepository,
                                    GenreRepository genreRepository,
                                    MovieTypeRepository typeRepository) {
        this.recommendationService = recommendationService;
        this.moodRepository = moodRepository;
        this.genreRepository = genreRepository;
        this.typeRepository = typeRepository;
    }

    @GetMapping("/recommend")
    public String recommend(@RequestParam(required = false) Long moodId,
                            @RequestParam(required = false) Long genreId,
                            @RequestParam(required = false) Long typeId,
                            @RequestParam(required = false) String submitted,
                            Model model) {
        // Списки для выпадающих меню
        model.addAttribute("moods", moodRepository.findAll(Sort.by("id")));
        model.addAttribute("genres", genreRepository.findAll(Sort.by("name")));
        model.addAttribute("types", typeRepository.findAll(Sort.by("id")));

        // Запомнить выбор, чтобы форма осталась заполненной
        model.addAttribute("moodId", moodId);
        model.addAttribute("genreId", genreId);
        model.addAttribute("typeId", typeId);

        if (submitted != null) {
            if (moodId == null && genreId == null && typeId == null) {
                model.addAttribute("error", "Выберите хотя бы что-нибудь: настроение, жанр или тип.");
            } else {
                model.addAttribute("results", recommendationService.recommend(moodId, genreId, typeId));
            }
        }
        return "recommend";
    }
}
