import axios from 'axios';

// Храним токен здесь
let csrfToken = null;
let csrfHeaderName = "X-CSRF-TOKEN";

/**
 * Загружает CSRF-токен (только если он еще не получен)
 */
const fetchCsrfToken = async () => {
    if (!csrfToken) {
        const response = await axios.get('/api/csrf', {withCredentials: true});
        csrfToken = response.data.token;
        // Если нужно подставлять custom header, берём из ответа сервера:
        csrfHeaderName = response.data.headerName || 'X-CSRF-TOKEN';
    }
    return {csrfToken, csrfHeaderName};
};

// Добавляем перехватчик для всех запросов
axios.interceptors.request.use(async config => {
    // Для "опасных" методов только
    if (["post", "put", "patch", "delete"].includes(config.method)) {
        const {csrfToken} = await fetchCsrfToken();
        config.headers[csrfHeaderName] = csrfToken;
    }
    return config;
}, error => Promise.reject(error));

export default axios;