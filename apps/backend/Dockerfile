# Dockerfile
FROM gradle:8.12-jdk21 AS build

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем только файлы, необходимые для сборки (для оптимизации кэширования)
COPY build.gradle .
COPY src ./src

# Собираем проект (без тестов)
RUN gradle build -x test

CMD ["gradle", "bootRun"]