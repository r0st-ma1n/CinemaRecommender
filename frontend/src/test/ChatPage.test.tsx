import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { api } from '../api';
import { EMPTY_CRITERIA } from '../lib/criteria';
import { renderApp } from './renderApp';
import type { ChatContext } from '../types';

const context: ChatContext = { criteria: { ...EMPTY_CRITERIA, moodId: 1 }, shownMovieIds: [58] };

afterEach(() => vi.restoreAllMocks());

describe('Чат', () => {
  it('клик по подсказке подставляет текст в поле', async () => {
    const user = userEvent.setup();
    renderApp('/chat');

    await user.click(screen.getByRole('button', { name: 'сериал из 90-х' }));
    expect(screen.getByLabelText('Сообщение')).toHaveValue('сериал из 90-х');
    expect(screen.getByText('14 / 500')).toBeInTheDocument();
  });

  it('контекст из ответа уходит со следующим сообщением', async () => {
    const user = userEvent.setup();
    const send = vi
      .spyOn(api, 'sendChat')
      .mockResolvedValueOnce({
        text: 'Вот что могу предложить:',
        movies: [{ id: 58, title: 'Зверополис', year: 2016, type: 'мультфильм', rating: 8, posterUrl: null }],
        context,
      })
      .mockResolvedValueOnce({ text: 'Больше ничего не нашлось.', movies: [], context: null });
    renderApp('/chat');

    await user.type(screen.getByLabelText('Сообщение'), 'хочу посмеяться{Enter}');
    expect(send).toHaveBeenLastCalledWith({ message: 'хочу посмеяться', context: null });
    expect(await screen.findByRole('link', { name: /Зверополис/ })).toHaveAttribute('href', '/movies/58');

    await user.type(screen.getByLabelText('Сообщение'), 'ещё{Enter}');
    expect(send).toHaveBeenLastCalledWith({ message: 'ещё', context });
    expect(await screen.findByText('Больше ничего не нашлось.')).toBeInTheDocument();
  });

  it('Shift+Enter переносит строку, а не отправляет', async () => {
    const user = userEvent.setup();
    const send = vi.spyOn(api, 'sendChat');
    renderApp('/chat');

    await user.type(screen.getByLabelText('Сообщение'), 'строка{Shift>}{Enter}{/Shift}вторая');
    expect(screen.getByLabelText('Сообщение')).toHaveValue('строка\nвторая');
    expect(send).not.toHaveBeenCalled();
  });

  it('сообщение длиннее 500 символов не отправляется', async () => {
    const user = userEvent.setup();
    renderApp('/chat');

    await user.click(screen.getByLabelText('Сообщение'));
    await user.paste('а'.repeat(501));
    expect(screen.getByRole('button', { name: 'Отправить' })).toBeDisabled();
  });

  it('«Новый разговор» очищает историю и контекст', async () => {
    const user = userEvent.setup();
    const send = vi.spyOn(api, 'sendChat').mockResolvedValue({ text: 'Ответ бота', movies: [], context });
    renderApp('/chat');

    await user.type(screen.getByLabelText('Сообщение'), 'хочу посмеяться{Enter}');
    expect(await screen.findByText('Ответ бота')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Новый разговор/ }));
    expect(screen.queryByText('Ответ бота')).not.toBeInTheDocument();

    await user.type(screen.getByLabelText('Сообщение'), 'привет{Enter}');
    expect(send).toHaveBeenLastCalledWith({ message: 'привет', context: null });
  });
});
