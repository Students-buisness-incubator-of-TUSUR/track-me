import React, {useState} from 'react';
import './Registration.css';
import axios from 'axios';

const Registration = () => {
    const [form, setForm] = useState({
        fullName: '',
        email: '',
        phoneNumber: '',
        telegramId: '',
        password: '',
        confirmPassword: '',
        role: '',
    });
    const [selectedFile, setSelectedFile] = useState(null);
    const [previewPhoto, setPreviewPhoto] = useState(null);
    const backendHost = (process.env.BACKEND_URI || 'http://localhost:8080') + '/backend';

    const handleChange = (e) => {
        setForm({...form, [e.target.name]: e.target.value});
    };

    // Обработчик выбора файла для загрузки фото
    const handlePhotoChange = (e) => {
        const file = e.target.files[0];
        if (!file) return;
        setSelectedFile(file);
        const imageUrl = URL.createObjectURL(file);
        setPreviewPhoto(imageUrl);
    };

    // Функция загрузки фото на сервер через запрос к /api/v1/users/{telegramId}/photo
    const uploadPhoto = async (telegramId) => {
        if (!selectedFile) return;
        // Если токен используется, можно его брать из localStorage
        const token = localStorage.getItem("accessToken");
        const formData = new FormData();
        formData.append("file", selectedFile);

        try {
            await axios.post(
                `${backendHost}/api/v1/users/${telegramId}/photo`,
                formData,
                {
                    headers: {
                        Authorization: `Bearer ${token}`,
                        'Content-Type': 'multipart/form-data'
                    }
                }
            );
            console.log("Фото успешно загружено");
        } catch (error) {
            console.error("Ошибка загрузки фото:", error.response?.data || error.message);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (form.password !== form.confirmPassword) {
            alert('Пароли не совпадают!');
            return;
        }

        // Разбиваем полное имя (если необходимо)
        const [lastName = '', firstName = '', middleName = ''] = form.fullName.split(' ');

        const userData = {
            fullName: form.fullName,
            firstName,
            lastName,
            middleName,
            phoneNumber: form.phoneNumber,
            telegramId: form.telegramId,
            email: form.email,
            password: form.password,
            role: form.role,
        };

        try {
            const response = await axios.post(
                `${backendHost}/api/v1/auth/sing-up`,
                userData,
                {
                    headers: {
                        'Content-Type': 'application/json',
                    },
                }
            );
            console.log('User registered successfully:', response.data);
            alert('Регистрация прошла успешно!');

            // После успешной регистрации загружаем фото пользователя
            await uploadPhoto(form.telegramId);

        } catch (error) {
            console.error('Registration failed:', error.response?.data || error.message);
            if (error.response) {
                alert(`Ошибка при регистрации: ${error.response.data.message || error.response.statusText}`);
            } else {
                alert('Ошибка при отправке запроса');
            }
        }
    };

    return (
        <div className="registration-container">
            <h2 className="registration-title">Регистрация</h2>
            <form className="registration-form" onSubmit={handleSubmit}>
                <div className="input-group fio-input">
                    <label>ФИО</label>
                    <input
                        type="text"
                        name="fullName"
                        value={form.fullName}
                        onChange={handleChange}
                        className="input-field"
                        required
                    />
                </div>

                <div className="input-group email-input">
                    <label>E-mail</label>
                    <input
                        type="email"
                        name="email"
                        value={form.email}
                        onChange={handleChange}
                        className="input-field"
                        required
                    />
                </div>

                <div className="input-group telephone-input">
                    <label>Телефон</label>
                    <input
                        type="tel"
                        name="phoneNumber"
                        value={form.phoneNumber}
                        onChange={handleChange}
                        className="input-field"
                        required
                    />
                </div>

                <div className="input-group telegram-input">
                    <label>Имя пользователя Telegram</label>
                    <input
                        type="text"
                        name="telegramId"
                        value={form.telegramId}
                        onChange={handleChange}
                        className="input-field"
                    />
                </div>

                <div className="input-group password-input">
                    <label>Пароль</label>
                    <input
                        type="password"
                        name="password"
                        value={form.password}
                        onChange={handleChange}
                        className="input-field"
                        required
                    />
                </div>

                <div className="input-group confirm-password-input">
                    <label>Повторите пароль</label>
                    <input
                        type="password"
                        name="confirmPassword"
                        value={form.confirmPassword}
                        onChange={handleChange}
                        className="input-field"
                        required
                    />
                </div>

                <div className="input-group role-select">
                    <select name="role" value={form.role} onChange={handleChange}
                            className="input-field" required>
                        <option value="">Выберите роль</option>
                        <option value="TRACKER">Трекер</option>
                        <option value="ADMIN">Администратор</option>
                    </select>
                </div>

                {/* Блок для загрузки аватара */}
                <div className="avatar-container">
                    <div className="avatar-box">
                        {previewPhoto ? (
                            <img src={previewPhoto} alt="Preview"/>
                        ) : (
                            <div></div>
                        )}
                        <label htmlFor="photo-upload" className="upload-icon">
                            <img src="./upload.png" alt="Загрузить"/>
                        </label>
                    </div>
                    <input
                        type="file"
                        id="photo-upload"
                        style={{display: "none"}}
                        onChange={handlePhotoChange}
                        accept="image/*"
                    />
                </div>

                <button type="submit" className="register-button">Зарегистрироваться</button>
            </form>
        </div>
    );
};

export default Registration;
