import type { Api } from './types';
import { realApi } from './realApi';
import { mockApi } from './mock/mockApi';

/** Режим задаётся переменной VITE_API_MODE (см. .env.example): mock по умолчанию или real. */
export const apiMode: 'mock' | 'real' = import.meta.env.VITE_API_MODE === 'real' ? 'real' : 'mock';

export const api: Api = apiMode === 'real' ? realApi : mockApi;
export { ApiError } from './http';
