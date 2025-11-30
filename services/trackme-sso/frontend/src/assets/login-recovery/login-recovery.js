import "./login-recovery.css";

const LoginRecovery = () => {
    const handleBack = () => {
        window.history.back();
    };
    return(
        <div className="recovery-container">
            <div className="recovery-box">
                <h1 className="recovery-title">Восстановление пароля</h1>
                <form className="recovery-form">
                    <input
                        type="email"
                        className="register-input"
                        placeholder="Email"
                        required
                    />
                    <button type="submit" className="recovery-button">
                        Восстановить
                    </button>
                </form>
                <button onClick={handleBack} className="recovery-button back-button">
                    Назад
                </button>
            </div>
        </div>
    );
};

export default LoginRecovery;