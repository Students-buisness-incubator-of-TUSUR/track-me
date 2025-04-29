import axios from "axios";
import {setUser} from "../store/userSlice";
import {useDispatch} from "react-redux";

function LoginService() {
    const backendUrl = process.env.REACT_APP_BACKEND_URI || "http://localhost:8081";
    const userinfoUrl = `${backendUrl}/sso/api/v1/account/info`;
    const logoutUrl = `${backendUrl}/logout`;
    const dispatch = useDispatch();

    const register = async (userData) => {
        try {
            const response = await axios.post(`${backendUrl}/register`, userData);
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
        return await axios.post(logoutUrl, {}, {
            withCredentials: true,
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
            }
        })
            .then(() => {
                console.log("Logout successful");
            })
    };

    const getUserInfo = async () => {
        try {
            const response = await axios.get(userinfoUrl, {
                withCredentials: true,
            });
            if (response.status === 200) {
                // Assuming you have a Redux store or similar to save user data
                dispatch(setUser(response.data));
                return response.data;
            }
        } catch (error) {
            console.error("Failed to fetch user info:", error);
            throw error;
        }
    };

    return {
        register,
        logout,
        getUserInfo,
    };
}

export default LoginService;