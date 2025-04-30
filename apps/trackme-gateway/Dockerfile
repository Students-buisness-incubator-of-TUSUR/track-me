# ----------- Build stage -----------
FROM gradle:8-jdk21-alpine AS builder

WORKDIR /app

COPY build.gradle settings.gradle gradle /app/
COPY apps/trackme-gateway /app/apps/trackme-gateway

RUN gradle :trackme-gateway:dependencies --no-daemon

COPY . /app

RUN gradle :trackme-gateway:clean :trackme-gateway:bootJar --no-daemon

# ----------- Runtime stage -----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user and use it
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && apk add --no-cache curl
USER appuser

# Copy JAR file from builder
COPY --from=builder /app/apps/trackme-gateway/build/libs/trackme-gateway-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]