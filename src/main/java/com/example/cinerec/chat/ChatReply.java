package com.example.cinerec.chat;

import java.util.List;

/**
 * Ответ бота: текст (строки разделены переводом строки), подобранные фильмы
 * и новый контекст для следующего сообщения (null — страница оставляет прежний).
 */
public record ChatReply(String text, List<ChatMovie> movies, ChatContext context) {

    /** Ответ без фильмов: приветствие, «пожалуйста», просьба переформулировать. */
    public static ChatReply textOnly(String text) {
        return new ChatReply(text, List.of(), null);
    }
}
