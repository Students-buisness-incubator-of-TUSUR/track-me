import React from "react";
import "./meeting-card.css";

const MeetingCard = ({ meetingNumber, date, nextTasks, previousTasks, teamStatus, screenshot, recordingLink }) => {
  return (
    <div className="unique-meeting-container">
      <div className="unique-meeting-card">
        
        {/* Кнопка редактирования */}
        <button className="unique-edit-button">Редактировать</button>

        {/* Номер встречи */}
        <div className="unique-meeting-info">
          <span className="unique-meeting-number">Встреча {meetingNumber}</span>
        </div>

        {/* Дата */}
        <div className="unique-meeting-info">
          <span className="unique-label">Дата:</span>
          <span className="unique-meeting-date">25.04</span>
        </div>

        {/* Задачи к следующей встрече */}
        <div className="unique-meeting-info">
          <span className="unique-label">Задачи к следующей встрече:</span>
          <div className="unique-task">{nextTasks}</div>
        </div>

        {/* Выполнение задач с прошлой встречи */}
        <div className="unique-meeting-info">
          <span className="unique-label">
            Выполнили задачи прошлой 
            встречи или нет, общая 
            информация по команде:
          </span>
          <div className="unique-task">{previousTasks}</div>
        </div>

        {/* Текущий статус команды */}
        <div className="unique-meeting-info">
          <span className="unique-label">Текущий статус команды:</span>
          <div className="unique-status">Все ок</div>
        </div>

        {/* Скриншот встречи */}
        <div className="unique-meeting-info">
          <span className="unique-label">Скриншот встречи:</span>
          {screenshot ? (
            <img src={screenshot} alt="Скриншот встречи" className="unique-screenshot" />
          ) : (
            <div className="unique-screenshot">Нет скриншота</div>
          )}
        </div>

        {/* Запись встречи */}
        <div className="unique-meeting-info">
          <span className="unique-label">Запись встречи:</span>
          {recordingLink ? (
            <a href={recordingLink} target="_blank" rel="noopener noreferrer" className="unique-link">
              Смотреть запись
            </a>
          ) : (
            <div className="unique-link">Нет записи</div>
          )}
        </div>
      </div>
    </div>
  );
};

export default MeetingCard;