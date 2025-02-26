# Build stage
FROM gradle:8-jdk21-alpine AS builder
WORKDIR /app
COPY . .
RUN gradle build -x test

EXPOSE 8080
CMD ["gradle", "bootRun"]