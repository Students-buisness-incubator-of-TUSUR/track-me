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
                        teamStatus: meeting.teamStatus || "Не указано",
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
        setMeetingData(prev => ({ ...prev, [name]: value }));
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
                teamStatus: meetingData.teamStatus || "Не указано",
                tasksCurrentMeeting: meetingData.tasksCurrentMeeting || "",
                tasksNextMeeting: meetingData.tasksNextMeeting || ""
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

            // Upload image if exists
            // Загрузка изображения, если оно есть
if (image && savedMeetingId) {
    const formData = new FormData();
    formData.append('file', image);  // Важно: используем 'file', а не 'image'
    
    const imageResponse = await fetch(`${backendHost}/api/v1/image/${savedMeetingId}`, {
        method: 'POST',
        headers: {
            ...getCsrfConfigForFetch()  // CSRF-защита
            // Не указываем Content-Type вручную, браузер сам добавит multipart/form-data
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

    return (
        <div className="unique-meeting-container">
            <div className="unique-meeting-card">
                <button className="unique-close-button" onClick={() => navigate(`/teamcard/${teamId}?userId=${userId}`)}>
                    <img src={closeIcon} alt="Закрыть" className="close-icon" />
                </button>
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

                <div className="unique-meeting-info-row unique-date-row">
                    <span className="unique-label">Дата:</span>
                    <span className="unique-meeting-date">
                        {meetingData.startDate
                            ? new Date(meetingData.startDate).toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit' })
                            : 'Не указана'}
                    </span>
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
                            <div className="status-selected" onClick={() => setShowStatusDropdown(prev => !prev)}>
                                {meetingData.teamStatus === "OK" && "Всё ок"}
                                {meetingData.teamStatus === "WITH_ISSUES" && "Есть проблемы"}
                                {meetingData.teamStatus === "MANY_ISSUES" && "Есть большие проблемы"}
                                {meetingData.teamStatus === "Не указано" && "Не указано"}
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
                            {meetingData.teamStatus === "Не указано" && "Не указано"}
                        </div>
                    )}
                </div>

                <div className="unique-meeting-info-row">
    <span className="unique-label">Скриншот встречи:</span>
    {isEditing ? (
        <div 
  className="unique-image-upload" 
  onClick={() => fileInputRef.current.click()}
  onKeyDown={(e) => {
    if (e.key === 'Enter' || e.key === ' ') {
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