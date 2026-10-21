import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { Poster } from '../components/MovieCard';
import { Button, Chip, Rating, cx } from '../components/ui';
import type { ChatContext, ChatMovie } from '../types';

/** Как ChatController.MAX_MESSAGE_LENGTH на бэкенде. */
export const MAX_MESSAGE_LENGTH = 500;

export const SUGGESTIONS = [
  'хочу посмеяться',
  'устал после работы, хочу мультик',
  'что-нибудь страшное, но не ужасы',
  'сериал из 90-х',
  'ещё',
  'а сериал?',
];

interface Message {
  id: number;
  author: 'user' | 'bot';
  text: string;
  movies?: ChatMovie[];
}

const GREETING: Message = {
  id: 0,
  author: 'bot',
  text: 'Привет! Я подберу фильм, сериал или мультфильм.\nОпишите настроение или желание обычной фразой — например, «хочу посмеяться».',
};

let messageId = 1;

/**
 * Чат-бот. История и контекст (условия прошлого запроса) живут только на странице:
 * контекст из ответа отправляется со следующим сообщением — на этом работают «ещё» и «а сериал?».
 */
export function ChatPage() {
  const [messages, setMessages] = useState<Message[]>([GREETING]);
  const [input, setInput] = useState('');
  const [context, setContext] = useState<ChatContext | null>(null);
  const [sending, setSending] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView?.({ behavior: 'smooth', block: 'end' });
  }, [messages, sending]);

  async function send(text: string) {
    const message = text.trim();
    if (!message || sending || message.length > MAX_MESSAGE_LENGTH) return;
    setMessages((list) => [...list, { id: messageId++, author: 'user', text: message }]);
    setInput('');
    setSending(true);
    try {
      const reply = await api.sendChat({ message, context });
      if (reply.context) setContext(reply.context); // null — оставить прежний
      setMessages((list) => [...list, { id: messageId++, author: 'bot', text: reply.text, movies: reply.movies }]);
    } catch {
      setMessages((list) => [
        ...list,
        { id: messageId++, author: 'bot', text: 'Не получилось связаться с сервером. Попробуйте ещё раз.' },
      ]);
    } finally {
      setSending(false);
      inputRef.current?.focus();
    }
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    send(input);
  }

  function onKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    // Enter — отправить, Shift+Enter — новая строка
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      send(input);
    }
  }

  function restart() {
    setMessages([GREETING]);
    setContext(null);
    setInput('');
  }

  const tooLong = input.length > MAX_MESSAGE_LENGTH;

  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="flex flex-col gap-2">
          <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Чат с ботом</h1>
          <p className="text-muted">Напишите, что хочется посмотреть, — бот поймёт настроение, жанр, тип и годы.</p>
        </div>
        <Button variant="secondary" size="sm" onClick={restart}>
          ↺ Новый разговор
        </Button>
      </div>

      <div className="card flex h-[min(65vh,640px)] flex-col overflow-hidden">
        <div className="flex flex-1 flex-col gap-5 overflow-y-auto p-4 sm:p-6" role="log" aria-label="Переписка">
          {messages.map((m) => (
            <MessageBubble key={m.id} message={m} />
          ))}
          {sending && (
            <div className="flex items-center gap-2 text-sm text-muted" role="status">
              <span className="flex gap-1" aria-hidden>
                {[0, 150, 300].map((delay) => (
                  <span
                    key={delay}
                    className="h-2 w-2 animate-bounce rounded-full bg-muted"
                    style={{ animationDelay: `${delay}ms` }}
                  />
                ))}
              </span>
              бот печатает…
            </div>
          )}
          <div ref={bottomRef} />
        </div>

        <form onSubmit={onSubmit} className="flex flex-col gap-3 border-t border-line bg-surface-2/40 p-4">
          <div className="flex gap-2 overflow-x-auto pb-1" aria-label="Подсказки">
            {SUGGESTIONS.map((s) => (
              <Chip key={s} className="shrink-0" onClick={() => setInput(s)}>
                {s}
              </Chip>
            ))}
          </div>
          <div className="flex items-end gap-3">
            <div className="flex flex-1 flex-col gap-1">
              <textarea
                ref={inputRef}
                rows={2}
                className={cx('field resize-none', tooLong && 'border-danger focus:border-danger')}
                placeholder="Например: устал после работы, хочу мультик"
                aria-label="Сообщение"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={onKeyDown}
              />
              <span className={cx('self-end text-xs', tooLong ? 'text-danger' : 'text-muted')}>
                {input.length} / {MAX_MESSAGE_LENGTH}
              </span>
            </div>
            <Button type="submit" className="mb-6" disabled={sending || !input.trim() || tooLong}>
              Отправить
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

function MessageBubble({ message }: { message: Message }) {
  const mine = message.author === 'user';
  return (
    <div className={cx('animate-fade-up flex flex-col gap-3', mine ? 'items-end' : 'items-start')}>
      <div
        className={cx(
          'max-w-[85%] whitespace-pre-line rounded-2xl px-4 py-3 leading-relaxed',
          mine ? 'rounded-br-md bg-accent text-black' : 'rounded-bl-md border border-line bg-surface-2',
        )}
      >
        {message.text}
      </div>
      {message.movies && message.movies.length > 0 && (
        <div className="grid w-full grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-5">
          {message.movies.map((movie) => (
            <Link
              key={movie.id}
              to={`/movies/${movie.id}`}
              className="card group flex flex-col overflow-hidden transition-colors hover:border-accent/50"
            >
              <Poster url={movie.posterUrl} title={movie.title} className="aspect-[3/4]" />
              <div className="flex flex-col gap-1 p-3">
                <span className="text-sm font-semibold leading-snug group-hover:text-accent">{movie.title}</span>
                <span className="flex items-center justify-between text-xs text-muted">
                  {movie.year} · {movie.type}
                  <Rating value={movie.rating} />
                </span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
