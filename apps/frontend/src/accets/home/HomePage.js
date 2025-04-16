import React from "react";
import "./HomePage.css";

const HomePage = () => {
    const ssoLoginUrl = process.env.REACT_APP_BACKEND_HOST || "http://localhost:8080";

    const handleSSOLogin = () => {

        window.location.href = `${ssoLoginUrl}/oauth2/authorization/track-me-client`;
    };

    const handleGithubLogin = () => {
        window.location.href = `${ssoLoginUrl}/oauth2/authorization/github`;
    };

    const handleGoogleLogin = () => {
        window.location.href = `${ssoLoginUrl}/oauth2/authorization/google`;
    };

    const handleTelegramLogin = () => {
        window.location.href = `${ssoLoginUrl}/oauth2/authorization/telegram`;
    };

    return (
        <div className="home-container">
            <div className="home-box">
                <h1 className="home-title">Добро пожаловать в TrackMe</h1>
                <p className="home-description">Управляйте своими потоками и командами с
                    легкостью.</p>
                <div className="home-provider-buttons">
                    <button className="home-provider-button" onClick={handleGithubLogin}>
                        <img src="/icons/github-logo.svg" alt="GitHub"/>
                    </button>
                    <button className="home-provider-button" onClick={handleGoogleLogin}>
                        <img src="/icons/google-logo.svg" alt="Google"/>
                    </button>
                    <button className="home-provider-button" onClick={handleTelegramLogin}>
                        <img src="/icons/telegram-logo.svg" alt="Telegram"/>
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