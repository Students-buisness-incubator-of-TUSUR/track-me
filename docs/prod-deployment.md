# Релизы и развёртывание на PROD

Документ описывает концепцию выпуска релизов Track Me и их выкатки на production-стенд.

## Содержание

1. [Модель веток и образов](#модель-веток-и-образов)
2. [Процедура релиза](#процедура-релиза)
3. [Откат (rollback)](#откат-rollback)
4. [Hotfix](#hotfix)
5. [Разовая настройка инфраструктуры](#разовая-настройка-инфраструктуры)
6. [Настройка GitHub](#настройка-github)
7. [Порты dev vs prod](#порты-dev-vs-prod)
8. [Проверка после деплоя](#проверка-после-деплоя)

---

## Модель веток и образов

Две долгоживущие ветки:

| Ветка     | Стенд    | Что собирается и куда едет                                                                                               |
|-----------|----------|--------------------------------------------------------------------------------------------------------------------------|
| `develop` | **dev**  | `build-artifacts` собирает образы с тегами `:<sha>`, `:develop`, `:latest`; `deploy-dev` авто-выкатывает `:<sha>` на dev |
| `main`    | **prod** | `build-artifacts` собирает `:<sha>`, `:main` (без `:latest`); релизный тег `vX.Y.Z` → образ `:vX.Y.Z`                    |

**Prod-образы собираются только из `main`** (и тегов на `main`). Из `develop` на prod ничего не попадает.
Релизный тег `vX.Y.Z` — единственный неизменяемый артефакт релиза.

```
feature/* ─PR─▶ develop ─push─▶ build-artifacts(develop) ─▶ deploy-dev (авто, :<sha>)
                   │
              (зелёный develop)
                   │  PR/merge
                   ▼
                 main ─push─▶ build-artifacts(main) ─▶ :<sha>, :main
                   │
          git tag vX.Y.Z (на main) ─push─▶ build-artifacts ─▶ :vX.Y.Z
                   │
        GitHub Release published ─▶ deploy-prod ─[ручной апрув]─▶ prod (:vX.Y.Z)
```

---

## Процедура релиза

Рекомендуемый порядок (исключает гонку «образ ещё не собран»):

1. Убедиться, что нужный коммит на `develop` зелёный (PR-checks + успешный deploy-dev).
2. Влить `develop → main` через PR; дождаться зелёной сборки `build-artifacts` для `main`.
3. Создать и запушить аннотированный тег на коммит `main`:
   ```bash
   git checkout main && git pull
   git tag -a v1.2.3 -m "Release 1.2.3"
   git push origin v1.2.3
   ```
   `build-artifacts` соберёт образы `:v1.2.3` для всех пяти сервисов.
4. Дождаться зелёной сборки тега.
5. На GitHub создать **Release** из тега `v1.2.3` (с release notes) → запустится workflow **Deploy to PROD**.
6. Ответственный подтверждает выкатку в окружении `production` (**Approve**) → деплой на prod.

Деплой: `deploy-prod` копирует на сервер `docker/docker-compose.prod.yaml` и `database/init_schemas.sql`
в `~/track-me-prod` (пути сохраняются), патчит в `.env` `IMAGE_TAG=v1.2.3` и `GOOGLE_*`/`YANDEX_*` из секретов
окружения `production`, выполняет
`docker compose -p track-me-prod --project-directory . -f docker/docker-compose.prod.yaml up -d`
и проверяет health sso, gateway, backend и frontend.

---

## Откат (rollback)

Образы `:vX.Y.Z` неизменяемы, поэтому откат — это выкатка предыдущего тега:

1. GitHub → Actions → **Deploy to PROD** → **Run workflow**.
2. В поле `tag` указать предыдущий релиз (напр. `v1.2.2`) и запустить.
3. Подтвердить апрув в `production`.

---

## Hotfix

1. Ветка от `main`, фикс, PR обратно в `main`.
2. Тег `vX.Y.(Z+1)` на `main` → релиз по обычной процедуре.
3. Обратный merge `main → develop`, чтобы фикс не потерялся.

---

## Разовая настройка инфраструктуры

Prod работает **на том же хосте, что и dev**, но как отдельный compose-проект (`-p track-me-prod`)
с изоляцией контейнеров/сети/volume'ов. Схема доменов — как на dev, поддомены за host-nginx:

| Домен                             | Куда              | Порт на хосте (только 127.0.0.1) |
|-----------------------------------|-------------------|----------------------------------|
| `trackme.startup-poligon.com`     | frontend          | `13000`                          |
| `api.trackme.startup-poligon.com` | gateway           | `18081`                          |
| `sso.trackme.startup-poligon.com` | SSO (OIDC issuer) | `19000`                          |

Все сервисы на prod работают на Spring-профиле **`production`** (Liquibase только с context `prod` —
без тестовых пользователей dev; строгие cookie/CORS; Swagger в gateway выключен). Профиль `docker-local`
на сервере использовать нельзя: в нём issuer зашит на `http://host.docker.internal:9000`.

Чек-лист первого запуска:

1. **DNS:** A-записи `trackme.`, `api.trackme.`, `sso.trackme.startup-poligon.com` → IP сервера.
2. **TLS:** один сертификат на три имени:
   ```bash
   sudo certbot certonly --nginx -d trackme.startup-poligon.com \
       -d api.trackme.startup-poligon.com -d sso.trackme.startup-poligon.com
   ```
3. **Host-nginx:** конфиг [`docker/nginx/host/trackme-prod.conf`](../docker/nginx/host/trackme-prod.conf)
   (отдельно от `default`, где живёт dev):
   ```bash
   sudo cp trackme-prod.conf /etc/nginx/sites-available/trackme-prod
   sudo ln -s /etc/nginx/sites-available/trackme-prod /etc/nginx/sites-enabled/trackme-prod
   sudo nginx -t && sudo systemctl reload nginx
   ```
4. **Каталог и `.env`:** `mkdir ~/track-me-prod`, положить туда `.env` на основе
   [`.env.prod.example`](../.env.prod.example), `chmod 600 .env`. Секреты — **отдельные от dev**
   (`POSTGRES_PASSWORD`, `DB_PASSWORD`, `REDIS_*`, `JWT_SECRET`, свой Telegram-бот). `FRONTEND_IMAGE` —
   зафиксированный тег. `TRACKME_CLIENT_SECRET` должен соответствовать bcrypt-хешу client-secret в
   `services/trackme-sso/src/main/resources/application-security.yaml`.
   `IMAGE_REPO`/`IMAGE_TAG`/`GOOGLE_*`/`YANDEX_*` патчит workflow — вручную не задавать.
5. **Google/Yandex OAuth:** в консолях провайдеров добавить redirect URI
   `https://api.trackme.startup-poligon.com/login/oauth2/code/google` и
   `https://api.trackme.startup-poligon.com/login/oauth2/code/yandex`.
6. **GitHub:** см. [Настройка GitHub](#настройка-github).
7. **Первый релиз** по [процедуре](#процедура-релиза).
8. **Сразу после первого деплоя** сменить пароль пользователя `superadmin` — он создаётся changeset'ом
   `users-data-1` (context `prod`) с известным хешем пароля.

> Файлы `docker-compose.prod.yaml` и `init_schemas.sql` workflow доставляет сам при каждом деплое.
> Конфиг host-nginx — нет: при его изменении обновить на сервере вручную.

Ручной запуск/диагностика на сервере (из `~/track-me-prod`):

```bash
alias dcp='docker compose -p track-me-prod --project-directory . -f docker/docker-compose.prod.yaml'
dcp ps
dcp logs -f trackme-sso
```

**Открытые вопросы:**

- Образ фронтенда запускает CRA dev-server (`npm start`): `REACT_APP_BACKEND_URI` читается в рантайме, но для
  prod лучше собрать статический бандл (nginx) в `track-me-frontend`.
- Контейнеры ходят к issuer `https://sso.trackme.startup-poligon.com` через публичный DNS и host-nginx (так же работает
  dev с `sso.trackme.test…`).
- Нужен ли backend'у `docker.sock` в prod (сейчас примонтирован для паритета с dev).
- Бэкапы Postgres prod (volume `track-me-prod_postgres_data`) — настроить `pg_dump` по расписанию.

---

## Настройка GitHub

- **Environment `production`** (Settings → Environments) с required reviewers — даёт ручной апрув
  и заодно гарантирует, что к моменту деплоя образы `:vX.Y.Z` уже собраны. **Deployment branches and tags** должны
  разрешать теги `v*` (Selected branches and tags: `main` + tag `v*`):
  workflow на событии Release выполняется на ref тега, и режим «Protected branches only» его отклонит.
- **Branch protection на `main`**: запретить прямой push, требовать PR + зелёные PR-checks.
- Секреты SSH те же, что у dev (`SBI_SSH_HOST`, `SBI_SSH_USERNAME`, `SBI_SSH_PRIVATE_KEY`) — хост общий.
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `YANDEX_CLIENT_ID`, `YANDEX_CLIENT_SECRET` сейчас заданы на уровне
  организации (общие с dev) — тогда в эти же OAuth-приложения нужно добавить prod redirect URI. Отдельные
  prod-приложения задаются одноимёнными секретами окружения `production` (они перекрывают org-level).
  Прод-специфика живёт в `~/track-me-prod/.env` на сервере.
- Default-веткой остаётся `develop`.

---

## Порты dev vs prod

|                                   | dev (`track-me-dev`)                                                         | prod (`track-me-prod`)                                        |
|-----------------------------------|------------------------------------------------------------------------------|---------------------------------------------------------------|
| Домены                            | `trackme.test.`, `api.trackme.test.`, `sso.trackme.test.startup-poligon.com` | `trackme.`, `api.trackme.`, `sso.trackme.startup-poligon.com` |
| frontend / gateway / sso на хосте | `3000` / `8081` / `9000`                                                     | `127.0.0.1:13000` / `127.0.0.1:18081` / `127.0.0.1:19000`     |
| БД / Redis / Kafka / backend      | проброшены на хост                                                           | только внутри сети проекта                                    |
| Spring-профиль                    | default                                                                      | `production`                                                  |
| Образы                            | `:<sha>` из `develop`                                                        | `:vX.Y.Z` из `main`                                           |
| `.env` / каталог                  | `~/track-me-dev`                                                             | `~/track-me-prod`                                             |

---

## Проверка после деплоя

```bash
curl -sf https://sso.trackme.startup-poligon.com/.well-known/openid-configuration   # issuer = https://sso.trackme.startup-poligon.com
curl -sf https://api.trackme.startup-poligon.com/actuator/health
```

Затем в браузере: открыть `https://trackme.startup-poligon.com`, войти логином/паролем и через Google/Yandex,
выйти — должен быть редирект обратно на фронт.
