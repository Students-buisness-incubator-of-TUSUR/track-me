import axios from "axios";

/**
 * Общий экземпляр axios с настроенными credentials и interceptor для OAuth2.
 *
 * Используйте этот экземпляр вместо голого `axios` для всех запросов к backend/gateway.
 * При получении 401 автоматически перенаправляет на страницу входа OAuth2.
 */
const api = axios.create({
    baseURL: process.env.REACT_APP_BACKEND_URI || "http://localhost:8081",
    withCredentials: true,
});

api.interceptors.response.use(
    response => response,
    error => {
        if (error.response && error.response.status === 401) {
            // Gateway возвращает 401 + X-Login-Url для AJAX-запросов
            const loginUrl = error.response.headers["x-login-url"];
            const baseUrl = process.env.REACT_APP_BACKEND_URI || "http://localhost:8081";

            // Полный редирект (не AJAX) — браузер пройдёт OAuth2 flow без CORS-проблем
            window.location.href = loginUrl
                ? `${baseUrl}${loginUrl}`
                : `${baseUrl}/oauth2/authorization/track-me-client`;

            // Не резолвим промис — страница уходит на редирект
            return new Promise(() => {});
        }
        return Promise.reject(error);
    }
);

export default api;
