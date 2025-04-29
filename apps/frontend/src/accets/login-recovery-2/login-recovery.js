import React, {useState} from "react";
import {useLocation, useNavigate} from "react-router-dom";
import "../login-recovery/login-recovery.css"; // Подключаем стили
import eyeOpen from "./Eye-open.png"; // Путь может потребовать корректировки
import eyeClosed from "./Eye-closed.png";

const LoginRecovery2 = () => {
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [successMessage, setSuccessMessage] = useState("");
    const [errorMessage, setErrorMessage] = useState("");
    const navigate = useNavigate();
    const {search} = useLocation();

    // Извлекаем токен из URL, например: ?token=...
    const queryParams = new URLSearchParams(search);
    const token = queryParams.get("token");

    const backendHost = (process.env.REACT_APP_BACKEND_URI || "http://localhost:8080") + '/backend';

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Сброс предыдущих сообщений
        setSuccessMessage("");
        setErrorMessage("");

        // Проверка наличия токена
        if (!token) {
            setErrorMessage("Отсутствует токен восстановления.");
            return;
        }

        const payload = {
            token: token,
            password: password,
        };

        try {
            const response = await fetch(`${backendHost}/api/v1/auth/reset-password`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(payload),
            });

            if (response.ok) {
                setSuccessMessage("Пароль успешно изменён!");
                console.log("Пароль успешно изменён!");
                setTimeout(() => {
                    navigate("/");
                }, 3000);
            } else {
                const data = await response.json();
                if (data && data.error) {
                    setErrorMessage(data.error);
                } else {
                    setErrorMessage("Пароль должен содержать минимум 8 символов, включая буквы и цифры");
                }
                console.error("Ошибка восстановления пароля:", response.status);
            }
        } catch (error) {
            console.error("Ошибка запроса:", error);
            setErrorMessage("Ошибка запроса. Попробуйте позже.");
        }
    };

    const handleRegisterClick = () => {
        navigate("/register");
    };

    return (
        <div className="recovery-container">
            <div className="recovery-box">
                <h2 className="recovery-title">ВХОД</h2>
                <form onSubmit={handleSubmit}>
                    <div className="recovery-input-container">
                        <label className="recovery-label" htmlFor="password">
                            Введите новый пароль
                        </label>
                        <div className="password-container">
                            <input
                                className="recovery-input"
                                id="password"
                                type={showPassword ? "text" : "password"}
                                placeholder="Введите новый пароль"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                            />
                            <img
                                src={showPassword ? eyeClosed : eyeOpen}
                                alt={showPassword ? "Скрыть пароль" : "Показать пароль"}
                                className="eye-icon"
                                onMouseDown={() => setShowPassword(true)}
                                onMouseUp={() => setShowPassword(false)}
                                onMouseLeave={() => setShowPassword(false)}
                                style={{cursor: "pointer"}}
                            />
                        </div>
                    </div>
                    {errorMessage && (
                        <div className="recovery-error">
                            {errorMessage}
                        </div>
                    )}
                    {successMessage && (
                        <div className="recovery-message">
                            {successMessage}
                        </div>
                    )}
                    <button className="recovery-button" type="submit">
                        Восстановить пароль
                    </button>
                    <a
                        href="/register"
                        className="recovery-register"
                        onClick={handleRegisterClick}
                    >
                        Зарегистрироваться
                    </a>
                </form>
            </div>
        </div>
    );
};

export default LoginRecovery2;
