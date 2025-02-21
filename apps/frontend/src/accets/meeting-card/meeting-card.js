import React from "react";
import "./meeting-card.css";

const MeetingCard = ({ meetingNumber, date, nextTasks, previousTasks, teamStatus, screenshot, recordingLink }) => {
  return (
    <div className="meeting-container">
      <div className="meeting-card">
        
        {/* Кнопка редактирования */}
        <button className="edit-button">Редактировать</button>

        {/* Номер встречи */}
        <div className="meeting-info">
          <span className="meeting-number">Встреча 1 {meetingNumber}</span>
        </div>

        {/* Дата */}
        <div className="meeting-info">
          <span className="label">Дата:</span>
          <span className="meeting-date">{date}</span>
        </div>

        {/* Задачи к следующей встрече */}
        <div className="meeting-info">
          <span className="label">Задачи к следующей встрече:</span>
          <div className="task">{nextTasks}</div>
        </div>

        {/* Выполнение задач с прошлой встречи */}
        <div className="meeting-info">
          <span className="label">
            Выполнили задачи прошлой встречи или нет, общая информация по команде:
          </span>
          <div className="task">{previousTasks}</div>
        </div>

        {/* Текущий статус команды */}
        <div className="meeting-info">
          <span className="label">Текущий статус команды:</span>
          <div className="status">{teamStatus}</div>
        </div>

        {/* Скриншот встречи */}
        <div className="meeting-info">
          <span className="label">Скриншот встречи:</span>
          {screenshot ? (
            <img src={screenshot} alt="Скриншот встречи" className="screenshot" />
          ) : (
            <div className="screenshot-placeholder">Нет скриншота</div>
          )}
        </div>

        {/* Запись встречи */}
        <div className="meeting-info">
          <span className="label">Запись встречи:</span>
          {recordingLink ? (
            <a href={recordingLink} target="_blank" rel="noopener noreferrer" className="link">
              Смотреть запись
            </a>
          ) : (
            <div className="link">Нет записи</div>
          )}
        </div>
      </div>
    </div>
  );
};

export default MeetingCard;
