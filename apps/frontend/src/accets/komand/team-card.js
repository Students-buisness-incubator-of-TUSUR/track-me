import React, {useEffect, useMemo, useState} from "react";
import {useLocation, useNavigate, useParams} from "react-router-dom";
import "./team-card.css";
import {useSelector} from "react-redux";
import penIcon from "./pen.png";

const backendHost = (process.env.REACT_APP_BACKEND_URI || "https://localhost:8080") + '/backend';
const backendHost1 = (process.env.REACT_APP_BACKEND_URI || "https://localhost:8080") + '/sso';
const TeamCard = () => {
    const navigate = useNavigate();
    const {id} = useParams();
    
    
   
    const location = useLocation();
    const streamId = location.state?.streamId;
    const passedUsername = location.state?.username;
    const query = new URLSearchParams(location.search);
    
    const [role, setRole] = useState(null);
const [username, setUsername] = useState(null);
const reduxUser = useSelector(state => state.user?.user);

const [allTeamCards, setAllTeamCards] = useState([]); // eslint-disable-line no-unused-vars
const [teamCardsCount, setTeamCardsCount] = useState(0);


    const [teamData, setTeamData] = useState({});
    const [editedData, setEditedData] = useState({});
    const [meetings, setMeetings] = useState([]);
    const [currentPage, setCurrentPage] = useState(0); // eslint-disable-line no-unused-vars
    const [totalPages, setTotalPages] = useState(1); // eslint-disable-line no-unused-vars
    const [streams, setStreams] = useState([]);
    const [ntiMarkets, setNtiMarkets] = useState([]);
    const [trackers, setTrackers] = useState([]);
    const forceEdit = query.get("edit") === "true";
    const [isEditing, setIsEditing] = useState(forceEdit);
    const [showTooltip, setShowTooltip] = useState(false);

    const [showNTI, setShowNTI] = useState(false);
    const [showTRL, setShowTRL] = useState(false);
    const [selectedMarket, setSelectedMarket] = useState(null);
    const [selectedTRL, setSelectedTRL] = useState(null);
    const [selectedStreamId, setSelectedStreamId] = useState(null);
const [showStreams, setShowStreams] = useState(false);


    const [isLoading, setIsLoading] = useState(false);
    const [apiError, setApiError] = useState(null); // eslint-disable-line no-unused-vars

    const trlLevels = useMemo(() => [
        {id: 1, label: "0-2"},
        {id: 2, label: "3-5"},
        {id: 3, label: "6-8"},
        {id: 4, label: "9-10"},
    ], []);
    const [trackerFullName, setTrackerFullName] = useState("");
    const [streamInfo, setStreamInfo] = useState(null);

// Добавляем эффект для загрузки данных о потоке
useEffect(() => {
    if (!streamId) return;

    const fetchStreamInfo = async () => {
    try {
        const response = await fetch(`${backendHost}/api/v1/streams?page=0&size=150`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'include',
            body: JSON.stringify({ filters: [] }), // без фильтров
        });

        if (!response.ok) throw new Error("Ошибка загрузки данных потоков");

        const result = await response.json();
        const found = result.content?.find(stream => stream.id === streamId);
        if (found) {
            setStreamInfo(found);
        } else {
            throw new Error("Поток с указанным ID не найден");
        }
    } catch (error) {
        handleApiError(error, "загрузке данных потока");
    }
};



    fetchStreamInfo();
}, [streamId]);

// Форматируем даты для отображения
const formatDates = (start, end) => {
    if (!start || !end) return '';
    
    const formatDate = (dateStr) => {
        const date = new Date(dateStr);
        return date.toLocaleDateString('ru-RU', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        }).replace(/\s/g, '');
    };
    
    return `${formatDate(start)} - ${formatDate(end)}`;
};
useEffect(() => {
  if (teamData.stream?.id) {
    setSelectedStreamId(teamData.stream.id);
  }
}, [teamData]);

useEffect(() => {
  if (!role) return;

  const fetchFullName = async () => {
    try {
      if (role === "ADMIN" || role === "SUPER_ADMIN") {
        const usernameToFetch = passedUsername || teamData.username;
        if (!usernameToFetch) return;

        const res = await fetch(`${backendHost1}/api/v1/users/${usernameToFetch}/info`, {
          credentials: "include"
        });
        if (!res.ok) throw new Error("Ошибка получения данных пользователя");
        const data = await res.json();
        setTrackerFullName(data.fullName);
      } else if (role === "TRACKER") {
        const res = await fetch(`${backendHost1}/api/v1/account/info`, {
          credentials: "include"
        });
        if (!res.ok) throw new Error("Ошибка получения данных текущего пользователя");
        const data = await res.json();
        setTrackerFullName(data.fullName);
      }
    } catch (err) {
      handleApiError(err, "загрузке ФИО трекера");
    }
  };

  fetchFullName();
}, [role, passedUsername, teamData.username]);



    useEffect(() => {
        const savedUser = localStorage.getItem('user');
        if (savedUser) {
            const parsed = JSON.parse(savedUser);
            setRole(parsed.roles?.[0] || null);
            setUsername(parsed.username || null);
        } else if (reduxUser) {
            localStorage.setItem('user', JSON.stringify(reduxUser));
            setRole(reduxUser.roles?.[0] || null);
            setUsername(reduxUser.username || null);
        }
    }, [reduxUser]);
    useEffect(() => {
  if (!streamId) return;

  const fetchTeamCardsCount = async () => {
    try {
      // Формируем URL с query-параметром streamId
      const url = new URL(`${backendHost}/api/v1/team-card/count`);
      url.searchParams.append("streamId", streamId);

      const response = await fetch(url.toString(), {
        credentials: "include"
      });

      if (!response.ok) {
        throw new Error(`Ошибка при получении количества карточек: ${response.status}`);
      }

      const data = await response.json();
      // Предполагается, что сервер возвращает число в поле count или просто число
      const count = typeof data === "number" ? data : data.count || 0;
      setTeamCardsCount(count);
    } catch (error) {
      handleApiError(error, "получении количества карточек команд");
    }
  };

  fetchTeamCardsCount();
}, [streamId]);


    useEffect(() => {
        const handleClickOutside = (event) => {
            if (!event.target.closest('.dropdown-block')) {
                setShowNTI(false);
                setShowTRL(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleApiError = (error, context) => {
        console.error(`Error in ${context}:`, error);
        setApiError(`Ошибка при ${context}: ${error.message}`);
    };

    useEffect(() => {
        const loadMeetings = async () => {
            try {
                const response = await fetch(`${backendHost}/api/v1/meetings?teamCardId=${id}&page=${currentPage}&size=1000`, {
                    credentials: 'include',
                });
                if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
                const data = await response.json();
                setMeetings(data.content || []);
                setTotalPages(data.totalPages || 1);
            } catch (error) {
                handleApiError(error, "загрузке встреч");
            }
        };
        loadMeetings();
    }, [id, currentPage]);
    

     useEffect(() => {
    if (!username || !role || !id) return;

    const endpoint = (role === "ADMIN" || role === "SUPER_ADMIN")
        ? `${backendHost}/api/v1/admin/team-cards?page=0&size=1000`
        : `${backendHost}/api/v1/team-cards?page=0&size=1000`;

    const payload = {
  filters: []
};
    fetch(endpoint, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        credentials: "include",
        body: JSON.stringify(payload)
    })
        .then(res => {
            if (!res.ok) throw new Error("Ошибка при получении карточек");
            return res.json();
        })
        .then(data => {
            console.log("Загруженные карточки:", data.content.map(card => card.id));
            const cards = data.content || [];
            setAllTeamCards(cards);
            const found = data.content?.find(card => String(card.id) === String(id));

            
            if (found) {
                setTeamData(found);
                setEditedData(found); // если нужно редактирование
            } else {
                setApiError("Карточка команды не найдена");
            }
        })
        .catch(err => handleApiError(err, "поиске карточки команды"));
}, [username, role, id]);



    useEffect(() => {
    if (role === "ADMIN" || role === "SUPER_ADMIN") {
        fetch(`${backendHost1}/api/v1/users/trackers`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            credentials: 'include',
            body: JSON.stringify({
                filters: [],
                page: 0,
                size: 150,
                order: { field: "fullName", direction: "ASC" }
            }),
        })
            .then(async (res) => {
                if (!res.ok) throw new Error(`Ошибка: ${res.status}`);
                return res.json();
            })
            .then((data) => {
                // ⚠️ фильтруем только enabled === true
                const activeTrackers = (data.content || []).filter(t => t.enabled === true);
                setTrackers(activeTrackers);
            })
            .catch((err) => {
                handleApiError(err, "загрузке трекеров");
                setTrackers([]);
            });
    }
}, [role]);


    useEffect(() => {
        if (role === "ADMIN" || role === "SUPER_ADMIN") {
            fetch(`${backendHost}/api/v1/streams?page=0&size=1500`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: 'include',
                body: JSON.stringify({filters: []}),
            })
                .then(async (res) => {
  if (!res.ok) throw new Error(`Ошибка: ${res.status}`);
  return res.json();
})

                .then((data) => {
                    const streamsWithNames = Array.isArray(data.content)
                        ? data.content.map((s) => ({id: s.id, name: s.name}))
                        : [];
                    setStreams(streamsWithNames);
                })
                .catch((err) => handleApiError(err, "загрузке потоков"));
        }
    }, [role]);

    useEffect(() => {
        fetch(`${backendHost}/api/v1/streams/nti-markets`, {
            credentials: 'include',
        })
            .then(res => res.json())
            .then(data => {
                setNtiMarkets(data);
            })
            .catch(err => handleApiError(err, "загрузке рынков НТИ"));
    }, []);
    useEffect(() => {
  if (!teamData || !teamData.id) return;

  setEditedData(prev => ({
    ...prev,
    // если у teamData есть вложенный объект ntiMarket
    ntiMarketId: teamData.ntiMarket?.id || prev.ntiMarketId,
    // готовность
    readinessLevel: teamData.readinessLevel || prev.readinessLevel,
    // описание
    description: teamData.description || prev.description,
  }));
}, [teamData]);
useEffect(() => {
  if (!selectedStreamId && streamInfo?.id) {
    setSelectedStreamId(streamInfo.id);
  }
}, [streamInfo, selectedStreamId]);

    useEffect(() => {
        if (teamData.ntiMarket) {
            setSelectedMarket(teamData.ntiMarket);
        }
        if (teamData.readinessLevel) {
            const trl = trlLevels.find(t => t.label === teamData.readinessLevel);
            setSelectedTRL(trl || null);
        }
    }, [teamData, trlLevels]);

    const handleMarketSelect = (market) => {
        setSelectedMarket(market);
        setShowNTI(false);
        if (isEditing) {
            setEditedData(prev => ({...prev, ntiMarketId: market.id}));
        }
    };

    const handleTRLSelect = (trl) => {
        setSelectedTRL(trl);
        setShowTRL(false);
        if (isEditing) {
            setEditedData(prev => ({...prev, readinessLevel: trl.label}));
        }
    };

    const handleChange = (e) => {
        setEditedData({...editedData, [e.target.name]: e.target.value});
    };


    const handleSave = async () => {
  setIsLoading(true);
  setApiError(null);

  try {
    // 1. Проверка заполненности
    if (!editedData.name?.trim() ||
        !editedData.description?.trim() ||
        !editedData.ntiMarketId ||
        !editedData.readinessLevel ||
        ((role === "ADMIN" || role === "SUPER_ADMIN") && !editedData.username)) {
      throw new Error("Пожалуйста, заполните все обязательные поля");
    }
    let usernameToSend = editedData.username;
    if ((role === "ADMIN" || role === "SUPER_ADMIN") && trackers.length) {
      const sel = trackers.find(t => t.id === editedData.username);
      if (sel && sel.username) {
        usernameToSend = sel.username;
      }
    }

    // 2. Выбираем endpoint
    const baseEndpoint =
      (role === "ADMIN" || role === "SUPER_ADMIN")
        ? `${backendHost}/api/v1/admin/team-card`
        : `${backendHost}/api/v1/team-card`;

    // 2. Параметры запроса
    const params = new URLSearchParams();
params.append("teamCardId", id);
params.append("streamId", selectedStreamId);
if (role === "ADMIN" || role === "SUPER_ADMIN") {
  params.append("username", usernameToSend);
}



    // 4. Тело запроса
    const patchData = {
      name: editedData.name.trim(),
      description: editedData.description.trim(),
      ntiMarketId: editedData.ntiMarketId,
      readinessLevel: editedData.readinessLevel,
      
    };

    // 5. Отправка PATCH
    const response = await fetch(
      `${baseEndpoint}?${params.toString()}`,
      {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(patchData),
      }
    );

    if (!response.ok) {
      const errText = await response.text();
      throw new Error(`Ошибка: ${response.status} ${errText}`);
    }

    const updated = await response.json();
    setTeamData(updated);
    setEditedData(updated);
    setIsEditing(false);

  } catch (error) {
    handleApiError(error, "сохранении карточки");
  } finally {
    setIsLoading(false);
  }
};



    const handleDeactivate = async () => {
  const isAdmin = role === "ADMIN" || role === "SUPER_ADMIN";

  if (!window.confirm('Вы уверены, что хотите деактивировать карточку команды?')) {
    return;
  }

  const baseEndpoint = isAdmin
    ? `${backendHost}/api/v1/admin/team-card`
    : `${backendHost}/api/v1/team-card`;

  const params = new URLSearchParams();
  params.append("id", id);

  if (isAdmin) {
    const userIdOrUsername = teamData?.user?.id || teamData?.username || "";
    params.append("username", userIdOrUsername); // ← важно, если бэкенд требует
  }

  try {
    const response = await fetch(`${baseEndpoint}?${params.toString()}`, {
      method: "DELETE",
      credentials: "include",
    });

    if (!response.ok) {
      throw new Error(`Ошибка при удалении: ${response.status}`);
    }

    navigate("/team-cards"); // или `navigate(-1)` для возврата
  } catch (error) {
    handleApiError(error, "удалении карточки");
  }
};

    
   

    return (
        <div className="team-card-widget-container">
            <button className="close-button-widget" onClick={() => navigate(`/team-cards`)}>×</button>

            <button
                className="edit-button-widget"
                onClick={isEditing ? handleSave : () => setIsEditing(true)}
                disabled={!teamData || isLoading}
            >
                {isLoading ? "Сохранение..." : (isEditing ? "Сохранить" : "Редактировать")}
            </button>


            <div className="team-card-left">
                <div className="team-card-info">
                    <span className="team-label-widget">Трекер:</span>
                    <div className="team-input-wrapper">
                        {(role === "ADMIN" || role === "SUPER_ADMIN") && isEditing ? (
                            <>
                                <select
                                    className="team-input-widget"
                                    name="username"
                                    value={editedData.username || ""}
                                    onChange={handleChange}
                                >
                                    <option value="">Выберите трекера</option>
                                    {Array.isArray(trackers) && trackers.map(tracker => (
                                        <option key={tracker.id} value={tracker.username}>
                                            {tracker.fullName}
                                        </option>
                                    ))}
                                </select>
                            </>
                        ) : (
                            <>
                                <input
            className="team-input-widget"
            value={trackerFullName || ""}
            readOnly
            placeholder="ФИО трекера"
        />
                                                            </>
                        )}
                    </div>
                    {isEditing && (
                            <img src={penIcon} alt="edit" className="team-edit-icon"/>
                    )}
                </div>

                <div className="team-card-info">
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
                                            </div>
                    {isEditing && (
                            <img src={penIcon} alt="edit" className="team-edit-icon"/>
                    )}
                </div>

                {isEditing ? (
  role === "TRACKER" ? (
    <div
      className="dropdown-block"
      onMouseEnter={() => setShowTooltip(true)}
      onMouseLeave={() => setShowTooltip(false)}
      style={{ position: "relative" }}
    >
      <div
        className={`dropdown-toggle editable`}
        style={{ cursor: "not-allowed", opacity: 0.6 }}
      >
        {streamInfo?.name || "Поток"}
      </div>
      {showTooltip && (
        <div className="stream-tooltip">
          Трекер не может редактировать привязку к потоку
        </div>
      )}
    </div>
  ) : (
    <div className={`dropdown-block${showStreams ? " open" : ""}`}>
      <div
        className="create-dropdown-toggle"
        onClick={() => setShowStreams(!showStreams)}
      >
        {streams.find(s => s.id === selectedStreamId)?.name || streamInfo?.name || "Поток"}

      </div>
      {showStreams && (
    <div className="create-checkbox-list">
      {streams.map(stream => (
        <div
          key={stream.id}
          className="create-checkbox-item create-radio-style"
        >
          <input
            type="radio"
            name="stream"
            checked={selectedStreamId === stream.id}
            onChange={() => {
              setSelectedStreamId(stream.id);
              setShowStreams(false);
              // при сохранении сюда уже попадёт selectedStreamId
            }}
          />
          <label
  className="data-create-team"
  tabIndex={0}
  onClick={() => {
    setSelectedStreamId(stream.id);
    setShowStreams(false);
  }}
  onKeyDown={(e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      setSelectedStreamId(stream.id);
      setShowStreams(false);
    }
  }}
>
  {stream.name}
</label>
        </div>
      ))}
        </div>
      )}
    </div>
  )
) : null}



                {isEditing ? (
                    <div className={`dropdown-block${showNTI ? " open" : ""}`}>
                        <div
                            className={`create-dropdown-toggle ${isEditing ? 'editable' : ''}`}
                            onClick={() => isEditing && setShowNTI(!showNTI)}
                        >
                            {selectedMarket?.displayName || "Рынок НТИ"}
                        </div>
                        {showNTI && (
    <div className="create-checkbox-list">
      {ntiMarkets.map(market => (
        <div
          key={market.id}
          className="create-checkbox-item create-radio-style"
        >
          <input
            type="radio"
            name="ntiMarket"
            checked={selectedMarket?.id === market.id}
            onChange={() => {
              handleMarketSelect(market);
            }}
          />
          <button
  type="button"
  className="data-create-team"
  onClick={() => handleMarketSelect(market)}
  onKeyDown={(e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      handleMarketSelect(market);
    }
  }}
>
  {market.displayName}
</button>
        </div>
      ))}
                            </div>
                        )}      
                    </div>
                ) : (
                        <div className="team-card-info">
                            <span className="team-label-widget">Рынки НТИ:</span>
                            <div className="team-input-list">
                                <input
    className="team-input-widget1"
    value={teamData.ntiMarket?.displayName || ""}
    readOnly
    placeholder="Рынок НТИ"
/>

                                
                            </div>
                            {isEditing && (
                                    <img src={penIcon} alt="edit" className="team-edit-icon"/>
                            )}
                        </div>
                    )}

                {isEditing ? (
                    <div className={`dropdown-block${showTRL ? " open" : ""}`}>
                        <div
                            className={`create-dropdown-toggle ${isEditing ? 'editable' : ''}`}
                            onClick={() => isEditing && setShowTRL(!showTRL)}
                        >
                            {selectedTRL?.label || "TRL"}
                        </div>
                        {showTRL && (
    <div className="create-checkbox-list">
      {trlLevels.map(trl => (
        <div
          key={trl.id}
          className="create-checkbox-item create-radio-style"
        >
          <input
            type="radio"
            name="trl"
            checked={selectedTRL?.id === trl.id}
            onChange={() => {
              handleTRLSelect(trl);
            }}
          />
          <button
    key={trl.id}
    type="button"
    className={`data-create-team`}
    onClick={() => handleTRLSelect(trl)}
    onKeyDown={(e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        handleTRLSelect(trl);
      }
    }}
  >
    {trl.label}
  </button>
        </div>
      ))}
                            </div>
                        )}                                   
                    </div>
                ) : (
                    <div className="team-card-info">
                    <span className="team-label-widget">TRL:</span>
                    <div className="team-input-list">
                        <div className="team-input-wrapper">
        <input
            className="team-input-widget2"
            value={teamData.readinessLevel || ""}
            readOnly
            placeholder="TRL"
        />
    </div>
                        
                    </div>
                    {isEditing && (
                            <img src={penIcon} alt="edit" className="team-edit-icon"/>
                    )}
                </div>
                )}

                <div className="team-description">
  <span className="team-description-label">Описание:</span>
  
  <div className="team-description-wrapper">
    <textarea
  name="description"
  className="team-description-input"
  value={editedData.description || ""}
  onChange={handleChange}
  readOnly={!isEditing}
  placeholder="Описание карточки"
/>

  </div>
</div>


                {isEditing ? (null) : (
                    <div className="team-stream-block">
                        {streamInfo ? (
              <div className="stream-info-block">
    <span className="stream-name">{streamInfo.name}</span>
    <span className="stream-count">{teamCardsCount} команд</span>
    <span className="stream-dates">{formatDates(streamInfo.startDate, streamInfo.endDate)}</span>
  </div>
        ) : (
            "Загрузка данных о потоке..."
        )}
                    </div>
                )}
   
            </div>

            <div className="right-panel">
            <div className="team-meetings-block">
                    <div className="team-meetings-exist">
                        {meetings.map((meeting) => (
    <div
        key={meeting.id}
        className="team-meeting"
        onClick={() => navigate(`/meeting/${meeting.id}?teamId=${id}&username=${username}`)}
    >
        <span className="meeting-date">
            {new Date(meeting.startDate).toLocaleDateString('ru-RU', {
                day: '2-digit',
                month: '2-digit'
            })}
        </span>
        <span className="meeting-title">
            Встреча {meeting.number || "Без номера"}
        </span>
    </div>
))}
                    </div>
                        <button className="team-meeting-add" onClick={() => navigate(`/meeting-create/${id}?username=${username}`)}>
                            Запланировать   
                        </button>
                    
                </div>
            </div>
            {isEditing ? (
                <div className="red-button-container">
                <button className="red-button-widget" onClick={handleDeactivate}>
                    <span>Деактивировать</span>
                </button>
            </div>
            ) : (null)}
            
        </div>
    );
};

export default TeamCard;
