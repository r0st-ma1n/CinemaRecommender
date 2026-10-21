import type { ChatContext, ChatReply, ChatRequest, Criteria, DictionaryItem, Movie } from '../../types';
import { EMPTY_CRITERIA, hasWishes } from '../../lib/criteria';
import { recommend } from '../../lib/recommend';

/**
 * Упрощённый бот для режима mock. Настоящий ChatService на бэкенде определяет
 * настроение моделью Weka; здесь — ключевые слова, но формат ответа и работа с контекстом те же.
 */
const PAGE_SIZE = 5;

const MOOD_WORDS: Record<string, string[]> = {
  весёлое: ['посмеяться', 'смешн', 'весел', 'весёл', 'поржать', 'развеселит', 'улыбн'],
  уютное: ['уютн', 'устал', 'расслаб', 'спокойн', 'тёпл', 'тепл', 'вечер с', 'отдохнуть'],
  напряжённое: ['напряж', 'страшн', 'пощекотать', 'адреналин', 'держит в напряжении', 'захватывающ'],
  грустное: ['грустн', 'поплакать', 'печальн', 'тоскл', 'плакать'],
  вдохновляющее: ['вдохнов', 'мотивир', 'поверить в себя', 'воодушев'],
  мрачное: ['мрачн', 'тёмн', 'темн', 'жёстк', 'жестк'],
};

const GENRE_WORDS: Record<string, string[]> = {
  драма: ['драм'],
  комедия: ['комеди'],
  боевик: ['боевик', 'экшн'],
  триллер: ['триллер'],
  фантастика: ['фантастик', 'космос', 'будущ'],
  фэнтези: ['фэнтези', 'волшеб', 'магия', 'магией'],
  ужасы: ['ужас', 'хоррор'],
  мелодрама: ['мелодрам', 'романтик', 'про любовь'],
  приключения: ['приключ'],
  детектив: ['детектив', 'расследован'],
  криминал: ['криминал', 'мафи', 'гангстер'],
  семейный: ['семейн', 'с детьми', 'с ребёнком'],
  исторический: ['историческ'],
  военный: ['военн', 'про войну'],
  биография: ['биограф', 'реальн'],
  мюзикл: ['мюзикл'],
};

const TYPE_WORDS: Record<string, RegExp> = {
  мультфильм: /мульт/,
  сериал: /сериал/,
  фильм: /(^|[^а-яё])фильм/,
};

interface Dictionaries {
  types: DictionaryItem[];
  genres: DictionaryItem[];
  moods: DictionaryItem[];
}

const idOf = (items: DictionaryItem[], name: string) => items.find((i) => i.name === name)?.id;

/** Слова сразу после «не» / «без» считаются исключением: «не ужасы», «без сериалов». */
const negated = (text: string, stem: string) => new RegExp(`(не|без)\\s+${stem}`).test(text);

function years(text: string): Pick<Criteria, 'yearFrom' | 'yearTo'> | null {
  const decade = text.match(/(\d0)-?х/);
  if (decade) {
    const d = Number(decade[1]);
    const from = d >= 30 ? 1900 + d : 2000 + d;
    return { yearFrom: from, yearTo: from + 9 };
  }
  const after = text.match(/(после|с|начиная с)\s+(\d{4})/);
  if (after) return { yearFrom: Number(after[2]), yearTo: null };
  const before = text.match(/до\s+(\d{4})/);
  if (before) return { yearFrom: null, yearTo: Number(before[1]) };
  if (/(нов(ое|ый|ые|енькое)|свеж)/.test(text)) return { yearFrom: 2010, yearTo: null };
  if (/(стар(ое|ый|ые|енькое)|классик)/.test(text)) return { yearFrom: null, yearTo: 1990 };
  return null;
}

function parse(text: string, d: Dictionaries): Criteria {
  const c: Criteria = structuredClone(EMPTY_CRITERIA);
  for (const [mood, words] of Object.entries(MOOD_WORDS)) {
    if (c.moodId == null && words.some((w) => text.includes(w))) c.moodId = idOf(d.moods, mood) ?? null;
  }
  for (const [genre, stems] of Object.entries(GENRE_WORDS)) {
    const id = idOf(d.genres, genre);
    if (id == null) continue;
    for (const stem of stems) {
      if (!text.includes(stem)) continue;
      (negated(text, stem) ? c.excludedGenreIds : c.genreIds).push(id);
      break;
    }
  }
  for (const [type, re] of Object.entries(TYPE_WORDS)) {
    const id = idOf(d.types, type);
    if (id == null || !re.test(text)) continue;
    const stem = re.source.replace(/^\(\^\|\[\^а-яё\]\)/, '');
    (negated(text, stem) ? c.excludedTypeIds : c.typeIds).push(id);
  }
  Object.assign(c, years(text));
  return c;
}

function describe(c: Criteria, d: Dictionaries): string {
  const names = (ids: number[], items: DictionaryItem[]) =>
    ids.map((id) => items.find((i) => i.id === id)?.name).filter(Boolean).join(', ');
  const parts: string[] = [];
  if (c.moodId != null) parts.push(`настроение — ${names([c.moodId], d.moods)}`);
  if (c.genreIds.length) parts.push(`жанр — ${names(c.genreIds, d.genres)}`);
  if (c.typeIds.length) parts.push(`тип — ${names(c.typeIds, d.types)}`);
  if (c.excludedGenreIds.length || c.excludedTypeIds.length) {
    parts.push(`без: ${[names(c.excludedGenreIds, d.genres), names(c.excludedTypeIds, d.types)].filter(Boolean).join(', ')}`);
  }
  if (c.yearFrom != null || c.yearTo != null) parts.push(`годы — ${c.yearFrom ?? '…'}–${c.yearTo ?? '…'}`);
  return parts.join('; ');
}

function answer(criteria: Criteria, shown: number[], movies: Movie[], d: Dictionaries, more: boolean): ChatReply {
  const found = recommend(movies, criteria, shown, PAGE_SIZE).map((r) => r.movie);
  if (found.length === 0) {
    return {
      text: more
        ? 'Больше ничего подходящего не нашлось.\nПопробуйте изменить запрос.'
        : `Понял так: ${describe(criteria, d)}.\nНо подходящих фильмов не нашлось — попробуйте ослабить условия.`,
      movies: [],
      context: null,
    };
  }
  const context: ChatContext = { criteria, shownMovieIds: [...shown, ...found.map((m) => m.id)] };
  return {
    text: more ? 'Вот ещё:' : `Понял так: ${describe(criteria, d)}.\nВот что могу предложить:`,
    movies: found.map((m) => ({
      id: m.id,
      title: m.title,
      year: m.year,
      type: m.type.name,
      rating: m.rating,
      posterUrl: m.posterUrl,
    })),
    context,
  };
}

export function mockChatReply(request: ChatRequest, movies: Movie[], d: Dictionaries): ChatReply {
  const text = request.message.trim().toLowerCase();
  const ctx = request.context ?? null;

  if (!text) return { text: 'Напишите, что хочется посмотреть.', movies: [], context: null };
  if (/^(привет|здравствуй|добрый|хай)/.test(text)) {
    return {
      text: 'Привет! Расскажите, какое у вас настроение или что хочется посмотреть.\nНапример: «хочу посмеяться» или «сериал из 90-х».',
      movies: [],
      context: null,
    };
  }
  if (/^(спасибо|благодарю|спс)/.test(text)) return { text: 'Пожалуйста! Хорошего просмотра 🍿', movies: [], context: null };

  if (ctx && /^(ещё|еще|другие|дальше|больше)(?![а-яё])/.test(text)) {
    return answer(ctx.criteria, ctx.shownMovieIds, movies, d, true);
  }

  const parsed = parse(text, d);
  const refining = ctx != null && /^(а|только|лучше)\s/.test(text);
  if (refining) {
    // «а сериал?» — меняется только то, что названо, остальное берётся из прошлого запроса
    const prev = ctx.criteria;
    const merged: Criteria = {
      moodId: parsed.moodId ?? prev.moodId,
      genreIds: parsed.genreIds.length ? parsed.genreIds : prev.genreIds,
      typeIds: parsed.typeIds.length ? parsed.typeIds : prev.typeIds,
      excludedGenreIds: [...prev.excludedGenreIds, ...parsed.excludedGenreIds],
      excludedTypeIds: [...prev.excludedTypeIds, ...parsed.excludedTypeIds],
      yearFrom: parsed.yearFrom ?? prev.yearFrom,
      yearTo: parsed.yearTo ?? prev.yearTo,
    };
    return answer(merged, [], movies, d, false);
  }

  if (!hasWishes(parsed)) {
    return {
      text: 'Не совсем понял. Опишите настроение, жанр или тип.\nНапример: «устал после работы, хочу мультик».',
      movies: [],
      context: null,
    };
  }
  return answer(parsed, [], movies, d, false);
}
