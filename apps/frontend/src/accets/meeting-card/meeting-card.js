import React, { useEffect, useState, useRef } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import "./meeting-card.css";
import closeIcon from "./free-icon-font-cross-3917759 (1) 1.png";
import pencilIcon from "./pen.png";
import { getCsrfConfigForFetch } from "../../utils/csrf-utils";

const MeetingCard = () => {
    const backendHost = (process.env.REACT_APP_BACKEND_URI || '') + '/meeting';
    const { meetingId } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const query = new URLSearchParams(location.search);
    const teamId = query.get("teamId");
    const username = query.get("username");
    const userId = query.get("userId");
    const [showStatusDropdown, setShowStatusDropdown] = useState(false);
    const isNewMeeting = meetingId === "new";
    const [error, setError] = useState(null);
    const [meetingData, setMeetingData] = useState({
        number: isNewMeeting ? "Новая встреча" : "",
        startDate: new Date().toISOString(),
        link: "",
        tasksCurrentMeeting: "",
        tasksNextMeeting: "",
        teamStatus: "",
        status: "SCHEDULED"
    });
    const [isEditing, setIsEditing] = useState(isNewMeeting);
    const [image, setImage] = useState(null);
    const [imagePreview, setImagePreview] = useState(null);
    const fileInputRef = useRef(null);

    useEffect(() => {
        if (!isNewMeeting && meetingId) {
            // Fetch meeting data
            const url = new URL(`${backendHost}/api/v1/meetings`);
            url.searchParams.append('teamCardId', teamId);
            url.searchParams.append('page', 0);
            url.searchParams.append('size', 10);

            fetch(url, {
                method: 'GET',
                headers: { "Content-Type": "application/json" },
                credentials: 'include',
            })
            .then(res => {
                if (!res.ok) throw new Error('Ошибка загрузки встречи');
                return res.json();
            })
            .then(data => {
                const meeting = data.content.find(m => m.id === meetingId);
                if (meeting) {
                    setMeetingData({
                        number: meeting.number || "Новая встреча",
                        startDate: meeting.startDate,
                        link: meeting.link || "",
                        tasksCurrentMeeting: meeting.tasksCurrentMeeting || "",
                        tasksNextMeeting: meeting.tasksNextMeeting || "",
                        teamStatus: meeting.teamStatus,
                        status: meeting.status || "SCHEDULED"
                    });
                } else {
                    throw new Error('Встреча не найдена');
                }
            })
            .catch(err => {
                console.error("Ошибка при загрузке встречи:", err);
                setError("Не удалось загрузить данные встречи");
            });

            // Fetch meeting image
            fetch(`${backendHost}/api/v1/image/${meetingId}`, {
                method: 'GET',
                credentials: 'include',
            })
            .then(res => {
                if (res.ok) return res.blob();
                throw new Error('Ошибка загрузки изображения');
            })
            .then(blob => {
                const imageUrl = URL.createObjectURL(blob);
                setImagePreview(imageUrl);
            })
            .catch(err => {
                console.error("Ошибка при загрузке изображения:", err);
            });
        }
    }, [meetingId, teamId, isNewMeeting, backendHost]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setMeetingData(prev => ({
            ...prev,
            [name]: name === 'startDate' ? new Date(value).toISOString() : value
        }));
    };

    const handleImageChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setImage(file);
            const reader = new FileReader();
            reader.onloadend = () => {
                setImagePreview(reader.result);
            };
            reader.readAsDataURL(file);
        }
    };

    const handleSave = async () => {
        try {
            if (!teamId) throw new Error("Отсутствует идентификатор команды");

            const meetingPayload = {
                link: meetingData.link || "",
                number: meetingData.number || "",
                teamStatus: meetingData.teamStatus,
                tasksCurrentMeeting: meetingData.tasksCurrentMeeting || "",
                tasksNextMeeting: meetingData.tasksNextMeeting || "",
                startDate: meetingData.startDate || new Date().toISOString(),
                status: meetingData.status || "SCHEDULED"
            };

            const url = isNewMeeting 
                ? `${backendHost}/api/v1/meetings?teamCardId=${teamId}`
                : `${backendHost}/api/v1/update-meeting/${meetingId}?teamCardId=${teamId}`;

            const method = isNewMeeting ? "POST" : "PATCH";
            const body = isNewMeeting 
                ? JSON.stringify({ ...meetingPayload, startDate: meetingData.startDate })
                : JSON.stringify(meetingPayload);

            const response = await fetch(url, {
                method,
                headers: { 
                    "Content-Type": "application/json",
                    "Accept": "application/json",
                    ...getCsrfConfigForFetch()
                },
                credentials: 'include',
                mode: 'cors',
                body
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || "Ошибка при сохранении встречи");
            }

            const result = await response.json();
            const savedMeetingId = isNewMeeting ? result.id : meetingId;

            if (image && savedMeetingId) {
                const formData = new FormData();
                formData.append('file', image);
                
                const imageResponse = await fetch(`${backendHost}/api/v1/image/${savedMeetingId}`, {
                    method: 'POST',
                    headers: {
                        ...getCsrfConfigForFetch()
                    },
                    credentials: 'include',
                    body: formData
                });

                if (!imageResponse.ok) {
                    throw new Error('Ошибка при загрузке изображения');
                }
            }

            if (isNewMeeting) {
                navigate(`/meeting/${savedMeetingId}?teamId=${teamId}&username=${username}`);
            } else {
                setMeetingData(result);
                setIsEditing(false);
                setImage(null);
            }
        } catch (error) {
            console.error("Ошибка при сохранении:", error);
            setError(error.message || "Произошла ошибка при сохранении. Проверьте консоль для подробностей.");
        }
    };

    const handleCompleteMeeting = async (completed) => {
    try {
        const newStatus = completed ? "COMPLETED" : "NOT_HAPPENED";
        
        // Сначала обновляем локальное состояние для мгновенного отображения
        setMeetingData(prev => ({
            ...prev,
            status: newStatus
        }));

        // Отправляем изменения на сервер
        const response = await fetch(`${backendHost}/api/v1/update-meeting/${meetingId}?teamCardId=${teamId}`, {
            method: 'PATCH',
            headers: { 
                "Content-Type": "application/json",
                "Accept": "application/json",
                ...getCsrfConfigForFetch()
            },
            credentials: 'include',
            mode: 'cors',
            body: JSON.stringify({
                status: newStatus,
                // Можно добавить другие поля, если нужно
                link: meetingData.link || "",
                number: meetingData.number || "",
                teamStatus: meetingData.teamStatus,
                tasksCurrentMeeting: meetingData.tasksCurrentMeeting || "",
                tasksNextMeeting: meetingData.tasksNextMeeting || "",
                startDate: meetingData.startDate || new Date().toISOString()
            })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || "Ошибка при сохранении статуса встречи");
        }

        const result = await response.json();
        setMeetingData(result); // Обновляем данные с сервера

    } catch (error) {
        console.error("Ошибка при сохранении статуса:", error);
        setError(error.message || "Произошла ошибка при сохранении статуса. Проверьте консоль для подробностей.");
        
        // Откатываем изменения, если не удалось сохранить
        setMeetingData(prev => ({
            ...prev,
            status: prev.status // Возвращаем предыдущий статус
        }));
    }
};

    const handleEditClick = () => {
    if (isMeetingLocked) {
        setError("Эту встречу нельзя редактировать, так как она завершена или не состоялась");
        // Добавим таймер для автоматического скрытия ошибки через 5 секунд
        setTimeout(() => setError(null), 5000);
        return;
    }
    setIsEditing(true);
};

    const isMeetingCompleted = meetingData.status === "COMPLETED" || 
                              meetingData.status === "NOT_HAPPENED" || 
                              meetingData.status === "COMPLETED_AS_NOT_HAPPENED";
    
    const isMeetingLocked = isMeetingCompleted;

    // const getStatusBadgeText = () => {
        // switch(meetingData.status) {
            // case "COMPLETED": 
                // return " (Завершена)";
            // case "NOT_HAPPENED": 
                // return " (Не состоялась)";
            // case "COMPLETED_AS_NOT_HAPPENED": 
                // return " (Не состоялась)";
            // default: 
                // return "";
        // }
    // };

    return (
        <div className="unique-meeting-container">
            <div className="unique-meeting-card">
                <button className="unique-close-button" onClick={() => navigate(`/teamcard/${teamId}?userId=${userId}`)}>
                    <img src={closeIcon} alt="Закрыть" className="close-icon" />
                </button>
                
                <button
    onClick={isEditing ? handleSave : handleEditClick}
    className="unique-edit-button"
    disabled={isMeetingLocked && !isEditing}
    style={{
        cursor: isMeetingLocked && !isEditing ? 'not-allowed' : 'pointer'
    }}
>
    {isEditing ? "Сохранить" : "Редактировать"}
</button>

                {error && (
    <div className="error-message" style={{
        backgroundColor: '#ffebee',
        color: '#d32f2f',
        padding: '10px',
        borderRadius: '4px',
        margin: '10px 0',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center'
    }}>
        {error}
        <button 
            className="error-close" 
            onClick={() => setError(null)}
            style={{
                background: 'none',
                border: 'none',
                color: '#d32f2f',
                fontSize: '16px',
                cursor: 'pointer'
            }}
        >
            ×
        </button>
    </div>
)}

                <div className="unique-meeting-info">
                    <span className="unique-meeting-number">
                        {isNewMeeting ? "Новая встреча" : `Встреча ${meetingData.number}`}
                        
                    </span>
                    {!isNewMeeting && !isEditing && (
    <div className="unique-meeting-status-buttons">
        <button 
            onClick={() => handleCompleteMeeting(true)}
            disabled={isMeetingLocked || meetingData.status === "COMPLETED"}
            className={`unique-status-button unique-status-completed ${
                meetingData.status === "COMPLETED" ? "" : 
                (meetingData.status === "NOT_HAPPENED" || meetingData.status === "COMPLETED_AS_NOT_HAPPENED") ? "hidden" : ""
            }`}
        >
            Состоялась
        </button>
        <button 
            onClick={() => handleCompleteMeeting(false)}
            disabled={isMeetingLocked || meetingData.status === "NOT_HAPPENED" || meetingData.status === "COMPLETED_AS_NOT_HAPPENED"}
            className={`unique-status-button unique-status-not-happened ${
                meetingData.status === "NOT_HAPPENED" || meetingData.status === "COMPLETED_AS_NOT_HAPPENED" ? "" : 
                meetingData.status === "COMPLETED" ? "hidden" : ""
            }`}
        >
            Не состоялась
        </button>
    </div>
)}
                </div>

                {/* Остальной код компонента остается без изменений */}
                <div className="unique-meeting-info-row unique-date-row">
                    <span className="unique-label">Дата:</span>
                    {isEditing ? (
                        <div className="unique-date-input-wrapper">
                            <input
                                type="date"
                                name="startDate"
                                value={meetingData.startDate ? new Date(meetingData.startDate).toISOString().split('T')[0] : ''}
                                onChange={handleChange}
                                className="unique-date-input"
                                disabled={isMeetingLocked}
                            />
                            <img 
                                src={pencilIcon} 
                                alt="Редактировать" 
                                style={{ marginTop: "-6px" }}
                                className="edit-icon23" 
                            />
                        </div>
                    ) : (
                        <span className="unique-meeting-date">
                            {meetingData.startDate
                                ? new Date(meetingData.startDate).toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit' })
                                : 'Не указана'}
                        </span>
                    )}
                </div>

                <div className="unique-meeting-info-row">
                    <span className="unique-label">Задачи к следующей встрече:</span>
                    {isEditing ? (
                        <>
                            <textarea
                                name="tasksCurrentMeeting"
                                value={meetingData.tasksCurrentMeeting || ''}
                                onChange={handleChange}
                                className="unique-textarea"
                                disabled={isMeetingCompleted}
                            />
                            <img src={pencilIcon} alt="Редактировать" className="edit-icon23" />
                        </>
                    ) : (
                        <div className="unique-task">{meetingData.tasksCurrentMeeting || "Не указаны"}</div>
                    )}
                </div>

                <div className="unique-meeting-info-row">
                    <span className="unique-label">Выполнили задачи прошлой встречи или нет, общая информация по команде:</span>
                    {isEditing ? (
                        <>
                            <textarea
                                name="tasksNextMeeting"
                                value={meetingData.tasksNextMeeting || ''}
                                onChange={handleChange}
                                className="unique-textarea"
                                disabled={isMeetingCompleted}
                            />
                            <img src={pencilIcon} alt="Редактировать" className="edit-icon23" />
                        </>
                    ) : (
                        <div className="unique-task">{meetingData.tasksNextMeeting || "Не указаны"}</div>
                    )}
                </div>

                <div className="unique-meeting-info-row">
                    <span className="unique-label">Текущий статус команды:</span>
                    {isEditing ? (
                        <div className="status-dropdown-wrapper">
                            <div 
  className="status-selected" 
  onClick={() => !isMeetingCompleted && setShowStatusDropdown(prev => !prev)}
  onKeyDown={(e) => {
    if ((e.key === 'Enter' || e.key === ' ') && !isMeetingCompleted) {
      setShowStatusDropdown(prev => !prev);
    }
  }}
  tabIndex={0}
  role="button"
  aria-expanded={showStatusDropdown}
  aria-haspopup="listbox"
>
  {meetingData.teamStatus === "OK" && "Всё ок"}
  {meetingData.teamStatus === "WITH_ISSUES" && "Есть проблемы"}
  {meetingData.teamStatus === "MANY_ISSUES" && "Есть большие проблемы"}
  {!meetingData.teamStatus && "Не указано"}
  <span className="dropdown-arrow">{showStatusDropdown ? "▲" : "▼"}</span>
</div>
                            
                            {showStatusDropdown && (
                                <div className="status-options">
                                    <div
                                        className="status-option ok"
                                        onClick={() => {
                                            setMeetingData(prev => ({ ...prev, teamStatus: "OK" }));
                                            setShowStatusDropdown(false);
                                        }}
                                    >
                                        Всё ок
                                    </div>
                                    <div
                                        className="status-option problems"
                                        onClick={() => {
                                            setMeetingData(prev => ({ ...prev, teamStatus: "WITH_ISSUES" }));
                                            setShowStatusDropdown(false);
                                        }}
                                    >
                                        Есть проблемы
                                    </div>
                                    <div
                                        className="status-option major-problems"
                                        onClick={() => {
                                            setMeetingData(prev => ({ ...prev, teamStatus: "MANY_ISSUES" }));
                                            setShowStatusDropdown(false);
                                        }}
                                    >
                                        Есть большие проблемы
                                    </div>
                                </div>
                            )}
                        </div>
                    ) : (
                        <div className={`unique-status ${meetingData.teamStatus?.toLowerCase() || ''}`}>
                            {meetingData.teamStatus === "OK" && "Всё ок"}
                            {meetingData.teamStatus === "WITH_ISSUES" && "Есть проблемы"}
                            {meetingData.teamStatus === "MANY_ISSUES" && "Есть большие проблемы"}
                            {!meetingData.teamStatus && "Не указано"}
                        </div>
                    )}
                </div>

                <div className="unique-meeting-info-row">
                    <span className="unique-label">Скриншот встречи:</span>
                    {isEditing ? (
                        <div 
                            className="unique-image-upload" 
                            onClick={() => !isMeetingCompleted && fileInputRef.current.click()}
                            onKeyDown={(e) => {
                                if ((e.key === 'Enter' || e.key === ' ') && !isMeetingCompleted) {
                                    fileInputRef.current.click();
                                }
                            }}
                            tabIndex={0}
                            role="button"
                            aria-label="Загрузить изображение"
                            style={{ marginLeft: '30px' }}
                        > 
                            <input
                                type="file"
                                accept="image/*"
                                onChange={handleImageChange}
                                className="unique-image-input"
                                ref={fileInputRef}
                                disabled={isMeetingCompleted}
                            />
                            {imagePreview ? (
                                <img src={imagePreview} alt="Превью" className="unique-meeting-image" />
                            ) : (
                                <div className="unique-screenshot-placeholder">
                                    <span>Выберите изображение</span>
                                </div>
                            )}
                            <img src={pencilIcon} alt="Редактировать" className="edit-icon23" />
                        </div>
                    ) : imagePreview ? (
                        <img src={imagePreview} alt="Скриншот встречи" className="unique-meeting-image" />
                    ) : (
                        <div className="unique-screenshot-placeholder">
                            <span>Изображение не загружено</span>
                        </div>
                    )}
                </div>

                <div className="unique-meeting-info-row">
                    <span className="unique-label">Запись встречи:</span>
                    {isEditing ? (
                        <>
                            <input
                                type="text"
                                name="link"
                                value={meetingData.link || ''}
                                onChange={handleChange}
                                className="unique-input"
                                disabled={isMeetingCompleted}
                            />
                            <img src={pencilIcon} alt="Редактировать" className="edit-icon23" />
                        </>
                    ) : meetingData.link ? (
                        <a href={meetingData.link} target="_blank" rel="noopener noreferrer" className="unique-link">
                            {meetingData.link || ''}
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