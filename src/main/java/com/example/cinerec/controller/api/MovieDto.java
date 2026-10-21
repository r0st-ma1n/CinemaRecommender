package com.example.cinerec.controller.api;

import com.example.cinerec.entity.Movie;

import java.util.List;

/**
 * Фильм в ответе API. Сущность наружу не отдаётся: так ответ не зависит от устройства таблиц
 * и не тянет за собой ленивые связи Hibernate.
 */
public record MovieDto(Long id, String title, Integer year, String description, String posterUrl, Double rating,
                       DictionaryItemDto type, List<DictionaryItemDto> genres, List<DictionaryItemDto> moods) {

    public static MovieDto from(Movie movie) {
        return new MovieDto(movie.getId(), movie.getTitle(), movie.getYear(), movie.getDescription(),
                movie.getPosterUrl(), movie.getRating(),
                DictionaryItemDto.from(movie.getType()),
                movie.getGenres().stream().map(DictionaryItemDto::from).toList(),
                movie.getMoods().stream().map(DictionaryItemDto::from).toList());
    }
}
