package com.example.cinerec.controller.admin;

import com.example.cinerec.repository.GenreRepository;
import com.example.cinerec.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Справочник жанров: пустые и повторяющиеся названия не сохраняются. */
@WebMvcTest(AdminGenreController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class AdminGenreControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GenreRepository genreRepository;

    @Test
    void emptyNameIsRejected() throws Exception {
        mvc.perform(post("/admin/genres").with(csrf()).param("name", "   "))
                .andExpect(redirectedUrl("/admin/genres"))
                .andExpect(flash().attribute("error", "Название не может быть пустым"));

        verify(genreRepository, never()).save(any());
    }

    @Test
    void duplicateNameIsRejectedIgnoringCase() throws Exception {
        when(genreRepository.existsByNameIgnoreCase("Комедия")).thenReturn(true);

        mvc.perform(post("/admin/genres").with(csrf()).param("name", " Комедия "))
                .andExpect(redirectedUrl("/admin/genres"))
                .andExpect(flash().attribute("error", "Жанр «Комедия» уже есть"));

        verify(genreRepository, never()).save(any());
    }

    @Test
    void newNameIsSavedTrimmed() throws Exception {
        mvc.perform(post("/admin/genres").with(csrf()).param("name", " вестерн "))
                .andExpect(redirectedUrl("/admin/genres"))
                .andExpect(flash().attribute("message", "Жанр «вестерн» добавлен"));

        verify(genreRepository).save(argThat(genre -> genre.getName().equals("вестерн")));
    }
}
