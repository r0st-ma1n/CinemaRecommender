package com.example.cinerec.chat;

import java.util.List;
import java.util.Set;

/**
 * Что бот помнит о прошлом запросе: условия подбора и уже показанные фильмы.
 * Хранится на странице чата: сервер отдаёт его в ответе, а скрипт присылает со следующим сообщением.
 * Поэтому на «ещё» бот показывает следующие фильмы, а на «а сериал» меняет только тип.
 */
public record ChatContext(Long moodId, Set<Long> genreIds, Set<Long> typeIds,
                          Set<Long> excludedGenreIds, Set<Long> excludedTypeIds,
                          List<Long> shownMovieIds) {

    public static final ChatContext EMPTY =
            new ChatContext(null, Set.of(), Set.of(), Set.of(), Set.of(), List.of());

    /** Контекст приходит со страницы, поэтому пропущенные списки заменяются пустыми. */
    public ChatContext {
        genreIds = genreIds == null ? Set.of() : Set.copyOf(genreIds);
        typeIds = typeIds == null ? Set.of() : Set.copyOf(typeIds);
        excludedGenreIds = excludedGenreIds == null ? Set.of() : Set.copyOf(excludedGenreIds);
        excludedTypeIds = excludedTypeIds == null ? Set.of() : Set.copyOf(excludedTypeIds);
        shownMovieIds = shownMovieIds == null ? List.of() : List.copyOf(shownMovieIds);
    }
}
