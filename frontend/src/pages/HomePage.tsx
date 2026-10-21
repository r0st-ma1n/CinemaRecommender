import { Link } from 'react-router-dom';
import { api } from '../api';
import { useAsync } from '../hooks/useAsync';
import { MovieCard, MovieCardSkeleton, movieGridClass } from '../components/MovieCard';
import { ButtonLink, Skeleton } from '../components/ui';
import { moodEmoji } from '../lib/moods';

const FEATURES = [
  { icon: '🎭', title: 'Под настроение', text: 'Выберите настроение, жанр и тип — подбор начислит баллы за каждое совпадение.' },
  { icon: '💬', title: 'Чат обычной фразой', text: 'Напишите «устал после работы, хочу мультик» — модель на Weka поймёт настроение.' },
  { icon: '🔍', title: 'Понятно почему', text: 'У каждого результата видно, что совпало: настроение +3, жанр +2, тип +1.' },
  { icon: '🍿', title: 'Всё в одном месте', text: 'Фильмы, сериалы и мультфильмы — от классики до новинок.' },
];

export function HomePage() {
  const moods = useAsync(() => api.getMoods());
  const movies = useAsync(() => api.getMovies());

  return (
    <div className="flex flex-col gap-24">
      {/* Hero */}
      <section className="relative overflow-hidden rounded-3xl border border-line bg-gradient-to-br from-surface-2 via-surface to-bg px-6 py-16 sm:px-12 sm:py-24">
        <div
          aria-hidden
          className="pointer-events-none absolute -top-32 right-0 h-96 w-96 rounded-full bg-accent/20 blur-3xl"
        />
        <div className="relative flex max-w-2xl flex-col gap-6">
          <span className="w-fit rounded-full border border-accent/30 bg-accent/10 px-3 py-1 text-xs font-semibold uppercase tracking-wider text-accent">
            Подбор фильмов по настроению
          </span>
          <h1 className="text-4xl font-extrabold leading-tight tracking-tight sm:text-6xl">
            Что посмотреть <span className="text-accent">под настроение?</span>
          </h1>
          <p className="text-lg leading-relaxed text-muted">
            Отметьте настроение, жанр и тип — или просто напишите боту, чего хочется. Мы подберём фильмы, сериалы и
            мультфильмы и объясним, почему именно они.
          </p>
          <div className="flex flex-wrap gap-3">
            <ButtonLink to="/recommend" size="lg">
              Подобрать фильм →
            </ButtonLink>
            <ButtonLink to="/chat" size="lg" variant="secondary">
              💬 Спросить бота
            </ButtonLink>
          </div>
        </div>
      </section>

      {/* Быстрый выбор настроения */}
      <section className="flex flex-col gap-6">
        <h2 className="text-2xl font-bold tracking-tight">Какое сегодня настроение?</h2>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
          {moods.loading
            ? Array.from({ length: 6 }, (_, i) => <Skeleton key={i} className="h-24 rounded-2xl" />)
            : moods.data?.map((mood) => (
                <Link
                  key={mood.id}
                  to={`/recommend?moodId=${mood.id}&submitted=1`}
                  className="card flex flex-col items-center justify-center gap-2 py-5 transition-all hover:-translate-y-0.5 hover:border-accent/50"
                >
                  <span className="text-3xl" aria-hidden>
                    {moodEmoji(mood.name)}
                  </span>
                  <span className="text-sm font-medium">{mood.name}</span>
                </Link>
              ))}
        </div>
      </section>

      {/* Преимущества */}
      <section className="flex flex-col gap-6">
        <h2 className="text-2xl font-bold tracking-tight">Почему здесь</h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {FEATURES.map((f) => (
            <div key={f.title} className="card flex flex-col gap-3 p-6">
              <span className="flex h-12 w-12 items-center justify-center rounded-xl bg-accent/10 text-2xl" aria-hidden>
                {f.icon}
              </span>
              <h3 className="font-semibold">{f.title}</h3>
              <p className="text-sm leading-relaxed text-muted">{f.text}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Лучшее в каталоге */}
      <section className="flex flex-col gap-6">
        <div className="flex items-end justify-between gap-4">
          <h2 className="text-2xl font-bold tracking-tight">Лучшее в каталоге</h2>
          <Link to="/catalog" className="text-sm font-medium text-accent hover:underline">
            Весь каталог →
          </Link>
        </div>
        <div className={movieGridClass}>
          {movies.loading
            ? Array.from({ length: 5 }, (_, i) => <MovieCardSkeleton key={i} />)
            : movies.data?.slice(0, 5).map((m) => <MovieCard key={m.id} movie={m} />)}
        </div>
      </section>
    </div>
  );
}
