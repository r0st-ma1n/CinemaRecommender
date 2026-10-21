/** Значок для настроения; для добавленных в админке — общий. */
export const MOOD_EMOJI: Record<string, string> = {
  весёлое: '😄',
  уютное: '☕',
  напряжённое: '😬',
  грустное: '🌧️',
  вдохновляющее: '✨',
  мрачное: '🌑',
};

export const moodEmoji = (name: string) => MOOD_EMOJI[name] ?? '🎞️';
