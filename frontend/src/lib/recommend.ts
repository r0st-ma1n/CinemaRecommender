import type { Criteria, Movie, Recommendation } from '../types';

export const MOOD_POINTS = 3;
export const GENRE_POINTS = 2;
export const TYPE_POINTS = 1;
export const MAX_RESULTS = 10;

/**
 * Те же правила, что в RecommendationService на бэкенде:
 * настроение +3, жанр +2, тип +1; исключения и годы — фильтры;
 * первые 10 фильмов с баллом > 0, при равных баллах выше рейтинг.
 */
export function recommend(movies: Movie[], c: Criteria, exclude: number[] = [], limit = MAX_RESULTS): Recommendation[] {
  return movies
    .filter((m) => !exclude.includes(m.id))
    .filter((m) => !c.excludedTypeIds.includes(m.type.id))
    .filter((m) => !m.genres.some((g) => c.excludedGenreIds.includes(g.id)))
    .filter((m) => (c.yearFrom == null || m.year >= c.yearFrom) && (c.yearTo == null || m.year <= c.yearTo))
    .map((movie) => {
      const moodMatched = c.moodId != null && movie.moods.some((md) => md.id === c.moodId);
      const genreMatched = movie.genres.some((g) => c.genreIds.includes(g.id));
      const typeMatched = c.typeIds.includes(movie.type.id);
      const score =
        (moodMatched ? MOOD_POINTS : 0) + (genreMatched ? GENRE_POINTS : 0) + (typeMatched ? TYPE_POINTS : 0);
      return { movie, score, moodMatched, genreMatched, typeMatched };
    })
    .filter((r) => r.score > 0)
    .sort((a, b) => b.score - a.score || b.movie.rating - a.movie.rating)
    .slice(0, limit);
}
