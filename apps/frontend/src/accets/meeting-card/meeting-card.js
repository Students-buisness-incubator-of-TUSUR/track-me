import React, { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import "./meeting-card.css";

const MeetingCard = () => {
  const { meetingId } = useParams();

  // Загружаем сохраненные данные, если они есть
  const savedData = JSON.parse(localStorage.getItem(`meeting-${meetingId}`)) || {
    date: "30.04",
    nextTasks: "",
    previousTasks: "",
    teamStatus: "Все ок",
    screenshot: "",
    recordingLink: "",
  };

  const [meetingData, setMeetingData] = useState(savedData);
  const [isEditing, setIsEditing] = useState(false);

  useEffect(() => {
    localStorage.setItem(`meeting-${meetingId}`, JSON.stringify(meetingData));
  }, [meetingData, meetingId]);

  // Функция для обновления данных
  const handleChange = (e) => {
    const { name, value } = e.target;
    setMeetingData({ ...meetingData, [name]: value });
  };

  return (
    <div className="unique-meeting-container">
      <div className="unique-meeting-card">
        <button onClick={() => setIsEditing(!isEditing)} className="unique-edit-button">
          {isEditing ? "Сохранить" : "Редактировать"}
        </button>

        <div className="unique-meeting-info">
          <span className="unique-meeting-number">Встреча {meetingId}</span>
        </div>

        <div className="unique-meeting-info">
          <span className="unique-label">Дата:</span>
          {isEditing ? (
            <input type="text" name="date" value={meetingData.date} onChange={handleChange} />
          ) : (
            <span className="unique-meeting-date">{meetingData.date}</span>
          )}
        </div>

        <div className="unique-meeting-info">
          <span className="unique-label">Задачи к следующей встрече:</span>
          {isEditing ? (
            <textarea name="nextTasks" value={meetingData.nextTasks} onChange={handleChange} />
          ) : (
            <div className="unique-task">{meetingData.nextTasks || "Не указано"}</div>
          )}
        </div>

        <div className="unique-meeting-info">
          <span className="unique-label">Выполнение задач с прошлой встречи:</span>
          {isEditing ? (
            <textarea name="previousTasks" value={meetingData.previousTasks} onChange={handleChange} />
          ) : (
            <div className="unique-task">{meetingData.previousTasks || "Не указано"}</div>
          )}
        </div>

        <div className="unique-meeting-info">
          <span className="unique-label">Текущий статус команды:</span>
          {isEditing ? (
            <input type="text" name="teamStatus" value={meetingData.teamStatus} onChange={handleChange} />
          ) : (
            <div className="unique-status">{meetingData.teamStatus}</div>
          )}
        </div>

        <div className="unique-meeting-info">
          <span className="unique-label">Запись встречи:</span>
          {isEditing ? (
            <input type="text" name="recordingLink" value={meetingData.recordingLink} onChange={handleChange} />
          ) : (
            meetingData.recordingLink ? (
              <a href={meetingData.recordingLink} target="_blank" rel="noopener noreferrer" className="unique-link">
                Смотреть запись
              </a>
            ) : (
              <div className="unique-link">Нет записи</div>
            )
          )}
        </div>
      </div>
    </div>
  );
};

export default MeetingCard;