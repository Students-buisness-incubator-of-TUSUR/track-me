#!/bin/sh
# Удаляет ротированные JSON-логи Docker старше заданного срока (по умолчанию 60 дней).
#
# Docker не умеет ротацию по возрасту: json-file поддерживает только max-size/max-file.
# Поэтому размер активных логов ограничивается в compose (см. docs/logging.md),
# а этот скрипт раз в сутки удаляет уже ротированные файлы, которым больше 60 дней.
#
# Установка (от root):
#   install -m 0755 docker/scripts/cleanup-docker-logs.sh /etc/cron.daily/trackme-docker-logs
#
# Срок можно переопределить переменной окружения LOG_RETENTION_DAYS.

set -eu

RETENTION_DAYS="${LOG_RETENTION_DAYS:-60}"

find /var/lib/docker/containers \
  -type f \
  -name '*-json.log.*' \
  -mtime +"${RETENTION_DAYS}" \
  -delete
