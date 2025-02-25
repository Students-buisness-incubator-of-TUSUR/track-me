# Build stage
FROM gradle:8-jdk21-alpine AS builder
WORKDIR /app
COPY . .
RUN gradle bootJar -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
COPY --from=builder /app/apps/backend/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]