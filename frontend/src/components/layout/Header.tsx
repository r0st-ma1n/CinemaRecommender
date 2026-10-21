import { useEffect, useState } from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';
import { cx } from '../ui';

export const NAV_ITEMS = [
  { to: '/', label: 'Главная', end: true },
  { to: '/catalog', label: 'Каталог' },
  { to: '/recommend', label: 'Подбор' },
  { to: '/chat', label: 'Чат' },
  { to: '/admin', label: 'Админка' },
] as const;

export function Header() {
  const [open, setOpen] = useState(false);
  const location = useLocation();
  useEffect(() => setOpen(false), [location.pathname]);

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    cx(
      'rounded-lg px-3 py-2 text-sm font-medium transition-colors',
      isActive ? 'bg-accent/10 text-accent' : 'text-muted hover:bg-surface-2 hover:text-fg',
    );

  return (
    <header className="sticky top-0 z-40 border-b border-line bg-bg/80 backdrop-blur-lg">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between gap-6 px-4 sm:px-6">
        <Link to="/" className="flex items-center gap-2 text-lg font-bold tracking-tight">
          <span aria-hidden>🎬</span>
          <span>
            Cinema<span className="text-accent">Recommender</span>
          </span>
        </Link>

        <nav aria-label="Основное меню" className="hidden items-center gap-1 md:flex">
          {NAV_ITEMS.map((item) => (
            <NavLink key={item.to} to={item.to} end={'end' in item} className={linkClass}>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <button
          type="button"
          className="flex h-10 w-10 items-center justify-center rounded-lg text-xl text-muted hover:bg-surface-2 md:hidden"
          aria-label={open ? 'Закрыть меню' : 'Открыть меню'}
          aria-expanded={open}
          onClick={() => setOpen((v) => !v)}
        >
          {open ? '✕' : '☰'}
        </button>
      </div>

      {open && (
        <nav aria-label="Мобильное меню" className="animate-fade-up border-t border-line px-4 py-3 md:hidden">
          <div className="flex flex-col gap-1">
            {NAV_ITEMS.map((item) => (
              <NavLink key={item.to} to={item.to} end={'end' in item} className={linkClass}>
                {item.label}
              </NavLink>
            ))}
          </div>
        </nav>
      )}
    </header>
  );
}
