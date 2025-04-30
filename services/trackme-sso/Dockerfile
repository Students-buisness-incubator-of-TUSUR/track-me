FROM gradle:8-jdk21-alpine AS builder

RUN apk add --no-cache nodejs npm

WORKDIR /app

COPY build.gradle settings.gradle /app/
COPY gradle /app/gradle
COPY services/trackme-sso /app/services/trackme-sso

COPY . /app

RUN --mount=type=cache,target=/root/.gradle gradle trackme-sso:bootJar -Pprod

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
