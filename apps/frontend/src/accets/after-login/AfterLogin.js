import {useEffect} from "react";
import loginService from "../../services/login-service";
import {useNavigate} from "react-router-dom";

function AfterLogin() {
    let service = loginService();
    const navigate = useNavigate();

    useEffect(() => {
        service.getUserInfo()
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