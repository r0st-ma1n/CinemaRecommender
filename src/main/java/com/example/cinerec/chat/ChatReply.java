package com.example.cinerec.chat;

import java.util.List;

/** Ответ бота: текст (строки разделены переводом строки) и подобранные фильмы. */
public record ChatReply(String text, List<ChatMovie> movies) {

    /** Ответ без фильмов: приветствие, «пожалуйста», просьба переформулировать. */
    public static ChatReply textOnly(String text) {
        return new ChatReply(text, List.of());
    }
}
