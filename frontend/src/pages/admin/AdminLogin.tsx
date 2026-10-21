import { useState, type FormEvent } from 'react';
import { api, apiMode } from '../../api';
import { MOCK_ADMIN } from '../../api/mock/mockApi';
import { Button, ErrorBox } from '../../components/ui';
import type { AuthStatus } from '../../types';

/** Форма входа в админку; после успешного входа сообщает новый статус наверх. */
export function AdminLogin({ onLogin }: { onLogin: (status: AuthStatus) => void }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setSending(true);
    setError(null);
    try {
      onLogin(await api.login(username.trim(), password));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Не удалось войти');
      setSending(false);
    }
  }

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="mb-6 text-3xl font-bold tracking-tight">Вход в админку</h1>
      <form onSubmit={submit} className="card flex flex-col gap-4 p-6">
        <div>
          <label htmlFor="username" className="label">
            Логин
          </label>
          <input
            id="username"
            className="field"
            autoComplete="username"
            autoFocus
            required
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
        </div>
        <div>
          <label htmlFor="password" className="label">
            Пароль
          </label>
          <input
            id="password"
            type="password"
            className="field"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>
        {error && <ErrorBox>{error}</ErrorBox>}
        <Button type="submit" disabled={sending}>
          {sending ? 'Входим…' : 'Войти'}
        </Button>
        {apiMode === 'mock' && (
          <p className="text-center text-xs text-muted">
            Режим моков: {MOCK_ADMIN.username} / {MOCK_ADMIN.password}
          </p>
        )}
      </form>
    </div>
  );
}
