# 1. Этап сборки
FROM gradle:8-jdk21-alpine AS builder

WORKDIR /app

# Сначала копируем только файлы, изменение которых встречается реже.
COPY build.gradle settings.gradle /app/
COPY gradle /app/gradle
COPY apps/backend /app/apps/backend

# Затем копируем весь исходный код проекта (для layer cache)
COPY . /app

# Собираем JAR только нужного модуля
RUN gradle :backend:clean :backend:bootJar

# 2. Этап запуска
FROM eclipse-temurin:21-jre-alpine


WORKDIR /app

RUN apk add --no-cache curl
# Копируем собранный jar-файл из build-образа
COPY --from=builder /app/apps/backend/build/libs/backend-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]