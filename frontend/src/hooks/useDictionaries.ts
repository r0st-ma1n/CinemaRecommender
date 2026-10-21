import { api } from '../api';
import { useAsync } from './useAsync';

/** Настроения, жанры и типы для форм и фильтров. */
export function useDictionaries() {
  return useAsync(async () => {
    const [moods, genres, types] = await Promise.all([api.getMoods(), api.getGenres(), api.getTypes()]);
    return { moods, genres, types };
  });
}
