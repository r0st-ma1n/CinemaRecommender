package com.example.cinerec.repository;

import com.example.cinerec.entity.Movie;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    // Все фильмы для каталога: сначала с высоким рейтингом.
    // @EntityGraph загружает тип и жанры одним запросом,
    // иначе для каждой карточки был бы отдельный запрос к базе.
    @EntityGraph(attributePaths = {"type", "genres"})
    List<Movie> findAllByOrderByRatingDescTitleAsc();

    // Список фильмов в админке — по алфавиту
    @EntityGraph(attributePaths = {"type"})
    List<Movie> findAllByOrderByTitleAsc();

    // Все фильмы вместе с типом, жанрами и настроениями — для подсчёта баллов при подборе
    @EntityGraph(attributePaths = {"type", "genres", "moods"})
    @Query("select m from Movie m")
    List<Movie> findAllWithDetails();
}
