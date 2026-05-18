import React, { useState, useEffect, useRef } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import "./meeting-card.css";
import { getCsrfConfigForFetch } from "../../utils/csrf-utils";
import { validateMeetingWeekLimit } from "../../utils/date-utils";
import CustomDateTimePicker from './CustomDateTimePicker';
import { adminRoleName, superadminRoleName } from "../../services/constants";
const backendHost = (process.env.REACT_APP_BACKEND_URI || 'http://localhost:8080') + '/meeting';
const API_HOST = (process.env.REACT_APP_BACKEND_URI || 'http://localhost:8080') + '/backend';

const MeetingCreate = ({ onClose, teamId, userRole }) => {
    const navigate = useNavigate();
    const location = useLocation();
    const query = new URLSearchParams(location.search);
    const userId = query.get("userId");
    const [error, setError] = useState(null);
    const [meetingData, setMeetingData] = useState({
        number: "1",
        startDate: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
    });
    const [meetings, setMeetings] = useState([]);
    const [streamEndDate, setStreamEndDate] = useState(null);
    const [streamName, setStreamName] = useState(null);
    const [isStreamActive, setIsStreamActive] = useState(true);
    const [isLoading, setIsLoading] = useState(true);
    const popupRef = useRef(null);

    useEffect(() => {
        const fetchTeamData = async () => {
            try {
                const teamUrl = new URL(`${API_HOST}/api/v1/${userRole === adminRoleName || userRole === superadminRoleName ? "admin/" : ""}team-cards`);
                teamUrl.searchParams.append('page', 0);
                teamUrl.searchParams.append('size', 1000);
                
                const response = await fetch(teamUrl, {
                    method: 'POST',
                    headers: { 
                        "Content-Type": "application/json",
                        "Accept": "application/json",
                        ...getCsrfConfigForFetch()
                    },
                    credentials: 'include',
                    body: JSON.stringify({ filters: [] })
                });

                if (!response.ok) {
                    throw new Error(`Ошибка загрузки данных команды: ${response.status}`);
                }

                const data = await response.json();
                
                if (data.content && data.content.length > 0) {
                    const teamCard = data.content.find(card => card.id === teamId);
                    
                    if (teamCard) {
                        // streams - это МАССИВ, берем первый элемент
                        if (teamCard.streams && teamCard.streams.length > 0) {
                            const stream = teamCard.streams[0];
                            
                            if (stream.endDate) {
                                setStreamEndDate(stream.endDate);
                                setStreamName(stream.name || 'Неизвестный поток');
                                setIsStreamActive(stream.active === true);
                                
                                // Проверяем, завершен ли поток
                                const today = new Date();
                                const endDate = new Date(stream.endDate);
                                endDate.setHours(23, 59, 59, 999);
                                
                                if (today > endDate) {
                                    console.log('Поток завершен по дате');
                                }
                            }
                        } else {
                            console.warn('У карточки нет данных о потоке');
                        }
                    }
                }

                await fetchMeetings();
            } catch (err) {
                console.error("Ошибка при загрузке данных команды:", err);
                setError(`Не удалось загрузить данные команды: ${err.message}`);
            } finally {
                setIsLoading(false);
            }
        };

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

                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || 'Ошибка загрузки встреч');
                }

                const data = await response.json();
                const meetingsList = data.content || [];
                setMeetings(meetingsList);

                let maxNumber = 0;
                meetingsList.forEach(meeting => {
                    const num = parseInt(meeting.number);
                    if (!isNaN(num) && num > maxNumber) {
                        maxNumber = num;
                    }
                });

                setMeetingData(prev => ({
                    ...prev,
                    number: (maxNumber + 1).toString()
                }));
            } catch (err) {
                console.error("Ошибка при загрузке встреч:", err);
            }
        };

        fetchTeamData();
    }, [teamId, userRole]);

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (popupRef.current && !popupRef.current.contains(event.target)) {
                onClose();
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [onClose]);

    // const handleChange = (e) => {
    //     const { name, value } = e.target;
    //     setMeetingData(prev => ({ ...prev, [name]: value }));
    //     if (error) setError(null);
    // };

    const validateMeetingData = () => {
        const selectedDate = new Date(meetingData.startDate);
        
        // Проверка 1: Поток активен
        if (!isStreamActive) {
            throw new Error(`Поток "${streamName}" не активен. Создание встреч запрещено.`);
        }
        
        // Проверка 2: Поток еще не завершен по дате
        if (streamEndDate) {
            const endDate = new Date(streamEndDate);
            endDate.setHours(23, 59, 59, 999);
            
            if (selectedDate > endDate) {
                throw new Error(`Поток "${streamName}" завершен ${endDate.toLocaleDateString()}. Дата встречи не может быть позже даты окончания потока.`);
            }
            
            // Дополнительная проверка: если поток завершен (дата в прошлом)
            const today = new Date();
            if (today > endDate) {
                throw new Error(`Поток "${streamName}" завершен ${endDate.toLocaleDateString()}. Создание новых встреч в завершенном потоке запрещено.`);
            }
        }

        // Проверка 3: Ограничение на количество встреч в неделю
        const validation = validateMeetingWeekLimit(
            meetings,
            selectedDate,
            true
        );
        
        if (!validation.isValid) {
            throw new Error(validation.errorMessage);
        }
    };

    const handleCreate = async () => {
        try {
            validateMeetingData();

            const requestData = {
                number: meetingData.number,
                startDate: new Date(meetingData.startDate).toISOString(),
            };

            const response = await fetch(`${backendHost}/api/v1/create-meeting?teamCardId=${teamId}`, {
                method: "POST",
                headers: { 
                    "Content-Type": "application/json",
                    "Accept": "application/json", 
                    ...getCsrfConfigForFetch()
                },
                credentials: 'include',
                body: JSON.stringify(requestData),
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || "Ошибка при создании встречи");
            }

            const result = await response.json();
            onClose();
            navigate(`/meeting/${result.id}?teamId=${teamId}&username=${userId}`);
        } catch (error) {
            console.error("Ошибка при создании:", error);
            setError(error.message || "Произошла ошибка при создании встречи");
        }
    };

    const getMaxDate = () => {
        if (!streamEndDate) return null;
        
        const endDate = new Date(streamEndDate);
        endDate.setHours(23, 59, 59, 999);
        return endDate.toISOString().slice(0, 16);
    };

    const getMinDate = () => {
        const now = new Date();
        now.setHours(now.getHours() + 1);
        return now.toISOString().slice(0, 16);
    };

    // Проверяем, завершен ли поток
    const isStreamFinished = () => {
        if (!streamEndDate) return false;
        
        const endDate = new Date(streamEndDate);
        endDate.setHours(23, 59, 59, 999);
        const today = new Date();
        
        return today > endDate;
    };

    if (isLoading) {
        return (
            <div className="meeting-create-popup" ref={popupRef}>
                <div className="popup-content">
                    <p>Загрузка данных команды...</p>
                </div>
            </div>
        );
    }

    const streamFinished = isStreamFinished();

    return (
        <div className="meeting-create-popup" ref={popupRef}>
            <button className="popup-close-button" onClick={onClose}>
                ×
            </button>
            <div className="popup-content">
                <h3>Запланировать встречу #{meetingData.number}</h3>
                
                {streamName && streamEndDate && (
                    <div className={`stream-info ${!isStreamActive || streamFinished ? 'error' : ''}`}>
                        <p className="stream-info-text">
                            Поток: <strong>{streamName}</strong><br />
                            Дата окончания: <strong>{new Date(streamEndDate).toLocaleDateString()}</strong><br />
                            Статус: <strong>{isStreamActive ? 'Активен' : 'Неактивен'}</strong>
                        </p>
                        {(!isStreamActive || streamFinished) && (
                            <p className="stream-warning-text">
                                ⚠️ {!isStreamActive ? 'Поток не активен' : 'Поток завершен'}. Создание встреч невозможно.
                            </p>
                        )}
                    </div>
                )}
                
                {error && (
                    <div className="error-message">
                        {error}
                        <button className="error-close" onClick={() => setError(null)}>×</button>
                    </div>
                )}
                
                <div className="date-selection">
                    <span className="date-label">Дата и время:</span>
                    <CustomDateTimePicker
                        value={meetingData.startDate.slice(0, 16)}
                        onChange={(newValue) => {
                            setMeetingData(prev => ({ 
                                ...prev, 
                                startDate: newValue 
                            }));
                            if (error) setError(null);
                        }}
                        min={getMinDate()}
                        max={getMaxDate()}
                        disabled={!isStreamActive || streamFinished || !streamEndDate}
                    />
                </div>
                
                <button 
                    onClick={handleCreate} 
                    className="create-button"
                    disabled={!isStreamActive || streamFinished || !streamEndDate}
                >
                    {(!isStreamActive || streamFinished) ? 
                        "Создание запрещено" : 
                        streamEndDate ? "Создать" : "Дождитесь загрузки данных"}
                </button>
            </div>
        </div>
    );
};

export default MeetingCreate;
