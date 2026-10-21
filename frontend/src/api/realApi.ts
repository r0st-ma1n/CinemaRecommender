import type { Api } from './types';
import { http, json } from './http';
import { criteriaToParams } from '../lib/criteria';

/** HTTP-реализация: запросы к REST API на Spring Boot (пакет controller/api и ChatController). */
export const realApi: Api = {
  getMovies: () => http('/api/movies'),
  getMovie: (id) => http(`/api/movies/${id}`),
  getTypes: () => http('/api/types'),
  getGenres: () => http('/api/genres'),
  getMoods: () => http('/api/moods'),
  recommend: (c) => http(`/api/recommend?${criteriaToParams(c)}`),
  sendChat: (request) => http('/api/chat', { method: 'POST', ...json(request) }),

  getAuth: () => http('/api/auth/me'),
  // Вход обрабатывает formLogin Spring Security, он ждёт обычную форму, а не JSON
  login: (username, password) =>
    http('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ username, password }),
    }),
  logout: () => http('/api/auth/logout', { method: 'POST' }),

  createMovie: (input) => http('/api/admin/movies', { method: 'POST', ...json(input) }),
  updateMovie: (id, input) => http(`/api/admin/movies/${id}`, { method: 'PUT', ...json(input) }),
  deleteMovie: (id) => http(`/api/admin/movies/${id}`, { method: 'DELETE' }),
  suggestMoods: (description, genreIds) =>
    http('/api/admin/movies/suggest-moods', { method: 'POST', ...json({ description, genreIds }) }),

  createDictionaryItem: (kind, name) => http(`/api/admin/${kind}`, { method: 'POST', ...json({ name }) }),
  renameDictionaryItem: (kind, id, name) => http(`/api/admin/${kind}/${id}`, { method: 'PUT', ...json({ name }) }),
  deleteDictionaryItem: (kind, id) => http(`/api/admin/${kind}/${id}`, { method: 'DELETE' }),
};
