package com.example.cinerec.controller;

import com.example.cinerec.repository.GenreRepository;
import com.example.cinerec.repository.MoodRepository;
import com.example.cinerec.repository.MovieTypeRepository;
import com.example.cinerec.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Форма подбора: без выбора — подсказка, с выбором — результаты, выбор остаётся в форме. */
@WebMvcTest(RecommendationController.class)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RecommendationService recommendationService;
    @MockitoBean
    private MoodRepository moodRepository;
    @MockitoBean
    private GenreRepository genreRepository;
    @MockitoBean
    private MovieTypeRepository typeRepository;

    @Test
    void openingFormDoesNotRecommend() throws Exception {
        mvc.perform(get("/recommend"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("results", "error"));

        verifyNoInteractions(recommendationService);
    }

    @Test
    void submittingEmptyFormShowsError() throws Exception {
        mvc.perform(get("/recommend").param("submitted", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Выберите хотя бы что-нибудь: настроение, жанр или тип."));

        verify(recommendationService, never()).recommend(any(), any(), any());
    }

    @Test
    void submittedChoiceIsRecommendedAndKeptInForm() throws Exception {
        when(recommendationService.recommend(1L, null, 3L)).thenReturn(List.of());

        mvc.perform(get("/recommend").param("moodId", "1").param("typeId", "3").param("submitted", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("results", List.of()))
                .andExpect(model().attribute("moodId", 1L))
                .andExpect(model().attribute("typeId", 3L));

        verify(recommendationService).recommend(1L, null, 3L);
    }
}
