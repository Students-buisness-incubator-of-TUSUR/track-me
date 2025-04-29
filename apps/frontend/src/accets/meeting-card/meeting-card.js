import React, {useEffect, useState} from "react";
import {useLocation, useNavigate, useParams} from "react-router-dom";
import "./meeting-card.css";
import closeIcon from "./free-icon-font-cross-3917759 (1) 1.png";

const backendHost = process.env.REACT_APP_BACKEND_URI + '/backend';

const MeetingCard = () => {
    const { meetingId } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const query = new URLSearchParams(location.search);
    const teamId = query.get("teamId");
    const username = query.get("username");

    const isNewMeeting = meetingId === "new";

    const [error, setError] = useState(null);
    const [meetingData, setMeetingData] = useState({
        number: isNewMeeting ? "Новая встреча" : "",
        startDate: new Date().toISOString(),
        link: "",
        tasksCurrentMeeting: "",
        tasksNextMeeting: "",
        status: "OK",
    });
    const [isEditing, setIsEditing] = useState(isNewMeeting);

    useEffect(() => {
        if (!isNewMeeting && meetingId) {
            fetch(`${backendHost}/api/v1/meetings/${meetingId}?teamCardId=${teamId}`, {
                credentials: 'include',
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
    }, [meetingId, teamId, isNewMeeting]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setMeetingData(prev => ({ ...prev, [name]: value }));
    };

    const handleSave = async () => {
        try {
            if (!teamId) throw new Error("Отсутствует идентификатор команды");

            const meetingPayload = {
                link: meetingData.link || "",
                number: meetingData.number || "",
                status: meetingData.status || "OK",
                tasksCurrentMeeting: meetingData.tasksCurrentMeeting || "",
                tasksNextMeeting: meetingData.tasksNextMeeting || ""
            };

            if (isNewMeeting) {
                const response = await fetch(`${backendHost}/api/v1/meetings?teamCardId=${teamId}`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: 'include',
                    body: JSON.stringify({ ...meetingPayload, startDate: meetingData.startDate }),
                });
                if (!response.ok) throw new Error("Ошибка при создании встречи");

                const newMeeting = await response.json();
                navigate(`/meeting/${newMeeting.id}?teamId=${teamId}&username=${username}`);
            } else if (meetingId) {
                const response = await fetch(`${backendHost}/api/v1/meetings/${meetingId}?teamCardId=${teamId}`, {
                    method: "PATCH",
                    headers: { "Content-Type": "application/json" },
                    credentials: 'include',
                    body: JSON.stringify(meetingPayload),
                });
                if (!response.ok) throw new Error("Ошибка при обновлении встречи");

                const updatedMeeting = await response.json();
                setMeetingData(updatedMeeting);
                setIsEditing(false);
            }
        } catch (error) {
            console.error("Ошибка при сохранении:", error);
            setError(error.message);
        }
    };

    return (
        <div className="unique-meeting-container">
            <div className="unique-meeting-card">
                <button className="unique-close-button" onClick={() => navigate(-1)}><img src={closeIcon} alt="Закрыть" className="close-icon" /></button>
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

                {/* Дата */}
                <div className="unique-meeting-info-row unique-date-row">
                    <span className="unique-label">Дата:</span>
                    {isEditing ? (
                        <input
                            type="datetime-local"
                            name="startDate"
                            value={meetingData.startDate ? new Date(meetingData.startDate).toISOString().slice(0, 16) : ''}
                            onChange={handleChange}
                            className="unique-input-date"
                        />
                    ) : (
                        <span className="unique-meeting-date">
                            {meetingData.startDate
                                ? new Date(meetingData.startDate).toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit' })
                                : 'Не указана'}
                        </span>
                    )}
                </div>

                {/* Задачи текущей встречи */}
                <div className="unique-meeting-info-row">
                    <span className="unique-label">Задачи к следующей встрече:</span>
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

                {/* Задачи к следующей встрече */}
                <div className="unique-meeting-info-row">
                    <span className="unique-label">Выполнили задачи прошлой встречи или нет, общая информация по команде:</span>
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

                {/* Статус встречи */}
                <div className="unique-meeting-info-row">
                    <span className="unique-label">Текущий статус команды:</span>
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

                {/* Скриншот встречи */}
                <div className="unique-meeting-info-row">
                    <span className="unique-label">Скриншот встречи:</span>
                    <div className="unique-screenshot-placeholder" />
                </div>

                {/* Ссылка на встречу */}
                <div className="unique-meeting-info-row">
                    <span className="unique-label">Запись встречи:</span>
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
                            Перейти к записи
                        </a>
                    ) : (
                        <div className="unique-link">Ссылка не указана</div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default MeetingCard;
