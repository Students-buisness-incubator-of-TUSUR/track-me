# ----------- Build stage -----------
FROM gradle:8-jdk21-alpine AS builder

WORKDIR /app

# 1. Копируем только файлы зависимостей и wrapper для кеша Gradle
COPY build.gradle settings.gradle gradle.properties gradle /app/
COPY apps/trackme-gateway/build.gradle /app/apps/trackme-gateway/

# 2. Прогреваем зависимости (токен нужен для чтения GitHub Packages — track-me-platform)
RUN --mount=type=cache,target=/root/.gradle --mount=type=secret,id=gpr_token \
    GITHUB_ACTOR=docker GITHUB_TOKEN=$(cat /run/secrets/gpr_token) \
    gradle :trackme-gateway:dependencies --no-daemon

# 3. Копируем исходники только нужного приложения
COPY apps/trackme-gateway /app/apps/trackme-gateway

# 4. Сборка JAR (кешируется благодаря шагам выше)
RUN --mount=type=cache,target=/root/.gradle --mount=type=secret,id=gpr_token \
    GITHUB_ACTOR=docker GITHUB_TOKEN=$(cat /run/secrets/gpr_token) \
    gradle :trackme-gateway:clean :trackme-gateway:bootJar --no-daemon

# ----------- Runtime stage -----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Не-root пользователь и curl (в 1 слой)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && apk add --no-cache curl
USER appuser

COPY --from=builder /app/apps/trackme-gateway/build/libs/trackme-gateway-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]