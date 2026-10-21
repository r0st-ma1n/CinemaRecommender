import { NavLink, Outlet } from 'react-router-dom';
import { cx } from '../../components/ui';

const ITEMS = [
  { to: '/admin', label: 'Фильмы', icon: '🎬', end: true },
  { to: '/admin/genres', label: 'Жанры', icon: '🏷️' },
  { to: '/admin/moods', label: 'Настроения', icon: '🎭' },
];

export function AdminLayout() {
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
      </aside>
      <div className="min-w-0">
        <Outlet />
      </div>
    </div>
  );
}
