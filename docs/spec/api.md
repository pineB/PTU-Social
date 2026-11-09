# API (backend)

Базовый префикс: `/api`. Формат данных — JSON, кодировка UTF-8. Аутентификация — `Authorization: Bearer <JWT>`.

## Единый формат ошибок

Все ошибки (в т.ч. отданные до контроллера: 401, 403, 429) имеют одинаковое тело `ApiError`:

```json
{
  "timestamp": "2026-10-31T09:12:44.512Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Слишком много запросов, попробуйте позже",
  "path": "/api/auth/login"
}
```

| Поле | Тип | Описание |
| --- | --- | --- |
| `timestamp` | ISO-8601 (UTC) | Момент формирования ответа |
| `status` | int | HTTP-код, дублируется в теле |
| `error` | string | Стандартная причина (`Unauthorized`, `Forbidden`, `Too Many Requests`, …) |
| `message` | string | Человекочитаемое сообщение (рус.) |
| `path` | string | URI запроса |

Источники ошибок: `GlobalExceptionHandler` (бизнес-ошибки и валидация), `ApiErrorWriter` — для 401/403/429, отдаваемых фильтрами Spring Security.

### Коды

| Код | `error` | `message` | Когда |
| --- | --- | --- | --- |
| 400 | `Bad Request` | текст валидации (`field: reason`) | невалидное тело/параметры |
| 401 | `Unauthorized` | `Требуется аутентификация` | нет/просрочен JWT (`RestAuthenticationEntryPoint`) |
| 403 | `Forbidden` | `Недостаточно прав` | нет роли/прав (`RestAccessDeniedHandler`) |
| 409 | `Conflict` | `Запись уже существует или нарушает ограничения` | нарушение уникальности |
| 429 | `Too Many Requests` | `Слишком много запросов, попробуйте позже` | превышен rate limit |
| 503 | `Service Unavailable` | `Сервис временно недоступен, попробуйте позже` | недоступен Redis |

## Rate Limiting

Реализация: Redis, алгоритм **fixed window** (окно = 1 минута), ключ `rate_limit:{scope}:{identity}:{windowIndex}`.
Счётчик инкрементируется на каждый запрос; при превышении — `429` + заголовок `Retry-After`.

| Scope | Идентификатор | Лимит | По умолчанию |
| --- | --- | --- | --- |
| `login` (`POST /api/auth/login`) | IP клиента (`X-Forwarded-For`, иначе remote addr) | `RATE_LIMIT_LOGIN_REQUESTS` / `RATE_LIMIT_LOGIN_WINDOW` | 5 / 1m |
| `user` (любой `/api/**` с валидным JWT) | id пользователя | `RATE_LIMIT_AUTHENTICATED_*` | 120 / 1m |
| `ip` (анонимный `/api/**`) | IP клиента | `RATE_LIMIT_ANONYMOUS_*` | 60 / 1m |

- Вне `/api/**` (например, `/actuator/**`) лимиты не применяются.
- Отключение: `RATE_LIMIT_ENABLED=false`.
- При недоступности Redis лимитер **fail-open** (запрос пропускается, в лог — `warn`).
- Отправка кода подтверждения лимитируется отдельно в сервисе: 3 отправки / 15 мин на заявку, resend не чаще 1 / 60 с (`429`).

Пример ответа при превышении:

```
HTTP/1.1 429 Too Many Requests
Content-Type: application/json
Retry-After: 37
```

```json
{
  "timestamp": "2026-10-31T09:12:44.512Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Слишком много запросов, попробуйте позже",
  "path": "/api/auth/login"
}
```

`Retry-After` — число секунд до начала следующего окна (1..60 для минутного окна).

## Эндпоинты

### Публичные (`/api/auth/**`)

| Метод | Путь | Успех | Описание |
| --- | --- | --- | --- |
| POST | `/api/auth/registration-requests` | 201 `RegistrationRequestView` | заявка студента + отправка кода на email |
| POST | `/api/auth/registration-requests/{requestId}/email-code` | 202 | повторная отправка кода (лимиты: 60 c, 3/15 мин) |
| POST | `/api/auth/registration-requests/{requestId}/email-verify` | 200 `RegistrationRequestView` | проверка кода (5 попыток) |
| POST | `/api/auth/login` | 200 `TokenResponse` | вход, JWT 24h |
| GET | `/api/auth/teacher-invites/{token}` | 200 `TeacherInviteInfoResponse` | данные инвайта преподавателя |
| POST | `/api/auth/teacher-invites/{token}/accept` | 201 `TokenResponse` | активация аккаунта преподавателя (задаёт свой пароль) |
| POST | `/api/auth/password/forgot` | 202 | запрос кода сброса пароля; ответ всегда `202` (аккаунт не раскрывается) |
| POST | `/api/auth/password/reset` | 204 | сброс пароля по коду `{email, code, newPassword}` |

Письмо-приглашение ведёт на `{APP_BASE_URL}/teacher/activate?token=…` (страница `TeacherActivationPage`), где преподаватель задаёт пароль.

После `email-verify` заявка уходит на рассмотрение: письмо «Заявка на регистрацию» получают преподаватели группы с `ACTIVE`-назначением в текущем семестре (ссылка `{APP_BASE_URL}/teacher/students`) и староста группы (`{APP_BASE_URL}/student/group-confirmation`). Учебная часть письма не получает, но может обработать заявку через API как fallback.

Сброс пароля: код 6 цифр, TTL 10 мин, 5 попыток, 3 отправки/15 мин, интервал 60 c (общие лимиты `app.verification.*`), ключи Redis `verification:password-reset:*`. Неизвестный email и неверный код возвращают одинаковый `400` `«Неверный или просроченный код подтверждения»`; перебор попыток — `429`.

### Аутентифицированные (`/api/auth/**`)

| Метод | Путь | Успех | Описание |
| --- | --- | --- | --- |
| POST | `/api/auth/password/change` | 204 | `{currentPassword, newPassword}`; неверный текущий / совпадение с новым → `400` |

Пароль: 8–72 символа, минимум одна буква и одна цифра (как при регистрации/инвайте).

### Учебная часть

| Метод | Путь | Роль | Описание |
| --- | --- | --- | --- |
| POST | `/api/academic/teacher-invites` | `ACADEMIC_OFFICE` | создать инвайт преподавателя (email) |
| GET | `/api/academic/registration-requests` | `ACADEMIC_OFFICE`, `TEACHER`, `GROUP_LEADER` | заявки, доступные на одобрение |
| POST | `/api/academic/registration-requests/{requestId}/approve` | то же | одобрить заявку в группу |
| POST | `/api/academic/registration-requests/{requestId}/reject` | то же | отклонить заявку |

### Справки (`/api/certificates`)

Типы: `EDUCATION`, `INCOME`, `MILITARY`. Статусы: `PENDING`, `APPROVED`, `REJECTED`.
Доступ к методам закрыт `@PreAuthorize`; при неверной роли — `403` в формате `ApiError`.

| Метод | Путь | Роль | Успех | Описание |
| --- | --- | --- | --- | --- |
| POST | `/api/certificates` | `STUDENT` | 201 `CertificateRequestView` | создать заявку (статус `PENDING`) |
| GET | `/api/certificates/my` | `STUDENT` | 200 `CertificateRequestView[]` | только свои заявки |
| GET | `/api/certificates?status=&page=&size=` | `ACADEMIC_OFFICE` | 200 `Page<CertificateRequestView>` | все заявки, фильтр по статусу, сортировка по `createdAt` ASC |
| PATCH | `/api/certificates/{id}/status` | `ACADEMIC_OFFICE` | 200 `CertificateRequestView` | `APPROVED` / `REJECTED` |

Тело создания: `{"type":"EDUCATION"}`.
Тело смены статуса: `{"status":"REJECTED","rejectionReason":"причина"}` — для `REJECTED` причина обязательна (иначе `400`), для `APPROVED` игнорируется и сохраняется `null`.

```json
{
  "id": "3f1c...",
  "studentId": "9a21...",
  "studentName": "Иванов Иван",
  "type": "EDUCATION",
  "status": "PENDING",
  "rejectionReason": null,
  "createdAt": "2026-10-31T10:00:00Z",
  "updatedAt": "2026-10-31T10:00:00Z"
}
```

Ошибки: `400` — причина не указана / статус `PENDING` в теле PATCH / невалидный `type` или `status`; `403` — не та роль; `404` — заявка не найдена; `409` — заявка уже обработана (`статус != PENDING`).

### Прочее

| Метод | Путь | Описание |
| --- | --- | --- |
| GET | `/actuator/health` | liveness/readiness (без лимитов) |

## Ошибки входа

`POST /api/auth/login` при неверных данных → `401` с `message = "Неверный email или пароль"`; 6-я попытка в минуту с одного IP → `429`.
