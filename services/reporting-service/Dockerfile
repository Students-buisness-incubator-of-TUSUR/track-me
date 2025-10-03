FROM gradle:8-jdk21-alpine AS builder

RUN apk add --no-cache nodejs npm

WORKDIR /app

# 1. Сначала только файлы зависимостей для кеша Gradle:
COPY build.gradle settings.gradle /app/
COPY gradle /app/gradle

# 2. Прогреваeм зависимости Gradle (минимально изменяется)
RUN --mount=type=cache,target=/root/.gradle gradle dependencies

# 3. Копируем исходники приложения
COPY libs/commons /app/libs/commons
COPY platform /app/platform
COPY services/reporting-service /app/services/reporting-service

# 4. Собираем проект
RUN --mount=type=cache,target=/root/.gradle gradle :reporting-service:bootJar --no-daemon --stacktrace

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl \
    && addgroup --system --gid 1001 appgroup \
    && adduser --system --uid 1001 --ingroup appgroup --home /app appuser

COPY --from=builder /app/services/reporting-service/build/libs/reporting-service-*.jar /app/app.jar
RUN chown appuser:appgroup /app/app.jar

EXPOSE 8080

USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]