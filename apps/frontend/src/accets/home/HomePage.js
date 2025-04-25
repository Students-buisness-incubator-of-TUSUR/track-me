import React from "react";
import "./HomePage.css";

const HomePage = () => {
    const clientGatewayUri = process.env.REACT_APP_CLIENT_GATEWAY_URI || "http://localhost:8080";

    const handleSSOLogin = () => {

        window.location.href = `${clientGatewayUri}/oauth2/authorization/track-me-client`;
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