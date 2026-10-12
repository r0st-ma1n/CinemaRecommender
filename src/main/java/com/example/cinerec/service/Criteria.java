package com.example.cinerec.service;

import java.util.Set;

/**
 * Условия подбора.
 * Настроение, жанры и типы дают баллы (жанров и типов может быть несколько — подходит любой из них).
 * Исключённые жанры и типы, а также годы — фильтры: такие фильмы не предлагаются совсем.
 *
 * @param yearFrom первый подходящий год или null; yearTo — последний подходящий год или null
 */
public record Criteria(Long moodId, Set<Long> genreIds, Set<Long> typeIds,
                       Set<Long> excludedGenreIds, Set<Long> excludedTypeIds,
                       Integer yearFrom, Integer yearTo) {

    public static final Criteria ANY = new Criteria(null, Set.of(), Set.of(), Set.of(), Set.of(), null, null);

    /** Условия приходят и со страницы чата, поэтому пропущенные наборы заменяются пустыми. */
    public Criteria {
        genreIds = genreIds == null ? Set.of() : Set.copyOf(genreIds);
        typeIds = typeIds == null ? Set.of() : Set.copyOf(typeIds);
        excludedGenreIds = excludedGenreIds == null ? Set.of() : Set.copyOf(excludedGenreIds);
        excludedTypeIds = excludedTypeIds == null ? Set.of() : Set.copyOf(excludedTypeIds);
    }

    /** Есть ли условия, за которые даются баллы. */
    public boolean hasWishes() {
        return moodId != null || !genreIds.isEmpty() || !typeIds.isEmpty();
    }

    public boolean hasYears() {
        return yearFrom != null || yearTo != null;
    }

    public boolean yearFits(int year) {
        return (yearFrom == null || year >= yearFrom) && (yearTo == null || year <= yearTo);
    }
}
