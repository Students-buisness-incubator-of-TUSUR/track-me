import React, { useState, useEffect } from "react";
import "./Register.css";
import LoginAPI from "../../services/login-service";

const Register = () => {
    const basePath = process.env.REACT_APP_BASE_PATH || "";
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [fullName, setFullName] = useState("");
    const [email, setEmail] = useState("");
    const [phoneNumber, setPhoneNumber] = useState("");
    const [role, setRole] = useState("TRACKER");
    const [errorMessage, setErrorMessage] = useState("");
    const [showModal, setShowModal] = useState(false);
    const [acceptedPolicy, setAcceptedPolicy] = useState(false);
    const [termsText, setTermsText] = useState("Загрузка...");

    const handleRegister = (e) => {
        e.preventDefault();
        setErrorMessage("");
        setShowModal(true); // Показываем модальное окно
    };

    const handleModalSubmit = () => {
        if (!acceptedPolicy) return; // Кнопка "Продолжить" неактивна, но на всякий случай проверяем

        const userData = { username, password, fullName, email, phoneNumber, role };
        LoginAPI.register(userData)
            .then((response) => {
                if (response.status === 200) {
                    window.location.href = `${basePath}/client/registration-success`;
                } else {
                    throw new Error("Registration failed");
                }
            })
            .catch((error) => {
                console.error("Registration failed", error);
                const message = error.response?.data?.message || "Ошибка регистрации. Попробуйте снова.";
                setErrorMessage(message);
                setShowModal(false);
            });
    };

    const handleBack = () => {
        if (showModal) {
            setShowModal(false); // Закрываем модальное окно
        } else {
            window.history.back();
        }
    };

    useEffect(() => {
        fetch('/terms-of-use.txt')
            .then(response => {
            if (!response.ok) throw new Error('Файл не найден');
            return response.text();
            })
            .then(text => {
            setTermsText(text);
            })
            .catch(err => {
            console.error("Ошибка загрузки соглашения:", err);
            setTermsText("Не удалось загрузить пользовательское соглашение. Пожалуйста, свяжитесь с поддержкой.");
            });
    }, []);

    return (
        <div className="register-container">
            <div className="register-box">
                <h1 className="register-title">Регистрация</h1>
                {errorMessage && <div className="error-message">{errorMessage}</div>}
                <form className="register-form" onSubmit={handleRegister}>
                    <input
                        type="text"
                        className="register-input"
                        placeholder="Имя пользователя в Telegram (без @)"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        required
                        pattern="^[a-zA-Z0-9_]+$"
                        title="Имя пользователя в Telegram без @, должно содержать только латинские буквы, цифры и _"
                    />
                    <input
                        type="password"
                        className="register-input"
                        placeholder="Пароль"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                    <input
                        type="text"
                        className="register-input"
                        placeholder="ФИО"
                        value={fullName}
                        onChange={(e) => setFullName(e.target.value)}
                        required
                    />
                    <input
                        type="email"
                        className="register-input"
                        placeholder="Email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />
                    <input
                        type="tel"
                        className="register-input"
                        placeholder="Номер телефона"
                        value={phoneNumber}
                        onChange={(e) => setPhoneNumber(e.target.value)}
                        required
                    />
                    <select
                        className="register-input"
                        value={role}
                        onChange={(e) => setRole(e.target.value)}
                        required
                    >
                        <option value="TRACKER">Трекер</option>
                        <option value="ADMIN">Администратор</option>
                        <option value="SUPER_ADMIN">Супер Администратор</option>
                    </select>
                    <button type="submit" className="register-button">
                        Зарегистрироваться
                    </button>
                </form>
                <button onClick={handleBack} className="register-button back-button">
                    Назад
                </button>
            </div>
            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <h2>Пользовательское соглашение</h2>
                        <div className="modal-text">
                            <div className="terms-text">
                                {termsText.split('\n').map((line, index) => (
                                    <p key={index}>{line}</p>
                                ))}
                            </div>
                        </div>

                        <label className="policy-agreement">
                            <input
                                type="checkbox"
                                checked={acceptedPolicy}
                                onChange={() => setAcceptedPolicy(!acceptedPolicy)}
                            />
                            Я даю согласие на обработку персональных данных
                        </label>

                        <div className="modal-buttons">
                            <button
                                className="register-button back-button"
                                onClick={() => setShowModal(false)}
                            >
                                Назад
                            </button>
                            <button
                                className={`register-button ${acceptedPolicy ? "" : "disabled"}`}
                                onClick={acceptedPolicy ? handleModalSubmit : undefined}
                                disabled={!acceptedPolicy}
                            >
                                Принимаю
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Register;