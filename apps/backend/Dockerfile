# 1. Build stage
FROM gradle:8-jdk21-alpine AS builder

WORKDIR /app

# Копируем только файлы, необходимые для кэширования зависимостей
COPY --chown=gradle:gradle build.gradle settings.gradle /app/
COPY --chown=gradle:gradle gradle /app/gradle

# Кэшируем зависимости
RUN gradle dependencies --no-daemon || true

# Копируем исходники backend
COPY apps/backend /app/apps/backend
COPY libs/commons /app/libs/commons
COPY platform /app/platform

# Собираем JAR только нужного модуля
RUN gradle :backend:clean :backend:bootJar --no-daemon

# 2. Run stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup && apk add --no-cache curl

# Копируем собранный jar-файл из build-образа
COPY --from=builder /app/apps/backend/build/libs/backend-*.jar app.jar

EXPOSE 8080
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]