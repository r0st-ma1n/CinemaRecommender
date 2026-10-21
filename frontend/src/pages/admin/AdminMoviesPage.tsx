import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api';
import { useAsync } from '../../hooks/useAsync';
import { Modal } from '../../components/Modal';
import { useToast } from '../../components/Toast';
import { Button, ButtonLink, ErrorBox, PageHeader, Rating, Skeleton } from '../../components/ui';
import type { Movie } from '../../types';

export function AdminMoviesPage() {
  const { data: movies, loading, error, reload } = useAsync(() => api.getMovies());
  const [query, setQuery] = useState('');
  const [toDelete, setToDelete] = useState<Movie | null>(null);
  const [deleting, setDeleting] = useState(false);
  const toast = useToast();

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (movies ?? []).filter((m) => !q || m.title.toLowerCase().includes(q));
  }, [movies, query]);

  async function confirmDelete() {
    if (!toDelete) return;
    setDeleting(true);
    try {
      await api.deleteMovie(toDelete.id);
      toast(`«${toDelete.title}» удалён`);
      reload();
    } catch (e) {
      toast(e instanceof Error ? e.message : 'Не удалось удалить', 'error');
    } finally {
      setDeleting(false);
      setToDelete(null);
    }
  }

  return (
    <div>
      <PageHeader
        title="Фильмы"
        subtitle={movies && `Всего: ${movies.length}`}
        actions={<ButtonLink to="/admin/movies/new">+ Добавить фильм</ButtonLink>}
      />

      <input
        type="search"
        className="field mb-5 sm:max-w-sm"
        placeholder="Поиск по названию…"
        aria-label="Поиск по названию"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />

      {error && <ErrorBox>{error}</ErrorBox>}

      <div className="card overflow-x-auto">
        <table className="w-full min-w-[640px] text-left text-sm">
          <thead className="border-b border-line text-xs uppercase tracking-wider text-muted">
            <tr>
              <th className="px-5 py-3 font-semibold">Название</th>
              <th className="px-5 py-3 font-semibold">Год</th>
              <th className="px-5 py-3 font-semibold">Тип</th>
              <th className="px-5 py-3 font-semibold">Настроения</th>
              <th className="px-5 py-3 font-semibold">Рейтинг</th>
              <th className="px-5 py-3" />
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {loading
              ? Array.from({ length: 6 }, (_, i) => (
                  <tr key={i}>
                    <td colSpan={6} className="px-5 py-3">
                      <Skeleton className="h-6 w-full" />
                    </td>
                  </tr>
                ))
              : filtered.map((m) => (
                  <tr key={m.id} className="transition-colors hover:bg-surface-2/50">
                    <td className="px-5 py-3 font-medium">
                      <Link to={`/movies/${m.id}`} className="hover:text-accent">
                        {m.title}
                      </Link>
                    </td>
                    <td className="px-5 py-3 text-muted">{m.year}</td>
                    <td className="px-5 py-3 text-muted">{m.type.name}</td>
                    <td className="px-5 py-3 text-muted">{m.moods.map((x) => x.name).join(', ') || '—'}</td>
                    <td className="px-5 py-3">
                      <Rating value={m.rating} />
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex justify-end gap-2">
                        <ButtonLink to={`/admin/movies/${m.id}/edit`} variant="secondary" size="sm">
                          Изменить
                        </ButtonLink>
                        <Button variant="danger" size="sm" onClick={() => setToDelete(m)}>
                          Удалить
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
            {!loading && filtered.length === 0 && (
              <tr>
                <td colSpan={6} className="px-5 py-10 text-center text-muted">
                  Ничего не найдено
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {toDelete && (
        <Modal title="Удалить фильм?" onClose={() => setToDelete(null)}>
          <p className="mb-6 text-muted">
            «{toDelete.title}» будет удалён без возможности восстановления.
          </p>
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => setToDelete(null)}>
              Отмена
            </Button>
            <Button variant="danger" onClick={confirmDelete} disabled={deleting}>
              Удалить
            </Button>
          </div>
        </Modal>
      )}
    </div>
  );
}
