import { Link } from 'react-router-dom';
import { NAV_ITEMS } from './Header';

export function Footer() {
  return (
    <footer className="mt-24 border-t border-line bg-surface/50">
      <div className="mx-auto grid max-w-7xl gap-10 px-4 py-12 sm:px-6 md:grid-cols-3">
        <div className="flex flex-col gap-3">
          <p className="text-lg font-bold">
            🎬 Cinema<span className="text-accent">Recommender</span>
          </p>
          <p className="text-sm leading-relaxed text-muted">
            Фильмы, сериалы и мультфильмы под настроение — через форму подбора или обычной фразой в чате.
          </p>
        </div>

        <div className="flex flex-col gap-3">
          <p className="text-sm font-semibold uppercase tracking-wider text-muted">Разделы</p>
          <ul className="grid grid-cols-2 gap-2 text-sm">
            {NAV_ITEMS.map((item) => (
              <li key={item.to}>
                <Link to={item.to} className="text-fg/80 hover:text-accent">
                  {item.label}
                </Link>
              </li>
            ))}
          </ul>
        </div>

        <div className="flex flex-col gap-3">
          <p className="text-sm font-semibold uppercase tracking-wider text-muted">Как работает подбор</p>
          <p className="text-sm leading-relaxed text-muted">
            Совпадение настроения даёт <b className="text-fg">+3</b> балла, жанра — <b className="text-fg">+2</b>, типа —{' '}
            <b className="text-fg">+1</b>. Показываются 10 лучших, при равных баллах выше фильм с бóльшим рейтингом.
          </p>
        </div>
      </div>
      <div className="border-t border-line">
        <p className="mx-auto max-w-7xl px-4 py-5 text-xs text-muted sm:px-6">
          Учебный проект. Описания и рейтинги фильмов приведены для примера и указаны приблизительно.
        </p>
      </div>
    </footer>
  );
}
