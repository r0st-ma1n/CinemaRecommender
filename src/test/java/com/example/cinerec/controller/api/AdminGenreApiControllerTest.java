package com.example.cinerec.controller.api;

import com.example.cinerec.entity.Genre;
import com.example.cinerec.repository.GenreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Справочник через API на примере жанров (настроения работают так же — общий DictionaryApiController). */
@WebMvcTest(AdminGenreApiController.class)
class AdminGenreApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GenreRepository genreRepository;

    @Test
    void emptyNameIsRejected() throws Exception {
        mvc.perform(post("/api/admin/genres").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Название не может быть пустым"));

        verify(genreRepository, never()).save(any());
    }

    @Test
    void duplicateNameGives409() throws Exception {
        when(genreRepository.existsByNameIgnoreCase("Комедия")).thenReturn(true);

        mvc.perform(post("/api/admin/genres").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \" Комедия \"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Жанр «Комедия» уже есть"));

        verify(genreRepository, never()).save(any());
    }

    @Test
    void newNameIsSavedTrimmed() throws Exception {
        when(genreRepository.save(any())).thenAnswer(call -> {
            Genre genre = call.getArgument(0);
            genre.setId(20L);
            return genre;
        });

        mvc.perform(post("/api/admin/genres").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \" вестерн \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.name").value("вестерн"));
    }

    @Test
    void renamingUnknownGenreGives404() throws Exception {
        when(genreRepository.findById(999L)).thenReturn(Optional.empty());

        mvc.perform(put("/api/admin/genres/999").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"вестерн\"}"))
                .andExpect(status().isNotFound());
    }
}
