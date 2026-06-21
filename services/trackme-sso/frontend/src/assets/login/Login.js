import React, {useEffect, useState} from "react";
import {useLocation} from "react-router-dom";
import InputBox from "../input-box/InputBox";
import "./Login.css";

const Login = () => {
    const basePath = process.env.REACT_APP_BASE_PATH || "";
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [errorMessage, setErrorMessage] = useState("");
    const location = useLocation();

    const [csrfToken, setCsrfToken] = useState("");
    const [csrfParameterName, setCsrfParameterName] = useState("_csrf");

    useEffect(() => {
        fetch(`${basePath}/api/csrf`, {credentials: 'same-origin'})
            .then(r => r.json())
            .then(data => {
                setCsrfToken(data.token);
                setCsrfParameterName(data.parameterName);
            });
    }, [basePath]);


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
                    action={`${basePath}/client/login`}
                    autoComplete="username">
                    <InputBox
                        type="text"
                        name="username"
                        className="login-input"
                        placeholder="Имя пользователя в Telegram (без @)"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        required
                        autoComplete="username"
                    />
                    <InputBox
                        type="password"
                        name="password"
                        className="login-input"
                        placeholder="Пароль"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                        autoComplete="current-password"
                    />
                    <input type="hidden" name={csrfParameterName} value={csrfToken}/>
                    <button type="submit" className="login-button">
                        Войти
                    </button>
                </form>
                {errorMessage && (
                    <p className="error-message">{errorMessage}</p>
                )}
                <div className="login-links">
                    <a href={`${basePath}/client/registration`} className="login-link">
                        Регистрация
                    </a>
                    <a href={`${basePath}/client/recovery`} className="login-link">
                        Забыли пароль?
                    </a>
                </div>
            </div>
        </div>
    );
};

export default Login;
