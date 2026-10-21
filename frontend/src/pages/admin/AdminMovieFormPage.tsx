import { useEffect, useState, type FormEvent, type ReactNode } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api } from '../../api';
import { useDictionaries } from '../../hooks/useDictionaries';
import { useToast } from '../../components/Toast';
import { Button, Chip, ErrorBox, PageHeader, Skeleton, cx } from '../../components/ui';
import { moodEmoji } from '../../lib/moods';
import type { MoodSuggestion, MovieInput } from '../../types';

interface FormState {
  title: string;
  year: string;
  description: string;
  posterUrl: string;
  rating: string;
  typeId: number | null;
  genreIds: number[];
  moodIds: number[];
}

type Errors = Partial<Record<keyof FormState, string>>;

const EMPTY: FormState = {
  title: '',
  year: '',
  description: '',
  posterUrl: '',
  rating: '',
  typeId: null,
  genreIds: [],
  moodIds: [],
};

/** Те же проверки, что в сущности Movie на бэкенде. */
export function validateMovie(f: FormState): Errors {
  const errors: Errors = {};
  if (!f.title.trim()) errors.title = 'Укажите название';
  else if (f.title.trim().length > 200) errors.title = 'Не длиннее 200 символов';
  const year = Number(f.year);
  if (!f.year) errors.year = 'Укажите год';
  else if (!Number.isInteger(year)) errors.year = 'Год — целое число';
  else if (year < 1888) errors.year = 'Слишком ранний год';
  else if (year > 2100) errors.year = 'Слишком поздний год';
  if (!f.description.trim()) errors.description = 'Добавьте описание';
  const rating = Number(f.rating.replace(',', '.'));
  if (!f.rating) errors.rating = 'Укажите рейтинг';
  else if (Number.isNaN(rating) || rating < 0 || rating > 10) errors.rating = 'Рейтинг от 0 до 10';
  if (f.posterUrl.length > 500) errors.posterUrl = 'Не длиннее 500 символов';
  if (f.typeId == null) errors.typeId = 'Выберите тип';
  return errors;
}

const toggle = (list: number[], id: number) => (list.includes(id) ? list.filter((x) => x !== id) : [...list, id]);

export function AdminMovieFormPage() {
  const params = useParams();
  const id = params.id ? Number(params.id) : null;
  const navigate = useNavigate();
  const toast = useToast();
  const dictionaries = useDictionaries();

  const [form, setForm] = useState<FormState>(EMPTY);
  const [errors, setErrors] = useState<Errors>({});
  const [loading, setLoading] = useState(id != null);
  const [saving, setSaving] = useState(false);
  const [suggesting, setSuggesting] = useState(false);
  const [suggestions, setSuggestions] = useState<MoodSuggestion[] | null>(null);
  const [suggestError, setSuggestError] = useState<string | null>(null);

  useEffect(() => {
    if (id == null) return;
    api.getMovie(id).then((m) => {
      if (!m) {
        toast('Фильм не найден', 'error');
        navigate('/admin');
        return;
      }
      setForm({
        title: m.title,
        year: String(m.year),
        description: m.description,
        posterUrl: m.posterUrl ?? '',
        rating: String(m.rating),
        typeId: m.type.id,
        genreIds: m.genres.map((g) => g.id),
        moodIds: m.moods.map((x) => x.id),
      });
      setLoading(false);
    });
  }, [id, navigate, toast]);

  const update = (patch: Partial<FormState>) => {
    setForm((f) => ({ ...f, ...patch }));
    setErrors((e) => {
      const next = { ...e };
      Object.keys(patch).forEach((k) => delete next[k as keyof FormState]);
      return next;
    });
  };

  async function submit(e: FormEvent) {
    e.preventDefault();
    const found = validateMovie(form);
    setErrors(found);
    if (Object.keys(found).length) return;

    const input: MovieInput = {
      title: form.title.trim(),
      year: Number(form.year),
      description: form.description.trim(),
      posterUrl: form.posterUrl.trim() || null,
      rating: Number(form.rating.replace(',', '.')),
      typeId: form.typeId!,
      genreIds: form.genreIds,
      moodIds: form.moodIds,
    };
    setSaving(true);
    try {
      if (id == null) await api.createMovie(input);
      else await api.updateMovie(id, input);
      toast('Сохранено');
      navigate('/admin');
    } catch (err) {
      toast(err instanceof Error ? err.message : 'Не удалось сохранить', 'error');
    } finally {
      setSaving(false);
    }
  }

  /** Подсказка модели: 1–2 настроения по описанию и жанрам. Фильм при этом не сохраняется. */
  async function suggest() {
    if (!form.description.trim()) {
      setSuggestError('Чтобы получить подсказку, сначала заполните описание');
      return;
    }
    setSuggestError(null);
    setSuggesting(true);
    try {
      setSuggestions(await api.suggestMoods(form.description, form.genreIds));
    } catch (err) {
      setSuggestError(err instanceof Error ? err.message : 'Модель недоступна');
    } finally {
      setSuggesting(false);
    }
  }

  const d = dictionaries.data;
  const moodByName = (name: string) => d?.moods.find((m) => m.name.toLowerCase() === name.toLowerCase());

  if (loading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-10 w-1/3" />
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  return (
    <div>
      <PageHeader title={id == null ? 'Новый фильм' : 'Изменить фильм'} />

      <form onSubmit={submit} noValidate className="card flex flex-col gap-6 p-6" aria-label="Фильм">
        <div className="grid gap-6 md:grid-cols-[1fr_140px_140px]">
          <Field label="Название" error={errors.title}>
            <input className="field" value={form.title} onChange={(e) => update({ title: e.target.value })} />
          </Field>
          <Field label="Год" error={errors.year}>
            <input
              type="number"
              className="field"
              value={form.year}
              onChange={(e) => update({ year: e.target.value })}
            />
          </Field>
          <Field label="Рейтинг" error={errors.rating}>
            <input
              inputMode="decimal"
              className="field"
              placeholder="0–10"
              value={form.rating}
              onChange={(e) => update({ rating: e.target.value })}
            />
          </Field>
        </div>

        <Field label="Описание" error={errors.description}>
          <textarea
            rows={5}
            className="field leading-relaxed"
            value={form.description}
            onChange={(e) => update({ description: e.target.value })}
          />
        </Field>

        <Field label="Ссылка на постер (необязательно)" error={errors.posterUrl}>
          <input
            type="url"
            className="field"
            placeholder="https://…"
            value={form.posterUrl}
            onChange={(e) => update({ posterUrl: e.target.value })}
          />
        </Field>

        <Group label="Тип" error={errors.typeId}>
          {d?.types.map((t) => (
            <Chip key={t.id} active={form.typeId === t.id} onClick={() => update({ typeId: t.id })}>
              {t.name}
            </Chip>
          ))}
        </Group>

        <Group label="Жанры">
          {d?.genres.map((g) => (
            <Chip key={g.id} active={form.genreIds.includes(g.id)} onClick={() => update({ genreIds: toggle(form.genreIds, g.id) })}>
              {g.name}
            </Chip>
          ))}
        </Group>

        <div className="flex flex-col gap-4 rounded-xl border border-dashed border-line p-4">
          <Group label="Настроения">
            {d?.moods.map((m) => (
              <Chip key={m.id} active={form.moodIds.includes(m.id)} onClick={() => update({ moodIds: toggle(form.moodIds, m.id) })}>
                {moodEmoji(m.name)} {m.name}
              </Chip>
            ))}
          </Group>

          <div className="flex flex-col gap-3">
            <Button variant="secondary" size="sm" className="w-fit" onClick={suggest} disabled={suggesting}>
              {suggesting ? 'Думаю…' : '🪄 Подсказать настроение'}
            </Button>
            {suggestError && <ErrorBox>{suggestError}</ErrorBox>}
            {suggestions && (
              <div className="flex flex-col gap-2">
                <p className="text-sm text-muted">Модель предлагает (по описанию и жанрам):</p>
                <div className="flex flex-wrap gap-2">
                  {suggestions.map((s) => {
                    const mood = moodByName(s.mood);
                    const added = mood && form.moodIds.includes(mood.id);
                    return (
                      <button
                        key={s.mood}
                        type="button"
                        disabled={!mood || added}
                        onClick={() => mood && update({ moodIds: [...form.moodIds, mood.id] })}
                        className={cx(
                          'inline-flex items-center gap-2 rounded-full border px-3 py-1.5 text-sm transition-colors',
                          added ? 'border-success/40 text-success' : 'border-accent/40 text-accent hover:bg-accent/10',
                          !mood && 'cursor-not-allowed opacity-50',
                        )}
                        title={mood ? undefined : 'Такого настроения больше нет в справочнике'}
                      >
                        {added ? '✓' : '+'} {s.mood}
                        <span className="text-xs opacity-70">{Math.round(s.probability * 100)}%</span>
                      </button>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="flex flex-wrap justify-end gap-3 border-t border-line pt-6">
          <Button variant="secondary" onClick={() => navigate('/admin')}>
            Отмена
          </Button>
          <Button type="submit" disabled={saving}>
            {saving ? 'Сохраняю…' : 'Сохранить'}
          </Button>
        </div>
      </form>
    </div>
  );
}

function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <label className="flex flex-col">
      <span className="label">{label}</span>
      {children}
      {error && <span className="mt-1.5 text-sm text-danger">{error}</span>}
    </label>
  );
}

function Group({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <fieldset className="flex flex-col gap-3">
      <legend className="label">{label}</legend>
      <div className="flex flex-wrap gap-2">{children}</div>
      {error && <span className="text-sm text-danger">{error}</span>}
    </fieldset>
  );
}
