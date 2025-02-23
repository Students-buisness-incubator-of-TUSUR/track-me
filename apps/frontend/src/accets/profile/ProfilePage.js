import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "./ProfilePage.css";

// Вариант импорта (если pen.png лежит в той же папке, что и ProfilePage.jsx)
import penIcon from "./pen.png"; 

function ProfilePage() {
  const navigate = useNavigate();
  const [userData, setUserData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Флаг редактирования
  const [isEditing, setIsEditing] = useState(false);

  // Состояние для редактируемых данных
  const [editedData, setEditedData] = useState({});

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
      .then((data) => {
        setUserData(data);
        setEditedData(data); // Заполняем редактируемые данные
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
        // Если telegramId изменился, перенаправляем на страницу авторизации
        if (userData && userData.telegramId !== data.telegramId) {
          navigate("/");
        } else {
          setUserData(data);
          setIsEditing(false);
        }
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

  // Функция для преобразования роли в русский вариант
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
    console.log("Роль пользователя:", role);
    if (role === "tracker" || role === "трекер") {
      navigate("/tracker");
    } else if (
      role === "admin" ||
      role === "админ" ||
      role === "superadmin" ||
      role === "суперадмин" ||
      role === "super_admin"
    ) {
      navigate("/stream");
    }
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
          {/* Кнопка "Редактировать" (только если НЕ в режиме редактирования) */}
          {!isEditing && (
            <button className="edit-button12" onClick={handleEditClick}>
              Редактировать
            </button>
          )}
        </div>

        <div className="profile-content">
          {/* Левая часть с полями */}
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
                  <img
                    src={penIcon}
                    alt="Pen icon"
                    className="edit-icon123"
                  />
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
                />
                {isEditing && (
                  <img
                    src={penIcon}
                    alt="Pen icon"
                    className="edit-icon123"
                  />
                )}
              </div>
            </div>

            <div className="field">
              <label className="profile-label">Телефон</label>
              <div className="input-container">
                <input
                  type="text"
                  name="phoneNumber"
                  className="profile-input"
                  value={editedData.phoneNumber || ""}
                  onChange={handleChange}
                  readOnly={!isEditing}
                />
                {isEditing && (
                  <img
                    src={penIcon}
                    alt="Pen icon"
                    className="edit-icon123"
                  />
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
                  <img
                    src={penIcon}
                    alt="Pen icon"
                    className="edit-icon123"
                  />
                )}
              </div>
            </div>
          </div>

          {/* Правая часть: аватар + роль */}
          <div className="profile-avatar-section">
            <div className="avatar-placeholder">
              {/* Место для картинки аватара */}
            </div>
            <div className="profile-role-text">
              {getRoleInRussian(userData.role)}
            </div>
          </div>
        </div>

        {/* Если не в режиме редактирования – кнопка "Карточки команд", иначе – кнопка "Сохранить" */}
        {!isEditing ? (
          <button
            className="profile-team-cards-button"
            onClick={handleTeamCardsClick}
          >
            Карточки команд
          </button>
        ) : (
          <button
            className="profile-team-cards-button"
            onClick={handleSaveClick}
          >
            Сохранить
          </button>
        )}
      </div>
    </div>
  );
}

export default ProfilePage;
