import React, { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import "./team-meeting-card.css";

const MeetingCard2 = () => {
  // ✅ ПРАВИЛЬНО получаем teamId из URL
  const params = useParams();
  console.log("Все параметры URL:", params);
  const { teamId } = params;

  const [loading, setLoading] = useState(true);
  const [teamData, setTeamData] = useState(null);
  const [meetings, setMeetings] = useState([]);

  useEffect(() => {
    if (!teamId || teamId === "undefined") {
      console.error("teamId не получен из URL");
      setLoading(false);
      return;
    }

    const fetchTeamData = async () => {
      try {
        setLoading(true);

        // 1. Получаем информацию о команде
        const teamResponse = await fetch(`/meeting/api/v1/team-cards/${teamId}`);
        const team = await teamResponse.json();
        setTeamData(team);

        // 2. Получаем список встреч команды
        const meetingsResponse = await fetch(`/meeting/api/v1/meetings?teamCardId=${teamId}&page=0&size=100`);
        const meetingsData = await meetingsResponse.json();
        setMeetings(meetingsData.content || []);

      } catch (error) {
        console.error("Ошибка загрузки:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchTeamData();
  }, [teamId]);

  if (loading) {
    return <div className="teamcard2-container">Загрузка...</div>;
  }

  if (!teamData) {
    return <div className="teamcard2-container">Команда не найдена. ID: {teamId}</div>;
  }

  // Форматируем встречи
  const formattedMeetings = meetings.map((meeting, index) => ({
    number: index + 1,
    date: new Date(meeting.startDate).toLocaleDateString("ru-RU", { day: "2-digit", month: "2-digit" }),
    completed: meeting.status === "COMPLETED"
  }));

  return (
    <div className="teamcard2-container">
      <div className="teamcard2-card">
        <button className="teamcard2-edit-button">Редактировать</button>
        <div className="teamcard2-header">
          <div className="teamcard2-tracker-field">
            <label className="teamcard2-tracker-label">Трекер:</label>
            <input
              className="teamcard2-tracker-input"
              value={teamData.trackerFullName || teamData.username || "—"}
              readOnly
            />
          </div>
          <div className="teamcard2-teamname-field">
            <label className="teamcard2-teamname-label">Название команды:</label>
            <input
              className="teamcard2-teamname-input"
              value={teamData.teamCardName || teamData.name || "—"}
              readOnly
            />
          </div>
        </div>

        <div className="teamcard2-meetings-grid">
          {formattedMeetings.map((m) => (
            <div
              key={m.number}
              className={`teamcard2-meeting-circle ${m.completed ? "completed" : ""}`}
            >
              <span className="teamcard2-meeting-number">{m.number}</span>
              <span className="teamcard2-meeting-date">{m.date}</span>
              {m.completed && <span className="teamcard2-checkmark">✓</span>}
            </div>
          ))}
        </div>

        <div className="teamcard2-footer">
          <button className="teamcard2-stream-button">
            {teamData.streamName || "Поток"}
          </button>
          <button className="teamcard2-deactivate-button">Деактивировать</button>
        </div>
      </div>
    </div>
  );
};

export default MeetingCard2;