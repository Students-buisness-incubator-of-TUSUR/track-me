import React, { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import "./meeting-card.css";

const backendHost = process.env.REACT_APP_BACKEND_HOST || "https://xn--b1afb6bcb.xn--e1aaowdh.xn----gtbbcb4bjf2ak.xn--p1ai";

const MeetingCard = () => {
  const { meetingId } = useParams();
  const token = localStorage.getItem("accessToken");

  const [currentUser, setCurrentUser] = useState(null);
  const [meetingData, setMeetingData] = useState(
    JSON.parse(localStorage.getItem(`meeting-${meetingId}`)) || {
      date: "30.04",
      nextTasks: "",
      previousTasks: "",
      teamStatus: "Все ок",
      screenshot: "",
      recordingLink: "",
    }
  );
  const [isEditing, setIsEditing] = useState(false);

  const isAdmin = (user) => {
    return user?.role === "ADMIN" || user?.role === "SUPER_ADMIN";
  };

  // Получаем информацию о текущем пользователе
  useEffect(() => {
    fetch(`${backendHost}/api/v1/users/current/info`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })
      .then((response) => response.json())
      .then((userData) => {
        setCurrentUser(userData);
      })
      .catch((error) => {
        console.error("Ошибка при получении данных пользователя:", error);
      });
  }, []);

  useEffect(() => {
    localStorage.setItem(`meeting-${meetingId}`, JSON.stringify(meetingData));
  }, [meetingData, meetingId]);

  // Функция для обновления данных
  const handleChange = (e) => {
    const { name, value } = e.target;
    setMeetingData({ ...meetingData, [name]: value });
  };

  // Используем имя из currentUser для отображения
  const renderTrackerName = () => {
    if (currentUser && !isAdmin) {
      return currentUser.fullName;
    }
    return meetingData?.tracker?.fullName || "Не назначен";
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
          <span className="unique-label">Трекер:</span>
          <span className="unique-value">{renderTrackerName()}</span>
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