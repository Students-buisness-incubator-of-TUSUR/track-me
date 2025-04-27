import React, {useState} from "react";
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

    const handleRegister = (e) => {
        e.preventDefault();
        const userData = {username, password, fullName, email, phoneNumber, role};
        LoginAPI.register(userData)
            .then((response) => {
                if (response.status === 200) {
                    window.location = `${basePath}/client/registration-success`;
                } else {
                    throw new Error("Registration failed");
                }
            })
            .catch((error) => {
                console.error("Registration failed", error);
                const message = error.response?.data?.message || "Ошибка регистрации. Попробуйте снова.";
                setErrorMessage(message);
            });
    };

    const handleBack = () => {
        window.history.back();
    };

    return (
        <div className="register-container">
            <div className="register-box">
                <h1 className="register-title">Регистрация</h1>
                {errorMessage && <div className="error-message">{errorMessage}</div>}
                <form className="register-form" onSubmit={handleRegister}>
                    <input
                        type="text"
                        className="register-input"
                        placeholder="Имя пользователя"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        required
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
        </div>
    );
};

export default Register;