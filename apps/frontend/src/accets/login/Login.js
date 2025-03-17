import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { jwtDecode } from "jwt-decode";
import "./Login.css"; // Подключаем стили
import eyeOpen from "./Eye-open.png";
import eyeClosed from "./Eye-closed.png";
const Login = () => {
  const [showPassword, setShowPassword] = useState(false);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate(); // Используем хук для навигации
  const backendHost = process.env.REACT_APP_BACKEND_HOST || 'http://localhost:8080';

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (username.length < 4 || username.length > 20) {
      setErrorMessage("Ошибка! Telegram ID должен быть от 4 до 20 символов");
      return;
    }
    if (password.length < 6 || password.length > 20) {
      setErrorMessage("Ошибка! Пароль должен быть от 6 до 20 символов");
      return;
    }

    setErrorMessage("");
    setLoading(true);

    try {
      
      const response = await fetch(
        `${backendHost}/api/v1/auth/sing-in`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            telegramId: username,
            password: password,
          }),
        }
      );
      

      const data = await response.json();

      if (response.ok) {
        localStorage.setItem("accessToken", data.accessToken); // Сохранение токена

        // Декодируем токен, чтобы получить роль
        const decodedToken = jwtDecode(data.accessToken);
        const userRole = decodedToken.role; // Роль из токена
        const userID = decodedToken.id;
        console.log("Роль пользователя:", userRole);
        console.log("ID пользователя:", userID);
        // Перенаправление в зависимости от роли
        if (userRole === "SUPER_ADMIN") {
          navigate("/streams");
        } else if (userRole === "ADMIN") {
          navigate("/streams");
        } else if (userRole === "TRACKER") {
          navigate("/team-cards");
        } else {
          setErrorMessage("Неизвестная роль пользователя.");
        }
      } else {
        setErrorMessage("Ошибка авторизации! Проверьте данные.");
      }
    } catch (error) {
      setErrorMessage("Ошибка сети! Попробуйте позже.");
    } finally {
      setLoading(false);
    }
  };

  const handleRegisterClick = () => {
    navigate("/register"); // Переход на страницу регистрации
  };

  const handleRecoveryClick = () => {
    navigate("/recovery"); // Переход на страницу регистрации
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <h2 className="login-h2">ВХОД</h2>
        {errorMessage && <p className="error-message oval">{errorMessage}</p>}
        <form onSubmit={handleSubmit}>
          <div className="input-container">
            <label className="login-label" htmlFor="username">Имя пользователя telegram</label>
            <input
              className="login-input"
              id="username"
              type="text"
              placeholder="Введите имя"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>

          <div className="input-container">
            <label className="login-label" htmlFor="password">Пароль</label>
            <div className="password-container">
              <input
                className="login-input"
                id="password"
                type={showPassword ? "text" : "password"}
                placeholder="Введите пароль"
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
                style={{ cursor: "pointer" }}
              />
            </div>
          </div>

          <a href="/recovery" className="forgot-password" onClick={handleRecoveryClick}>
            Забыли пароль? Восстановить?
          </a>

          <button className="login-button" type="submit" disabled={loading}>
            {loading ? "Вход..." : "Войти"}
          </button>

          <a
            href="/register"
            className="register"
            onClick={handleRegisterClick}
          >
            Зарегистрироваться
          </a>
        </form>
      </div>
    </div>
  );
};

export default Login;