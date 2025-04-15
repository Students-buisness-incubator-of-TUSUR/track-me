import React, { useState, useEffect } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import "./meeting-card.css";

const backendHost = process.env.REACT_APP_BACKEND_HOST || "https://xn--b1afb6bcb.xn--e1aaowdh.xn----gtbbcb4bjf2ak.xn--p1ai";

const MeetingCard = () => {
  const { meetingId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const query = new URLSearchParams(location.search);
  const teamId = query.get("teamId");
  const userId = query.get("userId");
  const token = localStorage.getItem("accessToken");

  const isNewMeeting = meetingId === "new";

  const [error, setError] = useState(null);
  const [meetingData, setMeetingData] = useState({
    number: isNewMeeting ? "Новая встреча" : "",
    startDate: new Date().toISOString(),
    link: "",
    tasksCurrentMeeting: "",
    tasksNextMeeting: "",
    status: "OK"
  });
  const [isEditing, setIsEditing] = useState(isNewMeeting);

  // Загружаем данные существующей встречи
  useEffect(() => {
    if (!isNewMeeting && meetingId) {
      fetch(`${backendHost}/api/v1/meetings/${meetingId}?teamCardId=${teamId}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        }
      })
      .then(res => {
        if (!res.ok) throw new Error('Ошибка загрузки встречи');
        return res.json();
      })
      .then(data => {
        setMeetingData(data);
      })
      .catch(err => {
        console.error("Ошибка при загрузке встречи:", err);
        if (!isNewMeeting) {
          setError("Не удалось загрузить данные встречи");
        }
      });
    }
  }, [meetingId, teamId, token, isNewMeeting]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setMeetingData(prev => ({ ...prev, [name]: value }));
  };

  const handleSave = async () => {
    try {
      // Проверяем, что у нас есть teamId
      if (!teamId) {
        throw new Error("Отсутствует идентификатор команды");
      }

      const meetingPayload = {
        link: meetingData.link || "",
        number: meetingData.number || "",
        status: meetingData.status || "OK",
        tasksCurrentMeeting: meetingData.tasksCurrentMeeting || "",
        tasksNextMeeting: meetingData.tasksNextMeeting || ""
      };

      // Для новой встречи используем POST
      if (isNewMeeting) {
        const response = await fetch(`${backendHost}/api/v1/meetings?teamCardId=${teamId}`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            ...meetingPayload,
            startDate: meetingData.startDate
          }),
        });

        if (!response.ok) {
          const errorText = await response.text();
          console.error("Response error:", errorText);
          throw new Error("Ошибка при создании встречи");
        }

        const newMeeting = await response.json();
        navigate(`/meeting/${newMeeting.id}?teamId=${teamId}&userId=${userId}`);
      } 
      // Для существующей встречи используем PATCH
      else if (meetingId) {  // Проверяем наличие meetingId
        const response = await fetch(
          `${backendHost}/api/v1/meetings/${meetingId}?teamCardId=${teamId}`,
          {
            method: "PATCH",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(meetingPayload)
          }
        );

        if (!response.ok) {
          const errorText = await response.text();
          console.error("Response error:", errorText);
          throw new Error("Ошибка при обновлении встречи");
        }

        const updatedMeeting = await response.json();
        setMeetingData(updatedMeeting);
        setIsEditing(false);
      } else {
        throw new Error("Некорректный идентификатор встречи");
      }
    } catch (error) {
      console.error("Ошибка при сохранении:", error);
      setError(error.message);
    }
  };

  return (
    <div className="unique-meeting-container">
      <div className="unique-meeting-card">
        <button className="unique-close-button" onClick={() => navigate(-1)}>×</button>
        <button 
          onClick={isEditing ? handleSave : () => setIsEditing(true)} 
          className="unique-edit-button"
        >
          {isEditing ? "Сохранить" : "Редактировать"}
        </button>

        {error && <div className="error-message">{error}</div>}

        <div className="unique-meeting-info">
          <span className="unique-meeting-number">
            {isNewMeeting ? "Новая встреча" : `Встреча ${meetingData.number}`}
          </span>
        </div>

        <div className="unique-meeting-info">
          <span className="unique-label">Дата встречи:</span>
          {isEditing ? (
            <input 
              type="datetime-local" 
              name="startDate" 
              value={meetingData.startDate ? new Date(meetingData.startDate).toISOString().slice(0, 16) : ''} 
              onChange={handleChange} 
            />
          ) : (
            <span className="unique-meeting-date">
              {meetingData.startDate ? new Date(meetingData.startDate).toLocaleString('ru-RU') : 'Не указана'}
            </span>
          )}
        </div>

        <div className="unique-meeting-info">
          <span className="unique-label">Задачи текущей встречи:</span>
          {isEditing ? (
            <textarea 
              name="tasksCurrentMeeting" 
              value={meetingData.tasksCurrentMeeting || ''} 
              onChange={handleChange}
              className="unique-textarea"
            />
          ) : (
            <div className="unique-task">{meetingData.tasksCurrentMeeting || "Не указаны"}</div>
          )}
        </div>

        <div className="unique-meeting-info">
          <span className="unique-label">Задачи к следующей встрече:</span>
          {isEditing ? (
            <textarea 
              name="tasksNextMeeting" 
              value={meetingData.tasksNextMeeting || ''} 
              onChange={handleChange}
              className="unique-textarea"
            />
          ) : (
            <div className="unique-task">{meetingData.tasksNextMeeting || "Не указаны"}</div>
          )}
        </div>

        <div className="unique-meeting-info">
          <span className="unique-label">Ссылка на встречу:</span>
          {isEditing ? (
            <input 
              type="text" 
              name="link" 
              value={meetingData.link || ''} 
              onChange={handleChange}
              className="unique-input"
            />
          ) : meetingData.link ? (
            <a href={meetingData.link} target="_blank" rel="noopener noreferrer" className="unique-link">
              Присоединиться к встрече
            </a>
          ) : (
            <div className="unique-link">Ссылка не указана</div>
          )}
        </div>

        <div className="unique-meeting-info">
          <span className="unique-label">Статус встречи:</span>
          {isEditing ? (
            <select
              name="status"
              value={meetingData.status}
              onChange={handleChange}
              className="unique-dropdown"
            >
              <option value="OK">Всё ок</option>
              <option value="PROBLEMS">Есть проблемы</option>
              <option value="MAJOR_PROBLEMS">Есть большие проблемы</option>
            </select>
          ) : (
            <div className={`unique-status ${meetingData.status.toLowerCase()}`}>
              {meetingData.status === "OK" && "Всё ок"}
              {meetingData.status === "PROBLEMS" && "Есть проблемы"}
              {meetingData.status === "MAJOR_PROBLEMS" && "Есть большие проблемы"}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default MeetingCard;