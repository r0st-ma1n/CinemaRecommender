import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { useAsync } from '../hooks/useAsync';
import { Poster } from '../components/MovieCard';
import { Badge, ButtonLink, ErrorBox, Rating, Skeleton } from '../components/ui';
import { criteriaToParams, EMPTY_CRITERIA } from '../lib/criteria';
import { NotFoundPage } from './NotFoundPage';
import { moodEmoji } from '../lib/moods';

export function MoviePage() {
  const id = Number(useParams().id);
  const { data: movie, loading, error } = useAsync(() => api.getMovie(id), [id]);

  if (loading) {
    return (
      <div className="grid gap-10 md:grid-cols-[320px_1fr]">
        <Skeleton className="aspect-[2/3] rounded-2xl" />
        <div className="flex flex-col gap-4">
          <Skeleton className="h-10 w-2/3" />
          <Skeleton className="h-5 w-1/3" />
          <Skeleton className="h-32 w-full" />
        </div>
      </div>
    );
  }
  if (error) return <ErrorBox>{error}</ErrorBox>;
  if (!movie) return <NotFoundPage what="Фильм не найден" />;

  const similar = criteriaToParams({
    ...EMPTY_CRITERIA,
    moodId: movie.moods[0]?.id ?? null,
    genreIds: movie.genres.slice(0, 1).map((g) => g.id),
  });

  return (
    <article className="flex flex-col gap-8">
      <Link to="/catalog" className="w-fit text-sm text-muted hover:text-accent">
        ← Каталог
      </Link>
      <div className="grid gap-10 md:grid-cols-[320px_1fr]">
        <Poster url={movie.posterUrl} title={movie.title} className="aspect-[2/3] w-full max-w-xs rounded-2xl border border-line" />
        <div className="flex flex-col gap-6">
          <div className="flex flex-col gap-3">
            <h1 className="text-3xl font-bold tracking-tight sm:text-5xl">{movie.title}</h1>
            <div className="flex flex-wrap items-center gap-3 text-muted">
              <span>{movie.year}</span>
              <span aria-hidden>·</span>
              <span>{movie.type.name}</span>
              <span aria-hidden>·</span>
              <Rating value={movie.rating} />
            </div>
          </div>

          <p className="max-w-2xl text-lg leading-8 text-fg/90">{movie.description}</p>

          <dl className="grid gap-5 sm:grid-cols-2">
            <div className="flex flex-col gap-2">
              <dt className="text-sm font-medium text-muted">Жанры</dt>
              <dd className="flex flex-wrap gap-2">
                {movie.genres.map((g) => (
                  <Badge key={g.id}>{g.name}</Badge>
                ))}
              </dd>
            </div>
            <div className="flex flex-col gap-2">
              <dt className="text-sm font-medium text-muted">Настроение</dt>
              <dd className="flex flex-wrap gap-2">
                {movie.moods.map((m) => (
                  <Badge key={m.id} tone="accent">
                    {moodEmoji(m.name)} {m.name}
                  </Badge>
                ))}
              </dd>
            </div>
          </dl>

          <div className="flex flex-wrap gap-3 pt-2">
            <ButtonLink to={`/recommend?${similar}&submitted=1`}>Найти похожее</ButtonLink>
            <ButtonLink to={`/admin/movies/${movie.id}/edit`} variant="ghost">
              Изменить
            </ButtonLink>
          </div>
        </div>
      </div>
    </article>
  );
}
