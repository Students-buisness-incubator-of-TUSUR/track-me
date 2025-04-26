import React, {useEffect, useState} from "react";
import {useLocation} from "react-router-dom";
import "./Login.css";

const Login = () => {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [errorMessage, setErrorMessage] = useState("");
    const location = useLocation();

    useEffect(() => {
        // Проверка: если пришли на страницу с query ?error или error в state - выводим ошибку
        const params = new URLSearchParams(location.search);
        if (params.has("error")) {
            setErrorMessage("Неверный логин или пароль.");
        }
    }, [location]);

    return (
        <div className="login-container">
            <div className="login-box">
                <h1 className="login-title">Вход в TrackMe</h1>
                <form
                    className="login-form"
                    method="POST"
                    action="/sso/client/login"
                    autoComplete="username">
                    <input
                        type="text"
                        name="username"
                        className="login-input"
                        placeholder="Логин"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        required
                        autoComplete="username"
                    />
                    <input
                        type="password"
                        name="password"
                        className="login-input"
                        placeholder="Пароль"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                        autoComplete="current-password"
                    />
                    <button type="submit" className="login-button">
                        Войти
                    </button>
                </form>
                {errorMessage && (
                    <p className="error-message">{errorMessage}</p>
                )}
                <div className="login-links">
                    <a href="/sso/client/registration" className="login-link">
                        Регистрация
                    </a>
                    <a href="/sso/client/reset-password" className="login-link">
                        Забыли пароль?
                    </a>
                </div>
            </div>
        </div>
    );
};

export default Login;