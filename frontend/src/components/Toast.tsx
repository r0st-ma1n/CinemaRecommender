import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';
import { cx } from './ui';

type Tone = 'success' | 'error';
interface ToastItem {
  id: number;
  text: string;
  tone: Tone;
}

const ToastContext = createContext<(text: string, tone?: Tone) => void>(() => {});

/** Всплывающие уведомления «Сохранено» / «Ошибка» (как flash-сообщения в старой админке). */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([]);

  const show = useCallback((text: string, tone: Tone = 'success') => {
    const id = Date.now() + Math.random();
    setItems((list) => [...list, { id, text, tone }]);
    setTimeout(() => setItems((list) => list.filter((t) => t.id !== id)), 3500);
  }, []);

  return (
    <ToastContext.Provider value={show}>
      {children}
      <div className="pointer-events-none fixed bottom-4 right-4 z-[60] flex w-[calc(100%-2rem)] max-w-sm flex-col gap-2">
        {items.map((t) => (
          <div
            key={t.id}
            role="status"
            className={cx(
              'animate-fade-up card flex items-center gap-3 px-4 py-3 text-sm shadow-xl',
              t.tone === 'success' ? 'border-success/40' : 'border-danger/40',
            )}
          >
            <span aria-hidden>{t.tone === 'success' ? '✅' : '⚠️'}</span>
            {t.text}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export const useToast = () => useContext(ToastContext);
