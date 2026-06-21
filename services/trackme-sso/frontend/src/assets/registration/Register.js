import React, { useState, useEffect } from "react";
import "./Register.css";
import LoginAPI from "../../services/login-service";
import InputBox from "../input-box/InputBox";

const Register = () => {
    const basePath = process.env.REACT_APP_BASE_PATH || "";
    const [username, _setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [fullName, setFullName] = useState("");
    const [email, setEmail] = useState("");
    const [phoneNumber, setPhoneNumber] = useState("");
    const [role, setRole] = useState("TRACKER");
    const [errorMessage, setErrorMessage] = useState("");
    const [showModal, setShowModal] = useState(false);
    const [acceptedPolicy, setAcceptedPolicy] = useState(false);
    const termsText = `1. ОБЩИЕ ПОЛОЖЕНИЯ
1.1. Настоящее пользовательское соглашение (далее – Соглашение) относится к использованию веб-сервиса TrackMe (далее – Сервис) и заключается между федеральным государственным автономным образовательным учреждением высшего образования «Томский государственный университет систем управления и радиоэлектроники» (далее – ТУСУР, ИНН 7021000043, ОГРН 1027000867068, зарегистрирован по адресу: 634050, г. Томск, пр. Ленина, д. 40), являющимся правообладателем Сервиса с одной стороны, и физическим лицом (далее – Пользователь), использующим данный сервис с другой стороны.
1.2 Соглашение является публичной офертой. Используя Сервис, в том числе, путем просмотра страниц и материалов, Пользователь подтверждает, что ознакомлен и согласен с Правовой информацией Сервиса и настоящим Соглашением и безоговорочно принимает все условия Соглашения.
2. ПРЕДМЕТ СОГЛАШЕНИЯ
2.1 Предметом Соглашения является предоставление Пользователю доступа к Сервису, согласно выбранной им и подтверждённой администрацией Сервиса роли (трекер или администратор).
2.2 Под действие Соглашения подпадают все существующие (реально функционирующие) на данный момент услуги Сервиса, а также любые их последующие модификации и появляющиеся в дальнейшем дополнительные услуги.
3. ПРИНИМАЯ СОГЛАШЕНИЕ
3.1 Пользователь подтверждает, что достиг возраста совершеннолетия по законодательству Российской Федерации.
3.2 Пользователь обязуется использовать Сервис в соответствии с действующими и применимыми в данной области законами Российской Федерации.
3.3 Пользователь соглашается, что не имеет права модифицировать, продавать, распространять любые материалы, размещённые на Сервисе (в том числе созданные самим пользователем) целиком или по частям.
3.4 Пользователь уведомляется, что Сервис является объектом авторского права и защищается законодательством РФ. Все материалы, созданные на Сервисе в рамках работы Пользователя, не могут стать принадлежащим ему объектом авторского права и могут свободно использоваться администрацией Сервиса в рамках действующего законодательства Российской Федерации.
3.5 Пользователь осознаёт, что при использовании Сервиса запрещена загрузка, передача, изменение или размещение информации, данных или изображений, которые нарушают исключительные права третьих лиц, в частности авторские и смежные с ними права, а также исключительные права на изобретение, полезную модель, промышленный образец или товарный знак.
3.6 Пользователь даёт согласие ТУСУРу на обработку своих персональных данных в целях организации работы Сервиса, то есть на совершение действий, предусмотренных п. 3 ст. 3 Федерального закона от 27.07.2006 № 152-ФЗ «О персональных данных». Перечень персональных данных, на обработку которых дается согласие:
- фамилия, имя, отчество;
- адрес электронной почты;
- ник-нейм в мессенджере Telegram;
- номер телефона;
- результаты работы в рамках Сервиса;
- фотографии.
4. ЗАКЛЮЧИТЕЛЬНЫЕ ПОЛОЖЕНИЯ
4.1 ТУСУР имеет право в любой момент вносить изменение в Соглашение в одностороннем порядке без какого-либо специального уведомления. Новая редакция Соглашения вступает в силу с момента ее размещения на Сервисе.
4.2 ТУСУР оставляет за собой право незамедлительно без предварительного предупреждения ограничить или блокировать доступ к Сервису или принимать иные меры в отношении Пользователя, нарушившего условия настоящего Соглашения. Кроме того, в случае нарушения Пользователем Соглашения, ТУСУР обязуется активно содействовать представителям органов охраны правопорядка в сборе и предоставлении информации о Пользователе, нарушившем настоящее Соглашение, а также законодательство Российской Федерации.
4.3 Давая согласие Пользователь подтверждает своё безоговорочное согласие со всеми пунктами Соглашения, а также своё соответствие всем условиям, отражённым в формах регистрации и необходимыми для успешной регистрации.
4.4. ТУСУР вправе в одностороннем порядке отказаться от настоящего Соглашения и прекратить предоставление услуг по использованию Сервиса.
4.5 Все споры между ТУСУРом и Пользователем разрешаются путём переговоров, а в случае невозможности мирного урегулирования – в соответствии с законодательством Российской Федерации.
4.6 Все отношения сторон по настоящему Соглашению регулируются законодательством Российской Федерации.`;

    const setUsername = (newUsername) => {
        _setUsername(newUsername.replace("@", ""));
    }

    const usernameChecks = () => {
        const usernameMin = 6;
        if (username.length === 0) return "";
        return ""
            + ((username.length < usernameMin) ? `• имя пользователя должно быть не менее ${usernameMin} символов\n` : "")
            + (/[^A-Za-z0-9_.-]/.test(username) ? "• имя пользователя должно содержать только латинские буквы, цифры, _, ., -\n" : "");
    }

    const passwordChecks = () => {
        const passwordMin = 6;
        
        if (password.length === 0) return "";
        
        const isValidChar = (char) => /[A-Za-z0-9@$!%*?&-]/.test(char);
        const validPassword = password.split("").every(isValidChar);
        
        return ""
            + (!/[A-Z]/.test(password) ? "• пароль должен содержать заглавную букву\n" : "")
            + (!/[a-z]/.test(password) ? "• пароль должен содержать строчную букву\n" : "")
            + (!/[0-9]/.test(password) ? "• пароль должен содержать цифру\n" : "")
            + (!password.includes("@") && !password.includes("$") && !password.includes("!") && !password.includes("%") && !password.includes("*") && !password.includes("?") && !password.includes("&") && !password.includes("-") ? "• пароль должен содержать специальный символ (@$!%*?&-)\n" : "")
            + ((password.length < passwordMin) ? `• длина пароля должна быть не менее ${passwordMin} символов\n` : "")
            + (!validPassword ? "• пароль должен содержать только латинские символы\n" : "");
    }

    const handleRegister = (e) => {
        e.preventDefault();
        setErrorMessage("");
        
        if (username.length < 6) {
            setErrorMessage("Имя пользователя должно быть не менее 6 символов");
            return;
        }
        
        if (passwordChecks()) {
            setErrorMessage("Пароль не удовлетворяет требованиям");
            return;
        }
        
        setShowModal(true);
    };

    const handleModalSubmit = () => {
        if (!acceptedPolicy) return;

        const userData = { username, password, fullName, email, phoneNumber, role };
        LoginAPI.register(userData)
            .then((response) => {
                if (response.status === 200) {
                    window.location.href = `${basePath}/client/registration-success`;
                } else {
                    throw new Error("Registration failed");
                }
            })
            .catch((error) => {
                console.error("Registration failed", error);
                const message = error.response?.data?.message || "Ошибка регистрации. Попробуйте снова.";
                setErrorMessage(message);
                setShowModal(false);
            });
    };

    const handleBack = () => {
        if (showModal) {
            setShowModal(false);
        } else {
            window.history.back();
        }
    };

    useEffect(() => {
    }, []);

    return (
        <div className="register-container">
            <div className="register-box">
                <h1 className="register-title">Регистрация</h1>
                {errorMessage && <div className="error-message">{errorMessage}</div>}
                <form className="register-form" onSubmit={handleRegister}>
                    <InputBox
                        type="text"
                        placeholder="Имя пользователя в Telegram (без @)"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        required
                        autoComplete="username"
                        errorText={usernameChecks()}
                    />
                    <InputBox
                        type="password"
                        placeholder="Пароль"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        errorText={
                            passwordChecks()
                        }
                        required
                    />
                    <InputBox
                        type="text"
                        placeholder="ФИО"
                        value={fullName}
                        onChange={(e) => setFullName(e.target.value)}
                        required
                    />
                    <InputBox
                        type="email"
                        placeholder="Email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                        autoComplete="off"
                    />
                    <InputBox
                        type="tel"
                        placeholder="Номер телефона"
                        value={phoneNumber}
                        onChange={(e) => setPhoneNumber(e.target.value)}
                        required
                    />
                    <select
                        className="register-input"
                        value={role}
                        onChange={(e) => setRole(e.target.value)}
                    >
                        <option value="TRACKER">Трекер</option>
                        <option value="ADMIN">Администратор</option>
                    </select>
                    <button type="submit" className="register-button" disabled={passwordChecks() !== "" || usernameChecks() !== ""}>
                        Зарегистрироваться
                    </button>
                </form>
                <button onClick={handleBack} className="register-button back-button">
                    Назад
                </button>
            </div>
            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <h2>Пользовательское соглашение</h2>
                        <div className="modal-text">
                            <div className="terms-text">
                                {termsText.split('\n').map((line, index) => (
                                    <p key={index}>{line}</p>
                                ))}
                            </div>
                        </div>

                        <label className="policy-agreement">
                            <input
                                type="checkbox"
                                checked={acceptedPolicy}
                                onChange={() => setAcceptedPolicy(!acceptedPolicy)}
                            />
                            Я даю согласие на обработку персональных данных
                        </label>

                        <div className="modal-buttons">
                            <button
                                className="register-button back-button"
                                onClick={() => setShowModal(false)}
                            >
                                Назад
                            </button>
                            <button
                                className={`register-button ${acceptedPolicy ? "" : "disabled"}`}
                                onClick={acceptedPolicy ? handleModalSubmit : undefined}
                                disabled={!acceptedPolicy}
                            >
                                Принимаю
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Register;