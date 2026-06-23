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

---

## Модель веток и образов

Две долгоживущие ветки:

| Ветка | Стенд | Что собирается и куда едет |
|---|---|---|
| `develop` | **dev** | `build-artifacts` собирает образы с тегами `:<sha>`, `:develop`, `:latest`; `deploy-dev` авто-выкатывает `:<sha>` на dev |
| `main` | **prod** | `build-artifacts` собирает `:<sha>`, `:main` (без `:latest`); релизный тег `vX.Y.Z` → образ `:vX.Y.Z` |

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

Деплой: `deploy-prod` копирует на сервер `docker-compose.prod.yaml`, `docker/nginx/nginx.conf`,
`database/init_schemas.sql` в `~/track-me-prod`, патчит `IMAGE_TAG=v1.2.3` в `.env` и выполняет
`docker compose -p track-me-prod -f docker-compose.prod.yaml up -d` с healthcheck'ом backend и sso.

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
с изоляцией контейнеров/сети/volume'ов. Один раз на сервере:

1. Создать каталог `~/track-me-prod`.
2. Положить туда `.env` на основе [`.env.prod.example`](../.env.prod.example) и заполнить реальными
   значениями (БД, `JWT_SECRET`, `TRACKME_CLIENT_SECRET`, домены/URI, `FRONTEND_IMAGE`, `PROXY_HTTP_PORT`).
   Секреты — **отдельные от dev**. `IMAGE_REPO`/`IMAGE_TAG` патчит workflow, вручную не задавать.
3. Определить внешний порт prod-nginx (`PROXY_HTTP_PORT`, по умолчанию `8000` — dev занимает `:80`)
   и поставить перед ним TLS-терминирующий прокси/домен.

> Файлы `docker-compose.prod.yaml`, `nginx.conf` и `init_schemas.sql` workflow доставляет сам
> при каждом деплое — вручную их класть не нужно.

**Открытые вопросы к ops до первого деплоя:**
- Реальный домен и TLS для prod-nginx.
- Корректные значения доменов/URI в `.env` (должны совпадать с issuer SSO).
- Версии инфра-образов (`bitnamilegacy/kafka` сейчас `:latest` — запиннить после проверки тега).
- Нужен ли backend'у `docker.sock` в prod (сейчас примонтирован для паритета с dev).

---

## Настройка GitHub

- **Environment `production`** (Settings → Environments) с required reviewers — даёт ручной апрув
  и заодно гарантирует, что к моменту деплоя образы `:vX.Y.Z` уже собраны.
- **Branch protection на `main`**: запретить прямой push, требовать PR + зелёные PR-checks.
- Секреты SSH те же, что у dev (`SBI_SSH_HOST`, `SBI_SSH_USERNAME`, `SBI_SSH_PRIVATE_KEY`) — хост общий.
  Прод-специфика живёт в `~/track-me-prod/.env` на сервере.
- Default-веткой остаётся `develop`.

---

## Порты dev vs prod

На общем хосте сервисные порты prod **наружу не публикуются** — внешний доступ только через nginx.

| | dev (`track-me-dev`) | prod (`track-me-prod`) |
|---|---|---|
| Внешний HTTP (nginx) | `:80` | `${PROXY_HTTP_PORT}` (по умолчанию `:8000`) |
| Порты сервисов/БД/Kafka | проброшены на хост | только внутри сети проекта |
| Образы | `:<sha>` из `develop` | `:vX.Y.Z` из `main` |
| `.env` / каталог | `~/track-me-dev` | `~/track-me-prod` |
