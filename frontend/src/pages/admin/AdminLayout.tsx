import { useEffect, useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { api } from '../../api';
import { AUTH_REQUIRED_EVENT } from '../../api/http';
import { Button, Skeleton, cx } from '../../components/ui';
import type { AuthStatus } from '../../types';
import { AdminLogin } from './AdminLogin';

const ITEMS = [
  { to: '/admin', label: 'Фильмы', icon: '🎬', end: true },
  { to: '/admin/genres', label: 'Жанры', icon: '🏷️' },
  { to: '/admin/moods', label: 'Настроения', icon: '🎭' },
];

const LOGGED_OUT: AuthStatus = { authenticated: false, username: null };

export function AdminLayout() {
  // undefined — ещё не знаем, вошёл ли администратор
  const [auth, setAuth] = useState<AuthStatus>();

  useEffect(() => {
    api.getAuth().then(setAuth, () => setAuth(LOGGED_OUT));
    // Сессия могла закончиться, пока страница открыта: любой ответ 401 возвращает к форме входа
    const onAuthRequired = () => setAuth(LOGGED_OUT);
    window.addEventListener(AUTH_REQUIRED_EVENT, onAuthRequired);
    return () => window.removeEventListener(AUTH_REQUIRED_EVENT, onAuthRequired);
  }, []);

  async function logout() {
    await api.logout();
    setAuth(LOGGED_OUT);
  }

  if (!auth) return <Skeleton className="h-64" />;
  if (!auth.authenticated) return <AdminLogin onLogin={setAuth} />;

  return (
    <div className="grid items-start gap-8 lg:grid-cols-[220px_1fr]">
      <aside className="card flex flex-col gap-1 p-3 lg:sticky lg:top-24">
        <p className="px-3 pb-2 pt-1 text-xs font-semibold uppercase tracking-wider text-muted">Админка</p>
        <nav aria-label="Разделы админки" className="flex gap-1 overflow-x-auto lg:flex-col">
          {ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                cx(
                  'flex shrink-0 items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                  isActive ? 'bg-accent/10 text-accent' : 'text-muted hover:bg-surface-2 hover:text-fg',
                )
              }
            >
              <span aria-hidden>{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="mt-2 flex items-center justify-between gap-2 border-t border-line px-3 pt-3 text-sm">
          <span className="truncate text-muted">{auth.username}</span>
          <Button variant="ghost" size="sm" onClick={logout}>
            Выйти
          </Button>
        </div>
      </aside>
      <div className="min-w-0">
        <Outlet />
      </div>
    </div>
  );
}
