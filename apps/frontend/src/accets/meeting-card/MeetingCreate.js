import React, { useState, useEffect } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";
import "./meeting-card.css";
import closeIcon from "./free-icon-font-cross-3917759 (1) 1.png";
import pencilIcon from "./pen.png";

const backendHost = process.env.REACT_APP_BACKEND_URI + '/backend';

const MeetingCreate = () => {
    const { teamId } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const query = new URLSearchParams(location.search);
    const userId = query.get("userId");
    const [error, setError] = useState(null);
    const [showStatusDropdown, setShowStatusDropdown] = useState(false);

    const [meetingData, setMeetingData] = useState({
        number: "1",
        startDate: new Date().toISOString(),
        link: "",
        tasksCurrentMeeting: "",
        tasksNextMeeting: "",
        status: "",
    });

    useEffect(() => {
        const fetchMeetings = async () => {
            try {
                const url = new URL(`${backendHost}/api/v1/meetings`);
                url.searchParams.append('teamCardId', teamId);
                url.searchParams.append('page', 0);
                url.searchParams.append('size', 100);
                const response = await fetch(url, {
                    method: 'GET',
                    headers: { "Content-Type": "application/json" },
                    credentials: 'include',
                });

                if (!response.ok) throw new Error('Ошибка загрузки встреч');

                const data = await response.json();
                let maxNumber = 0;
                if (data.content && data.content.length > 0) {
                    data.content.forEach(meeting => {
                        const num = parseInt(meeting.number);
                        if (!isNaN(num) && num > maxNumber) {
                            maxNumber = num;
                        }
                    });
                }

                setMeetingData(prev => ({
                    ...prev,
                    number: (maxNumber + 1).toString()
                }));
            } catch (err) {
                console.error("Ошибка при загрузке встреч:", err);
                setError("Не удалось загрузить список встреч");
            }
        };

        fetchMeetings();
    }, [teamId]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setMeetingData(prev => ({ ...prev, [name]: value }));
    };

    const handleCreate = async () => {
        try {
            if (!meetingData.number || isNaN(parseInt(meetingData.number))) {
                throw new Error("Номер встречи должен быть числом");
            }

            // Подготовка данных для отправки
            const requestData = {
                number: meetingData.number,
                startDate: new Date(meetingData.startDate).toISOString(),
                link: meetingData.link,
                tasksCurrentMeeting: meetingData.tasksCurrentMeeting,
                tasksNextMeeting: meetingData.tasksNextMeeting,
                status: meetingData.status, // статус берется из состояния
            };

            const response = await fetch(`${backendHost}/api/v1/meetings?teamCardId=${teamId}`, {
                method: "POST",
                headers: { 
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                credentials: 'include',
                body: JSON.stringify(requestData),
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || "Ошибка при создании встречи");
            }

            const result = await response.json();
            navigate(`/meeting/${result.id}?teamId=${teamId}`);
        } catch (error) {
            console.error("Ошибка при создании:", error);
            setError(error.message || "Произошла ошибка при создании. Проверьте консоль для подробностей.");
        }
    };
    return (
        <div className="unique-meeting-container">
            <div className="unique-meeting-card">
                <button className="unique-close-button" onClick={() => navigate(`/teamcard/${teamId}?userId=${userId}`)}>
                    <img src={closeIcon} alt="Закрыть" className="close-icon" />
                </button>

                <button onClick={handleCreate} className="unique-edit-button">
                    Создать 
                </button>

                {error && <div className="error-message">{error}</div>}

                <div className="unique-meeting-info">
                    <span className="unique-meeting-number">Новая встреча #{meetingData.number}</span>
                </div>

                <div className="unique-meeting-info-row unique-date-row">
                    <span className="unique-label">Дата:</span>
                    <input
                        type="datetime-local"
                        name="startDate"
                        value={new Date(meetingData.startDate).toISOString().slice(0, 16)}
                        onChange={handleChange}
                        className="unique-date-input"
                    />
                </div>

                <div className="unique-meeting-info-row">
                    <span className="unique-label">Задачи к следующей встрече:</span>
                    <textarea
                        name="tasksCurrentMeeting"
                        value={meetingData.tasksCurrentMeeting}
                        onChange={handleChange}
                        className="unique-textarea"
                        placeholder="Введите задачи для следующей встречи"
                    />
                    <img src={pencilIcon} alt="Редактировать" className="edit-icon23" />
                </div>

                <div className="unique-meeting-info-row">
                    <span className="unique-label">Выполнили задачи прошлой встречи или нет, общая информация по команде:</span>
                    <textarea
                        name="tasksNextMeeting"
                        value={meetingData.tasksNextMeeting}
                        onChange={handleChange}
                        className="unique-textarea"
                        placeholder="Опишите выполнение задач и общее состояние команды"
                    />
                    <img src={pencilIcon} alt="Редактировать" className="edit-icon23" />
                </div>

                <div className="unique-meeting-info-row">
                    <span className="unique-label">Текущий статус команды:</span>
                    <div className="status-dropdown-wrapper">
                        <div className="status-selected" onClick={() => setShowStatusDropdown(prev => !prev)}>
                            {meetingData.status === "OK" && "Всё ок"}
                            {meetingData.status === "WITH_ISSUES" && "Есть проблемы"}
                            {meetingData.status === "MANY_ISSUES" && "Есть большие проблемы"}
                            <span className="dropdown-arrow">{showStatusDropdown ? "▲" : "▼"}</span>
                        </div>
                        {showStatusDropdown && (
                            <div className="status-options">
                                <div className="status-option ok" onClick={() => { setMeetingData(prev => ({ ...prev, status: "OK" })); setShowStatusDropdown(false); }}>Всё ок</div>
                                <div className="status-option problems" onClick={() => { setMeetingData(prev => ({ ...prev, status: "WITH_ISSUES" })); setShowStatusDropdown(false); }}>Есть проблемы</div>
                                <div className="status-option major-problems" onClick={() => { setMeetingData(prev => ({ ...prev, status: "MANY_ISSUES" })); setShowStatusDropdown(false); }}>Есть большие проблемы</div>
                            </div>
                        )}
                    </div>
                </div>

                <div className="unique-meeting-info-row">
                    <span className="unique-label">Скриншот встречи:</span>
                    <div className="unique-screenshot-placeholder" />
                </div>

                <div className="unique-meeting-info-row">
                    <span className="unique-label">Запись встречи:</span>
                    <input
                        type="text"
                        name="link"
                        value={meetingData.link}
                        onChange={handleChange}
                        className="unique-input"
                        placeholder="Введите ссылку на запись встречи"
                    />
                    <img src={pencilIcon} alt="Редактировать" className="edit-icon23" />
                </div>
            </div>
        </div>
    );
};

export default MeetingCreate;
