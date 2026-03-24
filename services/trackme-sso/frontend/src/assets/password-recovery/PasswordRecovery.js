import { useState } from "react";
import LoginAPI from "../../services/login-service";
import "./PasswordRecovery.css";


const errorMessages = {
    "$account.does.not.exist": "Пользователь с таким email не найден.",
};


const PasswordRecovery = () => {
    const [email, setEmail] = useState("");
    const [status, setStatus] = useState(null);
    const [loading, setLoading] = useState(false);

    const handleBack = () => {
        window.history.back();
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setStatus(null);

        try {
            await LoginAPI.recoveryPassword(email);
            setStatus({ type: "success" });
        } catch (error) {
            const message = errorMessages[error.response?.data?.message]
                || error.response?.data?.message
                || "Что-то пошло не так.";

            setStatus({ type: "error", message });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="recovery-container">
            <div className="recovery-box">
                <h1 className="recovery-title">Восстановление пароля</h1>

                {status?.type === "success" ? (
                    <p className="recovery-success">
                        Письмо с инструкциями отправлено на {email}
                    </p>
                )  : (
                    <form className="recovery-form" onSubmit={handleSubmit}>
                        <input
                            type="email"
                            className="register-input"
                            placeholder="Email"
                            required
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            disabled={loading}
                        />

                {status?.type === "error" && (
                    <p className="recovery-error">
                        {status.message}
                    </p>
                )}
                        <button
                            type="submit"
                            className="recovery-button"
                            disabled={loading}
                        >
                            {loading ? "Отправка..." : "Восстановить"}
                        </button>
                    </form>
                )}

                <button onClick={handleBack} className="recovery-button back-button">
                    Назад
                </button>
            </div>
        </div>
    );
};

export default PasswordRecovery;