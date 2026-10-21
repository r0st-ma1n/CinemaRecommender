import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderApp } from './renderApp';

describe('Навигация', () => {
  const mainNav = () => screen.getByRole('navigation', { name: 'Основное меню' });

  it('переключает страницы и подсвечивает активный пункт', async () => {
    const user = userEvent.setup();
    const { router } = renderApp('/');

    expect(within(mainNav()).getByRole('link', { name: 'Главная' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Что посмотреть под настроение?');

    await user.click(within(mainNav()).getByRole('link', { name: 'Каталог' }));
    expect(router.state.location.pathname).toBe('/catalog');
    expect(screen.getByRole('heading', { level: 1, name: 'Каталог' })).toBeInTheDocument();
    expect(within(mainNav()).getByRole('link', { name: 'Каталог' })).toHaveAttribute('aria-current', 'page');
    expect(within(mainNav()).getByRole('link', { name: 'Главная' })).not.toHaveAttribute('aria-current');

    await user.click(within(mainNav()).getByRole('link', { name: 'Подбор' }));
    expect(screen.getByRole('heading', { level: 1, name: 'Что посмотреть?' })).toBeInTheDocument();

    await user.click(within(mainNav()).getByRole('link', { name: 'Чат' }));
    expect(screen.getByRole('heading', { level: 1, name: 'Чат с ботом' })).toBeInTheDocument();

    await user.click(within(mainNav()).getByRole('link', { name: 'Админка' }));
    expect(router.state.location.pathname).toBe('/admin');
    expect(await screen.findByRole('heading', { level: 1, name: 'Вход в админку' })).toBeInTheDocument();
  });

  it('мобильное меню открывается и закрывается после перехода', async () => {
    const user = userEvent.setup();
    renderApp('/');

    expect(screen.queryByRole('navigation', { name: 'Мобильное меню' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Открыть меню' }));
    const mobile = screen.getByRole('navigation', { name: 'Мобильное меню' });

    await user.click(within(mobile).getByRole('link', { name: 'Чат' }));
    expect(screen.getByRole('heading', { level: 1, name: 'Чат с ботом' })).toBeInTheDocument();
    expect(screen.queryByRole('navigation', { name: 'Мобильное меню' })).not.toBeInTheDocument();
  });

  it('неизвестный адрес — страница 404', () => {
    renderApp('/нет-такой-страницы');
    expect(screen.getByText('404')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Страница не найдена' })).toBeInTheDocument();
  });
});
