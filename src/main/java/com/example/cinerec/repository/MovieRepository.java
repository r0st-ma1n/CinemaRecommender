package com.example.cinerec.repository;

import com.example.cinerec.entity.Movie;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    // Все фильмы для каталога: сначала с высоким рейтингом.
    // @EntityGraph загружает тип и жанры одним запросом,
    // иначе для каждой карточки был бы отдельный запрос к базе.
    @EntityGraph(attributePaths = {"type", "genres"})
    List<Movie> findAllByOrderByRatingDescTitleAsc();
}
