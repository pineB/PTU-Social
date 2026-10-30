export interface HttpErrorNotice {
  status: 401 | 403 | 429;
  message: string;
}

declare global {
  interface WindowEventMap {
    'app:http-error': CustomEvent<HttpErrorNotice>;
  }
}

export {};
