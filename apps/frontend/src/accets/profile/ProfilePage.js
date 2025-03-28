import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "./ProfilePage.css";

// Импорт иконок
import penIcon from "./pen.png"; 
import uploadIcon from "./upload.png";

function ProfilePage() {
  const navigate = useNavigate();
  const [userData, setUserData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [userPhoto, setUserPhoto] = useState(null);
  const backendHost = process.env.REACT_APP_BACKEND_HOST || 'http://localhost:8080';

  // Флаг редактирования
  const [isEditing, setIsEditing] = useState(false);

  // Состояние для редактируемых данных
  const [editedData, setEditedData] = useState({});

  useEffect(() => {
    const token = localStorage.getItem("accessToken");

    if (!token) {
      setError("Ошибка: отсутствует токен авторизации. Выполните вход.");
      setLoading(false);
      return;
    }

    fetch(`${backendHost}/api/v1/users/current/info`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
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
  }, [backendHost]);

  useEffect(() => {
    if (!userData) return;

    const token = localStorage.getItem("accessToken");

    fetch(`${backendHost}/api/v1/users/${userData.telegramId}/photo`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
      },
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
  }, [userData, backendHost]);

  const handleEditClick = () => {
    setIsEditing(true);
  };

  const handleHomeButtonClick = () => {
    if (userData && userData.role) {
      const role = userData.role.toLowerCase();
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
      } else if (role === "tracker" || role === "трекер"){
        navigate("/team-cards");
      }
    } else {
      navigate("/"); 
    }
  };

  // Функция валидации email и телефона
  const validateForm = () => {
    const emailPattern = /^[\w-.]+@([\w-]+\.)+[\w-]{2,4}$/;
    if (!emailPattern.test(editedData.email)) {
      setError("Некорректный формат email");
      return false;
    }

    // Убираем все нецифровые символы для проверки телефона
    const phoneDigits = editedData.phoneNumber.replace(/\D/g, "");
    if (phoneDigits.length < 11) {
      setError("Некорректный формат телефона");
      return false;
    }

    setError(null);
    return true;
  };

  const handleSaveClick = () => {
    if (!validateForm()) {
      return; // Если данные невалидны, не отправляем запрос
    }

    const token = localStorage.getItem("accessToken");

    fetch(`${backendHost}/api/v1/users/current/update`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(editedData),
    })
      .then((response) => {
        if (!response.ok) {
          if (response.status === 400) {
            setError("Номер телефона уже существует");
          } else {
            setError(`Ошибка при сохранении данных. Статус: ${response.status}`);
          }
          throw new Error("Ошибка сохранения");
        }
        return response.json();
      })
      .then((result) => {
        // Если telegramId изменился, перенаправляем на страницу авторизации
        if (userData && userData.telegramId !== result.telegramId) {
          navigate("/");
        } else {
          setUserData(result);
          setIsEditing(false);
        }
      })
      .catch((err) => {
        console.error("Ошибка сохранения данных:", err);
      });
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
      formatted +=  digits.slice(1, 4);
    }
    if (digits.length >= 4) {
      formatted +=  digits.slice(4, 11);
    }
    setEditedData({ ...editedData, phoneNumber: formatted });
  };

  // Обработчик загрузки фото
  const handlePhotoChange = (event) => {
    const file = event.target.files[0];
    if (!file) return;

    const imageUrl = URL.createObjectURL(file);
    setUserPhoto(imageUrl);

    const token = localStorage.getItem("accessToken");
    const formData = new FormData();
    formData.append("file", file);

    fetch(`${backendHost}/api/v1/users/${userData.telegramId}/photo`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
      },
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
    if (!userData || !userData.role) return;
    const role = userData.role.toLowerCase();
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
                  <img src={penIcon} alt="Редактировать" className="edit-icon123" />
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
                  <img src={penIcon} alt="Редактировать" className="edit-icon123" />
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
                  <img src={penIcon} alt="Редактировать" className="edit-icon123" />
                )}
              </div>
            </div>

            <div className="field">
              <label className="profile-label">Мой телеграм в Telegram</label>
              <div className="input-container">
                <input
                  type="text"
                  name="telegramId"
                  className="profile-input"
                  value={editedData.telegramId || ""}
                  onChange={handleChange}
                  readOnly={!isEditing}
                />
                {isEditing && (
                  <img src={penIcon} alt="Редактировать" className="edit-icon123" />
                )}
              </div>
            </div>
          </div>

          <div className="profile-avatar-section" style={{ position: "relative" }}>
            {isEditing ? (
              <label htmlFor="photo-upload" className="avatar-placeholder clickable" style={{ cursor: "pointer" }}>
                {userPhoto ? (
                  <img src={userPhoto} alt="Аватар" className="user-avatar" />
                ) : (
                  <div className="default-avatar"></div>
                )}
                <div className="upload-photo-profile" style={{ position: "absolute", top: 0, right: 0 }}>
                  <img src={uploadIcon} alt="Загрузить" className="upload-icon-profile" />
                </div>
              </label>
            ) : (
              <div className="avatar-placeholder">
                {userPhoto ? (
                  <img src={userPhoto} alt="Аватар" className="user-avatar" />
                ) : (
                  <div className="default-avatar"></div>
                )}
              </div>
            )}
            {isEditing && (
              <input
                type="file"
                id="photo-upload"
                style={{ display: "none" }}
                onChange={handlePhotoChange}
                accept="image/*"
              />
            )}
            <div className="profile-role-text">
              {getRoleInRussian(userData.role)}
            </div>
          </div>
        </div>

        {/* Блок с сообщением об ошибке, выводится только в режиме редактирования */}
        {isEditing && error && (
          <div className="error-message42">{error}</div>
        )}

        {!isEditing ? (
          <button className="profile-team-cards-button" onClick={handleTeamCardsClick}>
            Карточки команд
          </button>
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
