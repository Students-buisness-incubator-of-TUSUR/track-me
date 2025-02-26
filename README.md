# Track Me

[![Build and Push Docker Images](https://github.com/akarmanov2022/track-me/actions/workflows/build-artifacts.yml/badge.svg)](https://github.com/akarmanov2022/track-me/actions/workflows/build-artifacts.yml)

Онлайн-сервис для автоматизации и мониторинга работы проектных команд (стартапов и стартап-проектов) и их кураторов (трекеров) в рамках университетских акселерационных программ.

## Стек технологий

- Java 21
- Spring Boot 3.3.4
- PostgreSQL 13

## Запуск проекта

### С использованием Docker

1. Убедитесь, что у вас установлен Docker.
2. В файле `docker-compose.yaml` в **services.backend.environment.JWT_SECRET** присвойте свое значение хеша в формате
   SHA512 (сгенерировать можно [тут](https://emn178.github.io/online-tools/sha512.html))
3. Выполните команду для запуска контейнеров:
    ```shell
    docker compose up -d
    ```

### Локально

1. Убедитесь, что у вас установлен PostgreSQL и настроена база данных:
    ```sql
    CREATE DATABASE project_place;
    CREATE USER project_place WITH PASSWORD 'project-place';
    GRANT ALL PRIVILEGES ON DATABASE project_place TO project_place;
    ```
2. Настройте переменные окружения в `application.yaml` или используйте значения по умолчанию.
3. Запустите приложение:
    ```shell
    ./gradlew bootRun
    ```
