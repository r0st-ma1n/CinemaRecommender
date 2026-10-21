import { mockApi } from '../api/mock/mockApi';
import { recommend } from '../lib/recommend';
import { EMPTY_CRITERIA } from '../lib/criteria';

/** Моки должны вести себя как бэкенд: те же баллы и тот же формат контекста. */
describe('Моки', () => {
  it('подбор: настроение +3, жанр +2, тип +1, не больше 10, при равенстве — по рейтингу', async () => {
    const movies = await mockApi.getMovies();
    const result = recommend(movies, { ...EMPTY_CRITERIA, moodId: 1, genreIds: [2], typeIds: [3] });

    expect(result.length).toBeLessThanOrEqual(10);
    result.forEach((r) =>
      expect(r.score).toBe((r.moodMatched ? 3 : 0) + (r.genreMatched ? 2 : 0) + (r.typeMatched ? 1 : 0)),
    );
    for (let i = 1; i < result.length; i++) {
      const [a, b] = [result[i - 1], result[i]];
      expect(a.score > b.score || (a.score === b.score && a.movie.rating >= b.movie.rating)).toBe(true);
    }
  });

  it('чат: «ещё» показывает следующие фильмы без повторов', async () => {
    const first = await mockApi.sendChat({ message: 'хочу посмеяться' });
    expect(first.movies).toHaveLength(5);
    expect(first.context?.criteria.moodId).toBe(1);

    const more = await mockApi.sendChat({ message: 'ещё', context: first.context });
    const firstIds = first.movies.map((m) => m.id);
    expect(more.movies.some((m) => firstIds.includes(m.id))).toBe(false);
  });

  it('чат: «а сериал?» меняет только тип', async () => {
    const first = await mockApi.sendChat({ message: 'хочу посмеяться' });
    const series = await mockApi.sendChat({ message: 'а сериал?', context: first.context });
    expect(series.context?.criteria).toMatchObject({ moodId: 1, typeIds: [2] });
  });

  it('чат: «не ужасы» — исключение, «из 90-х» — годы', async () => {
    const reply = await mockApi.sendChat({ message: 'что-нибудь страшное, но не ужасы из 90-х' });
    expect(reply.context?.criteria).toMatchObject({ excludedGenreIds: [7], yearFrom: 1990, yearTo: 1999 });
    reply.movies.forEach((m) => expect(m.year).toBeGreaterThanOrEqual(1990));
  });
});
