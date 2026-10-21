import { Link } from 'react-router-dom';
import type { Movie } from '../types';
import { Badge, Rating, Skeleton, cx } from './ui';

const typeIcon: Record<string, string> = { фильм: '🎬', сериал: '📺', мультфильм: '🧸' };

/** Постер или заглушка: в начальных данных постеров нет. */
export function Poster({ url, title, className }: { url: string | null; title: string; className?: string }) {
  return (
    <div className={cx('relative overflow-hidden bg-surface-2', className)}>
      <img
        src={url || '/no-poster.svg'}
        alt={url ? `Постер: ${title}` : ''}
        loading="lazy"
        onError={(e) => (e.currentTarget.src = '/no-poster.svg')}
        className="h-full w-full object-cover"
      />
    </div>
  );
}

export function MovieCard({ movie }: { movie: Movie }) {
  return (
    <Link
      to={`/movies/${movie.id}`}
      className="card group flex flex-col overflow-hidden transition-all hover:-translate-y-0.5 hover:border-accent/50"
    >
      <Poster url={movie.posterUrl} title={movie.title} className="aspect-[2/3]" />
      <div className="flex flex-1 flex-col gap-2 p-4">
        <div className="flex items-start justify-between gap-3">
          <h3 className="font-semibold leading-snug group-hover:text-accent">{movie.title}</h3>
          <Rating value={movie.rating} />
        </div>
        <p className="text-sm text-muted">
          {movie.year} · {typeIcon[movie.type.name] ?? '🎞️'} {movie.type.name}
        </p>
        <div className="mt-auto flex flex-wrap gap-1.5 pt-1">
          {movie.genres.slice(0, 3).map((g) => (
            <Badge key={g.id}>{g.name}</Badge>
          ))}
        </div>
      </div>
    </Link>
  );
}

export function MovieCardSkeleton() {
  return (
    <div className="card flex flex-col overflow-hidden">
      <Skeleton className="aspect-[2/3] rounded-none" />
      <div className="flex flex-col gap-3 p-4">
        <Skeleton className="h-5 w-3/4" />
        <Skeleton className="h-4 w-1/2" />
        <Skeleton className="h-5 w-2/3" />
      </div>
    </div>
  );
}

export const movieGridClass = 'grid grid-cols-2 gap-4 sm:grid-cols-3 sm:gap-6 lg:grid-cols-4 xl:grid-cols-5';
