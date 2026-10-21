import type { MoodSuggestion } from '../../types';

/** Упрощённая замена модели Weka: настроение по ключевым словам описания и жанрам. */
const HINTS: Record<string, string[]> = {
  весёлое: ['смеш', 'весел', 'весёл', 'комеди', 'забав', 'шутк', 'приключ'],
  уютное: ['дом', 'семь', 'друж', 'тёпл', 'тепл', 'добр', 'семейн', 'мелодрам'],
  напряжённое: ['убий', 'погон', 'опасн', 'тайн', 'расслед', 'триллер', 'боевик', 'детектив'],
  грустное: ['смерт', 'потер', 'одинок', 'болезн', 'драм', 'войн'],
  вдохновляющее: ['мечт', 'побед', 'надежд', 'путь', 'свобод', 'биограф', 'достоинств'],
  мрачное: ['мафи', 'преступ', 'тьм', 'проклят', 'хаос', 'ужас', 'криминал'],
};

export function mockSuggestMoods(description: string, genreNames: string[]): MoodSuggestion[] {
  const text = `${description} ${genreNames.join(' ')}`.toLowerCase();
  const scores = Object.entries(HINTS).map(([mood, stems]) => ({
    mood,
    hits: stems.filter((s) => text.includes(s)).length + 0.3,
  }));
  const total = scores.reduce((sum, s) => sum + s.hits, 0);
  return scores
    .map((s) => ({ mood: s.mood, probability: Math.round((s.hits / total) * 100) / 100 }))
    .sort((a, b) => b.probability - a.probability)
    .slice(0, 2);
}
