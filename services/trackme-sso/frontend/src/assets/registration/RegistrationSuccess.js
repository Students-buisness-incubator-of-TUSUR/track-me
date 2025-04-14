import React from "react";
import "./Register.css";

const RegistrationSuccess = () => {
    return (
        <div className="register-container">
            <div className="register-box">
                <h1 className="register-title">Регистрация успешна!</h1>
                <p>На вашу почту отправлено сообщение с ссылкой для подтверждения.</p>
            </div>
        </div>
    );
};

export default RegistrationSuccess;