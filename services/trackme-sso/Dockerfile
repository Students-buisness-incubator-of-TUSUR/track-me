FROM gradle:8-jdk21-alpine AS builder

RUN apk add --no-cache nodejs npm

WORKDIR /app

# 1. Сначала только файлы зависимостей для кеша Gradle:
COPY build.gradle settings.gradle gradle.properties /app/
COPY gradle /app/gradle

# 2. Прогреваeм зависимости Gradle (минимально изменяется)
RUN --mount=type=cache,target=/root/.gradle --mount=type=secret,id=gpr_token \
    GITHUB_ACTOR=docker GITHUB_TOKEN=$(cat /run/secrets/gpr_token) \
    gradle dependencies

# 3. Копируем исходники приложения
COPY services/trackme-sso /app/services/trackme-sso

# 4. Собираем проект (токен нужен для чтения GitHub Packages — track-me-platform)
RUN --mount=type=cache,target=/root/.gradle --mount=type=secret,id=gpr_token \
    GITHUB_ACTOR=docker GITHUB_TOKEN=$(cat /run/secrets/gpr_token) \
    gradle trackme-sso:bootJar -Pprod

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl \
    && addgroup --system --gid 1001 appgroup \
    && adduser --system --uid 1001 --ingroup appgroup --home /app appuser

COPY --from=builder /app/services/trackme-sso/build/libs/trackme-sso-*.jar app.jar
RUN chown appuser:appgroup /app/app.jar

EXPOSE 8080

USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]