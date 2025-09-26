import React, {useEffect, useMemo, useState} from "react";
import {useLocation, useNavigate, useParams} from "react-router-dom";
import "./team-card.css";
import {useSelector} from "react-redux";
import penIcon from "./pen.png";
import MeetingCreate from "../meeting-card/MeetingCreate.js";
import { getCsrfConfigForFetch } from "../../utils/csrf-utils";

const backendHost = (process.env.REACT_APP_BACKEND_URI || "https://localhost:8080") + '/backend';
const backendHost1 = (process.env.REACT_APP_BACKEND_URI || "https://localhost:8080") + '/sso';
const backendHost2 = (process.env.REACT_APP_BACKEND_URI || "https://localhost:8080") + '/meeting';
export const getMeetingStatusClass = (status) => {
    switch(status) {
        case "COMPLETED":
            return "meeting-status-completed";
        case "NOT_HAPPENED":
        case "COMPLETED_AS_NOT_HAPPENED":
            return "meeting-status-not-happened";
        default:
            return ""; // Для SCHEDULED оставляем без специального класса
    }
};
const TeamCard = () => {
    const navigate = useNavigate();
    const {id} = useParams();
    
    
   const [showMeetingCreate, setShowMeetingCreate] = useState(false);
    const location = useLocation();
    const passedUsername = location.state?.username;
    const query = new URLSearchParams(location.search);
    const from = location.state?.from || "/team-cards";

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
  const [editingMeetingId, setEditingMeetingId] = useState(null);
const [newMeetingDate, setNewMeetingDate] = useState('');
const [meetingError, setMeetingError] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const [apiError, setApiError] = useState(null); // eslint-disable-line no-unused-vars
const [maxMeetingsCount, setMaxMeetingsCount] = useState(0);
    const trlLevels = useMemo(() => [
        {id: 1, label: "0-2"},
        {id: 2, label: "3-5"},
        {id: 3, label: "6-8"},
        {id: 4, label: "9-10"},
    ], []);
    const [trackerFullName, setTrackerFullName] = useState("");
    const [streamInfo, setStreamInfo] = useState(null);

useEffect(() => {
  if (teamData.streams && teamData.streams.length > 0) {
    setStreamInfo(teamData.streams[0]); // берем первый поток
  }
}, [teamData]);

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
  if (streamInfo?.meetingsCount) {
    setMaxMeetingsCount(streamInfo.meetingsCount);
  }
}, [streamInfo]);
const checkMeetingCreation = () => {
  if (meetings.length >= maxMeetingsCount) {
    setMeetingError(`Невозможно создать новую встречу. Максимальное количество встреч в потоке: ${maxMeetingsCount}`);
    setTimeout(() => setMeetingError(""), 3000); // Автоскрытие через 3 секунды
    return false;
  }
  setMeetingError(""); // Сбрасываем ошибку если все ок
  return true;
};


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
  const streamIdFromTeam = teamData?.streams?.[0]?.id;

  if (!streamIdFromTeam) return;

  const fetchTeamCardsCount = async () => {
    try {
      const url = new URL(`${backendHost}/api/v1/team-card/count`);
      url.searchParams.append("streamId", streamIdFromTeam);

      const response = await fetch(url.toString(), {
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`Ошибка при получении количества карточек: ${response.status}`);
      }

      const data = await response.json();
      const count = typeof data === "number" ? data : data.count || 0;
      setTeamCardsCount(count);
    } catch (error) {
      handleApiError(error, "получении количества карточек команд");
    }
  };

  fetchTeamCardsCount();
}, [teamData]); // зависимость от teamData


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
            const response = await fetch(
                `${backendHost2}/api/v1/meetings?teamCardId=${id}&page=${currentPage}&size=1000&sort=number,asc`, // Добавляем сортировку
                { credentials: 'include' }
            );
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
            "Content-Type": "application/json", 
            ...getCsrfConfigForFetch()
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
                "Content-Type": "application/json", 
                ...getCsrfConfigForFetch()
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


    // Замените useEffect загрузки потоков на этот:
useEffect(() => {
    if (role === "ADMIN" || role === "SUPER_ADMIN") {
        fetch(`${backendHost}/api/v1/streams?page=0&size=1500`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                ...getCsrfConfigForFetch()
            },
            credentials: 'include',
            body: JSON.stringify({filters: []}),
        })
            .then(async (res) => {
                if (!res.ok) throw new Error(`Ошибка: ${res.status}`);
                return res.json();
            })
            .then((data) => {
                const allStreams = Array.isArray(data.content) ? data.content : [];
                
                // Фильтруем: активные потоки + текущий поток команды (если есть)
                const currentTeamStreamId = teamData.streams?.[0]?.id;
                const filteredStreams = allStreams.filter(stream => 
                    stream.active === true || stream.id === currentTeamStreamId
                );

                const streamsWithNames = filteredStreams.map((s) => ({
                    id: s.id, 
                    name: s.name,
                    active: s.active,
                    isCurrentTeamStream: s.id === currentTeamStreamId
                }));
                
                setStreams(streamsWithNames);
            })
            .catch((err) => handleApiError(err, "загрузке потоков"));
    }
}, [role, teamData.streams]); // Добавляем teamData.streams в зависимости

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
  ntiMarketIds: teamData.ntiMarkets?.map(m => m.id) || prev.ntiMarketIds || [],
  readinessLevel: teamData.readinessLevel || prev.readinessLevel,
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

useEffect(() => {
  if (Array.isArray(ntiMarkets)) {
    setSelectedMarket(
      ntiMarkets.filter(m =>
        editedData.ntiMarketIds?.includes(m.id)
      )
    );
  } else {
    setSelectedMarket([]); // Устанавливаем пустой массив, если ntiMarkets не массив
  }
}, [editedData.ntiMarketIds, ntiMarkets]);
    const handleSave = async () => {
  setIsLoading(true);
  setApiError(null);

  try {
    // 1. Проверка заполненности
    if (!editedData.name?.trim() ||
        !editedData.description?.trim() ||
        !editedData.ntiMarketIds ||
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
      ntiMarketIds: editedData.ntiMarketIds,
      readinessLevel: editedData.readinessLevel,
      
    };

    // 5. Отправка PATCH
    const response = await fetch(
      `${baseEndpoint}?${params.toString()}`,
      {
        method: "PATCH",
        headers: { "Content-Type": "application/json", ...getCsrfConfigForFetch() },
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


const handleDateChange = (meetingId, currentDate) => {
  try {
    // Конвертируем дату в формат, понятный для input[type="datetime-local"]
    const localDate = new Date(currentDate);
    const offset = localDate.getTimezoneOffset() * 60000; // коррекция часового пояса
    const localISOTime = new Date(localDate - offset).toISOString().slice(0, 16);
    
    setEditingMeetingId(meetingId);
    setNewMeetingDate(localISOTime);
  } catch (error) {
    handleApiError(error, "изменении даты встречи");
  }
};

const saveMeetingDate = async () => {
    if (!editingMeetingId || !newMeetingDate || !id) return;

    try {
        const isoDate = new Date(newMeetingDate).toISOString();

        const response = await fetch(
            `${backendHost2}/api/v1/update-meeting/${editingMeetingId}?teamCardId=${id}`,
            {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                    ...getCsrfConfigForFetch()
                },
                credentials: 'include',
                body: JSON.stringify({ startDate: isoDate })
            }
        );

        if (!response.ok) {
            throw new Error('Ошибка при обновлении даты');
        }

        // Обновляем только измененную встречу, сохраняя порядок
        setMeetings(prevMeetings => 
            prevMeetings.map(meeting => 
                meeting.id === editingMeetingId 
                    ? { ...meeting, startDate: isoDate } 
                    : meeting
            )
        );
        
        setEditingMeetingId(null);
    } catch (error) {
        handleApiError(error, "сохранении даты встречи");
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
      headers: {  ...getCsrfConfigForFetch() },
      credentials: "include",
    });

    if (!response.ok) {
      throw new Error(`Ошибка при удалении: ${response.status}`);
    }

    navigate(from); // или `navigate(-1)` для возврата
  } catch (error) {
    handleApiError(error, "удалении карточки");
  }
};

    
   

    return (
        <div className="team-card-widget-container">
          {teamData.averageGrade !== undefined && teamData.averageGrade !== null && (
  <div className="team-rating">
    {teamData.averageGrade.toLocaleString('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
  </div>
)}
            <button className="close-button-widget" onClick={() => navigate(from)}>×</button>

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
  className={`create-dropdown-toggle editable`}
  onClick={() => setShowNTI(!showNTI)}
  onKeyDown={(e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault(); // Предотвращаем прокрутку страницы при нажатии пробела
      setShowNTI(!showNTI);
    }
  }}
  tabIndex={0} // Делаем элемент фокусируемым
  role="button" // Указываем роль для лучшей семантики
  aria-expanded={showNTI} // Указываем состояние выпадающего списка
  aria-label="Выбрать рынки НТИ" // Улучшаем доступность для экранных читалок
>
  {(selectedMarket?.length > 0
    ? selectedMarket.slice(0, 2).map(m => m.displayName).join(", ") +
      (selectedMarket.length > 2 ? ` +${selectedMarket.length - 2}` : "")
    : "Рынки НТИ")}
</div>
  {showNTI && (
    <div className="create-checkbox-list">
      {ntiMarkets.map(market => (
        <div key={market.id} className="create-checkbox-item create-radio-style">
          <input
            type="checkbox"
            checked={editedData.ntiMarketIds?.includes(market.id)}
            onChange={() => {
              setEditedData(prev => {
                const already = prev.ntiMarketIds?.includes(market.id);
                return {
                  ...prev,
                  ntiMarketIds: already
                    ? prev.ntiMarketIds.filter(id => id !== market.id)
                    : [...(prev.ntiMarketIds || []), market.id]
                };
              });
            }}
          />
          <label className="data-create-team">{market.displayName}</label>
        </div>
      ))}
    </div>
  )}
</div>

                ) : (
                        <div className="team-card-info">
                            <span className="team-label-widget">Рынки НТИ:</span>
                            <div className="team-input-list">
  {(teamData.ntiMarkets || []).map((market) => (
    <input
      key={market.id}
      className="team-input-widget1"
      value={market.displayName}
      readOnly
      placeholder="Рынок НТИ"
    />
  ))}

                                
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


                {isEditing ? null : (
  <div className="team-stream-block">
    {streamInfo ? (
      <div className="stream-info-block">
  <div className="stream-header">
    <span className="stream-header-label">Название потока:</span>
    <span className="stream-header-label">Количество команд:</span>
    <span className="stream-header-label">Сроки потока:</span>
  </div>
  <div className="stream-data">
    <span className="stream-name">{streamInfo.name}</span>
    <span className="stream-count">{teamCardsCount}</span>
    <span className="stream-dates">{formatDates(streamInfo.startDate, streamInfo.endDate)}</span>
  </div>
</div>
    ) : (
  <div className="stream-loading-message">Загрузка данных о потоке...</div>
)}
  </div>
)}
   
            </div>

            <div className="right-panel">
  {/* Сообщение об ошибке */}
  {meetingError && (
    <div className="error-message">
      {meetingError}
    </div>
  )}
            <div className="team-meetings-block">
                    <div className="team-meetings-exist">
  {meetings.map((meeting) => (
    <div
      key={meeting.id}
      className={`team-meeting ${getMeetingStatusClass(meeting.status)}`}
      onClick={(e) => {
        // Переход если кликнули на саму карточку или на название, но не на дату
        if (e.target === e.currentTarget || 
            e.target.classList.contains('meeting-title')) {
          navigate(`/meeting/${meeting.id}?teamId=${id}&username=${username}`);
        }
      }}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          navigate(`/meeting/${meeting.id}?teamId=${id}&username=${username}`);
        }
      }}
      tabIndex={0}
      role="button"
      aria-label={`Встреча ${meeting.number} от ${new Date(meeting.startDate).toLocaleDateString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })}`}
    >
      {editingMeetingId === meeting.id ? (
        <div className="date-edit-container">
          <input
  type="datetime-local"
  value={newMeetingDate}
  onChange={(e) => setNewMeetingDate(e.target.value)}
  className="date-input"
  min={new Date().toISOString().slice(0, 16)} // запрет выбора прошедших дат
/>
          <button 
            onClick={(e) => {
              e.stopPropagation();
              saveMeetingDate();
            }}
            className="save-date-button"
          >
            Сохранить
          </button>
          <button 
            onClick={(e) => {
              e.stopPropagation();
              setEditingMeetingId(null);
            }}
            className="cancel-date-button"
          >
            Отмена
          </button>
        </div>
      ) : (
        <>
          <span 
  className="meeting-date"
  onClick={(e) => {
    e.stopPropagation();
    handleDateChange(meeting.id, meeting.startDate);
  }}
  tabIndex={0} // Make it focusable
  role="button" // Indicate it's interactive
  onKeyDown={(e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.stopPropagation();
      handleDateChange(meeting.id, meeting.startDate);
    }
  }}
  aria-label={`Изменить дату встречи ${meeting.number}`}
>
  {new Date(meeting.startDate).toLocaleDateString('ru-RU', {
    day: '2-digit',
    month: '2-digit'
  })}
</span>
          <span className="meeting-title">
            Встреча {meeting.number || "Без номера"}
          </span>
        </>
      )}
    </div>
  ))}
</div>
                        <button 
  className="team-meeting-add" 
  onClick={() => {
    if (checkMeetingCreation()) {
      setShowMeetingCreate(true);
    }
  }}
>
  Запланировать
</button>
                    {showMeetingCreate && (
                        <MeetingCreate 
                            teamId={id}
                            onClose={() => setShowMeetingCreate(false)}
                        />
                    )}
                </div>
            </div>
            {isEditing ? (
                <div className="red-button-container">
                    <button className="red-button-widget" onClick={handleDeactivate}>
                        <span>Деактивировать</span>
                    </button>
                </div>
            ) : null}
        </div>
    );
};


export default TeamCard;
