package com.example.cinerec.service;

import com.example.cinerec.entity.Movie;
import com.example.cinerec.repository.MovieRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Подбор фильмов по баллам:
 * совпало настроение +3, жанр +2, тип +1.
 * Возвращаются первые 10 фильмов с баллом больше нуля;
 * при равных баллах выше фильм с более высоким рейтингом.
 * Фильмы исключённых жанров и типов не предлагаются совсем (чат: «только не ужасы»).
 */
@Service
public class RecommendationService {

    public static final int MOOD_POINTS = 3;
    public static final int GENRE_POINTS = 2;
    public static final int TYPE_POINTS = 1;
    public static final int LIMIT = 10;

    private final MovieRepository movieRepository;

    public RecommendationService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    /**
     * @param moodId  выбранное настроение или null, если «любое»
     * @param genreId выбранный жанр или null
     * @param typeId  выбранный тип или null
     */
    public List<Recommendation> recommend(Long moodId, Long genreId, Long typeId) {
        return rank(moodId, idSet(genreId), idSet(typeId), Set.of(), Set.of()).stream().limit(LIMIT).toList();
    }

    /**
     * Все фильмы с баллом больше нуля в порядке подбора, без ограничения LIMIT —
     * чат показывает их по частям («ещё»).
     * Жанров и типов может быть несколько («комедию или мелодраму»): баллы за жанр даются,
     * если у фильма есть любой из них, за тип — если фильм любого из типов.
     *
     * @param genreIds         нужные жанры (пустой набор — любой)
     * @param typeIds          нужные типы (пустой набор — любой)
     * @param excludedGenreIds жанры, фильмы которых не нужно предлагать
     * @param excludedTypeIds  типы, которые не нужно предлагать
     */
    public List<Recommendation> rank(Long moodId, Set<Long> genreIds, Set<Long> typeIds,
                                     Set<Long> excludedGenreIds, Set<Long> excludedTypeIds) {
        List<Recommendation> result = new ArrayList<>();

        for (Movie movie : movieRepository.findAllWithDetails()) {
            if (excludedTypeIds.contains(movie.getType().getId())
                    || movie.getGenres().stream().anyMatch(g -> excludedGenreIds.contains(g.getId()))) {
                continue;
            }
            boolean moodMatched = moodId != null
                    && movie.getMoods().stream().anyMatch(m -> m.getId().equals(moodId));
            boolean genreMatched = movie.getGenres().stream().anyMatch(g -> genreIds.contains(g.getId()));
            boolean typeMatched = typeIds.contains(movie.getType().getId());

            int score = 0;
            if (moodMatched) score += MOOD_POINTS;
            if (genreMatched) score += GENRE_POINTS;
            if (typeMatched) score += TYPE_POINTS;

            if (score > 0) {
                result.add(new Recommendation(movie, score, moodMatched, genreMatched, typeMatched));
            }
        }

        // Сначала по баллам (по убыванию), при равенстве — по рейтингу (по убыванию),
        // а если совпал и рейтинг — по названию, чтобы порядок всегда был одинаковым
        result.sort(Comparator.comparingInt(Recommendation::score).reversed()
                .thenComparing(r -> r.movie().getRating(), Comparator.reverseOrder())
                .thenComparing(r -> r.movie().getTitle()));

        return result;
    }

    /** Один выбранный id из формы подбора как набор; null («любой») — пустой набор. */
    private static Set<Long> idSet(Long id) {
        return id == null ? Set.of() : Set.of(id);
    }
}
