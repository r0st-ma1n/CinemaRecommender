package com.example.cinerec.controller.api;

import com.example.cinerec.service.Recommendation;

/** Фильм в результатах подбора: баллы и что совпало. */
public record RecommendationDto(MovieDto movie, int score,
                                boolean moodMatched, boolean genreMatched, boolean typeMatched) {

    public static RecommendationDto from(Recommendation r) {
        return new RecommendationDto(MovieDto.from(r.movie()), r.score(),
                r.moodMatched(), r.genreMatched(), r.typeMatched());
    }
}
