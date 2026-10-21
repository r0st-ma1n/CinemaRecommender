package com.example.cinerec.controller.api;

import com.example.cinerec.entity.Genre;
import com.example.cinerec.entity.Mood;
import com.example.cinerec.entity.Movie;
import com.example.cinerec.entity.MovieType;
import com.example.cinerec.ml.MoodPredictor;
import com.example.cinerec.repository.GenreRepository;
import com.example.cinerec.repository.MoodRepository;
import com.example.cinerec.repository.MovieRepository;
import com.example.cinerec.repository.MovieTypeRepository;
import com.example.cinerec.security.SecurityConfig;
import com.example.cinerec.service.MovieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Админка фильмов через API: валидация, сохранение, удаление, подсказка настроений. */
@WebMvcTest(AdminMovieApiController.class)
@Import({MovieService.class, SecurityConfig.class})
@WithMockUser(roles = "ADMIN")
class AdminMovieApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MovieRepository movieRepository;
    @MockitoBean
    private MovieTypeRepository typeRepository;
    @MockitoBean
    private GenreRepository genreRepository;
    @MockitoBean
    private MoodRepository moodRepository;
    @MockitoBean
    private MoodPredictor moodPredictor;

    @Test
    void invalidMovieGives400WithMessageAndNothingIsSaved() throws Exception {
        mvc.perform(post("/api/admin/movies").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"title": "Фильм", "year": 2020, "description": "Описание", "rating": 7,
                         "typeId": 1, "genreIds": [], "moodIds": [3]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Выберите хотя бы один жанр"));

        verify(movieRepository, never()).save(any());
    }

    @Test
    void validMovieIsSavedAndReturnedWith201() throws Exception {
        MovieType film = new MovieType();
        film.setId(1L);
        film.setName("фильм");
        Genre comedy = new Genre();
        comedy.setId(2L);
        comedy.setName("комедия");
        Mood funny = new Mood();
        funny.setId(3L);
        funny.setName("весёлое");
        when(typeRepository.findById(1L)).thenReturn(Optional.of(film));
        when(genreRepository.findAllById(List.of(2L))).thenReturn(List.of(comedy));
        when(moodRepository.findAllById(List.of(3L))).thenReturn(List.of(funny));
        when(movieRepository.save(any())).thenAnswer(call -> {
            Movie movie = call.getArgument(0);
            movie.setId(100L);
            return movie;
        });

        mvc.perform(post("/api/admin/movies").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"title": "  Новый фильм  ", "year": 2021, "description": "Описание", "posterUrl": "",
                         "rating": 7.5, "typeId": 1, "genreIds": [2], "moodIds": [3]}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.title").value("Новый фильм"))
                .andExpect(jsonPath("$.posterUrl").isEmpty())
                .andExpect(jsonPath("$.genres[0].name").value("комедия"));
    }

    @Test
    void deletingUnknownMovieGives404() throws Exception {
        when(movieRepository.findById(999L)).thenReturn(Optional.empty());

        mvc.perform(delete("/api/admin/movies/999").with(csrf()))
                .andExpect(status().isNotFound());

        verify(movieRepository, never()).delete(any());
    }

    @Test
    void moodSuggestionNeedsDescription() throws Exception {
        mvc.perform(post("/api/admin/movies/suggest-moods").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \" \", \"genreIds\": [2]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Чтобы получить подсказку, сначала заполните описание"));

        verify(moodPredictor, never()).suggest(any(), any());
    }
}
