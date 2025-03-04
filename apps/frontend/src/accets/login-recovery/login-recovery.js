import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./login-recovery.css";

const LoginRecovery = () => {
  const [username, setUsername] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const navigate = useNavigate();

  const backendHost = process.env.REACT_APP_BACKEND_HOST || "http://localhost:8080";

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Сброс предыдущих сообщений
    setSuccessMessage("");
    setErrorMessage("");

    // Проверка формата почты с помощью регулярного выражения
    const emailRegex = /^\S+@\S+\.\S+$/;
    if (!emailRegex.test(username)) {
      setErrorMessage("Неверный формат почты");
      return;
    }

    const payload = { email: username };

    try {
      const response = await fetch(`${backendHost}/api/v1/auth/forgot-password`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      if (response.ok) {
        setSuccessMessage("Письмо для восстановления отправлено на почту");
        console.log("Письмо для восстановления отправлено");
      } else {
        // Попытка извлечь сообщение об ошибке из ответа
        const data = await response.json();
        if (data && data.error) {
          setErrorMessage(data.error);
        } else {
          setErrorMessage("Ошибка восстановления пароля. Попробуйте еще раз.");
        }
        console.error("Ошибка восстановления пароля:", response.status);
      }
    } catch (error) {
      setErrorMessage("Ошибка запроса. Попробуйте позже.");
      console.error("Ошибка запроса:", error);
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
            <label className="recovery-label" htmlFor="username">
              Введите почту
            </label>
            <input
              className="recovery-input"
              id="username"
              type="text"
              placeholder="Введите почту"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
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

export default LoginRecovery;
