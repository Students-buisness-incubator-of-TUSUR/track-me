import React from "react";
import "./team-meeting-card.css";

const MeetingCard2 = () => {
    // Данные можно получать из пропсов, контекста или из запроса к серверу
    const trackerName = "Иванов Иван";
    const teamName = "Команда 1";

    // Массив встреч (номер, дата, выполнена или нет)
    const meetings = [
        {number: 1, date: "25.04", completed: true},
        {number: 2, date: "27.04", completed: true},
        {number: 3, date: "01.05", completed: true},
        {number: 4, date: "03.05", completed: true},
        {number: 5, date: "04.05", completed: false},
        {number: 6, date: "05.06", completed: false},
        {number: 7, date: "30.04", completed: true},
        {number: 8, date: "30.04", completed: true},
        {number: 9, date: "30.04", completed: true},
        {number: 10, date: "30.04", completed: false},
        {number: 11, date: "30.04", completed: false},
        {number: 12, date: "30.04", completed: false},
    ];

    return (
        <div className="team-card-container">
            <div className="team-card">
                {/* Кнопка "Редактировать" в правом верхнем углу */}
                <button className="edit-button">Редактировать</button>

                {/* Блок с данными о трекере и названии команды */}
                <div className="team-header">
                    <div className="tracker-field">
                        <label className="tracker-label">Трекер:</label>
                        <input
                            className="tracker-input"
                            type="text"
                            value={trackerName}
                            readOnly
                        />
                    </div>
                    <div className="teamname-field">
                        <label className="teamname-label">Название команды:</label>
                        <input
                            className="teamname-input"
                            type="text"
                            value={teamName}
                            readOnly
                        />
                    </div>
                </div>

                {/* Сетка кружочков со встречами */}
                <div className="meetings-grid">
                    {meetings.map((meeting) => (
                        <div
                            key={meeting.number}
                            className={`meeting-circle ${
                                meeting.completed ? "completed" : ""
                            }`}
                        >
                            <span className="meeting-number">{meeting.number}</span>
                            <span className="meeting-date">{meeting.date}</span>
                            {meeting.completed && <span className="checkmark">✓</span>}
                        </div>
                    ))}
                </div>

                {/* Нижние кнопки */}
                <div className="team-footer">
                    <button className="stream-button">Название потока</button>
                    <button className="deactivate-button">Деактивировать</button>
                </div>
            </div>
        </div>
    );
};
export default MeetingCard2;
