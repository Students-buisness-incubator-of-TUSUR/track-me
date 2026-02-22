import React from "react";
import "./HomePage.css";

import {useLocation } from 'react-router-dom';

const HomePage = () => {
    const clientGatewayUri = process.env.REACT_APP_BACKEND_URI || "http://localhost:8080";
    
    const location = useLocation();
    const queryParams = new URLSearchParams(location.search);
    const sessionExpired = queryParams.get('sessionExpired') === 'true';
    const handleSSOLogin = () => {

        window.location.href = `${clientGatewayUri}/oauth2/authorization/track-me-client?redirect_uri=${encodeURIComponent(window.location.origin + '/after-login')}`;
    };


    const handleGithubLogin = () => {
        alert("в разработке");
    };

    const handleGoogleLogin = () => {
        alert("в разработке");
    };

    const handleTelegramLogin = () => {
        alert("в разработке");
    };

    return (
        <div className="home-container">
            <div className="home-box">
                {sessionExpired && (
                <div className="session-expired">
                    Ваша сессия истекла. Пожалуйста, авторизируйтесь заново.
                </div>
            )}
                <h1 className="home-title">Добро пожаловать в TrackMe</h1>
                <p className="home-description">Управляйте своими потоками и командами с
                    легкостью.</p>
                <div className="home-provider-buttons">
                    <button className="home-provider-button" onClick={handleGoogleLogin} disabled>
                        <img src="/icons/google-logo.svg" alt="Google"/>
                    </button>
                    <button className="home-provider-button" onClick={handleTelegramLogin} disabled>
                        <img src="/icons/telegram-logo.svg" alt="Telegram"/>
                    </button>
                    <button className="home-provider-button" onClick={handleGithubLogin} disabled>
                        <img src="/icons/github-logo.svg" alt="GitHub"/>
                    </button>
                </div>
                <button className="home-sso-button" onClick={handleSSOLogin}>
                    Войти через SSO
                </button>
            </div>
        </div>
    );
};

export default HomePage;