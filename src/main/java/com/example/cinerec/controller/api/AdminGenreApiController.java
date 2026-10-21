package com.example.cinerec.controller.api;

import com.example.cinerec.entity.Genre;
import com.example.cinerec.repository.GenreRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/genres")
public class AdminGenreApiController extends DictionaryApiController<Genre> {

    public AdminGenreApiController(GenreRepository genreRepository) {
        super(genreRepository, Genre::new, "Жанр");
    }
}
