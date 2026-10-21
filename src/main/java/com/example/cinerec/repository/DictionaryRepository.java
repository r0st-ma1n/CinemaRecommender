package com.example.cinerec.repository;

import com.example.cinerec.entity.NamedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/** Общие методы для справочников, которые можно править в админке (жанры и настроения). */
@NoRepositoryBean
public interface DictionaryRepository<T extends NamedEntity> extends JpaRepository<T, Long> {

    // Для проверки, что такое название ещё не занято
    boolean existsByNameIgnoreCase(String name);

    // То же при переименовании: ищем совпадение среди всех, кроме самой записи
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
