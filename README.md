# ПТУ-Сошиал

Университетская социально-образовательная платформа: учебные задания и их проверка, академический рейтинг,
мероприятия и отзывы, внутренняя валюта, батл-пасс с кейсами и косметикой, обращения в учебную часть.

MVP по ТЗ 2.1: Spring Boot 3.5 + PostgreSQL 17 + Redis 7 + Mailpit, SPA на React 19 + MUI 7,
всё поднимается одной командой Docker Compose.

## Возможности по ролям

| Роль | Что умеет |
| --- | --- |
| Студент | задания и решения, оценки, академический рейтинг, БОА/БУА, батл-пасс (уровни, free/premium награды), кейсы и инвентарь косметики, мероприятия и отзывы, справки, заявка на вступление в группу |
| Преподаватель | создание заданий, проверка решений (принять / вернуть на доработку), закреплённые дисциплины и группы, заявки на вступление в группу, расписание |
| Организатор | индивидуальные и групповые мероприятия, регистрация и подтверждение активности, отзывы участников |
| Учебная часть | приглашения преподавателей, дисциплины и назначения, заявки на регистрацию студентов, справки, модерация отзывов, расписание |
| Администратор | кейсы, каталог наград пасса, сезоны |

Староста — не отдельная роль, а признак `isGroupLeader` в профиле студента (подтверждение заявок группы,
регистрация группы на коллективные мероприятия).

## Стек

- **Backend:** Java 21, Spring Boot 3.5 (Web, Security, Data JPA, Validation, Mail), Lombok, Flyway (V1–V19), `ddl-auto=validate`.
- **Хранилища:** PostgreSQL 17 (данные), Redis 7 (коды подтверждения email, rate limiting).
- **Frontend:** React 19, TypeScript, Vite 7, MUI 7, axios, react-router 7.
- **Почта:** Mailpit (SMTP + веб-интерфейс) для локальной разработки.
- **Инфраструктура:** Docker Compose, Nginx (SPA + reverse proxy на `/api`).

## Архитектура

```
браузер ──> nginx :80 ──┬─ /            SPA (статика React)
                        └─ /api/**      proxy ──> backend :8080
                                                     ├─ PostgreSQL  :5432
                                                     ├─ Redis       :6379
                                                     └─ Mailpit SMTP:1025 / UI:8025
```

Сервисы Compose:

| Сервис | Образ / сборка | Порты | Назначение |
| --- | --- | --- | --- |
| `nginx` | `nginx/Dockerfile` (собирает SPA) | `${HTTP_PORT:-80}` | статика фронтенда, proxy `/api`, `/api/actuator/health` |
| `backend` | `backend/Dockerfile` (Maven → JRE 21) | внутренний 8080 | REST API, миграции Flyway, сидеры dev-данных |
| `postgres` | `postgres:17-alpine` | `${DB_PORT:-5432}` | основная БД, том `pgdata` |
| `redis` | `redis:7-alpine` | `${REDIS_PORT:-6379}` | коды email, счётчики rate limit, том `redisdata` |
| `mailpit` | `axllent/mailpit:v1.32` | `8025` (UI), `1025` (SMTP) | перехват писем в dev |

## Быстрый старт

Требования: Docker Engine/Desktop с Compose v2, ~4 ГБ RAM, свободные порты 80, 5432, 6379, 8025.

```bash
cp .env.example .env          # при необходимости поправьте пароли/порты
docker compose up -d --build  # сборка образов и запуск всех сервисов
docker compose ps             # дождитесь статуса healthy у backend/nginx
```

Адреса:

- приложение — <http://localhost>
- Mailpit (все письма: подтверждение email, инвайты преподавателей) — <http://localhost:8025>
- проверка API — <http://localhost/api/actuator/health>

Первый запуск автоматически применяет миграции и загружает демо-данные (профиль `dev`).
Остановка: `docker compose down`. Полный сброс с пересозданием БД и повторным сидом:
`docker compose down -v && docker compose up -d --build`.

## Демо-данные

Загружаются только в профиле `dev` двумя сидерами: `DevDataSeeder` (учебная часть, администратор)
и `DevDemoDataSeeder` (полный демонстрационный набор, идемпотентен).

Пароль студентов, преподавателей и организатора — `demo12345`.

При переключении между аккаунтами с одного IP срабатывает лимит логина (5 / минуту): при `429`
подождите минуту либо поднимите `RATE_LIMIT_LOGIN_REQUESTS` (например, до `50`) в `.env`.

| Роль | Email | Пароль | Что показать |
| --- | --- | --- | --- |
| Студент-староста ИС-21 | `student@ptu.local` | `demo12345` | пасс 50 уровня (премиум), 14 кейсов, 15 предметов косметики, задания, справки, отзывы |
| Староста ИС-22 | `student7@ptu.local` | `demo12345` | рейтинг 640, заявка на вступление в группу у преподавателя |
| Студент с премиум-пассом | `student25@ptu.local` | `demo12345` | premium-дорожка наград |
| Студент ЭК-21 | `student31@ptu.local` | `demo12345` | экономические дисциплины, мероприятия |
| Студент БД-21 | `student43@ptu.local` | `demo12345` | староста, отзывы на модерации |
| Преподаватель (IT) | `teacher@ptu.local` | `demo12345` | 8 заданий, решения в разных статусах, 8 групп |
| Преподаватель (экономика) | `teacher2@ptu.local` | `demo12345` | вторая нагрузка, экономические дисциплины |
| Организатор | `organizer@ptu.local` | `demo12345` | 7 мероприятий, регистрации групп, отзывы |
| Учебная часть | `office@ptu.local` | `office12345` | модерация отзывов, справки, заявки на регистрацию, инвайты преподавателей |
| Администратор | `admin@ptu.local` | `AdminPtu2026!` | кейсы, каталог наград пасса, сезоны |

Остальные студенты — `student2@ptu.local` … `student48@ptu.local` (пароль `demo12345`), по 6 человек в группе:

| Группа | Аккаунты | Староста |
| --- | --- | --- |
| ИС-21 | `student@ptu.local`, `student2` … `student6` | `student@ptu.local` |
| ИС-22 | `student7` … `student12` | `student7@ptu.local` |
| ИС-23 | `student13` … `student18` | `student13@ptu.local` |
| ПИ-21 | `student19` … `student24` | `student19@ptu.local` |
| ПИ-22 | `student25` … `student30` | `student25@ptu.local` |
| ЭК-21 | `student31` … `student36` | `student31@ptu.local` |
| ЭК-22 | `student37` … `student42` | `student37@ptu.local` |
| БД-21 | `student43` … `student48` | `student43@ptu.local` |

Премиум-пасс: `student@`, `student2@`, `student3@`, `student5@`, `student9@`, `student13@`,
`student17@`, `student21@`, `student25@`, `student29@`, `student33@`, `student37@`, `student41@`, `student45@`.

Что есть в демо-базе:

- 8 групп по 6 студентов, старосты, рейтинги 40…1250, кошельки 300…5000 жетонов и история транзакций;
- 8 кейсов (включая 7 тематических) с наградами всех типов косметики и рейтингами редкости;
- батл-пасс: 50 уровней × 2 дорожки (free — кейсы, premium — косметика), часть наград уже получена;
- 9 заданий и 9 решений в статусах «принято», «на доработку», «сдано»;
- расписание на 8 групп и изменения расписания;
- 7 мероприятий (индивидуальные и групповые), регистрации групп, участия, 15 отзывов
  (включая очередь модерации с нецензурным текстом);
- 8 справок во всех статусах (одна с PDF), 2 заявки на вступление в группу;
- 53 аккаунта, без «мусорных» записей от ручного тестирования.

## Конфигурация

Все настройки — через переменные окружения (`.env`, шаблон — `.env.example`).

| Переменная | По умолчанию | Назначение |
| --- | --- | --- |
| `HTTP_PORT` | `80` | внешний порт приложения |
| `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `ptu_social` / `ptu` / `ptu` | PostgreSQL |
| `REDIS_PASSWORD` | `redis` | пароль Redis |
| `JWT_SECRET` / `JWT_TTL` | dev-значение / `24h` | подпись и время жизни JWT |
| `MAIL_*`, `MAIL_FROM` | Mailpit | SMTP и адрес отправителя |
| `APP_BASE_URL` | `http://localhost` | базовый адрес для ссылок в письмах |
| `SEED_OFFICE_EMAIL` / `SEED_OFFICE_PASSWORD` | `office@ptu.local` / `office12345` | аккаунт учебной части в dev |
| `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD` | `admin@ptu.local` / `AdminPtu2026!` | администратор в dev |
| `ADMIN_BOOTSTRAP_EMAIL` / `ADMIN_BOOTSTRAP_PASSWORD` | — | создание администратора на пустой БД (прод) |
| `RATE_LIMIT_*` | см. ниже | лимиты API |
| `REVIEW_REWARD_AMOUNT` / `REVIEW_MONTHLY_LIMIT` | `5` / `5` | награда и месячный лимит за одобренные отзывы |

Rate limiting (Redis, fixed window 1 минута):

| Scope | Лимит по умолчанию |
| --- | --- |
| `login` (POST `/api/auth/login`, по IP) | 5 / 1 мин |
| `user` (мутации `/api/**` с JWT) | 120 / 1 мин |
| `user-read` (GET/HEAD с JWT) | 600 / 1 мин |
| `ip` (анонимные мутации) | 60 / 1 мин |
| `ip-read` (анонимные GET/HEAD) | 300 / 1 мин |

Чтение считается отдельно от изменений: загрузка страницы с десятками картинок не расходует лимит на действия.
Полное описание API и ошибок — `docs/spec/api.md`.

## Разработка

### Backend

```bash
cd backend
./mvnw compile                 # сборка
./mvnw test                    # интеграционные тесты (Testcontainers, нужен запущенный Docker)
./mvnw spring-boot:run         # запуск с локальными PostgreSQL/Redis/Mailpit
```

Профиль по умолчанию для Compose — `dev` (`SPRING_PROFILES_ACTIVE`), он включает демо-сидеры.

### Frontend

```bash
cd frontend
npm ci
npm run dev        # Vite dev-сервер, запросы идут на VITE_API_BASE_URL (по умолчанию /api)
npm run build      # tsc -b && vite build — используется в nginx/Dockerfile
```

Для разработки вне Docker задайте `VITE_API_BASE_URL=http://localhost/api` и добавьте origin dev-сервера
в `APP_CORS_ALLOWED_ORIGINS`. Штатный способ запуска — Docker Compose.

### Тесты

`cd backend && ./mvnw verify` — интеграционные тесты на Testcontainers (PostgreSQL + Redis):
регистрация и коды email, инвайты преподавателей, кошелёк (идемпотентность), rate limiting,
справки, задания, отзывы, планировщики.

## Структура репозитория

```
backend/            Spring Boot API, миграции Flyway, dev-сидеры, тесты
frontend/           React SPA (Vite, MUI)
nginx/              nginx.conf и Dockerfile сборки SPA
docs/
  information_about_project.md   техническое задание
  demo-data.md                   демо-аккаунты и данные
  agent-log.md                   журнал работ
  spec/api.md                    описание API и ошибок
  spec/AGENTS.md                 правила разработки
docker-compose.yml  все сервисы
.env.example        шаблон переменных окружения
```

Модули backend: `auth` (регистрация, JWT, коды email, пароли), `academic` (факультеты, группы, дисциплины,
инвайты, заявки), `workspace` (задания и решения), `campus` (пасс, кейсы, инвентарь, мероприятия, отзывы,
расписание, подтверждения групп), `certificate` (справки), `wallet` (валюта и транзакции), `community`
(рейтинги, лента), `review` (модерация отзывов), `common` (безопасность, ошибки), `config`, `dev`.

## Безопасность

- JWT (HS256, TTL 24 ч), BCrypt для паролей; самостоятельная регистрация преподавателей запрещена — только инвайт.
- Коды подтверждения email — в Redis с TTL, попытками и лимитами отправки (3 / 15 мин, resend 1 / 60 с).
- Rate limiting по IP и пользователю, `429` + `Retry-After`, fail-open при недоступности Redis.
- Единый формат ошибок API (`timestamp`, `status`, `error`, `message`, `path`) для 400/401/403/409/429/503.
- Nginx перезаписывает `X-Forwarded-For` адресом клиента, чтобы нельзя было подделать IP и обойти лимит логина.
- Секреты задаются только через переменные окружения; `.env` не коммитится.
