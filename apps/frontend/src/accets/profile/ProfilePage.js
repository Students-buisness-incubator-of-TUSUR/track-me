import React, {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import "./ProfilePage.css";
import { getCsrfConfigForFetch } from "../../utils/csrf-utils";
// Импорт иконок
import penIcon from "./pen.png";
import uploadIcon from "./upload.png";

function ProfilePage() {
    const navigate = useNavigate();
    const [userData, setUserData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [userPhoto, setUserPhoto] = useState(null);
    const ssoHost = (process.env.REACT_APP_BACKEND_URI || 'http://localhost:8080') + '/sso';
    const defaultAvatarUrl = "/images/no-photo.png";
    // Флаг редактирования
    const [isEditing, setIsEditing] = useState(false);

    // Состояние для редактируемых данных
    const [editedData, setEditedData] = useState({});

    // Состояние для отображения подсказки
    const [showTooltip, setShowTooltip] = useState(false);
    const [tooltipPosition, setTooltipPosition] = useState({ x: 0, y: 0 });

    const handleMouseMove = (event) => {
        const rect = event.currentTarget.getBoundingClientRect();
        setTooltipPosition({
            x: rect.left + (rect.width / 2),
            y: rect.top - 10
        });
    };

    // Количество команд, получаемое из userData
    const [teamCount, setTeamCount] = useState(0);

    useEffect(() => {

        fetch(`${ssoHost}/api/v1/account/info`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
            },
            credentials: "include"
        })
            .then((response) => {
                if (!response.ok) {
                    if (response.status === 401) {
                        setError("Ошибка авторизации! Пожалуйста, выполните вход заново.");
                    } else {
                        setError(`Ошибка при загрузке данных. Статус: ${response.status}`);
                    }
                    throw new Error("Ошибка запроса");
                }
                return response.json();
            })
            .then((result) => {
                setUserData(result);
                setEditedData(result); // Заполняем редактируемые данные
                setLoading(false);
            })
            .catch((err) => {
                console.error("Ошибка загрузки данных:", err);
                setLoading(false);
            });
    }, [ssoHost]);

    useEffect(() => {
        if (!userData) return;

        fetch(`${ssoHost}/api/v1/account/photo`, {
            method: "GET",
            credentials: "include",
        })
            .then((res) => {
                if (!res.ok) {
                    throw new Error("Фото не найдено");
                }
                return res.blob();
            })
            .then((blob) => {
                const imageUrl = URL.createObjectURL(blob);
                setUserPhoto(imageUrl);
            })
            .catch(() => {
                setUserPhoto(null);
            });
    }, [userData, ssoHost]);

    useEffect(() => {
    if (!userData?.roles?.includes("TRACKER")) return;
    const backendHost = (process.env.REACT_APP_BACKEND_URI || 'http://localhost:8080') + '/backend';
    
    fetch(`${backendHost}/api/v1/team-cards?page=0&size=1000`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...getCsrfConfigForFetch() },
        credentials: "include",
        body: JSON.stringify({
            filters: [
                {
                    fieldName: "username",
                    type: "EQ",
                    value: userData.username
                },
                {
                    fieldName: "enabled",
                    type: "EQ",
                    value: true
                }
            ]
        })
    })
        .then(res => {
            if (!res.ok) {
                throw new Error("Ошибка при загрузке карточек команд");
            }
            return res.json();
        })
        .then(data => {
            // Устанавливаем teamCount на основе totalElements, если доступно, или длины content
            setTeamCount(data.totalElements || (Array.isArray(data?.content) ? data.content.length : 0));
        })
        .catch(err => {
            console.error("Ошибка при загрузке карточек:", err);
            setTeamCount(0);
        });
}, [userData]);

    const handleEditClick = () => {
        setIsEditing(true);
    };

    const handleHomeButtonClick = () => {
        if (userData && userData.roles) {
            const role = userData.roles[0].toLowerCase();
            if (
                role === "superadmin" ||
                role === "суперадмин" ||
                role === "super_admin"
            ) {
                navigate("/streams");
            } else if (
                role === "admin" ||
                role === "админ"
            ) {
                navigate("/streams");
            } else if (role === "tracker" || role === "трекер") {
                navigate("/team-cards");
            }
        } else {
            navigate("/");
        }
    };

    // Функция валидации email и телефона
    const validateForm = () => {
    if (!editedData.fullName || editedData.fullName.trim() === "") {
        setError("Поле 'ФИО' обязательно для заполнения");
        return false;
    }

    const emailPattern = /^[\w-.]+@([\w-]+\.)+[\w-]{2,4}$/;
    if (!editedData.email || !emailPattern.test(editedData.email)) {
        setError("Некорректный формат email");
        return false;
    }

    const phoneDigits = editedData.phoneNumber?.replace(/\D/g, "") || "";
    if (phoneDigits.length < 11) {
        setError("Некорректный формат телефона");
        return false;
    }

    if (!editedData.username || editedData.username.trim() === "") {
        setError("Поле 'Телеграм' обязательно для заполнения");
        return false;
    }
    
    if (!editedData.username.match(/^\w+$/)) {
        setError("Введите корректный юзернейм");
        return false;
    }

    setError(null);
    return true;
};
    const handleSaveClick = async () => {
    if (!validateForm()) {
        return;
    }

    try {
        // Подготавливаем данные для отправки - заменяем null на пустую строку для avatarUrl
        const dataToSend = {
            ...editedData,
            avatarUrl: editedData.avatarUrl || defaultAvatarUrl // отправляем пустую строку вместо null
        };

        const response = await fetch(`${ssoHost}/api/v1/account/update`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                ...getCsrfConfigForFetch()
            },
            credentials: "include",
            body: JSON.stringify(dataToSend), // используем подготовленные данные
        });

        if (!response.ok) {
            if (response.status === 400) {
                // Пытаемся получить более детальную ошибку
                try {
                    const errorData = await response.json();
                    setError(errorData.message || "Ошибка валидации данных");
                } catch {
                    setError("Номер телефона уже существует или данные неверны");
                }
            } else {
                setError(`Ошибка при сохранении данных. Статус: ${response.status}`);
            }
            throw new Error("Ошибка сохранения");
        }

        // После успешного сохранения запрашиваем свежие данные
        const userResponse = await fetch(`${ssoHost}/api/v1/account/info`, {
            method: "GET",
            headers: { "Content-Type": "application/json" },
            credentials: "include"
        });

        if (userResponse.ok) {
            const freshData = await userResponse.json();
            setUserData(freshData);
            setEditedData(freshData);
        } else {
            // Если не удалось получить свежие данные, используем отправленные
            setUserData(dataToSend);
        }

        setIsEditing(false);
        setError(null);
        
    } catch (err) {
        console.error("Ошибка сохранения данных:", err);
        if (!err.message.includes('JSON')) {
            setError("Произошла ошибка при сохранении данных");
        }
    }
};

    // Обработчик для остальных полей
    const handleChange = (e) => {
        setEditedData({
            ...editedData,
            [e.target.name]: e.target.value,
        });
    };

    // Обработчик для форматирования номера телефона вручную
    const handlePhoneChange = (e) => {
        let digits = e.target.value.replace(/\D/g, "");
        if (!digits) {
            setEditedData({...editedData, phoneNumber: ""});
            return;
        }
        if (digits[0] !== "7") {
            digits = "7" + digits;
        }
        let formatted = "+7";
        if (digits.length > 1) {
            formatted += digits.slice(1, 4);
        }
        if (digits.length >= 4) {
            formatted += digits.slice(4, 11);
        }
        setEditedData({...editedData, phoneNumber: formatted});
    };

    // Обработчик загрузки фото
    const handlePhotoChange = (event) => {
        const file = event.target.files[0];
        if (!file) return;

        const imageUrl = URL.createObjectURL(file);
        setUserPhoto(imageUrl);
        const formData = new FormData();
        formData.append("file", file);

        fetch(`${ssoHost}/api/v1/account/photo`, {
            method: "POST",
            headers: { ...getCsrfConfigForFetch() },
            credentials: "include",
            body: formData,
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Ошибка загрузки фото");
                }
                console.log("Фото успешно загружено");
            })
            .catch((err) => {
                console.error("Ошибка загрузки фото:", err);
            });
    };

    // Преобразование роли в русский вариант
    const getRoleInRussian = (role) => {
        if (!role) return "";
        const lowerRole = role.toLowerCase();
        if (lowerRole === "tracker" || lowerRole === "трекер") {
            return "Трекер";
        } else if (lowerRole === "admin" || lowerRole === "админ") {
            return "Администратор";
        } else if (
            lowerRole === "superadmin" ||
            lowerRole === "суперадмин" ||
            lowerRole === "super_admin"
        ) {
            return "Супер администратор";
        }
        return role;
    };

    // Обработчик для кнопки "Карточки команд"
    const handleTeamCardsClick = () => {
        if (!userData || !userData.roles) return;
        const role = userData.roles[0].toLowerCase();
        if (role === "tracker" || role === "трекер") {
            navigate("/team-cards");
        } else if (
            role === "admin" ||
            role === "админ" ||
            role === "superadmin" ||
            role === "суперадмин" ||
            role === "super_admin"
        ) {
            navigate("/streams");
        }
    };

    if (loading) {
        return <div>Загрузка...</div>;
    }

    return (
        <div className="login-container">
            <div className="profile-container">
                <div className="profile-header">
                    <h1>Личный кабинет</h1>
                    {!isEditing && (
                        <button className="edit-button12" onClick={handleEditClick}>
                            Редактировать
                        </button>
                    )}
                </div>

                <div className="profile-content">
                    <div className="profile-fields">
                        <div className="field">
                            <label className="profile-label">ФИО</label>
                            <div className="input-container">
                                <input
                                    type="text"
                                    name="fullName"
                                    className="profile-input"
                                    value={editedData.fullName || ""}
                                    onChange={handleChange}
                                    readOnly={!isEditing}
                                />
                                {isEditing && (
                                    <img src={penIcon} alt="Редактировать"
                                         className="edit-icon123"/>
                                )}
                            </div>
                        </div>

                        <div className="field">
                            <label className="profile-label">E-mail</label>
                            <div className="input-container">
                                <input
                                    type="text"
                                    name="email"
                                    className="profile-input"
                                    value={editedData.email || ""}
                                    onChange={handleChange}
                                    readOnly={!isEditing}
                                    pattern="^[\w-.]+@([\w-]+\.)+[\w-]{2,4}$"
                                />
                                {isEditing && (
                                    <img src={penIcon} alt="Редактировать"
                                         className="edit-icon123"/>
                                )}
                            </div>
                        </div>

                        <div className="field">
                            <label className="profile-label">Телефон</label>
                            <div className="input-container">
                                {isEditing ? (
                                    <input
                                        type="text"
                                        name="phoneNumber"
                                        className="profile-input"
                                        value={editedData.phoneNumber || ""}
                                        onChange={handlePhoneChange}
                                    />
                                ) : (
                                    <input
                                        type="text"
                                        name="phoneNumber"
                                        className="profile-input"
                                        value={editedData.phoneNumber || ""}
                                        readOnly
                                    />
                                )}
                                {isEditing && (
                                    <img src={penIcon} alt="Редактировать"
                                         className="edit-icon123"/>
                                )}
                            </div>
                        </div>

                        <div className="field">
                            <label htmlFor="telegram-username" className="profile-label">Username в Telegram</label>
                            <div className="input-container">
                                <input
                                    id="telegram-username"
                                    type="text"
                                    name="username"
                                    className="profile-input"
                                    value={editedData.username || ""}
                                    onChange={handleChange}
                                    readOnly={!isEditing}
                                />
                                {isEditing && (
                                    <img src={penIcon} alt="Редактировать"
                                         className="edit-icon123"/>
                                )}
                            </div>
                        </div>
                    </div>

                    <div className="profile-avatar-section" style={{position: "relative"}}>
                        {isEditing ? (
                            <label htmlFor="photo-upload" className="avatar-placeholder clickable"
                                   style={{cursor: "pointer"}}>
                                {userPhoto ? (
                                    <img src={userPhoto} alt="Аватар" className="user-avatar"/>
                                ) : (
                                    <div className="default-avatar"></div>
                                )}
                                <div className="upload-photo-profile"
                                     style={{position: "absolute", top: 0, right: 0}}>
                                    <img src={uploadIcon} alt="Загрузить"
                                         className="upload-icon-profile"/>
                                </div>
                            </label>
                        ) : (
                            <div className="avatar-placeholder">
                                {userPhoto ? (
                                    <img src={userPhoto} alt="Аватар" className="user-avatar"/>
                                ) : (
                                    <div className="default-avatar"></div>
                                )}
                            </div>
                        )}
                        {isEditing && (
                            <input
                                type="file"
                                id="photo-upload"
                                style={{display: "none"}}
                                onChange={handlePhotoChange}
                                accept="image/*"
                            />
                        )}
                        <div className="profile-role-text">
                            {getRoleInRussian(userData.roles[0])}
                        </div>
                    </div>
                </div>

                {/* Блок с сообщением об ошибке, выводится только в режиме редактирования */}
                {isEditing && error && (
                    <div className="error-message42">{error}</div>
                )}

                {!isEditing ? (
                    <>
                        <button 
                            className="profile-team-cards-button" 
                            onClick={handleTeamCardsClick}
                        >
                            Карточки команд
                            {userData?.roles?.includes("TRACKER") && (
                                <span 
                                    className="count-container"
                                    onMouseEnter={() => setShowTooltip(true)}
                                    onMouseLeave={() => setShowTooltip(false)}
                                    onMouseMove={handleMouseMove}
                                >
                                    ({teamCount})
                                </span>
                            )}
                        </button>
                        {showTooltip && userData?.roles?.includes("TRACKER") && (
                            <div 
                                className="profile-tooltip"
                                style={{
                                    left: `${tooltipPosition.x}px`,
                                    top: `${tooltipPosition.y}px`
                                }}
                            >
                                Количество моих команд
                            </div>
                        )}
                    </>
                ) : (
                    <button className="profile-team-cards-button" onClick={handleSaveClick}>
                        Сохранить
                    </button>
                )}

                <button className="home-button" onClick={handleHomeButtonClick}>
                    Главная страница
                </button>
            </div>
        </div>
    );
}

export default ProfilePage;
