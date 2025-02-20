import React, { useEffect, useState } from "react";
import "./ProfilePage.css"; // Подключаем стили

function ProfilePage() {
  const [userData, setUserData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isEditing, setIsEditing] = useState(false); // Флаг редактирования
  const [editedData, setEditedData] = useState({}); // Состояние для редактируемых данных

  useEffect(() => {
    const token = localStorage.getItem("accessToken"); // Получаем токен из localStorage

    if (!token) {
      setError("Ошибка: отсутствует токен авторизации. Выполните вход.");
      setLoading(false);
      return;
    }

    fetch("http://127.0.0.1:8080/api/v1/users/current/info", {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`, // Передаем токен в заголовке
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
      .then((data) => {
        setUserData(data);
        setEditedData(data); // Заполняем редактируемые данные текущими
        setLoading(false);
      })
      .catch((err) => {
        console.error("Ошибка загрузки данных:", err);
        setLoading(false);
      });
  }, []);

  const handleEditClick = () => {
    setIsEditing(true);
  };

  const handleSaveClick = () => {
    const token = localStorage.getItem("accessToken");

    fetch("http://127.0.0.1:8080/api/v1/users/current/update", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(editedData),
    })
      .then((response) => {
        if (!response.ok) {
          setError(`Ошибка при сохранении данных. Статус: ${response.status}`);
          throw new Error("Ошибка сохранения");
        }
        return response.json();
      })
      .then((data) => {
        setUserData(data); // Обновляем данные пользователя
        setIsEditing(false);
      })
      .catch((err) => {
        console.error("Ошибка сохранения данных:", err);
      });
  };

  const handleChange = (e) => {
    setEditedData({
      ...editedData,
      [e.target.name]: e.target.value,
    });
  };

  if (loading) {
    return <div>Загрузка...</div>;
  }

  if (error) {
    return <div>{error}</div>;
  }

  return (
    <div className="login-container">
      <div className="profile-container">
        <div className="profile-header">
          <h1>Личный кабинет</h1>
          {!isEditing ? (
            <button className="edit-button12" onClick={handleEditClick}>
              Редактировать
            </button>
          ) : (
            <button className="save-button" onClick={handleSaveClick}>
              Сохранить
            </button>
          )}
        </div>

        <div className="profile-content">
          {/* Левая часть с полями */}
          <div className="profile-fields">
            <div className="field">
              <label>ФИО</label>
              <div className="input-container">
                <input
                  type="text"
                  name="fullName"
                  value={editedData.fullName}
                  onChange={handleChange}
                  readOnly={!isEditing}
                />
                {isEditing && <span className="edit-icon123">✏️</span>}
              </div>
            </div>
            <div className="field">
              <label>E-mail</label>
              <div className="input-container">
                <input
                  type="text"
                  name="email"
                  value={editedData.email}
                  onChange={handleChange}
                  readOnly={!isEditing}
                />
                {isEditing && <span className="edit-icon123">✏️</span>}
              </div>
            </div>
            <div className="field">
              <label>Телефон</label>
              <div className="input-container">
                <input
                  type="text"
                  name="phoneNumber"
                  value={editedData.phoneNumber}
                  onChange={handleChange}
                  readOnly={!isEditing}
                />
                {isEditing && <span className="edit-icon123">✏️</span>}
              </div>
            </div>
            <div className="field">
              <label>Мой телеграм в Telegram</label>
              <div className="input-container">
                <input
                  type="text"
                  name="telegramId"
                  value={editedData.telegramId}
                  onChange={handleChange}
                  readOnly={!isEditing}
                />
                {isEditing && <span className="edit-icon123">✏️</span>}
              </div>
            </div>
          </div>

          {/* Правая часть: аватар + роль */}
          <div className="profile-avatar-section">
            <div className="avatar-placeholder">
              {/* Место для картинки аватара */}
            </div>
            <div className="role-text">{userData.role}</div>
          </div>
        </div>

        {!isEditing ? (
          <button className="team-cards-button">Карточки команд</button>
        ) : null}
      </div>
    </div>
  );
}

export default ProfilePage;
