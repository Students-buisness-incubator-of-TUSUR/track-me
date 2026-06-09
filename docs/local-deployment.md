# Локальное развёртывание TrackMe (профиль `docker-local`)

## Содержание

1. [Предварительные требования](#предварительные-требования)
2. [Быстрый старт](#быстрый-старт)
3. [Описание сервисов и порты](#описание-сервисов-и-порты)
4. [Переменные окружения](#переменные-окружения)
5. [Разработка с автоперезапуском](#разработка-с-автоперезапуском)
6. [Полезные команды](#полезные-команды)
7. [Устранение неполадок](#устранение-неполадок)

---

## Предварительные требования

| Инструмент | Версия | Назначение |
|---|---|---|
| Docker Desktop | ≥ 24 | Сборка и запуск контейнеров |
| Docker Compose | ≥ 2.22 | Оркестрация сервисов (`compose watch`) |
| Git | любая | Клонирование репозитория |

> **macOS / Windows**: Docker Desktop включает Docker Compose и поддержку `host.docker.internal` из коробки.

---

## Быстрый старт

### 1. Клонировать репозиторий

```bash
git clone <repository-url>
cd track-me
```

### 2. Создать файл окружения

```bash
cp .env.example .env
```

Обязательно заполнить в `.env`:

| Переменная | Описание |
|---|---|
| `TRACKME_CLIENT_SECRET` | Секрет OAuth2-клиента (совпадает с настройкой в SSO) |
| `JWT_SECRET` | SHA-512 хеш для подписи токенов |
| `REDIS_PASSWORD` / `REDIS_USER_PASSWORD` | Пароли Redis |
| `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD` | Настройки SMTP (можно оставить пустыми — mail отключён) |
| `TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_USERNAME` | Telegram-бот (можно оставить `test` для разработки) |

Остальные значения из `.env.example` корректно работают из коробки с `docker-compose.yaml`.

### 3. Запустить проект

```bash
docker compose up --build
```

После запуска всех сервисов приложение доступно по адресу **http://localhost**.

> **Первый запуск** может занять 3–5 минут: Docker скачивает базовые образы и собирает Java-сервисы через Gradle.

---

## Описание сервисов и порты

Все Java-сервисы запускаются с профилем `docker-local` (`SPRING_PROFILES_ACTIVE=docker-local`), который настраивает внутренние адреса Docker-сети и параметры безопасности для локальной разработки.

### Точки входа для браузера

| URL | Описание |
|---|---|
| `http://localhost` | Приложение (через Nginx reverse proxy) |
| `http://localhost/swagger-ui` | Swagger UI со всеми API (backend, meeting, sso) |
| `http://localhost:9000` | SSO сервис (OAuth2 / OIDC) |

### Внутренняя архитектура

```
Browser
  └── Nginx :80
        ├── /backend/**        → trackme-client-gateway :8081
        ├── /meeting/**        → trackme-client-gateway :8081
        ├── /sso/**            → trackme-client-gateway :8081
        ├── /login/**          → trackme-client-gateway :8081
        ├── /logout/**         → trackme-client-gateway :8081
        ├── /oauth2/**         → trackme-client-gateway :8081
        ├── /csrf              → trackme-client-gateway :8081
        ├── /swagger-ui/**     → trackme-client-gateway :8081
        └── /**                → trackme-frontend :3000

trackme-client-gateway :8081
  ├── /backend/**  (StripPrefix) → trackme-backend :8080
  ├── /meeting/**  (StripPrefix) → trackme-meeting-service :8082
  └── /sso/**      (StripPrefix) → trackme-sso :9000
```

### Все сервисы

| Сервис | Контейнер | Внешний порт | Профиль |
|---|---|---|---|
| Nginx (reverse proxy) | `trackme-nginx` | `80` | — |
| Spring Cloud Gateway | `trackme-client-gateway` | `8081` | `docker-local` |
| Backend API | `trackme-backend` | `8080` | `docker-local` |
| Meeting Service | `trackme-meeting-service` | `8082` | `docker-local` |
| SSO (Authorization Server) | `trackme-sso` | `9000` | `docker-local` |
| Telegram Service | `telegram-service` | `8084` | `docker-local` |
| Frontend (React) | `trackme-frontend` | `3000` | — (образ из отдельного репозитория `track-me-frontend`) |
| PostgreSQL | `trackme-postgres` | `5432` | — |
| Redis | `trackme-redis` | `6380` | — |
| Kafka | `trackme-kafka` | `9092`, `29092` | — |

---

## Переменные окружения

Файл `.env` используется всеми сервисами. Критически важные переменные, которые уже корректно настроены в `docker-compose.yaml` через секцию `environment:` (и переопределяют `.env`):

| Переменная | Значение в docker-compose | Описание |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `docker-local` | Активный Spring-профиль |
| `SSO_URI` | `http://host.docker.internal:9000` | Внутренний адрес SSO для OIDC discovery |
| `BACKEND_URI` | `http://localhost` | Публичный адрес (через Nginx) |
| `AFTER_LOGIN_URL` | `http://localhost` | Fallback-редирект после логина |
| `AFTER_LOGOUT_URI` | `http://localhost` | Редирект после логаута |
| `SESSION_COOKIE_SAME_SITE` | `Lax` | Настройка куки сессии |
| `SESSION_COOKIE_SECURE` | `false` | HTTP допустим локально |
| `CORS_ORIGINS` | `http://localhost,...` | Разрешённые CORS-источники |
| `REACT_APP_BACKEND_URI` | `http://localhost` | Адрес gateway для фронтенда |

> **Почему `host.docker.internal` для SSO?** Gateway и backend обращаются к SSO изнутри Docker-сети для OIDC discovery и валидации JWT. Браузер редиректится на `http://localhost:9000` напрямую (SSO экспонирует порт 9000).

---

## Разработка с автоперезапуском

Docker Compose Watch отслеживает изменения в исходном коде и автоматически пересобирает нужные контейнеры.

```bash
docker compose watch
```

### Что отслеживается

| Сервис | Пути | Действие |
|---|---|---|
| `trackme-backend` | `apps/backend/src/`, `libs/commons/src/`, `platform/`, `build.gradle` | `rebuild` |
| `trackme-meeting-service` | `services/meeting-service/src/`, `libs/commons/src/`, `platform/`, `build.gradle` | `rebuild` |
| `trackme-sso` | `services/trackme-sso/src/`, `libs/commons/src/`, `platform/`, `build.gradle` | `rebuild` |
| `trackme-client-gateway` | `apps/trackme-gateway/src/`, `apps/trackme-gateway/build.gradle`, `platform/`, `build.gradle` | `rebuild` |
| `telegram-service` | `services/telegram-service/src/`, `libs/commons/src/`, `platform/`, `build.gradle` | `rebuild` |
| `trackme-nginx` | `docker/nginx/nginx.conf` | `sync+restart` |

> Фронтенд вынесен в отдельный репозиторий `track-me-frontend`. Локально он поднимается как готовый
> образ из GHCR, исходники здесь не отслеживаются; для разработки фронта запускайте `npm start` из
> того репозитория.

---

## Полезные команды

```bash
# Запустить все сервисы (с пересборкой образов)
docker compose up --build

# Запустить в фоне
docker compose up -d --build

# Запустить с автоперезагрузкой при изменении кода
docker compose watch

# Остановить все сервисы
docker compose down

# Остановить и удалить тома (сбросить БД и Redis)
docker compose down -v

# Посмотреть логи конкретного сервиса
docker compose logs -f trackme-client-gateway
docker compose logs -f trackme-sso
docker compose logs -f trackme-backend

# Пересобрать только один сервис
docker compose up --build trackme-client-gateway

# Статус сервисов
docker compose ps
```

---

## Устранение неполадок

### Сервис не запускается — ошибка health check

Некоторые сервисы зависят от других (через `depends_on: condition: service_healthy`). Если PostgreSQL или Redis не готовы, зависимые сервисы не стартуют. Проверьте:

```bash
docker compose ps
docker compose logs trackme-postgres
docker compose logs trackme-redis
```

### `Unable to resolve Configuration with the provided Issuer`

**Причина**: в `.env` задана переменная `SSO_URI`, которая переопределяет значение из `docker-compose.yaml`.
**Решение**: убедитесь, что в `.env` переменная `SSO_URI` отсутствует или закомментирована. В `docker-compose.yaml` уже выставлено правильное значение `http://host.docker.internal:9000`.

### Браузер редиректит на `http://host.docker.internal:9000`

**Причина**: `authorization-uri` настроен на внутренний адрес вместо публичного.
**Решение**: в профиле `docker-local` gateway-а значение `authorization-uri` должно быть `http://localhost:9000/oauth2/authorize` — проверьте `apps/trackme-gateway/src/main/resources/application.yaml`.

### Ошибка `500` на `/.well-known/appspecific/com.chrome.devtools.json`

Chrome DevTools автоматически запрашивает этот путь. Он разрешён в Spring Security SSO через паттерн `/.well-known/**` — это ожидаемое поведение, ошибки нет.

### После логина редирект на главную вместо приложения

**Причина**: `redirect_uri` не передаётся или не сохраняется gateway-ом.
**Решение**: gateway использует `CustomAuthorizationRequestResolver`, который сохраняет `redirect_uri` в WebSession при инициации OAuth2-авторизации. Убедитесь, что фронтенд передаёт параметр при переходе на `/oauth2/authorization/track-me-client?redirect_uri=...`.

### `502 Bad Gateway` при обращении к `/after-login`

**Вероятные причины**:
- Контейнер `trackme-frontend` ещё не готов (подождите 30–60 секунд после старта)
- `REACT_APP_BACKEND_URI` не установлен — убедитесь, что в `docker-compose.yaml` для `trackme-frontend` прописано `REACT_APP_BACKEND_URI=http://localhost`

### Порты уже заняты

Если порт `80`, `8080`, `8081`, `5432` и т.д. уже используется другим процессом:

```bash
# Найти процесс на порту (macOS/Linux)
lsof -i :80

# Либо изменить маппинг портов в docker-compose.yaml
# например: '9080:80' вместо '80:80'
```
