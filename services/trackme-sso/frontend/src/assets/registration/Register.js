import React, {useState} from "react";
import "./Register.css";
import LoginAPI from "../../services/login-service";

const Register = () => {
    const [fullName, setFullName] = useState("");
    const [email, setEmail] = useState("");
    const [phone, setPhone] = useState("");
    const [telegramId, setTelegramId] = useState("");
    const [role, setRole] = useState("TRACKER");

    const handleRegister = (e) => {
        e.preventDefault();
        const userData = {fullName, email, phone, telegramId, role};
        LoginAPI.register(userData)
            .then(() => {
                window.location = "/registration-success";
            })
            .catch((error) => {
                console.error("Registration failed", error);
                alert("Ошибка! Проверьте введенные данные.");
            });
    };

    return (
        <div className="register-container">
            <div className="register-box">
                <h1 className="register-title">Регистрация</h1>
                <form className="register-form" onSubmit={handleRegister}>
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
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        required
                    />
                    <input
                        type="text"
                        className="register-input"
                        placeholder="Telegram ID"
                        value={telegramId}
                        onChange={(e) => setTelegramId(e.target.value)}
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
            </div>
        </div>
    );
};

export default Register;