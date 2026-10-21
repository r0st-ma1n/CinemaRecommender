package com.example.cinerec.controller.admin;

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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Админка фильмов на уровне HTTP: проверка формы, сохранение и редирект, несуществующий фильм.
 * Репозитории подменены заглушками, поэтому база не нужна.
 */
@WebMvcTest(AdminMovieController.class)
@Import({MovieService.class, SecurityConfig.class})
@WithMockUser(roles = "ADMIN")
class AdminMovieControllerTest {

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
    void invalidFormIsShownAgainWithErrorsAndNothingIsSaved() throws Exception {
        mvc.perform(post("/admin/movies").with(csrf())
                        .param("title", " ")
                        .param("year", "1800")
                        .param("rating", "11"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/movie-form"))
                .andExpect(model().attributeHasFieldErrors("movieForm",
                        "title", "year", "description", "rating", "typeId", "genreIds", "moodIds"));

        verify(movieRepository, never()).save(any());
    }

    @Test
    void validFormSavesMovieAndRedirectsToList() throws Exception {
        MovieType film = new MovieType();
        film.setId(1L);
        Genre comedy = new Genre();
        comedy.setId(2L);
        Mood funny = new Mood();
        funny.setId(3L);
        when(typeRepository.findById(1L)).thenReturn(Optional.of(film));
        when(genreRepository.findAllById(List.of(2L))).thenReturn(List.of(comedy));
        when(moodRepository.findAllById(List.of(3L))).thenReturn(List.of(funny));

        mvc.perform(post("/admin/movies").with(csrf())
                        .param("title", "  Новый фильм  ")
                        .param("year", "2021")
                        .param("description", "Описание")
                        .param("posterUrl", "   ")
                        .param("rating", "7.5")
                        .param("typeId", "1")
                        .param("genreIds", "2")
                        .param("moodIds", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/movies"))
                .andExpect(flash().attribute("message", "Фильм «Новый фильм» добавлен"));

        ArgumentCaptor<Movie> saved = ArgumentCaptor.forClass(Movie.class);
        verify(movieRepository).save(saved.capture());
        Movie movie = saved.getValue();
        assertEquals("Новый фильм", movie.getTitle()); // пробелы по краям обрезаны
        assertNull(movie.getPosterUrl());              // пустая ссылка на постер — NULL, покажется заглушка
        assertEquals(film, movie.getType());
        assertEquals(1, movie.getGenres().size());
        assertEquals(1, movie.getMoods().size());
    }

    @Test
    void editingUnknownMovieGives404() throws Exception {
        when(movieRepository.findById(999L)).thenReturn(Optional.empty());

        mvc.perform(get("/admin/movies/999/edit"))
                .andExpect(status().isNotFound());
    }

    @Test
    void moodSuggestionNeedsDescription() throws Exception {
        mvc.perform(post("/admin/movies/suggest-moods").with(csrf()).param("title", "Фильм"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Чтобы получить подсказку, сначала заполните описание"));

        verify(moodPredictor, never()).suggest(any(), any());
    }
}
