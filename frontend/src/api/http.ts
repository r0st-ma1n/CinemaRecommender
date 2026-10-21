/** Ошибка API с текстом для пользователя. */
export class ApiError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

export async function http<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  });
  if (!response.ok) {
    let message = `Ошибка сервера (${response.status})`;
    try {
      const body = await response.json();
      if (body?.message) message = body.message;
    } catch {
      // тело ответа не JSON — оставляем общее сообщение
    }
    throw new ApiError(message, response.status);
  }
  return response.status === 204 ? (undefined as T) : response.json();
}

export const json = (body: unknown): RequestInit => ({ body: JSON.stringify(body) });
