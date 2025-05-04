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
        <div className="teamcard2-container">
    <div className="teamcard2-card">
        <button className="teamcard2-edit-button">Редактировать</button>
        <div className="teamcard2-header">
            <div className="teamcard2-tracker-field">
                <label className="teamcard2-tracker-label">Трекер:</label>
                <input className="teamcard2-tracker-input" value={trackerName} readOnly />
            </div>
            <div className="teamcard2-teamname-field">
                <label className="teamcard2-teamname-label">Название команды:</label>
                <input className="teamcard2-teamname-input" value={teamName} readOnly />
            </div>
        </div>
        <div className="teamcard2-meetings-grid">
            {meetings.map((m) => (
                <div key={m.number} className={`teamcard2-meeting-circle ${m.completed ? "completed" : ""}`}>
                    <span className="teamcard2-meeting-number">{m.number}</span>
                    <span className="teamcard2-meeting-date">{m.date}</span>
                    {m.completed && <span className="teamcard2-checkmark">✓</span>}
                </div>
            ))}
        </div>
        <div className="teamcard2-footer">
            <button className="teamcard2-stream-button">Название потока</button>
            <button className="teamcard2-deactivate-button">Деактивировать</button>
        </div>
    </div>
</div>

    );
};
export default MeetingCard2;