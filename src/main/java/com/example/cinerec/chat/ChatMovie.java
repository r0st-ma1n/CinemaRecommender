package com.example.cinerec.chat;

import com.example.cinerec.entity.Movie;

/**
 * Фильм в ответе бота — только то, что нужно для карточки на странице чата.
 * Отдельный класс вместо сущности Movie, чтобы в JSON не попадали связи с жанрами и настроениями.
 */
public record ChatMovie(Long id, String title, Integer year, String type, Double rating, String posterUrl) {

    public static ChatMovie from(Movie movie) {
        return new ChatMovie(movie.getId(), movie.getTitle(), movie.getYear(),
                movie.getType().getName(), movie.getRating(), movie.getPosterUrl());
    }
}
