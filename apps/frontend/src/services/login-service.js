import axios from "axios";
import {setUser} from "../store/userSlice";
import {useDispatch} from "react-redux";

function LoginService() {
    const backendUrl = process.env.REACT_APP_BACKEND_URI || "http://localhost:8081";
    const dispatch = useDispatch();

    // Применяем withCredentials ко всем запросам через отдельный экземпляр axios
    const axiosWithCredentials = axios.create({
        baseURL: backendUrl,
        withCredentials: true,
    });

    const register = async (userData) => {
        try {
            // Регистрация чаще всего не использует кук, но на всякий случай добавим
            const response = await axiosWithCredentials.post("/register", userData);
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