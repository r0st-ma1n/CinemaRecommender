import type { Criteria } from '../types';

export const EMPTY_CRITERIA: Criteria = {
  moodId: null,
  genreIds: [],
  typeIds: [],
  excludedGenreIds: [],
  excludedTypeIds: [],
  yearFrom: null,
  yearTo: null,
};

/** Есть ли условия, за которые даются баллы (настроение, жанр или тип). */
export function hasWishes(c: Criteria): boolean {
  return c.moodId != null || c.genreIds.length > 0 || c.typeIds.length > 0;
}

const ids = (value: string | null) =>
  (value ?? '')
    .split(',')
    .map(Number)
    .filter((n) => Number.isInteger(n) && n > 0);

const num = (value: string | null) => {
  const n = Number(value);
  return value && Number.isInteger(n) ? n : null;
};

/** Условия подбора ⇄ query-строка, чтобы результат можно было обновить или отправить ссылкой. */
export function criteriaFromParams(params: URLSearchParams): Criteria {
  return {
    ...EMPTY_CRITERIA,
    moodId: num(params.get('moodId')),
    genreIds: ids(params.get('genreIds') ?? params.get('genreId')),
    typeIds: ids(params.get('typeIds') ?? params.get('typeId')),
    yearFrom: num(params.get('yearFrom')),
    yearTo: num(params.get('yearTo')),
  };
}

export function criteriaToParams(c: Criteria): URLSearchParams {
  const params = new URLSearchParams();
  if (c.moodId != null) params.set('moodId', String(c.moodId));
  if (c.genreIds.length) params.set('genreIds', c.genreIds.join(','));
  if (c.typeIds.length) params.set('typeIds', c.typeIds.join(','));
  if (c.yearFrom != null) params.set('yearFrom', String(c.yearFrom));
  if (c.yearTo != null) params.set('yearTo', String(c.yearTo));
  return params;
}
