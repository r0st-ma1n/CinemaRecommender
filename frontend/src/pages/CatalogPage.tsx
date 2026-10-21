import { useMemo, useState } from 'react';
import { api } from '../api';
import { useAsync } from '../hooks/useAsync';
import { MovieCard, MovieCardSkeleton, movieGridClass } from '../components/MovieCard';
import { Chip, EmptyState, ErrorBox, PageHeader } from '../components/ui';

export function CatalogPage() {
  const { data: movies, loading, error } = useAsync(() => api.getMovies());
  const [query, setQuery] = useState('');
  const [typeId, setTypeId] = useState<number | null>(null);
  const [genreId, setGenreId] = useState<number | null>(null);

  // Типы и жанры для фильтров берутся из самих фильмов
  const { types, genres } = useMemo(() => {
    const t = new Map<number, string>();
    const g = new Map<number, string>();
    movies?.forEach((m) => {
      t.set(m.type.id, m.type.name);
      m.genres.forEach((x) => g.set(x.id, x.name));
    });
    const sorted = (map: Map<number, string>) =>
      [...map].map(([id, name]) => ({ id, name })).sort((a, b) => a.name.localeCompare(b.name, 'ru'));
    return { types: sorted(t), genres: sorted(g) };
  }, [movies]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (movies ?? []).filter(
      (m) =>
        (!q || m.title.toLowerCase().includes(q)) &&
        (typeId == null || m.type.id === typeId) &&
        (genreId == null || m.genres.some((g) => g.id === genreId)),
    );
  }, [movies, query, typeId, genreId]);

  return (
    <div>
      <PageHeader title="Каталог" subtitle={movies && `Всего: ${movies.length}`} />

      <div className="card mb-8 flex flex-col gap-5 p-5">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center">
          <input
            type="search"
            className="field lg:max-w-sm"
            placeholder="Поиск по названию…"
            aria-label="Поиск по названию"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <div className="flex flex-wrap gap-2">
            <Chip active={typeId == null} onClick={() => setTypeId(null)}>
              Все типы
            </Chip>
            {types.map((t) => (
              <Chip key={t.id} active={typeId === t.id} onClick={() => setTypeId(typeId === t.id ? null : t.id)}>
                {t.name}
              </Chip>
            ))}
          </div>
        </div>
        <select
          className="field sm:max-w-xs"
          aria-label="Жанр"
          value={genreId ?? ''}
          onChange={(e) => setGenreId(e.target.value ? Number(e.target.value) : null)}
        >
          <option value="">Все жанры</option>
          {genres.map((g) => (
            <option key={g.id} value={g.id}>
              {g.name}
            </option>
          ))}
        </select>
      </div>

      {error && <ErrorBox>{error}</ErrorBox>}
      {loading ? (
        <div className={movieGridClass}>
          {Array.from({ length: 10 }, (_, i) => (
            <MovieCardSkeleton key={i} />
          ))}
        </div>
      ) : movies?.length === 0 ? (
        <EmptyState icon="🎞️" title="В каталоге пока нет фильмов" />
      ) : filtered.length === 0 ? (
        <EmptyState icon="🔍" title="Ничего не найдено">
          Попробуйте изменить запрос или сбросить фильтры.
        </EmptyState>
      ) : (
        <div className={movieGridClass}>
          {filtered.map((m) => (
            <MovieCard key={m.id} movie={m} />
          ))}
        </div>
      )}
    </div>
  );
}
