package com.example.cinerec.controller.admin;

/** Общая проверка названия жанра или настроения. */
public final class DictionaryNames {

    public static final int MAX_LENGTH = 50;

    private DictionaryNames() {
    }

    /** @return текст ошибки или null, если название подходит */
    public static String check(String name) {
        if (name == null || name.isBlank()) {
            return "Название не может быть пустым";
        }
        if (name.trim().length() > MAX_LENGTH) {
            return "Название не длиннее " + MAX_LENGTH + " символов";
        }
        return null;
    }
}
