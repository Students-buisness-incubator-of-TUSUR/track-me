//import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
//import { useParams } from "react-router-dom";
import "./team-card.css";

const TeamCard = () => {
  const navigate = useNavigate();
  // const [teamData, setTeamData] = useState({});
  // const [meetings, setMeetings] = useState([]);
  // const [error, setError] = useState(null);
  // const { id, userId } = useParams();
  // //const [loading, setLoading] = useState(false);
  // const backendHost = process.env.REACT_APP_BACKEND_HOST || 'http://localhost:8080';

  // //Флаг редактирования
  // const [isEditing, setIsEditing] = useState(false);

  // // Состояние для редактируемых данных
  // const [editedData, setEditedData] = useState({});

  // useEffect(() => {
  //   const token = localStorage.getItem("token");

  //   fetch(`${backendHost}/api/v1/admin/team-card?id=${id}&userId=${userId}`, {
  //       method: "GET",
  //       headers: {
  //         "Content-Type": "application/json",
  //         Authorization: `Bearer ${token}`,
  //       },
  //   })
  //       .then((response) => {
  //           if (!response.ok) {
  //               setError("Ошибка получения данных")
  //           }
  //       })
  //       .then((result) => {
  //           setTeamData(result);
  //           setEditedData(result);
  //           setMeetings(result.meetings || []);
  //           //setLoading(false);
  //       })
  //       .catch((err) => {
  //           console.error("Ошибка загрузки данных:", err);
  //           //setLoading(false);
  //       });
  // }, [backendHost])

  // const handleChange = (e) => {
  //   setEditedData({
  //     ...editedData,
  //     [e.target.name]: e.target.value,
  //   });
  // };

  // const handleSave = () => {

  //   const token = localStorage.getItem("accessToken");

  //   fetch(`${backendHost}/api/team-cards/update`, {
  //     method: "PATCH",
  //     headers: {
  //       "Content-Type": "application/json",
  //       Authorization: `Bearer ${token}`,
  //     },
  //     body: JSON.stringify(editedData),
  //   })
  //     .then((response) => {
  //       if (!response.ok) throw new Error("Ошибка сохранения");
  //       return response.json();
  //     })
  //     .then((result) => {
  //       setTeamData(result);
  //       setIsEditing(false);
  //     })
  //     .catch((err) => {
  //       console.error("Ошибка сохранения данных:", err);
  //     });
  // };

  // if (loading) {
  //   return <div>Загрузка...</div>;
  // }

  return (
    <div className="team-card-container">
      <div className="team-card">
        <h2 className="team-title">Карточка команды</h2>
        <div className="team-info">
          <span className="team-label">Трекер:</span>
          {/* {isEditing ? (
              <input
                  type="text"
                  className="team-input"
                  name="trackerName"
                  value={editedData.userId || ""}
                  onChange={handleChange}
              />
          ) : (
              <div className="team-input">{teamData.userId}</div>
          )} */}
        </div>

        <div className="team-info">
          <span className="team-label">Название команды:</span>
          {/* {isEditing ? (
            <input
              type="text"
              className="team-input"
              name="teamName"
              value={editedData.name || ""}
              onChange={handleChange}
            />
          ) : (
            <div className="team-input">{teamData.name}</div>
          )} */}
        </div>

        <h3 className="meeting-title">Встречи:</h3>
        <div className="meetings-grid">
          {[...Array(12)].map((_, index) => (
            <div
              className="meeting-item"
              key={index}
              onClick={() => navigate(`/meeting/${index + 1}`)}
              style={{ cursor: "pointer" }}
            >
              <span className="meeting-number">{index + 1}</span>
              <span className="meeting-date">{index === 2 ? "01.05" : "30.04"}</span>
            </div>
          ))}
        </div>

        <div className="pagination">
          <span className="dot active"></span>
          <span className="dot"></span>
          <span className="dot"></span>
        </div>

        <div className="button-group">
          <button className="purple-button">Название потока</button>
          <button className="red-button">Деактивировать</button>
          {/* {!isEditing ? (
            <button className="edit-button" onClick={() => setIsEditing(true)}>
              Редактировать
            </button>
          ) : (
            <button className="edit-button" onClick={handleSave}>
              Сохранить
            </button>
          )} */}
        </div>
      </div>
    </div>
  );
};

export default TeamCard;