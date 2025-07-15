import React, { useState, useEffect, useRef } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import "./meeting-card.css";

const backendHost = (process.env.REACT_APP_BACKEND_URI || 'http://localhost:8080') + '/backend';

const MeetingCreate = ({ onClose, teamId }) => {
    const navigate = useNavigate();
    const location = useLocation();
    const query = new URLSearchParams(location.search);
    const userId = query.get("userId");
    const [error, setError] = useState(null);
    const [meetingData, setMeetingData] = useState({
        number: "1",
        startDate: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
    });
    const popupRef = useRef(null);

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

                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || 'Ошибка загрузки встреч');
                }

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

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (popupRef.current && !popupRef.current.contains(event.target)) {
                onClose();
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [onClose]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setMeetingData(prev => ({ ...prev, [name]: value }));
        if (error) setError(null);
    };

    const validateMeetingData = () => {
        if (!meetingData.number || isNaN(parseInt(meetingData.number))) {
            throw new Error("Номер встречи должен быть числом");
        }

        const selectedDate = new Date(meetingData.startDate);
        const currentDate = new Date();
        
        if (selectedDate <= currentDate) {
            throw new Error("Дата встречи должна быть в будущем");
        }

        return true;
    };

    const handleCreate = async () => {
        try {
            validateMeetingData();

            const requestData = {
                number: meetingData.number,
                startDate: new Date(meetingData.startDate).toISOString(),
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

    return (
        <div className="meeting-create-popup" ref={popupRef}>
            <button className="popup-close-button" onClick={onClose}>
                ×
            </button>
            <div className="popup-content">
                <h3>Запланировать встречу #{meetingData.number}</h3>
                {error && (
                    <div className="error-message">
                        {error}
                        <button className="error-close" onClick={() => setError(null)}>×</button>
                    </div>
                )}
                <div className="date-selection">
                    <span className="date-label">Дата и время:</span>
                    <input
                        type="datetime-local"
                        name="startDate"
                        value={meetingData.startDate.slice(0, 16)}
                        onChange={handleChange}
                        className="date-input"
                        min={new Date().toISOString().slice(0, 16)}
                    />
                </div>
                <button onClick={handleCreate} className="create-button">
                    Создать
                </button>
            </div>
        </div>
    );
};

export default MeetingCreate;