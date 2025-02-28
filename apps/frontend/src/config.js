// src/config.js
require('dotenv').config(); // Загружает переменные из .env

const config = {
    env: process.env.NODE_ENV || 'development',
    backendHost: process.env.BACKEND_HOST,
    // Проверка обязательных переменных
    validate: () => {
        if (!config.backendHost) {
            throw new Error('BACKEND_HOST не задан в переменных окружения!');
        }
    }
};

config.validate(); // Вызов проверки при старте приложения

module.exports = config;