import api from './api';
import { getCsrfConfig } from '../utils/csrf-utils';
import {useDispatch} from "react-redux";
import { setUser, clearUser } from '../store/userSlice';

function LoginService() {
    const dispatch = useDispatch();

    const register = async (userData) => {
        try {
            const response = await api.post("/register", userData, {
                headers: {
                    "Content-Type": "application/json",
                    ...getCsrfConfig().headers
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
            await api.post("/logout", {}, {
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
            const response = await api.get("/sso/api/v1/account/info");
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
