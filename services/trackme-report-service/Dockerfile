# ----------- Build stage -----------
FROM gradle:8-jdk21-alpine AS builder

WORKDIR /app

# 1. Копируем только файлы зависимостей и wrapper для кеша Gradle
COPY build.gradle settings.gradle gradle /app/
COPY services/trackme-report-service/build.gradle /app/services/trackme-report-service/

# 2. Прогреваем зависимости (кеширует максимально)
RUN --mount=type=cache,target=/root/.gradle gradle :trackme-report-service:dependencies --no-daemon

# 3. Копируем исходники только нужного приложения и его зависимости
COPY services/trackme-report-service /app/services/trackme-report-service

# 4. Сборка JAR (кешируется благодаря шагам выше)
RUN --mount=type=cache,target=/root/.gradle gradle :trackme-report-service:clean :trackme-report-service:bootJar --no-daemon

# ----------- Runtime stage -----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Не-root пользователь и curl (в 1 слой)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && apk add --no-cache curl
USER appuser

COPY --from=builder /app/apps/trackme-report-service/build/libs/trackme-report-service-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]