/** Ошибка API с текстом для пользователя. */
export class ApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

/** Событие на window: сервер ответил 401 — админка снова показывает форму входа. */
export const AUTH_REQUIRED_EVENT = 'cinerec:auth-required';

/** Токен CSRF, который Spring Security кладёт в cookie XSRF-TOKEN. */
function csrfToken(): string | null {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : null;
}

export async function http<T>(url: string, init?: RequestInit): Promise<T> {
  const method = init?.method ?? 'GET';
  const token = method === 'GET' ? null : csrfToken();
  const response = await fetch(url, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { 'X-XSRF-TOKEN': token } : {}),
      ...init?.headers,
    },
  });
  if (!response.ok) {
    let message = `Ошибка сервера (${response.status})`;
    try {
      const body = await response.json();
      if (body?.message) message = body.message;
    } catch {
      // тело ответа не JSON — оставляем общее сообщение
    }
    if (response.status === 401) window.dispatchEvent(new Event(AUTH_REQUIRED_EVENT));
    throw new ApiError(message, response.status);
  }
  return response.status === 204 ? (undefined as T) : response.json();
}

export const json = (body: unknown): RequestInit => ({ body: JSON.stringify(body) });
