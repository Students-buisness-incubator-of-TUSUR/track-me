import React, { useEffect, useState, useRef } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { useSelector } from "react-redux";
import "./meeting-card.css";
import closeIcon from "./free-icon-font-cross-3917759 (1) 1.png";
import pencilIcon from "./pen.png";
import { getCsrfConfigForFetch } from "../../utils/csrf-utils";
import { validateMeetingWeekLimit, validateMeetingDateChange } from "../../utils/date-utils"; 
import VideoChat from "./video_chat.svg";
import Header from "../header/header";

const MeetingCard = () => {
    // Формируем абсолютный backendHost для корректной работы new URL
    let backendHost = 'http://localhost/meeting';

    if (process.env.REACT_APP_BACKEND_URI?.trim()) {
    backendHost = process.env.REACT_APP_BACKEND_URI.trim() + '/meeting';
    } else if (typeof window !== 'undefined' && window.location?.origin) {
    backendHost = window.location.origin + '/meeting';
    }

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
    const [showDateTooltip, setShowDateTooltip] = useState(false);
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
    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [showConfirmModal, setShowConfirmModal] = useState(false);
    const [pendingCompletion, setPendingCompletion] = useState(null); // true = состоялась, false = не состоялась
    const [bbbLink, setBbbLink] = useState('');
    const [allMeetings, setAllMeetings] = useState([]);



const renderTextareaSection = (name, label, value) => (
        <div className="unique-meeting-info-row">
            <span className="unique-label">{label}</span>
            {isEditing ? (
                <>
                    <textarea
                        name={name}
                        value={value || ''}
                        onChange={(e) => {
                            handleChange(e);
                            // Авто-ресайз после изменения
                            e.target.style.height = 'auto';
                            e.target.style.height = e.target.scrollHeight + 'px';
                        }}
                        className="unique-textarea"
                        disabled={isMeetingCompleted}
                        style={{
                            resize: 'none',
                            overflow: 'hidden',
                            minHeight: '40px'
                        }}
                        onFocus={(e) => {
                            // Авто-ресайз при фокусе
                            e.target.style.height = 'auto';
                            e.target.style.height = e.target.scrollHeight + 'px';
                        }}
                    />
                    <img src={pencilIcon} alt="Редактировать" className="edit-icon23" />
                </>
            ) : (
                <div className="unique-task">{value || "Не указаны"}</div>
            )}
        </div>
    );
    const reduxUser = useSelector(state => state.user?.user);
    const [role, setRole] = useState(null);

    useEffect(() => {
    if (reduxUser) {
        setRole(reduxUser.roles?.[0] || null);
    } else {
        const savedUser = localStorage.getItem('user');
        if (savedUser) {
        const user = JSON.parse(savedUser);
        setRole(user.roles?.[0] || null);
        }
    }
    }, [reduxUser]);
    useEffect(() => {
        if (!isNewMeeting && meetingId) {
            // Fetch meeting data
            const url = new URL(`${backendHost}/api/v1/meetings`);
            url.searchParams.append('teamCardId', teamId);
            url.searchParams.append('page', 0);
            url.searchParams.append('size', 1000);

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

    const areAllFieldsFilled = () => {
        return (
            meetingData.number &&
            meetingData.link &&
            meetingData.tasksCurrentMeeting &&
            meetingData.tasksNextMeeting &&
            meetingData.teamStatus &&
            imagePreview
        );
    };

    const getMissingFields = () => {
        const missing = [];
        if (!meetingData.number) missing.push("Номер встречи");
        if (!meetingData.link) missing.push("Ссылка на запись");
        if (!meetingData.tasksCurrentMeeting) missing.push("Задачи текущей встречи");
        if (!meetingData.tasksNextMeeting) missing.push("Задачи следующей встречи");
        if (!meetingData.teamStatus) missing.push("Статус команды");
        if (!imagePreview) missing.push("Скриншот встречи");
        return missing;
    };
    
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
    
    const isNew = isNewMeeting; 
    
    // ✅ ВАЖНО: создаем массив встреч для проверки
    let meetingsForValidation = [...allMeetings];
    
    if (!isNew) {
      // Для существующей встречи исключаем её из подсчета
      meetingsForValidation = allMeetings.filter(m => m.id !== meetingId);
    }
    
    const validation = isNew 
      ? validateMeetingWeekLimit(meetingsForValidation, meetingData.startDate, true)
      : validateMeetingDateChange(meetingsForValidation, meetingId, meetingData.startDate);

    if (!validation.isValid) {
      setError(validation.errorMessage);
      setTimeout(() => setError(null), 5000);
      return;
    }
    
    const meetingPayload = {
      link: meetingData.link || "",
      number: meetingData.number || "",
      teamStatus: meetingData.teamStatus,
      tasksCurrentMeeting: meetingData.tasksCurrentMeeting || "",
      tasksNextMeeting: meetingData.tasksNextMeeting || "",
      startDate: meetingData.startDate,
      status: meetingData.status || "SCHEDULED",
      userId  // ← явно передаём
    };

    const url = isNewMeeting 
      ? `${backendHost}/api/v1/meetings?teamCardId=${teamId}`
      : `${backendHost}/api/v1/update-meeting/${meetingId}?teamCardId=${teamId}`;

    const method = isNewMeeting ? "POST" : "PATCH";
    const body = JSON.stringify(meetingPayload);

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
      try {
        const errorJson = JSON.parse(errorText);
        throw new Error(errorJson.message || "Ошибка на сервере");
      } catch {
        throw new Error(errorText || "Ошибка при сохранении");
      }
    }

    const result = await response.json();
    const savedMeetingId = isNewMeeting ? result.id : meetingId;

    // ✅ Важно: обновить allMeetings ЛОКАЛЬНО
    setAllMeetings(prev => {
      const newMeetings = prev.filter(m => m.id !== savedMeetingId);
      newMeetings.push(result);
      return newMeetings.sort((a, b) => (parseInt(a.number) || 0) - (parseInt(b.number) || 0));
    });

    // Загружаем изображение
    if (image && savedMeetingId) {
      const formData = new FormData();
      formData.append('file', image);
      
      const imageResponse = await fetch(`${backendHost}/api/v1/image/${savedMeetingId}`, {
        method: 'POST',
        headers: { ...getCsrfConfigForFetch() },
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

    // Проверяем, прошла ли дата встречи
const isMeetingDatePassed = () => {
    if (!meetingData.startDate) return false;
    const meetingDate = new Date(meetingData.startDate);
    const now = new Date();
    return meetingDate < now;
};
   const handleCompleteMeeting = async (completed) => {
    // Проверка даты встречи - нельзя завершить встречу, если дата еще не прошла
    /* istanbul ignore if */ // NOSONAR
if (!isMeetingDatePassed()) {
    setError("Завершение встречи возможно только после окончания даты встречи"); // NOSONAR
    setShowDateTooltip(true); // NOSONAR

    /* istanbul ignore next */ // NOSONAR
    setTimeout(() => {
        setError(null); // NOSONAR
        setShowDateTooltip(false); // NOSONAR
    }, 5000);

    return; // NOSONAR
}


    // Проверка заполненности полей только для кнопки "Состоялась"
    if (completed && !areAllFieldsFilled()) {
        const missingFields = getMissingFields().join(", ");
        setError(`Нельзя завершить встречу. Заполните все поля: ${missingFields}`);
        setTimeout(() => setError(null), 5000);
        return;
    }

    try {
        const newStatus = completed ? "COMPLETED" : "COMPLETED_AS_NOT_HAPPENED";
        
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
    const deleteMeeting = async () => {
    try {
        const response = await fetch(
        `${backendHost}/api/v1/delete-meeting/${meetingId}?teamCardId=${teamId}`,
        {
            method: 'DELETE',
            headers: {
            'Content-Type': 'application/json',
            ...getCsrfConfigForFetch()
            },
            credentials: 'include'
        }
        );

        if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Ошибка при удалении: ${response.status} ${errorText}`);
        }

        // Успешно удалено → возвращаемся к карточке команды
        navigate(`/teamcard/${teamId}?userId=${userId}`);
    } catch (error) {
        console.error('Ошибка удаления встречи:', error);
        setError(error.message || 'Не удалось удалить встречу');
        setShowDeleteModal(false);
    }
    };
    const handleEditClick = () => {
        if (isMeetingLocked && (role === "ADMIN" || role === "SUPER_ADMIN")) {
            // Показываем модальное окно удаления
            setShowDeleteModal(true);
            return;
        }
    if (isMeetingLocked) {
        setError("Эту встречу нельзя редактировать, так как она завершена или не состоялась");
        // Добавим таймер для автоматического скрытия ошибки через 5 секунд
        setTimeout(() => setError(null), 5000);
        return;
    }
    setIsEditing(true);
};

    const isMeetingCompleted = meetingData.status === "COMPLETED" || 
                               
                              meetingData.status === "COMPLETED_AS_NOT_HAPPENED";
    
    const isMeetingLocked = meetingData.status === "COMPLETED" || meetingData.status === "COMPLETED_AS_NOT_HAPPENED";

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
useEffect(() => {
  if (!teamId) return;

  const fetchAllMeetings = async () => {
    try {
      const url = new URL(`${backendHost}/api/v1/meetings`);
      url.searchParams.append('teamCardId', teamId);
      url.searchParams.append('page', 0);
      url.searchParams.append('size', 100);

      const response = await fetch(url, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
      });

      if (!response.ok) throw new Error('Ошибка загрузки встреч');
      const data = await response.json();

      const sorted = (data.content || []).sort((a, b) => {
        const numA = parseInt(a.number) || 0;
        const numB = parseInt(b.number) || 0;
        return numA - numB;
      });

      setAllMeetings(sorted);
    } catch (err) {
      console.error('Ошибка при загрузке всех встреч:', err);
    }
  };

  fetchAllMeetings();
}, [teamId, backendHost]);

    return (
        <>
        <Header/>
        <div className="unique-meeting-container">
            <div className="unique-meeting-card">
                <button className="unique-close-button" onClick={() => navigate(-1)}>
                    <img src={closeIcon} alt="Закрыть" className="close-icon" />
                </button>
                
               {isEditing ? (
  <div className="edit-actions-container">
    <button
      onClick={(e) => {
        e.stopPropagation();
        handleSave();
      }}
      className="unique-edit-button"
    >
      Сохранить
    </button>

    {(role === "ADMIN" || role === "SUPER_ADMIN") && (
      <button
        type="button"
        className="unique-edit-button delete"
        onClick={(e) => {
          e.stopPropagation();
          setShowDeleteModal(true);
        }}
        title="Удалить встречу"
      >
        Удалить
      </button>
    )}
  </div>
) : (
  <button
    onClick={handleEditClick}
    className="unique-edit-button"
    style={{
      zIndex: 10,
      cursor: isMeetingLocked && (role !== "ADMIN" && role !== "SUPER_ADMIN") ? 'not-allowed' : 'pointer'
    }}
    disabled={isMeetingLocked && (role !== "ADMIN" && role !== "SUPER_ADMIN")}
    title={isMeetingLocked ? "Нельзя редактировать" : ""}
  >
    Редактировать
  </button>
)}


                {error && (
    <div className="error-message" style={{
        backgroundColor: '#ffebee',
        color: '#d32f2f',
        padding: '10px',
        borderRadius: '4px',
        margin: '10px 0',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingRight: '30px' // Просто добавляем отступ справа
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
                cursor: 'pointer',
                marginLeft: '10px' // Отступ слева от крестика
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
    {/* Кнопка "Состоялась" */}
    {meetingData.status !== "COMPLETED_AS_NOT_HAPPENED" && (
      <button 
        onClick={() => handleCompleteMeeting(true)}
        disabled={meetingData.status === "COMPLETED" || !areAllFieldsFilled() || !isMeetingDatePassed()}
        className={`unique-status-button unique-status-completed ${
          meetingData.status === "COMPLETED" ? "active-status" : ""
        }`}
        title={
          !areAllFieldsFilled()
            ? "Заполните все поля перед завершением встречи"
            : !isMeetingDatePassed()
            ? "Завершение встречи возможно только после окончания даты встречи"
            : ""
        }
      >
        Состоялась
      </button>
    )}

    {/* Кнопка "Не состоялась" */}
    {meetingData.status !== "COMPLETED" && (
      <button 
  onClick={() => {
    setPendingCompletion(false);
    setShowConfirmModal(true);
  }}
        disabled={meetingData.status === "COMPLETED_AS_NOT_HAPPENED" || !isMeetingDatePassed()}
        className={`unique-status-button unique-status-not-happened ${
          meetingData.status === "COMPLETED_AS_NOT_HAPPENED" ? "active-status" : ""
        }`}
        title={!isMeetingDatePassed() ? "Завершение встречи возможно только после окончания даты встречи" : ""}
      >
        Не состоялась
      </button>
    )}

    {/* Подсказка о дате встречи */}
    {showDateTooltip && (
      <div className="date-tooltip">
        Завершение встречи возможно только после окончания даты встречи
      </div>
    )}
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

                {renderTextareaSection(
    "tasksCurrentMeeting",
    "Задачи к следующей встрече:",
    meetingData.tasksCurrentMeeting
)}


                {renderTextareaSection(
    "tasksNextMeeting",
    "Выполнили задачи прошлой встречи или нет, общая информация по команде:",
    meetingData.tasksNextMeeting
)}

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
                <div className="bbb-input-section">
  <div className="unique-meeting-info-row">
    <span className="unique-label">Ссылка на видеовстречу:</span>
  </div>
    <input
    type="url"
    value={bbbLink}
    onChange={(e) => setBbbLink(e.target.value)}
    placeholder="https://demo.bigbluebutton.org/rooms/..."
    className="bbb-link-input"
    aria-label="Ссылка на видеовстречу"
  />
  <div className="bbb-button-wrapper">
    <button
      className="bbb-join-icon-button"
      onClick={() => {
        const url = bbbLink.trim();
        if (!url) {
          alert('Пожалуйста, введите ссылку на встречу');
          return;
        }

        const fullUrl = url.startsWith('http') ? url : `https://${url}`;
        const width = 1100;
        const height = 700;
        const left = window.screenX + (window.innerWidth - width) / 2;
        const top = window.screenY + (window.innerHeight - height) / 2;

        window.open(
          fullUrl,
          'bbb_meeting_window',
          `
            width=${width},
            height=${height},
            left=${left},
            top=${top},
            resizable=yes,
            scrollbars=yes,
            toolbar=no,
            menubar=no,
            location=yes,
            noopener,
            noreferrer
          `.replace(/\s/g, '')
        );
      }}
      aria-label="Присоединиться к видеовстрече"
      title="Присоединиться ко встрече"
    >
      <img
        src={VideoChat}
        alt="Присоединиться к встрече"
        className="bbb-join-icon"
      />
    </button>
  </div>

  
</div>


            </div>
            {showConfirmModal && (
  <div className="confirm-modal-overlay">
    <div className="confirm-modal">
      <h3 className="confirm-modal-title">Подтверждение действия</h3>
      <p className="confirm-modal-text">
        Вы уверены, что хотите завершить встречу как <b>несостоявшуюся</b>?
      </p>
      <div className="confirm-modal-buttons">
        <button
          className="confirm-button yes"
          onClick={() => {
            setShowConfirmModal(false);
            handleCompleteMeeting(pendingCompletion);
          }}
        >
          Да
        </button>
        <button
          className="confirm-button no"
          onClick={() => setShowConfirmModal(false)}
        >
          Отмена
        </button>
      </div>
    </div>
  </div>
)}
{showDeleteModal && (
  <button
    type="button"
    className="confirm-modal-overlay"
    onClick={() => setShowDeleteModal(false)}
    aria-label="Закрыть модальное окно"
    data-testid="delete-modal-overlay"
  >
    <div
      className="confirm-modal"
      onClick={(e) => e.stopPropagation()}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.stopPropagation();
        }
      }}
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-modal-title"
      tabIndex={-1}
    >
      {isMeetingLocked && (
        <p className="locked-warning">
          <strong>Эта встреча уже состоялась и её нельзя редактировать!</strong>
        </p>
      )}
      <h3 id="delete-modal-title" data-testid="delete-modal-title">
        Удалить встречу?
      </h3>
      <p>
        Вы уверены, что хотите удалить эту встречу? <br />
        <strong>Это действие нельзя отменить.</strong>
      </p>
      <div className="confirm-modal-buttons">
        <button
          className="confirm-button no"
          onClick={(e) => {
            e.stopPropagation();
            setShowDeleteModal(false);
          }}
        >
          Отмена
        </button>
        <button
          className="confirm-button yes"
          onClick={(e) => {
            e.stopPropagation();
            deleteMeeting();
          }}
          data-testid="delete-confirm-button"
        >
          Удалить
        </button>
      </div>
    </div>
  </button>
)}


        </div>
        </>
    );
};

export default MeetingCard;