import axios, { type AxiosError } from 'axios';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
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
    const messages: Partial<Record<number, string>> = {
      401: 'Сессия истекла. Войдите в систему снова.',
      403: 'У вас недостаточно прав для этого действия.',
      429: getRateLimitMessage(error),
    };
    const message = status ? messages[status] : undefined;

    if (message && (status === 401 || status === 403 || status === 429)) {
      window.dispatchEvent(
        new CustomEvent('app:http-error', {
          detail: { status, message },
        }),
      );
    }

    return Promise.reject(error);
  },
);

export default apiClient;
