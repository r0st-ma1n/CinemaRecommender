import { useEffect, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../api';
import { useDictionaries } from '../hooks/useDictionaries';
import { Poster } from '../components/MovieCard';
import { Badge, Button, Chip, EmptyState, ErrorBox, PageHeader, Rating, Skeleton } from '../components/ui';
import { criteriaFromParams, criteriaToParams, EMPTY_CRITERIA, hasWishes } from '../lib/criteria';
import { GENRE_POINTS, MAX_RESULTS, MOOD_POINTS, TYPE_POINTS } from '../lib/recommend';
import { moodEmoji } from '../lib/moods';
import type { Criteria, Recommendation } from '../types';

export const EMPTY_FORM_ERROR = 'Выберите хотя бы что-нибудь: настроение, жанр или тип.';

type Results =
  | { status: 'empty' }
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'done'; items: Recommendation[] };

const toggle = (list: number[], id: number) => (list.includes(id) ? list.filter((x) => x !== id) : [...list, id]);
const yearValue = (v: string) => (v === '' ? null : Number(v));

/**
 * Подбор. Условия хранятся в адресе (?moodId=1&genreIds=2,5&submitted=1),
 * поэтому страницу с результатом можно обновить или отправить ссылкой.
 */
export function RecommendPage() {
  const [params, setParams] = useSearchParams();
  const dictionaries = useDictionaries();
  const [form, setForm] = useState<Criteria>(() => criteriaFromParams(params));
  const [formError, setFormError] = useState<string | null>(null);
  const [results, setResults] = useState<Results>({ status: 'empty' });

  const paramsKey = params.toString();
  const submitted = params.has('submitted');

  // Адрес изменился (отправка формы, «Найти похожее», кнопка «назад») — обновить форму и результаты
  useEffect(() => {
    const criteria = criteriaFromParams(params);
    setForm(criteria);
    if (!submitted || !hasWishes(criteria)) {
      setResults({ status: 'empty' });
      return;
    }
    let cancelled = false;
    setResults({ status: 'loading' });
    api
      .recommend(criteria)
      .then((items) => !cancelled && setResults({ status: 'done', items }))
      .catch((e: Error) => !cancelled && setResults({ status: 'error', message: e.message }));
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [paramsKey]);

  function submit(e: FormEvent) {
    e.preventDefault();
    if (!hasWishes(form)) {
      setFormError(EMPTY_FORM_ERROR);
      return;
    }
    if (form.yearFrom != null && form.yearTo != null && form.yearFrom > form.yearTo) {
      setFormError('Год «с» не может быть больше года «по».');
      return;
    }
    setFormError(null);
    const next = criteriaToParams(form);
    next.set('submitted', '1');
    setParams(next);
  }

  function reset() {
    setForm(EMPTY_CRITERIA);
    setFormError(null);
    setParams(new URLSearchParams());
  }

  const update = (patch: Partial<Criteria>) => {
    setForm((f) => ({ ...f, ...patch }));
    setFormError(null);
  };

  const d = dictionaries.data;

  return (
    <div>
      <PageHeader
        title="Что посмотреть?"
        subtitle="Отметьте настроение, жанры и тип — подбор начислит баллы за каждое совпадение."
      />

      <div className="grid items-start gap-8 lg:grid-cols-[2fr_3fr]">
        {/* Форма */}
        <form onSubmit={submit} className="card flex flex-col gap-7 p-6 lg:sticky lg:top-24" aria-label="Параметры подбора">
          {dictionaries.error && <ErrorBox>{dictionaries.error}</ErrorBox>}

          <fieldset className="flex flex-col gap-3">
            <legend className="label">
              Настроение <span className="text-accent">+{MOOD_POINTS}</span>
            </legend>
            <div className="flex flex-wrap gap-2">
              <Chip active={form.moodId == null} onClick={() => update({ moodId: null })}>
                любое
              </Chip>
              {d?.moods.map((m) => (
                <Chip key={m.id} active={form.moodId === m.id} onClick={() => update({ moodId: m.id })}>
                  {moodEmoji(m.name)} {m.name}
                </Chip>
              ))}
              {!d && dictionaries.loading && <Skeleton className="h-9 w-full" />}
            </div>
          </fieldset>

          <fieldset className="flex flex-col gap-3">
            <legend className="label">
              Жанры <span className="text-accent">+{GENRE_POINTS}</span>{' '}
              <span className="font-normal">— подходит любой из отмеченных</span>
            </legend>
            <div className="flex flex-wrap gap-2">
              {d?.genres.map((g) => (
                <Chip
                  key={g.id}
                  active={form.genreIds.includes(g.id)}
                  onClick={() => update({ genreIds: toggle(form.genreIds, g.id) })}
                >
                  {g.name}
                </Chip>
              ))}
              {!d && dictionaries.loading && <Skeleton className="h-20 w-full" />}
            </div>
          </fieldset>

          <fieldset className="flex flex-col gap-3">
            <legend className="label">
              Тип <span className="text-accent">+{TYPE_POINTS}</span>
            </legend>
            <div className="flex flex-wrap gap-2">
              {d?.types.map((t) => (
                <Chip
                  key={t.id}
                  active={form.typeIds.includes(t.id)}
                  onClick={() => update({ typeIds: toggle(form.typeIds, t.id) })}
                >
                  {t.name}
                </Chip>
              ))}
            </div>
          </fieldset>

          <fieldset className="flex flex-col gap-3">
            <legend className="label">Годы выпуска (необязательно)</legend>
            <div className="grid grid-cols-2 gap-3">
              <input
                type="number"
                className="field"
                placeholder="с"
                aria-label="Год с"
                min={1888}
                max={2100}
                value={form.yearFrom ?? ''}
                onChange={(e) => update({ yearFrom: yearValue(e.target.value) })}
              />
              <input
                type="number"
                className="field"
                placeholder="по"
                aria-label="Год по"
                min={1888}
                max={2100}
                value={form.yearTo ?? ''}
                onChange={(e) => update({ yearTo: yearValue(e.target.value) })}
              />
            </div>
          </fieldset>

          {formError && <ErrorBox>{formError}</ErrorBox>}

          <div className="flex flex-col gap-3 sm:flex-row">
            <Button type="submit" className="flex-1">
              Подобрать
            </Button>
            <Button variant="secondary" onClick={reset}>
              Сбросить
            </Button>
          </div>
        </form>

        {/* Результаты */}
        <section aria-label="Результаты подбора" aria-live="polite" className="flex flex-col gap-4">
          <ResultsBlock results={results} />
        </section>
      </div>
    </div>
  );
}

function ResultsBlock({ results }: { results: Results }) {
  switch (results.status) {
    case 'empty':
      return (
        <div className="card flex flex-col gap-5 p-8">
          <div className="text-4xl" aria-hidden>
            🎟️
          </div>
          <h2 className="text-xl font-semibold">Как работает подбор</h2>
          <ol className="flex flex-col gap-3 text-muted">
            {[
              <>
                За совпадение <b className="text-fg">настроения</b> фильм получает <b className="text-accent">+3</b> балла.
              </>,
              <>
                За любой из отмеченных <b className="text-fg">жанров</b> — <b className="text-accent">+2</b>.
              </>,
              <>
                За <b className="text-fg">тип</b> (фильм, сериал, мультфильм) — <b className="text-accent">+1</b>.
              </>,
              <>
                Показываются до {MAX_RESULTS} фильмов с баллом больше нуля; при равных баллах выше тот, у кого рейтинг
                выше. Годы — фильтр: остальные фильмы не предлагаются.
              </>,
            ].map((text, i) => (
              <li key={i} className="flex gap-3 leading-relaxed">
                <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-accent/15 text-xs font-bold text-accent">
                  {i + 1}
                </span>
                <span>{text}</span>
              </li>
            ))}
          </ol>
        </div>
      );
    case 'loading':
      return (
        <>
          <span className="sr-only">Подбираем…</span>
          {Array.from({ length: 4 }, (_, i) => (
            <div key={i} className="card flex gap-5 p-4" data-testid="result-skeleton">
              <Skeleton className="h-36 w-24 shrink-0" />
              <div className="flex flex-1 flex-col gap-3 py-1">
                <Skeleton className="h-5 w-1/2" />
                <Skeleton className="h-4 w-1/3" />
                <Skeleton className="h-12 w-full" />
              </div>
            </div>
          ))}
        </>
      );
    case 'error':
      return <ErrorBox>{results.message}</ErrorBox>;
    case 'done':
      if (results.items.length === 0) {
        return (
          <EmptyState icon="🤷" title="Ничего не нашлось">
            Попробуйте ослабить условия: выбрать другое настроение, добавить жанров или убрать годы.
          </EmptyState>
        );
      }
      return (
        <>
          <p className="text-sm text-muted">Найдено: {results.items.length}</p>
          {results.items.map((r, i) => (
            <ResultCard key={r.movie.id} item={r} place={i + 1} />
          ))}
        </>
      );
  }
}

function ResultCard({ item, place }: { item: Recommendation; place: number }) {
  const { movie } = item;
  return (
    <article
      className="card animate-fade-up flex gap-4 p-4 sm:gap-5"
      style={{ animationDelay: `${place * 40}ms` }}
      aria-label={movie.title}
    >
      <Link to={`/movies/${movie.id}`} className="shrink-0">
        <Poster url={movie.posterUrl} title={movie.title} className="h-36 w-24 rounded-xl" />
      </Link>
      <div className="flex min-w-0 flex-1 flex-col gap-2">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <Link to={`/movies/${movie.id}`} className="font-semibold hover:text-accent sm:text-lg">
            {movie.title}
          </Link>
          <span className="rounded-lg bg-accent px-2.5 py-1 text-xs font-bold text-black">Баллы: {item.score}</span>
        </div>
        <p className="flex flex-wrap items-center gap-2 text-sm text-muted">
          {movie.year} · {movie.type.name} · {movie.genres.map((g) => g.name).join(', ')} · <Rating value={movie.rating} />
        </p>
        <p className="line-clamp-2 text-sm leading-relaxed text-fg/75">{movie.description}</p>
        <div className="mt-auto flex flex-wrap gap-1.5 pt-1">
          {item.moodMatched && <Badge tone="success">настроение +{MOOD_POINTS}</Badge>}
          {item.genreMatched && <Badge tone="success">жанр +{GENRE_POINTS}</Badge>}
          {item.typeMatched && <Badge tone="success">тип +{TYPE_POINTS}</Badge>}
        </div>
      </div>
    </article>
  );
}
