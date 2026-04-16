import React, { useState, useEffect } from "react";
import "./Register.css";
import LoginAPI from "../../services/login-service";
import InputBox from "../input-box/InputBox";

const Register = () => {
    const basePath = process.env.REACT_APP_BASE_PATH || "";
    const [username, _setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [fullName, setFullName] = useState("");
    const [email, setEmail] = useState("");
    const [phoneNumber, setPhoneNumber] = useState("");
    const [role, setRole] = useState("TRACKER");
    const [errorMessage, setErrorMessage] = useState("");
    const [showModal, setShowModal] = useState(false);
    const [acceptedPolicy, setAcceptedPolicy] = useState(false);
    const [termsText, setTermsText] = useState("Загрузка...");

    const setUsername = (newUsername) => {
        _setUsername(newUsername.replace("@", ""));
    }

    // if non-empty then wrong
    const passwordChecks = () => {
        const passwordMin = 6;
        return ""
            + (!/[A-Z]/.test(password) ? "\u{2022} пароль должен содержать заглавную букву\n" : "")
            + (!/[a-z]/.test(password) ? "\u{2022} пароль должен содержать строчную букву\n" : "")
            + (!/[0-9]/.test(password) ? "\u{2022} пароль должен содержать цифру\n" : "")
            + (!/[@$!%*?&]/.test(password) ? "\u{2022} пароль должен содержать специальный символ (@$!%*?&)\n" : "")
            + ((password.length < passwordMin) ? `\u{2022} длина пароля должна быть не менее ${passwordMin} символов\n` : "")
            + (password.length !== 0 && (!/[A-Za-z0-9@$!%*?&]+$/.test(password)) ? "\u{2022} пароль должен содержать только латинские символы\n" : "");

    }

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
        fetch(`${basePath}/terms-of-use.txt`)
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
    }, [basePath]);

    return (
        <div className="register-container">
            <div className="register-box">
                <h1 className="register-title">Регистрация</h1>
                {errorMessage && <div className="error-message">{errorMessage}</div>}
                <form className="register-form" onSubmit={handleRegister}>
                    <InputBox
                        type="text"
                        placeholder="Имя пользователя в Telegram (без @)"
                        value={username}
                        // TODO: почему некторые telegram ники не проходят (скорее всего бэкенд)
                        onChange={(e) => setUsername(e.target.value)}
                        required
                    />
                    <InputBox
                        type="password"
                        placeholder="Пароль"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        errorText={
                            passwordChecks()
                        }
                        required
                    />
                    <InputBox
                        type="text"
                        placeholder="ФИО"
                        value={fullName}
                        onChange={(e) => setFullName(e.target.value)}
                        required
                    />
                    <InputBox
                        type="email"
                        placeholder="Email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />
                    <InputBox
                        type="tel"
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
                    </select>
                    <button type="submit" className="register-button" disabled={passwordChecks() !== ""}>
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

