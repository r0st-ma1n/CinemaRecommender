package com.example.cinerec.repository;

import com.example.cinerec.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    // Для проверки, что такое название ещё не занято
    boolean existsByNameIgnoreCase(String name);

    // То же при переименовании: ищем совпадение среди всех, кроме самой записи
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
