import React, { useEffect, useState, useCallback, useId, useRef } from "react";
import { useNavigate, useParams } from "react-router-dom";
import "./ProfilePage.css";
import Header from "../header/header";
import { fetchTeams, fetchUserInfo, fetchUserPhoto, updateUserInfo, updateUserPhoto } from "../../services/requests";
import InputBox from "../input-box/input-box";
import { ReactComponent as CloseIcon } from '../../files/close.svg';
import { ReactComponent as UploadIcon } from '../../files/upload.svg';
import noUserPhoto from '../../files/no-user-photo.png';

function ProfilePage() {
    const navigate = useNavigate();
    const { username } = useParams(); // Получаем username из URL, если есть
    const [userData, setUserData] = useState(null);
    const [currentUser, setCurrentUser] = useState(null); // Текущий авторизованный пользователь
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const photoId = useId();
    const [isEditing, setIsEditing] = useState(false);
    const [isOwnProfile, setIsOwnProfile] = useState(false);
    const [editedData, setEditedData] = useState({});
    const [teamCount, setTeamCount] = useState(0);
    const [totalTeamCount, setTotalTeamCount] = useState(0);

    const photoImgRef = useRef(null);
    const setUserPhoto = useCallback((blobOrUrl) => {
        if (!photoImgRef.current) return;

        if (!blobOrUrl) {
            photoImgRef.current.src = noUserPhoto;
            return;
        }
        try {
            const url = typeof blobOrUrl === "string"
                ? blobOrUrl
                : URL.createObjectURL(blobOrUrl);
            const parsed = new URL(url);
            if (parsed.protocol === "blob:") {
                photoImgRef.current.src = url;
            } else {
                photoImgRef.current.src = noUserPhoto;
            }
        } catch {
            photoImgRef.current.src = noUserPhoto;
        }
    }, []);

    const loadTargetUserData = useCallback((targetUsername) => {
        setUserData(null);
        setEditedData({});
        setLoading(true);
        setError(null);

        fetchUserInfo({ username: targetUsername })
            .then(res => {
                if (res.status === 403) throw new Error("Нет доступа к просмотру этого профиля");
                if (res.status === 404) throw new Error("Пользователь не найден");
                if (!res.ok) throw new Error("Ошибка загрузки данных пользователя");
                return res.json();
            })
            .then(data => {
                setUserData(data);
                setEditedData(data);
            })
            .catch(err => {
                setError(err.message || "Ошибка загрузки данных пользователя");
                setUserData(null);
                setEditedData({});
            })
            .finally(() => setLoading(false));
    }, []);

    // 1. Загружаем текущего авторизованного пользователя
    useEffect(() => {
        fetchUserInfo({})
            .then((response) => {
                if (!response.ok) {
                    if (response.status === 401) {
                        throw new Error("Ошибка авторизации! Пожалуйста, выполните вход заново.");
                    }
                    throw new Error(`Ошибка при загрузке данных. Статус: ${response.status}`);
                }
                return response.json();
            })
            .then((result) => {
                setCurrentUser(result);

                // Проверяем, это свой профиль или чужой
                if (!username || username === result.username) {
                    setIsOwnProfile(true);
                    setUserData(result);
                    setEditedData(result);
                    setLoading(false);
                } else {
                    setIsOwnProfile(false);
                    // Загружаем данные целевого пользователя
                    loadTargetUserData(username, result);
                }
            })
            .catch((err) => {
                console.error("Ошибка загрузки данных:", err);
                setError(err.message);
                setUserData(null);             // ← обязательное
                setEditedData({});
                setLoading(false);
            });

    }, [username, loadTargetUserData]);


    const handleCloseProfile = () => {
        navigate(-1); // Возвращаемся на предыдущую страницу
    };
    // Загрузка фото пользователя
    useEffect(() => {
        if (!userData) return;

        const targetUsername = isOwnProfile ? null : username;

        // if targetUsername == null then it should fetch current user info
        fetchUserPhoto({ username: targetUsername })
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
    }, [setUserPhoto, userData, username, isOwnProfile]);

    // Загрузка количества команд (только для трекеров и только для своего профиля)
    useEffect(() => {
        if (!userData) return;
        if (!(isOwnProfile || ((currentUser?.roles?.includes("SUPER_ADMIN") || currentUser?.roles?.includes("ADMIN")) && !isOwnProfile))) return;

        const common = {
            page: 0, size: 10000,
            filters: [
                { fieldName: "username", type: "EQ", value: userData.username }
            ],
            admin: currentUser?.roles?.includes("SUPER_ADMIN") || currentUser?.roles?.includes("ADMIN"),
        };

        fetchTeams(common)
            .then(res => {
                if (!res.ok) throw new Error("Ошибка при загрузке карточек команд");
                return res.json();
            })
            .then(data => {
                const cards = Array.isArray(data?.content) ? data.content : [];
                const total = data.totalElements || cards.length;
                const active = cards.filter(c => c.enabled).length;
                setTeamCount(active);
                setTotalTeamCount(total);
            })
            .catch(err => {
                console.error("Ошибка при загрузке карточек:", err);
                setTeamCount(0);
                setTotalTeamCount(0);
            });
    }, [userData, isOwnProfile, currentUser]);

    const handleEditClick = () => {
        setIsEditing(true);
    };

    const handleHomeButtonClick = () => {
        if (currentUser && currentUser.roles) {
            const role = currentUser.roles[0].toLowerCase();
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
                avatarUrl: editedData.avatarUrl || noUserPhoto
            };

            const response = await updateUserInfo({ newUserData: dataToSend });

            if (!response.ok) {
                if (response.status === 400) {
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
            const userResponse = await fetchUserInfo({});

            if (userResponse.ok) {
                const freshData = await userResponse.json();
                setUserData(freshData);
                setEditedData(freshData);
            } else {
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
            setEditedData({ ...editedData, phoneNumber: "" });
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
        setEditedData({ ...editedData, phoneNumber: formatted });
    };

    // Обработчик загрузки фото (только для своего профиля)
    const handlePhotoChange = (event) => {
        if (!isOwnProfile) return;

        const file = event.target.files[0];
        if (!file) return;

        const imageUrl = URL.createObjectURL(file);
        setUserPhoto(imageUrl);

        updateUserPhoto({ newUserPhotoFile: file })
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
        if (!currentUser || !currentUser.roles) return;
        const role = currentUser.roles[0].toLowerCase();
        if (role === "tracker" || role === "трекер") {
            navigate("/team-cards");
        } else if (
            role === "admin" ||
            role === "админ" ||
            role === "superadmin" ||
            role === "суперадмин" ||
            role === "super_admin"
        ) {
            navigate(`/all-team-cards?username=${userData.username}`);
        }
    };

    if (loading) {
        return <div>Загрузка...</div>;
    }

    if (error && !userData) {
        return (
            <>
                <Header userRole="TRACKER" />
                <div className="profile-page_main">
                    <div className="profile-page_container" style={{ alignItems: "center" }}>
                        <div className="profile-page_error-msg" data-testid="error-message">{error}</div>
                        <button className="profile-page_btn" onClick={handleHomeButtonClick}>
                            Главная страница
                        </button>
                    </div>
                </div>
            </>
        );
    }

    console.log(userData);
    return (
        <>
            <Header userRole={currentUser.roles?.[0]} />
            <div className="profile-page_main">
                <div className="profile-page_container">
                    {error && <div className="profile-page_error-msg" data-testid="error-message">{error}</div>}
                    <div className="profile-page_header">
                        <h1>Личный кабинет</h1>
                        {!isEditing && isOwnProfile && (
                            <button
                                onClick={handleEditClick}
                            >
                                Редактировать
                            </button>
                        )}
                        {isEditing && isOwnProfile && (
                            <button
                                onClick={() => { window.location.reload() }}
                            >
                                Отменить
                            </button>
                        )}
                        {!isOwnProfile && (
                            <button
                                onClick={handleCloseProfile}
                            >
                                <CloseIcon className="profile-page_close-icon" />
                            </button>
                        )}
                    </div>
                    <div className="profile-page_row">
                        <div className="profile-page_fields">
                            <InputBox
                                className="profile-page_input-box"
                                placeholder="ФИО"
                                placeholderIsAbove
                                type="text"
                                name="fullName"
                                value={editedData.fullName || ""}
                                onChange={handleChange}
                                readOnly={!isEditing}
                                onEditClick={!isEditing ? null : () => { }}
                            />
                            <InputBox
                                className="profile-page_input-box"
                                placeholder="E-mail"
                                placeholderIsAbove
                                type="text"
                                name="email"
                                value={editedData.email || ""}
                                onChange={handleChange}
                                readOnly={!isEditing}
                                onEditClick={!isEditing ? null : () => { }}
                            />
                            <InputBox
                                className="profile-page_input-box"
                                placeholder="Телефон"
                                placeholderIsAbove
                                type="text"
                                name="phoneNumber"
                                value={editedData.phoneNumber || ""}
                                onChange={handlePhoneChange}
                                readOnly={!isEditing}
                                onEditClick={!isEditing ? null : () => { }}
                            />
                            <InputBox
                                className="profile-page_input-box"
                                placeholder="Имя пользователя в Telegram"
                                placeholderIsAbove
                                type="text"
                                name="username"
                                value={editedData.username || ""}
                                onChange={handleChange}
                                readOnly={true}
                            />
                        </div>
                        <div className="profile-page_etc">
                            <label
                                htmlFor={photoId}
                                style={isEditing ? { cursor: "pointer" } : {}}
                                className="profile-page_photo-container"
                                disabled={!(isEditing && isOwnProfile)}
                            >
                                <img
                                    ref={photoImgRef}
                                    data-testid="user-photo"
                                    src={noUserPhoto}
                                    alt="Аватар"
                                />
                                {isEditing && isOwnProfile && <UploadIcon className="profile-page_upload-icon" />}
                            </label>
                            {isEditing && (
                                <input
                                    type="file"
                                    id={photoId}
                                    style={{ display: "none" }}
                                    onChange={handlePhotoChange}
                                    accept="image/*"
                                />
                            )}
                            <button
                                className="profile-page_btn profile-page_role"
                                disabled
                            >
                                {getRoleInRussian(userData.roles?.[0])}
                            </button>
                        </div>
                    </div>
                    <div className="profile-page_row">
                        {!isEditing && (
                            <span className="profile-page_btn-wrapper">
                                <button
                                    className="profile-page_btn"
                                    onClick={handleTeamCardsClick}
                                >
                                    {`Карточки команд ${teamCount} (${totalTeamCount})`}
                                </button>
                                <span className="profile-page_tooltip">
                                    {`Активных карточек команд: ${teamCount}\nВсего карточек команд: ${totalTeamCount}`}
                                </span>
                            </span>
                        )}
                        {isEditing && isOwnProfile && (
                            <button
                                className="profile-page_btn"
                                onClick={handleSaveClick}
                            >
                                Сохранить
                            </button>
                        )}
                        <button
                            className="profile-page_btn"
                            onClick={handleCloseProfile}
                        >
                            Главная страница
                        </button>

                    </div>
                </div>
            </div>
        </>
    );
}

export default ProfilePage;

