import React from "react";
import "./HomePage.css"; // Подключаем стили

const HomePage = () => {
    const ssoLoginUrl = process.env.REACT_APP_BACKEND_HOST || "http://localhost:8080";
    const clientId = process.env.REACT_APP_OAUTH2_CLIENT_ID;

    const handleSSOLogin = () => {
        window.location.href = `${ssoLoginUrl}/oauth2/authorization/${clientId}`;
    };

    return (
        <div className="home-container">
            <div className="home-box">
                <h1 className="home-title">Добро пожаловать в TrackMe</h1>
                <p className="home-description">Управляйте своими потоками и командами с
                    легкостью.</p>
                <button className="home-sso-button" onClick={handleSSOLogin}>
                    Войти через SSO
                </button>
            </div>
        </div>
    );
};

export default HomePage;