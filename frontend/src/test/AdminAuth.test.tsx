import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MOCK_ADMIN } from '../api/mock/mockApi';
import { renderApp } from './renderApp';

describe('Вход в админку', () => {
  it('без входа показывает форму, с неверным паролем — ошибку, после входа — админку и выход', async () => {
    const user = userEvent.setup();
    renderApp('/admin');

    expect(await screen.findByRole('heading', { level: 1, name: 'Вход в админку' })).toBeInTheDocument();
    expect(screen.queryByRole('navigation', { name: 'Разделы админки' })).not.toBeInTheDocument();

    await user.type(screen.getByLabelText('Логин'), MOCK_ADMIN.username);
    await user.type(screen.getByLabelText('Пароль'), 'неверный');
    await user.click(screen.getByRole('button', { name: 'Войти' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Неверный логин или пароль');

    await user.clear(screen.getByLabelText('Пароль'));
    await user.type(screen.getByLabelText('Пароль'), MOCK_ADMIN.password);
    await user.click(screen.getByRole('button', { name: 'Войти' }));
    expect(await screen.findByRole('navigation', { name: 'Разделы админки' })).toBeInTheDocument();
    expect(await screen.findByRole('heading', { level: 1, name: 'Фильмы' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Выйти' }));
    expect(await screen.findByRole('heading', { level: 1, name: 'Вход в админку' })).toBeInTheDocument();
  });
});
