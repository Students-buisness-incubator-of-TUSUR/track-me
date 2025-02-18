import React from "react";
import "./meeting-card.css";

const MeetingCard = () => {
  return (
    <div className="meeting-container">
      <div className="meeting-card">

        <div className="meeting-info">
          <span className="meeting-number">Встреча: 1</span>
        </div>

        <div className="meeting-info">
          <span className="label">Дата:</span>
          <span className="meeting-date">25.04</span>
        </div>

        <div className="meeting-info">
          <span className="label">Задачи к следующей встрече:</span>
          <div className="task">
            Цель проекта на акселераторе: получить первые продажи
            <br />
            Ограничения проекта: нет понимания, как продавать продукт
          </div>
        </div>

        <div className="meeting-info">
          <span className="label">
            Выполнили задачи прошлой встречи или нет, общая информация по команде:
          </span>
          <div className="task">
            Выполнили только часть задач, лидер команды замотивирован в проекте, 
            но остальные члены команды не заинтересованы.
          </div>
        </div>

        <div className="meeting-info">
          <span className="label">Текущий статус команды:</span>
          <div className="status">Всё ок</div>
        </div>

        <div className="meeting-info">
          <span className="label">Скриншот встречи:</span>
          <div className="screenshot"></div>
        </div>

        <div className="meeting-info">
          <span className="label">Запись встречи:</span>
          <div className="link">Ссылка</div>
        </div>

        <button className="edit-button">Редактировать</button>
      </div>
    </div>
  );
};

export default MeetingCard;
