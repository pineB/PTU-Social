# Agent Log

Формат: дата → запрос → решения → шаги (кратко).

## 2026-10-30 — Онбординг агента
- Запрос: изучить `docs/spec/AGENTS.md`, ТЗ `docs/information_about_project.md`, вести лаконичные логи.
- Решения:
  - Правила AGENTS.md держать в постоянном контексте: роль Senior Java Backend/DevOps; зона — инфраструктура, безопасность, регистрация, кошелёк, справки.
  - ТЗ читать по необходимости (модули: аккаунты, роли, преподаватели/дисциплины, задания+АР, мероприятия, БОА/БУА, кошелёк, батл-пасс, отзывы/модерация, рейтинги).
  - Логи — только суть, без пересказа кода.
- Шаги:
  1. Прочитаны оба документа, зафиксированы ключевые правила.
  2. Осмотр репозитория: `backend/` — пустой Spring Boot скелет (только `BackendApplication`, `application.properties`, пустые `db/migration`), `dockercompose.yml` пуст, `docs/` с ТЗ.
  3. Создан этот лог.
- Статус: проект на стадии инициализации; следующий шаг — каркас инфраструктуры (Docker Compose, БД/Redis, Flyway, security).

## 2026-10-30 — Настройка Spring Boot, Flyway, Docker Compose
- Запрос: завершить настройку Spring Boot; подготовить Flyway и `docker-compose.yml` (PostgreSQL, Redis) под бэкенд.
- Решения (согласованы):
  - Spring Boot 3.5.16 (не 4.x); зависимости переведены на имена Boot 3 (`starter-web`, `flyway-core` + `flyway-database-postgresql`, `starter-test`).
  - Compose-файл: `docker-compose.yml` в корне, старый пустой `dockercompose.yml` удалён.
  - `V1` — только расширения (`pgcrypto`, `citext`); доменные таблицы с `V2` при старте auth.
  - Testcontainers-зависимости добавлены сразу, smoke-тест только на контекст.
  - Схема управляется только Flyway: `jpa.ddl-auto=validate`.
- Шаги:
  1. `pom.xml`: Boot 3.5.16, добавлены `data-redis`, `mail`, `spring-boot-testcontainers`, `testcontainers:junit-jupiter`, `testcontainers:postgresql`.
  2. `application.properties`: env-driven datasource/redis/mail (`DB_*`, `REDIS_*`, `MAIL_*`), Flyway, JPA validate, actuator health+probes.
  3. `db/migration/V1__init_extensions.sql`.
  4. `backend/Dockerfile` (multi-stage maven→JRE21-alpine, non-root) + `.dockerignore`.
  5. `docker-compose.yml`: postgres 17-alpine, redis 7-alpine (AOF+пароль), mailpit v1.32, backend; healthchecks, volumes, `.env`-переменные. Добавлен `.env.example`, `.env` в `.gitignore`.
  6. `BackendApplicationTests`: `@SpringBootTest` + PostgreSQL Testcontainer (`@ServiceConnection`).
- Проверка: `docker compose config` OK; `mvnw package` OK; `mvnw test` — 1 тест, Flyway V1 применён; `docker compose up -d --build` — все 4 сервиса healthy, `/actuator/health` = UP, миграция и расширения в БД подтверждены, Redis PING OK.
- Замечание: `docs/` в `.gitignore` — логи и ТЗ не версионируются (решить отдельно).

## 2026-10-30 — Регистрация, инвайты преподавателей, JWT
- Запрос: создание преподавателей Учебной частью (email-инвайт), регистрация студента (код в Redis: TTL 10м, 5 попыток, 3 отправки/15м, resend 60с), подтверждение email + одобрение группы (препод/староста), BCrypt, JWT access 24h, dev-seeder.
- Решения:
  - `V2__auth_schema.sql`: справочники, `users`, профили, `teacher_assignments`, `teacher_invites`, `registration_requests`; role/status — varchar+CHECK; пароль в заявке с TTL 7 дней и затиранием при reject; partial unique index на активный email; `semesters.is_current` + partial unique.
  - Одобрение: препод по своим группам текущего семестра, староста — своя группа, fallback — ACADEMIC_OFFICE; pessimistic lock.
  - Код в Redis: `email_verification:{id}` (salt+SHA-256, TTL 10м) + ключи sends/attempts/resend; ошибки Redis → 503.
  - JWT HS256, BCrypt(12), единый JSON ошибок; инвайт-токен хранится хэшем.
- Шаги: домен/репозитории → security/JWT → mail → сервисы регистрации/верификации/инвайтов/одобрения → контроллеры → scheduler → dev-seeder → IT-тесты.
- Проверка: `./mvnw verify` — 17 тестов зелёные (Postgres+Redis Testcontainers); dev-стек: регистрация → код из Mailpit → verify → одобрение → логин студента; `/actuator/health` UP, сидер идемпотентен.
- Исправлено по ходу: IT не видны в `mvn test` (failsafe → гонять `verify`); Testcontainers поднимать singleton-ом, иначе кэш контекста держит старые порты; FK дисциплины в тесте; `.signWith(key)` выбирал HS384 → явный HS256.

## 2026-10-31 — Rate Limiting + единый JSON для 401/403/429
- Запрос: лимиты из ТЗ (120/мин авторизованные, 5/мин логин, 3 отправки кода/15 мин), единый `ApiError` для 401/403/429, IT-тесты, обновить `docs/spec/api.md`.
- Решения:
  - Fixed window в Redis: ключ `rate_limit:{scope}:{identity}:{windowIndex}` (`INCR` + `EXPIRE`), fail-open при недоступности Redis.
  - Скоупы: `login` — IP, `user` — id из JWT, `ip` — аноним; всё вне `/api/**` не лимитируется. Лимиты в `app.rate-limit.*` (env `RATE_LIMIT_*`).
  - `RateLimitFilter` — не бин, а фильтр внутри security-цепочки (иначе Boot дублирует его как сервлет-фильтр); стоит после `JwtAuthenticationFilter`.
  - Единый писатель `ApiErrorWriter` для 429 (`RateLimitFilter`), 401 (`RestAuthenticationEntryPoint`), 403 (`RestAccessDeniedHandler`); при 429 — заголовок `Retry-After`.
  - Отложено: «3 заявки/час» и лимиты на задания/мероприятия/отзывы.
- Шаги: `AppProperties.RateLimit/Limit` + `application.properties` + `.env.example` → `RateLimiter` → `RateLimitFilter` → `ApiErrorWriter`/entry point/denied handler → `SecurityConfig` → `RateLimitIT` → `docs/spec/api.md`.
- Проверка: `./mvnw verify` — 21 тест зелёный (unit 1 + IT 20), в т.ч. `RateLimitIT`: 6-й логин → 429 с `Retry-After` и полным `ApiError`, 401/403 в том же формате, 121-й запрос авторизованного → 429.
- Замечание: `docs/spec/api.md` отсутствовал — создан (формат ошибок, таблица лимитов, перечень эндпоинтов).

## 2026-10-31 — Справки (certificate requests)
- Запрос: модуль заявок на справки: `V3`, сущность/репозиторий, DTO/сервис, контроллер с `@PreAuthorize`, IT-тесты, docs.
- Решения (согласованы): `type`/`status` — varchar+CHECK; индексы по `student_id`, `status`; `ON DELETE CASCADE`; причина отклонения обязательна (400), при `APPROVED` обнуляется; смена статуса только из `PENDING` (иначе 409); пагинация общего списка (20); `studentName` через `@EntityGraph`/`join fetch` — без N+1.
- Шаги: `V3__certificate_requests.sql` → `certificate/domain` (`CertificateRequest` с `@CreationTimestamp/@UpdateTimestamp`) → `CertificateRequestRepository` (`@EntityGraph`, `findWithStudentById`) → DTO (`CreateCertificateRequest`, `UpdateCertificateStatusRequest`, `CertificateRequestView`) → `CertificateRequestService` → `CertificateController` → `@EnableMethodSecurity` + обработчики `AccessDeniedException` (403) и `HttpMessageNotReadableException` (400) в `GlobalExceptionHandler` → `CertificateRequestIT` → `docs/spec/api.md`.
- Проверка: `./mvnw verify` — 30 тестов зелёные (unit 1 + IT 29): старые 21 не сломались, новый `CertificateRequestIT` — 9 (создание/свои, чужой не видит, одобрение, отказ с причиной, отказ без причины 400, студент 403 на PATCH/GET, фильтр `?status`, повторная обработка 409).
- Найдено по ходу: `@EnableMethodSecurity` отсутствовала — `@PreAuthorize` не применялся; catch-all `Exception` перехватывал `AccessDeniedException` → 500 вместо 403.

## 2026-10-31 — Nginx как единая точка входа
- Запрос: reverse proxy + статика в compose, порт бэкенда наружу не публиковать, healthcheck, заглушка фронта.
- Решения:
  - `nginx/nginx.conf`: сервер :80, `prov /api/` → `http://backend:8080` без rewrite (контроллеры уже с `/api`); статика `root /usr/share/nginx/html` + `try_files ... /index.html`; `client_max_body_size 10M`.
  - `/api/actuator/health` → `backend:8080/actuator/health` (у бэкенда actuator не под `/api`), иначе healthcheck 404.
  - `X-Forwarded-For $remote_addr` (перезапись, не `$proxy_add_x_forwarded_for`) — бэкенд берёт первый адрес, иначе подделка IP обходила бы лимит логина.
  - compose: сервис `nginx:alpine`, `${HTTP_PORT:-80}:80`, volume конфига и `frontend/dist`; у `backend` убран `ports 8080`, оставлен `expose`; `depends_on: backend: service_healthy`.
  - healthcheck nginx — busybox `wget` (в `nginx:alpine` нет curl, лишний пакет не тащим).
  - `frontend/dist/index.html` — заглушка «Frontend is building...» (папки ещё нет).
- Шаги: конфиг → compose → `.env.example` (`BACKEND_PORT` → `HTTP_PORT`) → заглушка → запуск → проверки → лог → коммит.
- Проверка: `docker compose up -d --build` — все сервисы healthy, порт 8080 снаружи закрыт (connection refused); `curl localhost/api/actuator/health` → `{"status":"UP"}`; `curl localhost/` → заглушка; `Authorization` проходит через прокси (200 на `/api/certificates` с JWT); 6 логинов через `localhost/api/auth/login` → 6-й `429` + `Retry-After`, при этом ротация поддельного `X-Forwarded-For` (9.9.9.1..6) лимит **не** обходит; `./mvnw verify` — 30 тестов зелёные.

## 2026-11-04 — Кошелёк, транзакции, планировщик
- Запрос: `V4` (wallets/transactions), `WalletService.credit` с идемпотентностью, планировщик вознаграждений за отзывы + заглушка месячной конвертации, просроченные инвайты, тесты.
- Решения:
  - `V4`: `wallets` (user_id UNIQUE, balance numeric(19,2), CHECK >= 0), `transactions` (amount > 0, type varchar+CHECK, `idempotency_key` NOT NULL UNIQUE, индекс `(wallet_id, created_at)`).
  - `WalletService`: быстрый pre-check по ключу; `insert ... on conflict do nothing` для ленивого создания кошелька; `SELECT ... FOR UPDATE` (пессимистичная блокировка) против lost update; гонка по ключу → 409.
  - `reviews` создаёт коллега → в коде планировщика только `JdbcTemplate` (+ проверка `to_regclass`), JPA-сущность `Review` не дублируем; в тестах — стаб `src/test/resources/db/test/reviews-stub.sql` (`@Sql BEFORE_TEST_CLASS`), UUID для `author_id`.
  - Награда 5 и лимит 5/мес — конфиг `app.reviews.*` (env `REVIEW_REWARD_AMOUNT`, `REVIEW_MONTHLY_LIMIT`); лимит считается по транзакциям `REVIEW_REWARD` текущего месяца.
  - Идемпотентность начисления: ключ `review-reward:{reviewId}` + флаг `reward_awarded`.
  - Просрочки инвайтов перенесены из `ExpirationScheduler` в `ScheduledTasks.processOverdueTasks()` (cron каждый час), чтобы не было двух владельцев; `accept()` и так проверяет `expires_at`.
  - `@EnableScheduling` уже был на `BackendApplication` — не менял.
- Шаги: V4 → `wallet/domain`+`repo` → `WalletService` → `AppProperties.Reviews` + properties/.env → `ScheduledTasks` → чистка `ExpirationScheduler` → IT (`WalletServiceIT`, `ReviewRewardSchedulerIT`, `OverdueTasksIT`) → логи → dev-стенд.
- Проверка: `./mvnw verify` — 38 тестов зелёные (старые 30 не сломаны); Flyway V4 применён на dev-БД, приложение стартует, `/api/actuator/health` UP через nginx.
- Исправлено по ходу: FK в `OverdueTasksIT` — `saveAndFlush` вместо `save` (JDBC-вставка не видела неотфлашенную кафедру).

## 2026-11-06 — Запуск проекта с реальным фронтендом (Docker)
- Запрос: фронт (Vite+React+MUI) лежит в `frontend/`, не трогать; поднять всё через Docker, ошибки фиксить.
- Решения:
  - `nginx/Dockerfile` (multi-stage): `node:22-alpine` → `npm ci` → `npm run build` (tsc -b + vite) → `nginx:alpine` с запечённым `dist`; в compose сервис `nginx` собирается из корня, bind-mount `frontend/dist` убран.
  - `.dockerignore`: `.git`, `.idea`, docs, `backend/target`, `frontend/node_modules|dist`.
  - `backend/Dockerfile`: `-Dmaven.test.skip` + `--mount=type=cache=/root/.m2` + retry (флейки сети, сломанный тест коллеги).
  - Legacy-слой коллеги: `@Repository("legacy…")` + `@Entity(name="Legacy…")` (коллизии bean/entity имён с `academic`).
  - Коллега (9c73ed5) перенумеровал Flyway (V3–V11) и удалил сидер → dev-БД пересоздана (`drop schema public cascade`), вернул `DevDataSeeder` + `app.seed.*` + `SPRING_PROFILES_ACTIVE=dev` по умолчанию.
  - Тесты: `TeacherBudgetConcurrencyTest` → `AbstractIntegrationTest` (Spring Boot 3, реальный Postgres); `ReviewRewardSchedulerIT` — `activity_id` и удалён стаб `reviews-stub.sql` (таблица `reviews` теперь есть в V4).
- Проверка: 5/5 healthy; `/` и `/students` → 200 (SPA), `/api/actuator/health` → 200; `/api/academic/faculties` 200 (сиды); `./mvnw verify` — 37 зелёных.

## 2026-11-07 — Инвайты преподавателей и пароли
- Запрос: после ссылки из письма нельзя задать пароль; проверить и, при отсутствии, сделать смену/сброс пароля (вариант B).
- Найдено: активация с заданием пароля уже есть (`POST /api/auth/teacher-invites/{token}/accept`), но письмо вело на несуществующий роут `/accept-invite` и `APP_BASE_URL=http://localhost:3000`.
- Решения:
  - Ссылка → `/teacher/activate?token=…`; `APP_BASE_URL` по умолчанию `http://localhost` (properties, compose, `.env.example`).
  - `EmailVerificationService` → `VerificationCodeService(scope, subject)`, ключи `verification:{scope}:…` (регистрация — `registration`).
  - `PasswordManagementService`: `POST /api/auth/password/change` (204, текущий+новый, политика 8–72 буква+цифра), `POST /api/auth/password/forgot` (всегда 202, анти-энумерация, лимиты 3/15 мин, 60 c), `POST /api/auth/password/reset` (204, код 6 цифр, 5 попыток).
  - `PasswordManagement` не раскрывает существование аккаунта: неизвестный email = неверный код (400).
  - `SecurityConfig`: точечный `authenticated()` для `/api/auth/password/change` до `permitAll("/api/auth/**")`.
- Проверка: `./mvnw verify` — 82 теста (47 IT + 35 unit), 0 ошибок; `PasswordManagementIT` — 10; `TeacherInviteIT` дополнительно проверяет путь `/teacher/activate`.
- E2E через nginx: `forgot` 202 → код из Mailpit → `reset` 204 → логин новым паролем 200 → `change` обратно 204. По ходу: Docker VM переполнился (`/tmp ... No space left`), `docker builder prune -f` освободил 5.4 ГБ кеша.

## 2026-11-09 — Заявки на регистрацию: получатель и текст расходятся
- Запрос: заявка студента не «падает» преподавателю; текст обещает учебную часть, а функционал нацелен на преподавателя.
- Факты: бэкенд `GET/POST /api/academic/registration-requests` — учебная часть (все заявки), преподаватель (только группы с ACTIVE-назначением текущего семестра), староста (своя группа); спека п.5.5 — преподаватель + староста, учебная часть не упомянута. Во фронте экрана для этих заявок нет; «Заявки на вступление» на `/teacher/students` — другой механизм (`campus_group_confirmations`). Текст `AccountPages.tsx:151` = «на рассмотрение учебной части». Уведомлений (писем/бейджей) нет ни у кого.
- Ожидает решения: письмо-уведомление преподавателю/старосте (бэкенд) и/или экран со списком заявок (фронт-команда) + кто утверждает по факту.

## 2026-11-09 — Уведомление о заявке на регистрацию (вариант A)
- Запрос: письмо о новой заявке — преподавателям группы и старосте, учебной части не отправлять.
- Сделано: `RegistrationRequestNotifier` вызывается из `RegistrationService.verifyEmail` после `EMAIL_VERIFIED`; получатели — преподаватели с `ACTIVE`-назначением в текущем семестре (`findTeacherIdsByGroup`) + староста (`findAllByGroupIdAndGroupLeaderTrue`), applicant исключается; ошибки отправки логируются, верификация не ломается. Письмо `sendRegistrationPendingNotice`, ссылки `/teacher/students` и `/student/group-confirmation`.
- Проверка: `RegistrationNotificationIT` (2: адресаты/чужие/староста, ARCHIVED не уведомляется); `./mvnw verify` — 86 тестов (49 IT + 37 unit), 0 ошибок; E2E: заявка ИС-21 → verify 200 → письмо на `alex_fomof@mail.ru` («E2E Нотификация, группа ИС-21»), учебной части писем нет.

## 2026-11-09 — Заявки на регистрацию в UI «Мои студенты» + текст
- Запрос: преподаватель по-прежнему не видит заявок, текст говорит про учебную часть.
- Решения: `GET /api/campus/group-confirmations` (его дергает экран «Мои студенты») теперь дополнительно отдаёт `registration_requests` группы (`EMAIL_VERIFIED`→`pending`, `APPROVED`/`REJECTED`→`approved`/`rejected`), `PATCH /api/campus/group-confirmations/{id}` для id заявки делегирует в `RegistrationApprovalService` (права те же). Фронт не переписываем — только строка `AccountPages.tsx:151`: «…преподавателю группы и старосте».
- Проверка: `ApprovalAccessIT` +2 (список содержит заявку; PATCH одобряет и создаёт студента) → 96 тестов (51 IT + 45 unit), 0 ошибок; E2E: новый преподаватель (инвайт) видит 4 заявки ИС-21, PATCH `approve` → заявка APPROVED, создан `gjfighfi@mail.ru/STUDENT`.

## 2026-11-10 — Модерация отзывов: учебная часть + звёзды
- Запрос: вынести модерацию отзывов о мероприятиях из организаторов в учебную часть (валидация, в основном мат), на фронте заменить выбор оценки на звёзды.
- Решения:
  - `GET /api/campus/reviews/moderation` (`ACADEMIC_OFFICE`) — очередь `PENDING_MODERATION` с `flaggedWords`; `PATCH /api/campus/events/{eventId}/reviews/{reviewId}` теперь только `ACADEMIC_OFFICE`, тело `{approved}`, повторное решение → `409`.
  - Мат больше не авто-отклонение: `BlockedWordsReviewFilterStrategy.matchingWords` (нормализация `ё`→`е`) отдаёт найденные корни в `flaggedWords`; жёстко отклоняются только спам и длина.
  - `ReviewService` — два конструктора → `@Autowired` на публичном (иначе Spring не поднимал контекст).
  - Фронт: `Rating` (звёзды) вместо `TextField select`; страница `AcademicReviewModerationPage` + роут `/academic-office/reviews` и пункт меню; у организатора блок модерации отзывов удалён.
- Проверка: `EventReviewModerationIT` 3/3; `ReviewModerationWorkflowTest` (+flag) и `ActivityReviewApiRulesTest` — зелёные; `docker compose build nginx` — OK.
- `./mvnw verify`: 53/56 — падают 3 старых `CertificateRequestIT` (тесты не обновлены под новый workflow справок: `deliveryMethod` обязателен, `APPROVED`→`READY` требует PDF). Добавил в `createRequest` `deliveryMethod:"PICKUP"` (8→3 фейла); остальное — отдельной задачей.
- Docs: `docs/spec/api.md` — добавлен раздел «Мероприятия и отзывы», актуализирован раздел справок.

## 2026-11-12 — Кейсы (фронт) и большая демо-база
- Запрос: после прокрутки кейс «возвращается» не к той карточке и выдаёт не ту награду; нужна большая база для демонстрации (группы, пасс, тематические кейсы с рабочими наградами, задания и т.д.).
- Решения:
  - `CampusPages.tsx` (`InventoryPage`): отказ от захардкоженного финального отступа. Победитель уже приходит с бэка (`openCase`), лента строится вокруг него; после прокрутки центр считается по реальной геометрии карточки-победителя в DOM (`offsetLeft + offsetWidth/2`), с перелётом 24–64 px и возвратом к ней же. Подсветка suspense — по индексу победителя. Поддержан `prefers-reduced-motion`.
  - `DevDemoDataSeeder` (`@Profile("dev")`, `@Order(2)`, `JdbcTemplate`, одна транзакция): 8 групп по 6 студентов, преподаватели/организатор, рейтинги, кошельки, 7 тематических кейсов с наградами и SVG-картинками в БД, пасс 50 уровней × free/premium, 9 заданий и 13 решений, расписание, 7 мероприятий с отзывами (включая очередь модерации), справки, заявки в группы. Сидер идемпотентен: маркер — предметы с источником `Демо-набор`.
  - `DevDataSeeder` помечен `@Order(1)`.
- Проверка: `./mvnw compile` — OK; `docker compose build nginx` (`tsc -b && vite build`) — OK; сиды: 67 users, 9 групп, 8 кейсов, 40 наград, 100 строк пасса, 13 заданий, 8 мероприятий, 16 отзывов, 10 справок, 134 предмета инвентаря. E2E: логин всех демо-ролей 200; открытие кейса списало 100 жетонов и выдало награду из своего каталога; взятие free/premium награды пасса — OK; очередь модерации у учебной части (3, одно с матом) — OK; картинки кейсов/наград/инвентаря — `image/svg+xml`.
- Docs: `docs/demo-data.md` — аккаунты и что показывать.

## 2026-11-12 — Rate limiting: разделение чтения и записи
- Запрос: при обычном использовании пользователь часто получает `429`.
- Причина: один счётчик на все `/api/**` (120/мин); SPA за одну страницу делает 130+ GET (каждая картинка — отдельный запрос к `/api/...`).
- Решения:
  - `GET`/`HEAD` считаются в отдельных ключах `user-read`/`ip-read`: 600/мин авторизованным, 300/мин анонимным; мутации — прежние 120/60/5 из ТЗ.
  - `AppProperties.RateLimit` + `authenticatedReadOrFallback()`/`anonymousReadOrFallback()`; если `*_READ_*` не заданы — берётся общий лимит.
  - Новые env: `RATE_LIMIT_AUTHENTICATED_READ_*`, `RATE_LIMIT_ANONYMOUS_READ_*` (`application.properties`, `.env.example`).
- Проверка: `RateLimitIT` 5/5 (120 мутаций → 429; 130 GET → 200 и запись после них не блокируется); в Docker 200 GET `/api/workspace/student/tasks` — все 200.
- Docs: `docs/spec/api.md` — таблица scope'ов.

## 2026-11-12 — Список демо-аккаунтов
- Запрос: список логинов/паролей для демонстрации (студенты, преподаватели, админ, учебная часть).
- Решения: `docs/demo-data.md` — пароли в таблице, разбивка студентов по 8 группам, список премиум-пассов. Пароль демо-аккаунтов `demo12345`, `office@ptu.local/office12345`, `admin@ptu.local/AdminPtu2026!`.
- Проверка: данные сверены с БД (`users`, `student_profiles`, `campus_pass_accounts`).

## 2026-11-12 — README + очистка «битых» данных
- Запрос: финальный `README.md` (описание, сборка/запуск, тестовые данные) и удаление мусорных аккаунтов/записей от ручного тестирования.
- Решения:
  - Бэкап: `pg_dump` → `/tmp/ptu_social_backup_20261112_145445.sql`.
  - Одна транзакция (`ON_ERROR_STOP=1`): удалены 14 мусорных пользователей и их связанные строки (отзывы, участия, подтверждения, решения, расписание, задания, мероприятия, инвентарь), все `teacher_invites` (8), все `registration_requests` (12), группа «гапеев-21».
  - Учебная часть — единственный модератор отзывов: 2 отзыва возвращены в `PENDING_MODERATION` (демо-очередь).
  - `README.md` переписан: стек, архитектура/сервисы, quick start, роли, демо-аккаунты по группам, env-таблица, rate limit, dev/тесты, структура, безопасность.
- Проверка: 53 users (48 STUDENT + 5 staff), 8 групп, 9 заданий/9 решений, 7 мероприятий, 15 отзывов (2 pending), 8 справок, 2 заявки в группу; логин всех 9 демо-ролей — 200; цифры в README сверены с БД (14 кейсов + 15 косметики).
- Нюанс: лимит логина 5/мин на IP даёт `429` при быстром переключении аккаунтов с одной машины — в README добавлена подсказка про `RATE_LIMIT_LOGIN_REQUESTS`.
