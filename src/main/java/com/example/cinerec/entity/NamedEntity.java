package com.example.cinerec.entity;

/** Запись справочника с id и названием: тип, жанр, настроение. */
public interface NamedEntity {

    Long getId();

    String getName();

    void setName(String name);
}
