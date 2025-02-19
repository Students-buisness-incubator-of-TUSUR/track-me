import React from "react";
import "./ProfilePage.css"; // Подключаем стили

function ProfilePage() {
  return (
    <div className="login-container">
    <div className="profile-container">
      <div className="profile-header">
        <h1>Личный кабинет</h1>
        <button className="edit-button12">Редактировать</button>
      </div>

      <div className="profile-content">
        {/* Левая часть с полями */}
        <div className="profile-fields">
          <div className="field">
            <label>ФИО</label>
            <input type="text" placeholder="Текст" />
          </div>
          <div className="field">
            <label>E-mail</label>
            <input type="text" placeholder="Текст" />
          </div>
          <div className="field">
            <label>Телефон</label>
            <input type="text" placeholder="Текст" />
          </div>
          <div className="field">
            <label>Мой телеграм в telegram</label>
            <input type="text" placeholder="Текст" />
          </div>
        </div>

        {/* Правая часть: аватар + роль */}
        <div className="profile-avatar-section">
          <div className="avatar-placeholder">
            {/* Место для картинки аватара */}
          </div>
          <div className="role-text">Роль</div>
        </div>
      </div>

      <button className="team-cards-button">Карточки команд</button>
    </div>
    </div>
  );
}

export default ProfilePage;
