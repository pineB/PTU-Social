import axios, { type AxiosError } from 'axios';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  const stored = window.sessionStorage.getItem('ptu-social-auth-session');
  if (!stored) return config;

  try {
    const session: unknown = JSON.parse(stored);
    if (typeof session === 'object' && session !== null && 'accessToken' in session
      && typeof session.accessToken === 'string' && 'tokenType' in session && session.tokenType === 'Bearer') {
      config.headers.Authorization = `Bearer ${session.accessToken}`;
    }
  } catch {
    window.sessionStorage.removeItem('ptu-social-auth-session');
  }
  return config;
});

function getRateLimitMessage(error: AxiosError): string {
  const retryAfter = error.response?.headers['retry-after'];

  if (typeof retryAfter === 'string' && /^\d+$/.test(retryAfter)) {
    return `Слишком много запросов. Повторите через ${retryAfter} сек.`;
  }

  return 'Слишком много запросов. Пожалуйста, попробуйте позже.';
}

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const status = error.response?.status;
    const isLoginRequest = error.config?.url?.replace(/^\//, '').startsWith('auth/login') ?? false;
    const apiMessage = (error.response?.data as { message?: unknown } | undefined)?.message;
    const message = typeof apiMessage === 'string' ? apiMessage : undefined;
    const messages: Partial<Record<number, string>> = {
      401: 'Сессия истекла. Войдите в систему снова.',
      403: 'У вас недостаточно прав для этого действия.',
      429: getRateLimitMessage(error),
    };
    const errorMessage = message ?? (status ? messages[status] : undefined);

    if (!isLoginRequest && errorMessage && (status === 401 || status === 403 || status === 429)) {
      if (status === 401 && window.sessionStorage.getItem('ptu-social-auth-session')) {
        window.sessionStorage.removeItem('ptu-social-auth-session');
        window.dispatchEvent(new Event('app:auth-expired'));
      }
      window.dispatchEvent(
        new CustomEvent('app:http-error', {
          detail: { status, message: errorMessage },
        }),
      );
    }

    return Promise.reject(new Error(errorMessage ?? 'Не удалось выполнить запрос. Проверьте данные и попробуйте ещё раз.', {
      cause: error,
    }));
  },
);

export default apiClient;
