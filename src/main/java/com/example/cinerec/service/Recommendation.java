package com.example.cinerec.service;

import com.example.cinerec.entity.Movie;

/**
 * Один фильм в результатах подбора: сам фильм, его баллы
 * и что именно совпало (чтобы показать пользователю, почему он предложен).
 */
public record Recommendation(Movie movie, int score,
                             boolean moodMatched, boolean genreMatched, boolean typeMatched) {
}
