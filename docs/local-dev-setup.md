# Локальная разработка (Docker + Frontend)

Инструкция по поднятию окружения проекта для локальной разработки.  
**Backend-сервисы и инфраструктура** работают в Docker, **frontend** запускается локально.

## Требования

- [Docker](https://docs.docker.com/get-docker/) и Docker Compose v2+
- [Node.js](https://nodejs.org/) 18+ и npm
- ~4 ГБ свободной оперативной памяти (Kafka + JVM-сервисы)

## Архитектура окружения

```
┌─────────────────────── Docker ───────────────────────┐
│                                                      │
│  PostgreSQL :5432    Redis :6380    Kafka :29092      │
│       │                │               │             │
│  ┌────┴────┐    ┌──────┴──────┐   ┌────┴────┐       │
│  │ Backend │    │     SSO     │   │ Kafka UI│       │
│  │  :8080  │    │    :9000    │   │  :8085  │       │
│  └────┬────┘    └──────┬──────┘   └─────────┘       │
│       │                │                             │
│  ┌────┴────────────────┴────┐                        │
│  │      Gateway :8081       │                        │
│  └────────────┬─────────────┘                        │
│               │                                      │
│  ┌────────────┴──┐  ┌──────────────┐                 │
│  │Meeting Service│  │Telegram Svc  │                 │
│  │    :8082      │  │    :8084     │                 │
│  └───────────────┘  └──────────────┘                 │
└──────────────────────────────────────────────────────┘
                        │
                   http://localhost:8081
                        │
              ┌─────────┴─────────┐
              │  Frontend (local) │
              │   localhost:3000  │
              └───────────────────┘
```

## Быстрый старт

### 1. Настроить переменные окружения

```bash
cp .env.example .env
```

Откройте `.env` и заполните обязательные значения:

| Переменная | Описание | Обязательно |
|---|---|---|
| `POSTGRES_PASSWORD` | Пароль PostgreSQL | ✅ (есть дефолт) |
| `REDIS_PASSWORD` | Пароль Redis | ✅ (сменить!) |
| `REDIS_USER_PASSWORD` | Пароль пользователя Redis | ✅ (сменить!) |
| `JWT_SECRET` | SHA-512 хеш для JWT | ✅ |
| `TRACKME_CLIENT_SECRET` | Секрет OAuth2-клиента | ✅ (сменить!) |
| `GITHUB_CLIENT_ID` | GitHub OAuth App ID | ❌ |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth App Secret | ❌ |
| `TELEGRAM_BOT_TOKEN` | Токен Telegram-бота | ❌ |

> 💡 Сгенерировать JWT_SECRET (SHA-512): https://emn178.github.io/online-tools/sha512.html

### 2. Поднять backend и инфраструктуру

```bash
docker compose -f docker-compose.local.yaml up -d --build
```

Первая сборка займёт несколько минут (скачивание образов + компиляция Gradle).

Проверить что всё поднялось:

```bash
docker compose -f docker-compose.local.yaml ps
```

Все сервисы должны быть в статусе `Up (healthy)`.

### 3. Запустить frontend

```bash
cd apps/frontend
npm install
npm start
```

Frontend запустится на http://localhost:3000 и будет проксировать API-запросы на Gateway (`localhost:8081`).

> ⚠️ Убедитесь, что файл `apps/frontend/src/.env.development` содержит правильный адрес gateway:
> ```
> BACKEND_URI=http://localhost:8081
> ```

## Полезные команды

### Управление контейнерами

```bash
# Посмотреть логи всех сервисов
docker compose -f docker-compose.local.yaml logs -f

# Логи конкретного сервиса
docker compose -f docker-compose.local.yaml logs -f trackme-backend

# Перезапустить один сервис
docker compose -f docker-compose.local.yaml restart trackme-backend

# Пересобрать и перезапустить один сервис (после изменений в коде)
docker compose -f docker-compose.local.yaml up -d --build trackme-backend

# Остановить всё
docker compose -f docker-compose.local.yaml down

# Остановить и удалить volumes (полный сброс данных)
docker compose -f docker-compose.local.yaml down -v
```

### Доступ к сервисам

| Сервис | URL |
|---|---|
| Frontend (локальный) | http://localhost:3000 |
| Gateway (API) | http://localhost:8081 |
| Backend | http://localhost:8080 |
| SSO | http://localhost:9000 |
| Meeting Service | http://localhost:8082 |
| Telegram Service | http://localhost:8084 |
| Kafka UI | http://localhost:8085 |
| PostgreSQL | `localhost:5432` |
| Redis | `localhost:6380` |

### Подключение к БД

```bash
# Через docker
docker compose -f docker-compose.local.yaml exec trackme-postgres \
  psql -U project-place -d project-place

# Или напрямую (если установлен psql)
psql -h localhost -p 5432 -U project-place -d project-place
```

## Пересборка после изменений

### Изменения в backend-коде

Пересобрать нужный сервис:

```bash
# Пример: изменения в backend
docker compose -f docker-compose.local.yaml up -d --build trackme-backend

# Пример: изменения в SSO
docker compose -f docker-compose.local.yaml up -d --build trackme-sso
```

### Изменения в frontend-коде

Ничего делать не нужно — React dev server подхватывает изменения автоматически (hot reload).

### Изменения в SQL-схемах

Пересоздать volume PostgreSQL:

```bash
docker compose -f docker-compose.local.yaml down
docker volume rm track-me_postgres_data
docker compose -f docker-compose.local.yaml up -d --build
```

## Решение проблем

### Порт уже занят

```bash
# Найти процесс на порту
lsof -i :8081

# Или изменить порт в docker-compose.local.yaml
# Например: '8081:8081' → '18081:8081'
```

### Сервис не стартует

```bash
# Посмотреть логи
docker compose -f docker-compose.local.yaml logs trackme-backend

# Типичные причины:
# - Не заполнены переменные в .env
# - PostgreSQL ещё не готов (подождать ~30 сек)
# - Ошибка компиляции (смотреть логи билда)
```

### CORS-ошибки в браузере

Убедитесь, что gateway запущен с правильными CORS-настройками. В `docker-compose.local.yaml` они уже прописаны для `localhost:3000`. Если frontend запущен на другом порту — добавьте его в `CORS_ORIGINS` в `.env`.

### Недостаточно памяти

Kafka и JVM-сервисы потребляют много памяти. Если Docker падает — увеличьте лимит памяти в Docker Desktop (рекомендуется ≥ 6 ГБ).
