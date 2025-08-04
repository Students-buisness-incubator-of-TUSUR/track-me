import {useEffect} from "react";
import loginService from "../../services/login-service";
import {useNavigate} from "react-router-dom";
import axios from "axios";

function AfterLogin() {
    let service = loginService();
    const navigate = useNavigate();

    useEffect(() => {
        // Сначала получаем CSRF-токен
        const fetchCsrfToken = async () => {
            try {
                const response = await axios.get('https://api.trackme.test.startup-poligon.com/csrf', {
                    withCredentials: true
                });
                
                // Сохраняем токен в localStorage или в памяти
                localStorage.setItem('csrfToken', response.data.token);
                localStorage.setItem('csrfHeaderName', response.data.headerName);
                
                // После получения токена запрашиваем информацию о пользователе
                return service.getUserInfo();
            } catch (error) {
                console.error("Error fetching CSRF token:", error);
                throw error;
            }
        };

        fetchCsrfToken()
            .then((data) => {
                let roles = data.roles;
                let isAdmin = roles.includes("ADMIN");
                let isTracker = roles.includes("TRACKER");
                let isSuperadmin = roles.includes("SUPER_ADMIN");

                if (isAdmin) {
                    navigate("/streams");
                } else if (isTracker) {
                    navigate("/team-cards");
                } else if (isSuperadmin) {
                    navigate("/streams");
                } else {
                    navigate("/home");
                }
            })
            .catch(error => {
                console.error("Error during login:", error);
            });
    }, [navigate, service]);
}

export default AfterLogin;