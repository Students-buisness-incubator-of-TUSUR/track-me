import React, { useEffect, useState } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";
import { jwtDecode } from "jwt-decode";
import "./team-card.css";

const backendHost = process.env.REACT_APP_BACKEND_HOST || "http://localhost:8080";

const TeamCard = () => {
  const navigate = useNavigate();
  const { id } = useParams();
  const location = useLocation();
  const query = new URLSearchParams(location.search);
  const token = localStorage.getItem("accessToken");
  const decoded = token ? jwtDecode(token) : {};

  const userId = query.get("userId");
  const role = decoded.role;
  const currentUserName = decoded.fullName || "ФИО трекера";

  const [teamData, setTeamData] = useState({});
  const [editedData, setEditedData] = useState({});
  const [meetings, setMeetings] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [streams, setStreams] = useState([]);
  const [ntiMarkets, setNtiMarkets] = useState([]);
  const [trackers, setTrackers] = useState([]); // Initialize as empty array
  const forceEdit = query.get("edit") === "true";
  const [isEditing, setIsEditing] = useState(forceEdit);

  useEffect(() => {
    fetch(`${backendHost}/api/v1/meetings?teamCardId=${id}&page=${currentPage}&size=10`, {
      headers: {
        "Authorization": `Bearer ${token}`,
      }
    })
      .then(res => res.json())
      .then(data => {
        setMeetings(data.content || []);
        setTotalPages(data.totalPages || 1);
      })
      .catch(err => console.error("Ошибка загрузки встреч:", err));
  }, [id, currentPage, token]);

  useEffect(() => {
    const endpoint = (role === "ADMIN" || role === "SUPER_ADMIN")
      ? `${backendHost}/api/v1/admin/team-card?id=${id}&userId=${userId}`
      : `${backendHost}/api/v1/team-card?id=${id}`;

    fetch(endpoint, {
      headers: {
        "Authorization": `Bearer ${token}`,
      }
    })
      .then(res => res.json())
      .then(data => {
        setTeamData(data);
        setEditedData({
          ...data,
          streamId: data.streamId,
          ntiMarketId: data.ntiMarket?.id || "",
          readinessLevel: data.readinessLevel || ""
        });
      })
      .catch(err => console.error("Ошибка загрузки карточки:", err));
  }, [id, userId, role, token]);

  useEffect(() => {
    // Load trackers list only for admin/superadmin
    if (role === "ADMIN" || role === "SUPER_ADMIN") {
      fetch(`${backendHost}/api/v1/users`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          filters: [{ key: "role", value: "TRACKER" }],
          page: 0,
          size: 150,
          order: { field: "fullName", direction: "ASC" }
        }),
      })
        .then((res) => res.json())
        .then((data) => {
          setTrackers(data.content || []);
        })
        .catch((err) => {
          console.error("Ошибка загрузки трекеров:", err);
          setTrackers([]);
        });
    }
  }, [role, token]);

  useEffect(() => {
    // Load streams only for admin/superadmin
    if (role === "ADMIN" || role === "SUPER_ADMIN") {
      fetch(`${backendHost}/api/v1/streams?page=0&size=150`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ filters: [] }),
      })
        .then((res) => res.json())
        .then((data) => {
          const streamsWithNames = Array.isArray(data.content)
            ? data.content.map((s) => ({ id: s.id, name: s.name }))
            : [];
          setStreams(streamsWithNames);
        })
        .catch((err) => console.error("Ошибка загрузки потоков:", err));
    }
  }, [role, token]);

  useEffect(() => {
    // Загрузка рынков НТИ
    fetch(`${backendHost}/api/v1/streams/nti-markets`, {
      headers: {
        Authorization: `Bearer ${token}`,
      }
    })
      .then(res => res.json())
      .then(data => {
        setNtiMarkets(data);
      })
      .catch(err => console.error("Ошибка загрузки рынков НТИ:", err));
  }, [token]);

  const handleChange = (e) => {
    setEditedData({ ...editedData, [e.target.name]: e.target.value });
  };

  const handleSave = () => {
    const patchData = {
      ntiMarketId: editedData.ntiMarketId,
      readinessLevel: editedData.readinessLevel,
    };

    // Add additional fields only for admin/superadmin
    if (role === "ADMIN" || role === "SUPER_ADMIN") {
      patchData.name = editedData.name;
      patchData.description = editedData.description;
    }

    const baseEndpoint = (role === "ADMIN" || role === "SUPER_ADMIN")
      ? `${backendHost}/api/v1/admin/team-card`
      : `${backendHost}/api/v1/team-card`;
    
    const params = new URLSearchParams();
    params.append("teamCardId", id);

    // Add admin-only params
    if (role === "ADMIN" || role === "SUPER_ADMIN") {
      params.append("userId", editedData.userId || userId);
      if (editedData.streamId) {
        params.append("streamId", editedData.streamId);
      }
    }

    const endpoint = `${baseEndpoint}?${params.toString()}`;

    fetch(endpoint, {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(patchData),
    })
      .then((res) => {
        if (!res.ok) {
          throw new Error("Ошибка при сохранении: " + res.status);
        }
        return res.json();
      })
      .then((updated) => {
        setTeamData(updated);
        setIsEditing(false);
      })
      .catch((err) => console.error("Ошибка при сохранении:", err));
  };

  const handleAddMeeting = () => {
    navigate(`/meeting/new?teamId=${id}&userId=${userId}`);
  };

  const handlePageChange = (pageIndex) => {
    setCurrentPage(pageIndex);
  };

  return (
    <div className="team-card-widget-container">
      <button className="close-button-widget" onClick={() => navigate(-1)}>×</button>
      {!isEditing ? (
        <button
          className="edit-button-widget"
          onClick={() => setIsEditing(true)}
        >
          Редактировать
        </button>
      ) : (
        <button
          className="edit-button-widget"
          onClick={handleSave}
        >
          Сохранить
        </button>
      )}

      <div className="team-card-content">
        <div className="left-column">
          <div className="inputs-container">
            <div className="team-info-widget">
              <span className="team-label-widget">Трекер:</span>
              <div className="team-input-wrapper">
                {(role === "ADMIN" || role === "SUPER_ADMIN") && isEditing ? (
                  <select
                    className="team-input-widget"
                    name="userId"
                    value={editedData.userId || ""}
                    onChange={handleChange}
                  >
                    <option value="">Выберите трекера</option>
                    {Array.isArray(trackers) && trackers.map(tracker => (
                      <option key={tracker.id} value={tracker.id}>
                        {tracker.fullName}
                      </option>
                    ))}
                  </select>
                ) : (
                  <input
                    className="team-input-widget"
                    value={teamData.user?.fullName || ""}
                    readOnly
                    placeholder="ФИО трекера"
                  />
                )}
              </div>
            </div>

            <div className="team-info-widget">
              <span className="team-label-widget">Название команды:</span>
              <div className="team-input-wrapper">
                <input
                  className="team-input-widget"
                  name="name"
                  value={editedData.name || ""}
                  onChange={handleChange}
                  readOnly={!isEditing}
                  placeholder="Карточка команды"
                />
                {isEditing && (
                  <img src={require("./pen.png")} alt="edit" className="edit-icon" />
                )}
              </div>
            </div>

            <div className="meetings-section">
              <div className="meetings-left">
                <span className="team-label-widget">Встречи:</span>
                <div className="meetings-grid-widget">
                  {meetings.map((meeting, index) => (
                    <div 
                      key={meeting.id} 
                      className={`meeting-item-widget ${meeting.status === 'DONE' ? 'done' : 'planned'}`}
                      onClick={() => navigate(`/meeting/${meeting.id}?teamId=${id}&userId=${userId}`)}
                    >
                      <span className="meeting-number-widget">{meeting.number || index + 1}</span>
                      <span className="meeting-date-widget">
                        {new Date(meeting.startDate).toLocaleDateString('ru-RU', {
                          day: '2-digit',
                          month: '2-digit'
                        })}
                      </span>
                      {meeting.status === 'DONE' && <span className="check-mark-widget">✓</span>}
                    </div>
                  ))}
                  <div className="meeting-item-widget add" onClick={handleAddMeeting}>+</div>
                </div>

                {totalPages > 1 && (
                  <div className="pagination-widget">
                    {[...Array(totalPages)].map((_, index) => (
                      <span
                        key={index}
                        className={`dot-widget ${currentPage === index ? 'active' : ''}`}
                        onClick={() => handlePageChange(index)}
                      />
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="right-panel">
          <div className="dropdown-block">
            <div className="dropdown-toggle">
              Рынок НТИ
            </div>
            <div className="team-input-wrapper">
              <select
                className="team-input-widget"
                name="ntiMarketId"
                value={editedData.ntiMarketId || ""}
                onChange={handleChange}
                disabled={!isEditing}
              >
                <option value="">Выберите рынок НТИ</option>
                {ntiMarkets.map(market => (
                  <option key={market.id} value={market.id}>
                    {market.displayName}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="dropdown-block">
            <div className="dropdown-toggle">
              TRL
            </div>
            <div className="team-input-wrapper">
              <select
                className="team-input-widget"
                name="readinessLevel"
                value={editedData.readinessLevel || ""}
                onChange={handleChange}
                disabled={!isEditing}
              >
                <option value="">Выберите уровень</option>
                <option value="0-2">0-2</option>
                <option value="3-5">3-5</option>
                <option value="6-8">6-8</option>
                <option value="9-10">9-10</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      {/* Show stream selection only for admin/superadmin */}
      {(role === "ADMIN" || role === "SUPER_ADMIN") && isEditing ? (
        <select
          className="stream-select"
          name="streamId"
          value={editedData.streamId || ""}
          onChange={handleChange}
          style={{
            position: 'absolute',
            left: '40px',
            bottom: '40px',
            width: '300px'
          }}
        >
          <option value="" disabled>Выберите поток</option>
          {streams.map((s) => (
            <option key={s.id} value={s.id}>{s.name}</option>
          ))}
        </select>
      ) : (
        <button className="purple-button-widget" disabled>
          <span>{teamData.stream ? teamData.stream.name : 'Не привязан к потоку'}</span>
        </button>
      )}
      
      <div className="button-group-widget">
        <button className="red-button-widget">
          <span>Деактивировать</span>
        </button>
      </div>
    </div>
  );
};

export default TeamCard;
