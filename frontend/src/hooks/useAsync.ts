import { useCallback, useEffect, useState, type DependencyList } from 'react';

interface AsyncState<T> {
  data: T | undefined;
  loading: boolean;
  error: string | null;
  reload: () => void;
}

/** Загрузка данных при монтировании и при изменении зависимостей. */
export function useAsync<T>(load: () => Promise<T>, deps: DependencyList = []): AsyncState<T> {
  const [data, setData] = useState<T>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    load()
      .then((value) => !cancelled && setData(value))
      .catch((e: unknown) => !cancelled && setError(e instanceof Error ? e.message : 'Не удалось загрузить данные'))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, version]);

  const reload = useCallback(() => setVersion((v) => v + 1), []);
  return { data, loading, error, reload };
}
