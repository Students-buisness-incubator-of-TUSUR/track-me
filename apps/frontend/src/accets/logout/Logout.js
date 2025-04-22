import React, {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import "./../login/Login.css";
import loginService from "../../services/login-service";

const Logout = () => {
    const [errorMessage, setErrorMessage] = useState("");
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();
    let service = loginService();

    useEffect(() => {
        const doLogout = async () => {
            try {
                // Здесь url можно вынести в константу, если часто используется
                await service.logout();
                setLoading(false);
                setTimeout(() => navigate("/"), 2500);
            } catch (error) {
                setErrorMessage("Ошибка при выходе из аккаунта. Попробуйте ещё раз.");
                setLoading(false);
                console.log(error);
            }
        };
        doLogout().then(r => {
            console.log(r);
        });
    }, [navigate, service]);

    return (
        <div className="login-container">
            <div className="login-box">
                <h2 className="login-h2">Выход</h2>
                {loading && <p style={{color: "white", fontSize: 28}}>Выходим...</p>}
                {errorMessage && <p className="error-message oval">{errorMessage}</p>}
                {!loading && !errorMessage && (
                    <p style={{color: "white", fontSize: 24}}>Вы успешно вышли!<br/>Сейчас
                        произойдет перенаправление.</p>
                )}
            </div>
        </div>
    );
};

export default Logout;