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

  createMovie: (input) => http('/api/admin/movies', { method: 'POST', ...json(input) }),
  updateMovie: (id, input) => http(`/api/admin/movies/${id}`, { method: 'PUT', ...json(input) }),
  deleteMovie: (id) => http(`/api/admin/movies/${id}`, { method: 'DELETE' }),
  suggestMoods: (description, genreIds) =>
    http('/api/admin/movies/suggest-moods', { method: 'POST', ...json({ description, genreIds }) }),

  createDictionaryItem: (kind, name) => http(`/api/admin/${kind}`, { method: 'POST', ...json({ name }) }),
  renameDictionaryItem: (kind, id, name) => http(`/api/admin/${kind}/${id}`, { method: 'PUT', ...json({ name }) }),
  deleteDictionaryItem: (kind, id) => http(`/api/admin/${kind}/${id}`, { method: 'DELETE' }),
};
