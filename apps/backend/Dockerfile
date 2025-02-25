# Build stage
FROM gradle:8-jdk21-alpine AS builder
WORKDIR /app
COPY . .
RUN gradle bootWar -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
COPY --from=builder /app/build/libs/*.war /app/app.war
EXPOSE 8080
CMD ["java", "-jar", "/app/app.war"]