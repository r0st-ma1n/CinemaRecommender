package com.example.cinerec.chat;

import com.example.cinerec.service.Criteria;

import java.util.List;

/**
 * Что бот помнит о прошлом запросе: условия подбора и уже показанные фильмы.
 * Хранится на странице чата: сервер отдаёт его в ответе, а скрипт присылает со следующим сообщением.
 * Поэтому на «ещё» бот показывает следующие фильмы, а на «а сериал» меняет только тип.
 */
public record ChatContext(Criteria criteria, List<Long> shownMovieIds) {

    public static final ChatContext EMPTY = new ChatContext(Criteria.ANY, List.of());

    /** Контекст приходит со страницы, поэтому пропущенные части заменяются пустыми. */
    public ChatContext {
        criteria = criteria == null ? Criteria.ANY : criteria;
        shownMovieIds = shownMovieIds == null ? List.of() : List.copyOf(shownMovieIds);
    }
}
