import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { api } from '../api';
import { EMPTY_FORM_ERROR } from '../pages/RecommendPage';
import { renderApp } from './renderApp';
import type { Movie } from '../types';

const moods = [
  { id: 1, name: 'весёлое' },
  { id: 2, name: 'уютное' },
];
const genres = [
  { id: 2, name: 'комедия' },
  { id: 9, name: 'приключения' },
];
const types = [
  { id: 1, name: 'фильм' },
  { id: 3, name: 'мультфильм' },
];
const zootopia: Movie = {
  id: 58,
  title: 'Зверополис',
  year: 2016,
  description: 'Крольчиха Джуди становится первой полицейской своего вида.',
  posterUrl: null,
  rating: 8,
  type: types[1],
  genres: [genres[0]],
  moods: [moods[0]],
};

beforeEach(() => {
  vi.spyOn(api, 'getMoods').mockResolvedValue(moods);
  vi.spyOn(api, 'getGenres').mockResolvedValue(genres);
  vi.spyOn(api, 'getTypes').mockResolvedValue(types);
});
afterEach(() => vi.restoreAllMocks());

describe('Страница подбора', () => {
  it('клики по чипам меняют состояние формы', async () => {
    const user = userEvent.setup();
    renderApp('/recommend');
    const form = await screen.findByRole('form', { name: 'Параметры подбора' });

    const any = within(form).getByRole('button', { name: 'любое' });
    const funny = await within(form).findByRole('button', { name: /весёлое/ });
    expect(any).toHaveAttribute('aria-pressed', 'true');

    await user.click(funny);
    expect(funny).toHaveAttribute('aria-pressed', 'true');
    expect(any).toHaveAttribute('aria-pressed', 'false');

    // Жанры — мультивыбор: повторный клик снимает отметку
    const comedy = within(form).getByRole('button', { name: 'комедия' });
    const adventure = within(form).getByRole('button', { name: 'приключения' });
    await user.click(comedy);
    await user.click(adventure);
    expect(comedy).toHaveAttribute('aria-pressed', 'true');
    expect(adventure).toHaveAttribute('aria-pressed', 'true');
    await user.click(comedy);
    expect(comedy).toHaveAttribute('aria-pressed', 'false');

    const yearFrom = within(form).getByLabelText('Год с');
    await user.type(yearFrom, '2000');
    expect(yearFrom).toHaveValue(2000);
  });

  it('пустая форма — ошибка, запрос не отправляется', async () => {
    const user = userEvent.setup();
    const recommend = vi.spyOn(api, 'recommend');
    renderApp('/recommend');

    await user.click(await screen.findByRole('button', { name: 'Подобрать' }));
    expect(screen.getByRole('alert')).toHaveTextContent(EMPTY_FORM_ERROR);
    expect(recommend).not.toHaveBeenCalled();
  });

  it('отправка записывает условия в адрес и показывает баллы с совпадениями', async () => {
    const user = userEvent.setup();
    const recommend = vi
      .spyOn(api, 'recommend')
      .mockResolvedValue([{ movie: zootopia, score: 6, moodMatched: true, genreMatched: true, typeMatched: true }]);
    const { router } = renderApp('/recommend');

    await user.click(await screen.findByRole('button', { name: /весёлое/ }));
    await user.click(screen.getByRole('button', { name: 'комедия' }));
    await user.click(screen.getByRole('button', { name: 'мультфильм' }));
    await user.click(screen.getByRole('button', { name: 'Подобрать' }));

    expect(new URLSearchParams(router.state.location.search).toString()).toBe(
      'moodId=1&genreIds=2&typeIds=3&submitted=1',
    );
    expect(recommend).toHaveBeenCalledWith(expect.objectContaining({ moodId: 1, genreIds: [2], typeIds: [3] }));

    const card = await screen.findByRole('article', { name: 'Зверополис' });
    expect(within(card).getByText('Баллы: 6')).toBeInTheDocument();
    expect(within(card).getByText('настроение +3')).toBeInTheDocument();
    expect(within(card).getByText('жанр +2')).toBeInTheDocument();
    expect(within(card).getByText('тип +1')).toBeInTheDocument();
  });

  it('открытие по ссылке восстанавливает форму и сразу подбирает', async () => {
    const recommend = vi.spyOn(api, 'recommend').mockResolvedValue([]);
    renderApp('/recommend?moodId=2&genreIds=9&submitted=1');

    expect(await screen.findByRole('button', { name: /уютное/ })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'приключения' })).toHaveAttribute('aria-pressed', 'true');
    expect(recommend).toHaveBeenCalledWith(expect.objectContaining({ moodId: 2, genreIds: [9] }));
    expect(await screen.findByText('Ничего не нашлось')).toBeInTheDocument();
  });

  it('«Сбросить» очищает форму и адрес', async () => {
    const user = userEvent.setup();
    vi.spyOn(api, 'recommend').mockResolvedValue([]);
    const { router } = renderApp('/recommend?moodId=1&submitted=1');

    const funny = await screen.findByRole('button', { name: /весёлое/ });
    expect(funny).toHaveAttribute('aria-pressed', 'true');
    await user.click(screen.getByRole('button', { name: 'Сбросить' }));

    expect(funny).toHaveAttribute('aria-pressed', 'false');
    expect(router.state.location.search).toBe('');
    expect(screen.getByRole('heading', { name: 'Как работает подбор' })).toBeInTheDocument();
  });
});
