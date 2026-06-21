import "./PasswordReset.css"

import { useState } from "react";
import { useLocation } from "react-router-dom";

import LoginAPI from "../../services/login-service";
import InputBox from "../input-box/InputBox";

import { passwordChecks } from "../../utils/password-validation";

const PasswordReset = () => {
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [status, setStatus] = useState(null);
    const [loading, setLoading] = useState(false);

    const token = new URLSearchParams(useLocation().search).get("token");

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setStatus(null);
        try {
            await LoginAPI.resetPassword(token, password);
            setStatus("success");
        } catch {
            setStatus("error");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="reset-container">
            <div className="reset-box">
                <h1 className="reset-title">Сброс пароля</h1>

                {status === "success" ? (
                    <p className="reset-success">
                        Пароль успешно изменён.{" "}
                        <a href="/client/login" className="reset-link">Войти</a>
                    </p>
                ) : (
                    <form className="reset-form" onSubmit={handleSubmit}>
                        <InputBox
                            type="password"
                            placeholder="Новый пароль"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            errorText={passwordChecks(password)}
                            required
                        />
                        <InputBox
                            type="password"
                            placeholder="Повторите пароль"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            errorText={
                                confirmPassword && password !== confirmPassword
                                    ? "\u{2022} пароли не совпадают"
                                    : ""
                            }
                            required
                        />
                        {status === "error" && (
                            <p className="reset-error">
                                Что-то пошло не так. Ссылка могла устареть.
                            </p>
                        )}
                        <button
                            type="submit"
                            className="reset-button"
                            disabled={
                                loading ||
                                passwordChecks(password) !== ""||
                                password !== confirmPassword
                            }
                        >
                            {loading ? "Сохранение..." : "Сохранить пароль"}
                        </button>
                    </form>
                )}
            </div>
        </div>
    );
};

export default PasswordReset;