import axios from "axios";
import { getCsrfConfig } from '../utils/csrf-utils';
import {useDispatch} from "react-redux";
import { setUser, clearUser } from '../store/userSlice';

function LoginService() {
    const backendUrl = process.env.REACT_APP_BACKEND_URI || "http://localhost:8081";
    const dispatch = useDispatch();

    // Применяем withCredentials ко всем запросам через отдельный экземпляр axios
    const axiosWithCredentials = axios.create({
        baseURL: backendUrl,
        withCredentials: true,
    });
    axiosWithCredentials.interceptors.response.use(
        response => response,
        error => {
            if (error.response && [401, 302].includes(error.response.status)) {
                // Если получили 401 Unauthorized - сессия истекла
                dispatch(clearUser());
                window.location.href = '/'; // Перенаправляем на страницу входа
            }
            return Promise.reject(error);
        }
    );

    const register = async (userData) => {
        try {
            // Регистрация чаще всего не использует кук, но на всякий случай добавим
            const response = await axiosWithCredentials.post("/register", userData, {
                headers: {
                    "Content-Type": "application/json", // Обычно для регистрации используется JSON
                    ...getCsrfConfig().headers // Добавляем CSRF-заголовки
                }
            });
            if (response.status === 200) {
                console.log("Registration successful");
            } else {
                throw new Error("Registration failed");
            }
        } catch (error) {
            console.error("Registration failed:", error);
            throw error;
        }
    };

    const logout = async () => {
        try {
            await axiosWithCredentials.post("/logout", {}, {
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded",
                    ...getCsrfConfig().headers
                }
            });
            console.log("Logout successful");
        } catch (error) {
            console.error("Logout failed:", error);
            throw error;
        }
    };

    const getUserInfo = async () => {
        try {
            const response = await axiosWithCredentials.get("/sso/api/v1/account/info");
            if (response.status === 200) {
                dispatch(setUser(response.data));
                return response.data;
            }
        } catch (error) {
            console.error("Failed to fetch user info:", error);
            throw error;
        }
    };

    return {register, logout, getUserInfo};
}

export default LoginService;