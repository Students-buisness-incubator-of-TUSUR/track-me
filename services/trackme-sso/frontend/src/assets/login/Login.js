import React, {useState} from "react";
import "./Login.css";
import LoginAPI from "../../services/login-service";

const Login = () => {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");

    const handleLogin = (e) => {
        e.preventDefault();
        LoginAPI.login(username, password)
            .catch((error) => {
                console.error("Login failed", error);
                alert("Ошибка! Неверный логин или пароль");
            });
    };

    return (
        <div className="login-container">
            <div className="login-box">
                <h1 className="login-title">Вход в TrackMe</h1>
                <form className="login-form" onSubmit={handleLogin}>
                    <input
                        type="text"
                        className="login-input"
                        placeholder="Логин"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        required
                    />
                    <input
                        type="password"
                        className="login-input"
                        placeholder="Пароль"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                    <button type="submit" className="login-button">
                        Войти
                    </button>
                </form>
                <div className="login-links">
                    <a href={`/register`} className="login-link">
                        Регистрация
                    </a>
                    <a href={`/reset-password`} className="login-link">
                        Забыли пароль?
                    </a>
                </div>
            </div>
        </div>
    );
};

export default Login;