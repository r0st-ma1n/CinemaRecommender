import seed from './seed.json';
import type { Api } from '../types';
import { ApiError } from '../http';
import type { DictionaryItem, DictionaryKind, Movie, MovieInput } from '../../types';
import { recommend } from '../../lib/recommend';
import { mockChatReply } from './mockChat';
import { mockSuggestMoods } from './mockSuggest';

/**
 * Моки на начальных данных из V2__data.sql (seed.json получен из миграции).
 * Данные живут в памяти: правки из админки видны сразу, но сбрасываются при перезагрузке страницы.
 */
interface StoredMovie extends Omit<MovieInput, 'posterUrl'> {
  id: number;
  posterUrl: string | null;
}

const db = {
  types: structuredClone(seed.types) as DictionaryItem[],
  genres: structuredClone(seed.genres) as DictionaryItem[],
  moods: structuredClone(seed.moods) as DictionaryItem[],
  movies: structuredClone(seed.movies) as StoredMovie[],
};
let nextId = 1000;

/** В режиме моков вход проверяется здесь же; логин и пароль подсказывает форма входа. */
export const MOCK_ADMIN = { username: 'admin', password: 'admin' };
let loggedInAs: string | null = null;

/** Небольшая задержка, чтобы были видны состояния загрузки. */
const delay = <T>(value: T, ms = 250): Promise<T> =>
  new Promise((resolve) => setTimeout(() => resolve(structuredClone(value)), ms));

const byName = (a: DictionaryItem, b: DictionaryItem) => a.name.localeCompare(b.name, 'ru');

function toMovie(m: StoredMovie): Movie {
  return {
    id: m.id,
    title: m.title,
    year: m.year,
    description: m.description,
    posterUrl: m.posterUrl,
    rating: m.rating,
    type: db.types.find((t) => t.id === m.typeId)!,
    genres: db.genres.filter((g) => m.genreIds.includes(g.id)).sort(byName),
    moods: db.moods.filter((md) => m.moodIds.includes(md.id)).sort(byName),
  };
}

export const allMovies = () =>
  db.movies.map(toMovie).sort((a, b) => b.rating - a.rating || a.title.localeCompare(b.title, 'ru'));
export const dictionaries = () => db;

function checkName(kind: DictionaryKind, name: string, id?: number): string {
  const trimmed = name.trim();
  if (!trimmed) throw new ApiError('Название не может быть пустым', 400);
  if (db[kind].some((d) => d.id !== id && d.name.toLowerCase() === trimmed.toLowerCase())) {
    throw new ApiError(`«${trimmed}» уже есть`, 400);
  }
  return trimmed;
}

export const mockApi: Api = {
  getMovies: () => delay(allMovies()),
  getMovie: (id) => {
    const m = db.movies.find((x) => x.id === id);
    return delay(m ? toMovie(m) : null);
  },
  getTypes: () => delay([...db.types]),
  getGenres: () => delay([...db.genres].sort(byName)),
  getMoods: () => delay([...db.moods]),
  recommend: (criteria) => delay(recommend(allMovies(), criteria), 600),
  sendChat: (request) => delay(mockChatReply(request, allMovies(), db), 700),

  getAuth: () => delay({ authenticated: loggedInAs != null, username: loggedInAs }),
  login: (username, password) => {
    if (username !== MOCK_ADMIN.username || password !== MOCK_ADMIN.password) {
      return Promise.reject(new ApiError('Неверный логин или пароль', 401));
    }
    loggedInAs = username;
    return delay({ authenticated: true, username });
  },
  logout: () => {
    loggedInAs = null;
    return delay(undefined);
  },

  createMovie: (input) => {
    const stored = { ...input, id: nextId++ };
    db.movies.push(stored);
    return delay(toMovie(stored));
  },
  updateMovie: (id, input) => {
    const index = db.movies.findIndex((m) => m.id === id);
    if (index < 0) return Promise.reject(new ApiError('Фильм не найден', 404));
    db.movies[index] = { ...input, id };
    return delay(toMovie(db.movies[index]));
  },
  deleteMovie: (id) => {
    db.movies = db.movies.filter((m) => m.id !== id);
    return delay(undefined);
  },
  suggestMoods: (description, genreIds) =>
    delay(mockSuggestMoods(description, db.genres.filter((g) => genreIds.includes(g.id)).map((g) => g.name)), 500),

  createDictionaryItem: (kind, name) => {
    try {
      const item = { id: nextId++, name: checkName(kind, name) };
      db[kind].push(item);
      return delay(item);
    } catch (e) {
      return Promise.reject(e);
    }
  },
  renameDictionaryItem: (kind, id, name) => {
    try {
      const item = db[kind].find((d) => d.id === id);
      if (!item) throw new ApiError('Не найдено', 404);
      item.name = checkName(kind, name, id);
      return delay(item);
    } catch (e) {
      return Promise.reject(e);
    }
  },
  deleteDictionaryItem: (kind, id) => {
    db[kind] = db[kind].filter((d) => d.id !== id);
    // Как каскадное удаление в базе: связь с фильмами пропадает
    const key = kind === 'genres' ? 'genreIds' : 'moodIds';
    db.movies.forEach((m) => (m[key] = m[key].filter((x) => x !== id)));
    return delay(undefined);
  },
};
