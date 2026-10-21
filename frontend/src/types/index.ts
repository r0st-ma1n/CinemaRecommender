/** Справочники: тип (фильм / сериал / мультфильм), жанр, настроение. */
export interface DictionaryItem {
  id: number;
  name: string;
}
export type MovieType = DictionaryItem;
export type Genre = DictionaryItem;
export type Mood = DictionaryItem;

/** Фильм так, как его отдаёт API: связи развёрнуты в объекты. */
export interface Movie {
  id: number;
  title: string;
  year: number;
  description: string;
  posterUrl: string | null;
  rating: number;
  type: MovieType;
  genres: Genre[];
  moods: Mood[];
}

/** Данные формы фильма в админке (связи — по id). */
export interface MovieInput {
  title: string;
  year: number;
  description: string;
  posterUrl: string | null;
  rating: number;
  typeId: number;
  genreIds: number[];
  moodIds: number[];
}

/** Условия подбора — как record Criteria на бэкенде. */
export interface Criteria {
  moodId: number | null;
  genreIds: number[];
  typeIds: number[];
  excludedGenreIds: number[];
  excludedTypeIds: number[];
  yearFrom: number | null;
  yearTo: number | null;
}

/** Фильм в результатах подбора: баллы и что совпало. */
export interface Recommendation {
  movie: Movie;
  score: number;
  moodMatched: boolean;
  genreMatched: boolean;
  typeMatched: boolean;
}

/** Фильм в ответе бота — только поля для карточки. */
export interface ChatMovie {
  id: number;
  title: string;
  year: number;
  type: string;
  rating: number;
  posterUrl: string | null;
}

/** Что бот помнит о прошлом запросе; хранится на клиенте. */
export interface ChatContext {
  criteria: Criteria;
  shownMovieIds: number[];
}

export interface ChatRequest {
  message: string;
  context?: ChatContext | null;
}

/** Ответ бота. context === null — оставить прежний. */
export interface ChatReply {
  text: string;
  movies: ChatMovie[];
  context: ChatContext | null;
}

/** Подсказка модели настроений: вероятность от 0 до 1. */
export interface MoodSuggestion {
  mood: string;
  probability: number;
}

export type DictionaryKind = 'genres' | 'moods';

/** Вошёл ли администратор (GET /api/auth/me). */
export interface AuthStatus {
  authenticated: boolean;
  username: string | null;
}
