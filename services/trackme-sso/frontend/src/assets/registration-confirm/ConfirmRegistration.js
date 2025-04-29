import React, {useEffect, useState} from "react";
import {useNavigate, useSearchParams} from "react-router-dom";
import axios from "../../csrf-axios-interceptor";
import "./ConfirmRegistration.css";

const ConfirmRegistration = () => {
    const [searchParams] = useSearchParams();
    const [status, setStatus] = useState("pending");
    const [message, setMessage] = useState("");
    const navigate = useNavigate();

    useEffect(() => {
        const token = searchParams.get("token");
        if (!token) {
            setStatus("error");
            setMessage("Токен подтверждения не найден в ссылке.");
            return;
        }
        axios.post(
            `/api/v1/registration/confirm?token=${encodeURIComponent(token)}`,
            {},                         // post body
            {withCredentials: true}   // <--- добавлено!
        ).then(() => {
            setStatus("success");
            setMessage("Ваш аккаунт успешно подтвержден! Теперь вы можете войти.");
        }).catch((error) => {
            setStatus("error");
            const msg = error.response?.data?.message || "Ошибка подтверждения регистрации. Попробуйте позже.";
            setMessage(msg);
        });
    }, [searchParams]);

    const handleGoToLogin = () => {
        navigate("/client/login");
    };

    return (
        <div className="confirm-container">
            <div className="confirm-box">
                <h1 className="confirm-title">Подтверждение регистрации</h1>
                <div className={`confirm-message ${status}`}>
                    {message || "Подтверждение..."}
                </div>
                <button onClick={handleGoToLogin} className="confirm-button">
                    Перейти ко входу
                </button>
            </div>
        </div>
    );
};

export default ConfirmRegistration;