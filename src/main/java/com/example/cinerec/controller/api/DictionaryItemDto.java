package com.example.cinerec.controller.api;

import com.example.cinerec.entity.NamedEntity;

/** Тип, жанр или настроение в ответе API. */
public record DictionaryItemDto(Long id, String name) {

    public static DictionaryItemDto from(NamedEntity item) {
        return new DictionaryItemDto(item.getId(), item.getName());
    }
}
