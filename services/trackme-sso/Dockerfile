# 1. Этап сборки
FROM gradle:8-jdk21-alpine AS builder

# Установим Node.js, если проекту нужен фронтенд
RUN apk add --no-cache nodejs npm

WORKDIR /app

# Сначала копируем только файлы, изменение которых встречается реже.
COPY build.gradle settings.gradle /app/
COPY gradle /app/gradle
COPY services/trackme-sso /app/services/trackme-sso


# Затем копируем весь исходный код проекта (для layer cache)
COPY . /app

# Собираем JAR только нужного модуля
RUN gradle :trackme-sso:clean :trackme-sso:bootJar -Pprod

# 2. Этап запуска
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl \
    && addgroup --system --gid 1001 appgroup \
    && adduser --system --uid 1001 --ingroup appgroup --home /app appuser

# Копируем собранный jar-файл из build-образа, делаем владельцем appuser
COPY --from=builder /app/services/trackme-sso/build/libs/trackme-sso-*.jar app.jar
RUN chown appuser:appgroup /app/app.jar

EXPOSE 8080

# Переключаемся на некорневого пользователя
USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]