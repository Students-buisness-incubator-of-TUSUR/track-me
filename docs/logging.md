# Логи Docker: лимиты и хранение не дольше 60 дней

## Проблема

По умолчанию Docker пишет stdout/stderr контейнеров в `json-file` **без ограничения размера**.
Файлы копятся в `/var/lib/docker/containers/<id>/<id>-json.log`, пока не закончится диск,
после чего сервисы не могут стартовать.

Spring Boot в контейнерах пишет только в stdout, отдельного файлового appender'а нет,
поэтому все логи приложения попадают именно в Docker-логи.

## Что настроено в репозитории

Во все compose-файлы добавлен общий лимит через YAML-anchor:

```yaml
x-logging: &logging-limits
  driver: json-file
  options:
    max-size: "20m"
    max-file: "5"
```

и у каждого сервиса:

```yaml
    logging: *logging-limits
```

Итого максимум **100 МиБ на контейнер** (20 МиБ × 5 файлов).
Для ~10 контейнеров это не более ~1 ГБ на стенд.

Файлы:

- `docker-compose.yaml` — локальный полный стек;
- `docker-compose.dev.yaml` — dev-стек с pgAdmin;
- `docker/docker-compose.prod.yaml` — prod (workflow `deploy-prod` доставляет его на сервер сам).

> Важно: `deploy-dev` **не** копирует compose-файл на сервер. На dev-стенде нужно
> обновить используемый compose-файл вручную (или добавить те же `logging`-блоки).

## Docker не умеет ротацию по возрасту

`json-file` поддерживает только `max-size` и `max-file`. Поэтому:

- размер логов ограничен сверху — диск больше не забьётся;
- для требования «не старше 60 дней» на хосте ставится ежедневная очистка
  **ротированных** файлов (`*-json.log.1`, `*-json.log.2`, ...).

Скрипт: `docker/scripts/cleanup-docker-logs.sh` (по умолчанию 60 дней).

Установка на сервере (один раз, от root):

```bash
sudo install -m 0755 docker/scripts/cleanup-docker-logs.sh /etc/cron.daily/trackme-docker-logs
sudo /etc/cron.daily/trackme-docker-logs
```

Если файла скрипта на сервере нет, его можно создать сразу:

```bash
sudo tee /etc/cron.daily/trackme-docker-logs >/dev/null <<'EOF'
#!/bin/sh
find /var/lib/docker/containers -type f -name '*-json.log.*' -mtime +60 -delete
EOF
sudo chmod 0755 /etc/cron.daily/trackme-docker-logs
```

Скрипт затрагивает все контейнеры хоста, поэтому покрывает и dev, и prod.

## Применение на сервере

### Prod

Compose-файл доставляется workflow `deploy-prod` при следующем релизе/деплое.
После обновления контейнеры пересоздаются, и лимиты начинают действовать.

Проверка:

```bash
cd ~/track-me-prod
compose() { docker compose -p track-me-prod --project-directory . -f docker/docker-compose.prod.yaml "$@"; }
docker inspect --format '{{.Name}} {{.HostConfig.LogConfig.Type}} {{json .HostConfig.LogConfig.Config}}' $(compose ps -q)
```

Ожидается `json-file {"max-file":"5","max-size":"20m"}`.

### Dev

1. Обновить compose-файл на сервере (добавить `x-logging` и `logging`-блоки из репозитория).
2. Пересоздать контейнеры:

```bash
cd ~/track-me-dev
docker compose up -d --force-recreate
```

Проверка — аналогично prod.

## Немедленная очистка уже накопленных логов

Старые файлы не удаляются сами при включении лимитов — их нужно обнулить.
Команды выполняются из каталога стека. Контейнеры останавливаются, чтобы Docker
закрыл файлы и место реально освободилось.

### Dev

```bash
cd ~/track-me-dev
IDS=$(docker compose ps -aq)
docker compose stop
for id in $IDS; do
  sudo truncate -s 0 /var/lib/docker/containers/"$id"/"$id"-json.log 2>/dev/null || true
done
docker compose up -d
```

### Prod

```bash
cd ~/track-me-prod
compose() { docker compose -p track-me-prod --project-directory . -f docker/docker-compose.prod.yaml "$@"; }
IDS=$(compose ps -aq)
compose stop
for id in $IDS; do
  sudo truncate -s 0 /var/lib/docker/containers/"$id"/"$id"-json.log 2>/dev/null || true
done
compose up -d
```

> На общем хосте останавливается только выбранный стек; второй продолжает работать.

## Диагностика

```bash
df -h /
docker system df -v
du -sh /var/lib/docker/containers
```

Если логи всё ещё растут быстро, проверить уровень логирования: в профиле
`docker-local` у backend / meeting-service / SSO включён
`org.springframework.security: trace` (см. `application*.yaml`). Для постоянного
стенда разумнее `info`.
