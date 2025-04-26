# 1. Этап сборки
FROM gradle:8-jdk21-alpine AS builder

# Установим Node.js, если проекту нужен фронтенд
RUN apk add --no-cache nodejs npm

WORKDIR /app

# Сначала копируем только файлы, изменение которых встречается реже.
COPY prod.build.gradle settings.gradle /app/
COPY gradle /app/gradle
COPY services/trackme-sso /app/services/trackme-sso


# Затем копируем весь исходный код проекта (для layer cache)
COPY . /app

# Собираем JAR только нужного модуля
RUN gradle --init-scrip prod.build.gradle :trackme-sso:clean :trackme-sso:bootJar

# 2. Этап запуска
FROM eclipse-temurin:21-jre-alpine


WORKDIR /app

RUN apk add --no-cache curl
# Копируем собранный jar-файл из build-образа
COPY --from=builder /app/services/trackme-sso/build/libs/trackme-sso-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]