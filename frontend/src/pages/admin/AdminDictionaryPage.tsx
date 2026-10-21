import { useState, type FormEvent } from 'react';
import { api } from '../../api';
import { useAsync } from '../../hooks/useAsync';
import { Modal } from '../../components/Modal';
import { useToast } from '../../components/Toast';
import { Button, ErrorBox, PageHeader, Skeleton } from '../../components/ui';
import type { DictionaryItem, DictionaryKind } from '../../types';

const TEXT: Record<DictionaryKind, { title: string; placeholder: string; deleteNote: string }> = {
  genres: {
    title: 'Жанры',
    placeholder: 'Новый жанр',
    deleteNote: 'Жанр пропадёт у всех фильмов, сами фильмы останутся.',
  },
  moods: {
    title: 'Настроения',
    placeholder: 'Новое настроение',
    deleteNote: 'Настроение пропадёт у всех фильмов, сами фильмы останутся.',
  },
};

/** Справочник жанров или настроений: добавление, переименование на месте, удаление. */
export function AdminDictionaryPage({ kind }: { kind: DictionaryKind }) {
  const load = () => (kind === 'genres' ? api.getGenres() : api.getMoods());
  const { data: items, loading, error, reload } = useAsync(load, [kind]);
  const [newName, setNewName] = useState('');
  const [addError, setAddError] = useState<string | null>(null);
  const [editing, setEditing] = useState<{ id: number; name: string; error?: string } | null>(null);
  const [toDelete, setToDelete] = useState<DictionaryItem | null>(null);
  const toast = useToast();
  const text = TEXT[kind];

  async function add(e: FormEvent) {
    e.preventDefault();
    if (!newName.trim()) {
      setAddError('Название не может быть пустым');
      return;
    }
    try {
      await api.createDictionaryItem(kind, newName);
      setNewName('');
      setAddError(null);
      toast('Сохранено');
      reload();
    } catch (err) {
      setAddError(err instanceof Error ? err.message : 'Ошибка');
    }
  }

  async function saveEdit(e: FormEvent) {
    e.preventDefault();
    if (!editing) return;
    try {
      await api.renameDictionaryItem(kind, editing.id, editing.name);
      setEditing(null);
      toast('Сохранено');
      reload();
    } catch (err) {
      setEditing({ ...editing, error: err instanceof Error ? err.message : 'Ошибка' });
    }
  }

  async function confirmDelete() {
    if (!toDelete) return;
    try {
      await api.deleteDictionaryItem(kind, toDelete.id);
      toast(`«${toDelete.name}» удалено`);
      reload();
    } catch (err) {
      toast(err instanceof Error ? err.message : 'Не удалось удалить', 'error');
    } finally {
      setToDelete(null);
    }
  }

  return (
    <div className="max-w-2xl">
      <PageHeader title={text.title} subtitle={items && `Всего: ${items.length}`} />

      <form onSubmit={add} className="mb-6 flex flex-col gap-2">
        <div className="flex gap-3">
          <input
            className="field"
            placeholder={text.placeholder}
            aria-label={text.placeholder}
            value={newName}
            onChange={(e) => {
              setNewName(e.target.value);
              setAddError(null);
            }}
          />
          <Button type="submit">Добавить</Button>
        </div>
        {addError && <span className="text-sm text-danger">{addError}</span>}
      </form>

      {error && <ErrorBox>{error}</ErrorBox>}

      <ul className="card divide-y divide-line">
        {loading
          ? Array.from({ length: 5 }, (_, i) => (
              <li key={i} className="px-5 py-3">
                <Skeleton className="h-6 w-1/2" />
              </li>
            ))
          : items?.map((item) =>
              editing?.id === item.id ? (
                <li key={item.id} className="px-5 py-3">
                  <form onSubmit={saveEdit} className="flex flex-col gap-2">
                    <div className="flex gap-2">
                      <input
                        autoFocus
                        className="field h-9 py-1"
                        aria-label="Новое название"
                        value={editing.name}
                        onChange={(e) => setEditing({ id: item.id, name: e.target.value })}
                        onKeyDown={(e) => e.key === 'Escape' && setEditing(null)}
                      />
                      <Button type="submit" size="sm">
                        Сохранить
                      </Button>
                      <Button variant="ghost" size="sm" onClick={() => setEditing(null)}>
                        Отмена
                      </Button>
                    </div>
                    {editing.error && <span className="text-sm text-danger">{editing.error}</span>}
                  </form>
                </li>
              ) : (
                <li key={item.id} className="flex items-center justify-between gap-3 px-5 py-3">
                  <span className="font-medium">{item.name}</span>
                  <div className="flex gap-2">
                    <Button variant="ghost" size="sm" onClick={() => setEditing({ id: item.id, name: item.name })}>
                      Переименовать
                    </Button>
                    <Button variant="danger" size="sm" onClick={() => setToDelete(item)}>
                      Удалить
                    </Button>
                  </div>
                </li>
              ),
            )}
      </ul>

      {toDelete && (
        <Modal title={`Удалить «${toDelete.name}»?`} onClose={() => setToDelete(null)}>
          <p className="mb-6 text-muted">{text.deleteNote}</p>
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => setToDelete(null)}>
              Отмена
            </Button>
            <Button variant="danger" onClick={confirmDelete}>
              Удалить
            </Button>
          </div>
        </Modal>
      )}
    </div>
  );
}
