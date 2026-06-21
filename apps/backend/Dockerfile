# 1. Build stage
FROM gradle:8-jdk21-alpine AS builder

WORKDIR /app

# Копируем только файлы, необходимые для кэширования зависимостей
COPY --chown=gradle:gradle build.gradle settings.gradle gradle.properties /app/
COPY --chown=gradle:gradle gradle /app/gradle

# Кэшируем зависимости (токен нужен для чтения GitHub Packages — track-me-platform)
RUN --mount=type=secret,id=gpr_token \
    GITHUB_ACTOR=docker GITHUB_TOKEN=$(cat /run/secrets/gpr_token) \
    gradle dependencies --no-daemon || true

# Копируем исходники backend
COPY apps/backend /app/apps/backend

# Собираем JAR только нужного модуля
RUN --mount=type=secret,id=gpr_token \
    GITHUB_ACTOR=docker GITHUB_TOKEN=$(cat /run/secrets/gpr_token) \
    gradle :backend:clean :backend:bootJar --no-daemon

# 2. Run stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup && apk add --no-cache curl

# Копируем собранный jar-файл из build-образа
COPY --from=builder /app/apps/backend/build/libs/backend-*.jar app.jar

EXPOSE 8080
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]