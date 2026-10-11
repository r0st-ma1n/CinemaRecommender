package com.example.cinerec.ml;

/** Подсказка модели: настроение и его вероятность (от 0 до 1). */
public record MoodSuggestion(String mood, double probability) {

    /** Вероятность в процентах, для вывода на странице. */
    public long percent() {
        return Math.round(probability * 100);
    }
}
