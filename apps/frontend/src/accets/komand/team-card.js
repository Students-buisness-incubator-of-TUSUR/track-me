import React, {useEffect, useMemo, useState} from "react";
import {useLocation, useNavigate, useParams} from "react-router-dom";
import "./team-card.css";
import {useSelector} from "react-redux";

const backendHost = (process.env.REACT_APP_CLIENT_GATEWAY_URI || "https://localhost:8080") + '/backend';

const TeamCard = () => {
    const navigate = useNavigate();
    const {id} = useParams();
    const location = useLocation();
    const query = new URLSearchParams(location.search);
    const user = useSelector(state => state.user);
    const role = user.roles[0];
    const username = user.username;

    const [teamData, setTeamData] = useState({});
    const [editedData, setEditedData] = useState({});
    const [meetings, setMeetings] = useState([]);
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);
    const [streams, setStreams] = useState([]);
    const [ntiMarkets, setNtiMarkets] = useState([]);
    const [trackers, setTrackers] = useState([]);
    const forceEdit = query.get("edit") === "true";
    const [isEditing, setIsEditing] = useState(forceEdit);

    const [showNTI, setShowNTI] = useState(false);
    const [showTRL, setShowTRL] = useState(false);
    const [selectedMarket, setSelectedMarket] = useState(null);
    const [selectedTRL, setSelectedTRL] = useState(null);

    const [isCreatingMeeting, setIsCreatingMeeting] = useState(false);
    const [newMeetingData, setNewMeetingData] = useState({
        number: "",
        startDate: new Date().toISOString(),
        link: "",
        tasksCurrentMeeting: "",
        tasksNextMeeting: "",
    });

    const [isLoading, setIsLoading] = useState(false);
    const [apiError, setApiError] = useState(null);

    const trlLevels = useMemo(() => [
        {id: 1, label: "0-2"},
        {id: 2, label: "3-5"},
        {id: 3, label: "6-8"},
        {id: 4, label: "9-10"},
    ], []);

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
                const response = await fetch(`${backendHost}/api/v1/meetings?teamCardId=${id}&page=${currentPage}&size=10`, {
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
        const endpoint = (role === "ADMIN" || role === "SUPER_ADMIN")
            ? `${backendHost}/api/v1/admin/team-card?id=${id}&username=${username}`
            : `${backendHost}/api/v1/team-card?id=${id}`;

        fetch(endpoint, {
            credentials: 'include',
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
            .catch(err => handleApiError(err, "загрузке карточки"));
    }, [id, username, role]);

    useEffect(() => {
        if (role === "ADMIN" || role === "SUPER_ADMIN") {
            fetch(`${backendHost}/api/v1/users`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: 'include',
                body: JSON.stringify({
                    filters: [{key: "role", value: "TRACKER"}],
                    page: 0,
                    size: 150,
                    order: {field: "fullName", direction: "ASC"}
                }),
            })
                .then((res) => res.json())
                .then((data) => {
                    setTrackers(data.content || []);
                })
                .catch((err) => {
                    handleApiError(err, "загрузке трекеров");
                    setTrackers([]);
                });
        }
    }, [role]);

    useEffect(() => {
        if (role === "ADMIN" || role === "SUPER_ADMIN") {
            fetch(`${backendHost}/api/v1/streams?page=0&size=150`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: 'include',
                body: JSON.stringify({filters: []}),
            })
                .then((res) => res.json())
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

    const handleNewMeetingChange = (e) => {
        const {name, value} = e.target;
        setNewMeetingData((prev) => ({...prev, [name]: value}));
    };

    const handleCreateMeeting = async () => {
        try {
            const response = await fetch(`${backendHost}/api/v1/meetings?teamCardId=${id}`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                credentials: 'include',
                body: JSON.stringify({
                    ...newMeetingData,
                    startDate: new Date(newMeetingData.startDate).toISOString(),
                }),
            });

            if (!response.ok) {
                throw new Error("Ошибка при создании встречи");
            }

            const createdMeeting = await response.json();
            setMeetings((prev) => [...prev, createdMeeting]);
            setIsCreatingMeeting(false);
            setNewMeetingData({
                number: "",
                startDate: new Date().toISOString(),
                link: "",
                tasksCurrentMeeting: "",
                tasksNextMeeting: "",
            });
        } catch (error) {
            handleApiError(error, "создании встречи");
        }
    };

    const handleSave = async () => {
        setIsLoading(true);
        setApiError(null);

        const patchData = {
            ntiMarketId: editedData.ntiMarketId,
            readinessLevel: editedData.readinessLevel,
        };

        if (role === "ADMIN" || role === "SUPER_ADMIN") {
            patchData.name = editedData.name;
            patchData.description = editedData.description;
        }

        const baseEndpoint = (role === "ADMIN" || role === "SUPER_ADMIN")
            ? `${backendHost}/api/v1/admin/team-card`
            : `${backendHost}/api/v1/team-card`;

        const params = new URLSearchParams();
        params.append("teamCardId", id);

        if (role === "ADMIN" || role === "SUPER_ADMIN") {
            params.append("username", editedData.username || username);
            if (editedData.streamId) {
                params.append("streamId", editedData.streamId);
            }
        }

        try {
            const response = await fetch(`${baseEndpoint}?${params.toString()}`, {
                method: "PATCH",
                headers: {
                    "Content-Type": "application/json",
                },
                credentials: 'include',
                body: JSON.stringify(patchData),
            });

            if (!response.ok) {
                throw new Error("Ошибка при сохранении: " + response.status);
            }

            const updated = await response.json();
            setTeamData(updated);
            setIsEditing(false);
        } catch (error) {
            handleApiError(error, "сохранении изменений");
        } finally {
            setIsLoading(false);
        }
    };

    const handleDeactivate = async () => {
        if (!window.confirm('Вы уверены, что хотите деактивировать карточку команды?')) {
            return;
        }

        const baseEndpoint = (role === "ADMIN" || role === "SUPER_ADMIN")
            ? `${backendHost}/api/v1/admin/team-card`
            : `${backendHost}/api/v1/team-card`;

        const params = new URLSearchParams();

        if (role === "ADMIN" || role === "SUPER_ADMIN") {
            params.append("teamCardId", id);
            params.append("username", teamData.user.id);
        } else {
            params.append("teamCardId", id);
        }

        try {
            const response = await fetch(`${baseEndpoint}?${params.toString()}`, {
                method: "DELETE",
                credentials: 'include',
            });

            if (!response.ok) {
                throw new Error("Ошибка при деактивации карточки");
            }

            navigate(-1);
        } catch (error) {
            handleApiError(error, "деактивации карточки");
        }
    };

    const handlePageChange = (pageIndex) => {
        setCurrentPage(pageIndex);
    };

    return (
        <div className="team-card-widget-container">
            <button className="close-button-widget" onClick={() => navigate(-1)}>×</button>

            <button
                className="edit-button-widget"
                onClick={isEditing ? handleSave : () => setIsEditing(true)}
                disabled={!teamData || isLoading}
            >
                {isLoading ? "Сохранение..." : (isEditing ? "Сохранить" : "Редактировать")}
            </button>

            {apiError && (
                <div className="error-message" style={{
                    position: 'absolute',
                    top: '80px',
                    left: '50%',
                    transform: 'translateX(-50%)',
                    backgroundColor: '#ffebee',
                    padding: '10px 20px',
                    borderRadius: '4px',
                    color: '#c62828'
                }}>
                    {apiError}
                </div>
            )}


            <div className="left-column">
                <div className="inputs-container">
                    <div className="create-card-info">
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
                                            <option key={tracker.id} value={tracker.id}>
                                                {tracker.fullName}
                                            </option>
                                        ))}
                                    </select>
                                    <img src={require("./pen.png")} alt="edit"
                                         className="edit-icon"/>
                                </>
                            ) : (
                                <>
                                    <input
                                        className="team-input-widget"
                                        value={teamData.user?.fullName || ""}
                                        readOnly
                                        placeholder="ФИО трекера"
                                    />
                                    {/* Не показываем pen для readonly */}
                                </>
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
                                <img src={require("./pen.png")} alt="edit" className="edit-icon"/>
                            )}
                        </div>
                    </div>

                    <div className="meetings-section">
                        <div className="meetings-left">
                            <span className="team-label-widget">Встречи:</span>
                            <div className="meetings-grid-widget">
                                {meetings.map((meeting) => (
                                    <div
                                        key={meeting.id}
                                        className={`meeting-item-widget ${meeting.status === 'DONE' ? 'done' : 'planned'}`}
                                        onClick={() => navigate(`/meeting/${meeting.id}?teamId=${id}&username=${username}`)}
                                    >
                                        <span
                                            className="meeting-number-widget">{meeting.number || "-"}</span>
                                        <span className="meeting-date-widget">
                        {new Date(meeting.startDate).toLocaleDateString('ru-RU', {
                            day: '2-digit',
                            month: '2-digit',
                            year: 'numeric'
                        })}
                      </span>
                                        {meeting.link && (
                                            <a href={meeting.link} target="_blank"
                                               rel="noopener noreferrer"
                                               className="meeting-link-widget">
                                                Ссылка на встречу
                                            </a>
                                        )}
                                        {meeting.tasks && meeting.tasks.length > 0 && (
                                            <ul className="meeting-tasks-widget">
                                                {meeting.tasks.map((task) => (
                                                    <li key={task.id}>{task.description}</li>
                                                ))}
                                            </ul>
                                        )}
                                        {meeting.status === 'DONE' &&
                                            <span className="check-mark-widget">✓</span>}
                                    </div>
                                ))}
                                {isCreatingMeeting && (
                                    <div className="new-meeting-form">
                                        <input
                                            type="text"
                                            name="number"
                                            placeholder="Номер встречи"
                                            value={newMeetingData.number}
                                            onChange={handleNewMeetingChange}
                                        />
                                        <input
                                            type="datetime-local"
                                            name="startDate"
                                            value={new Date(newMeetingData.startDate).toISOString().slice(0, 16)}
                                            onChange={handleNewMeetingChange}
                                        />
                                        <input
                                            type="text"
                                            name="link"
                                            placeholder="Ссылка на встречу"
                                            value={newMeetingData.link}
                                            onChange={handleNewMeetingChange}
                                        />
                                        <textarea
                                            name="tasksCurrentMeeting"
                                            placeholder="Задачи текущей встречи"
                                            value={newMeetingData.tasksCurrentMeeting}
                                            onChange={handleNewMeetingChange}
                                        />
                                        <textarea
                                            name="tasksNextMeeting"
                                            placeholder="Задачи к следующей встрече"
                                            value={newMeetingData.tasksNextMeeting}
                                            onChange={handleNewMeetingChange}
                                        />
                                        <button onClick={handleCreateMeeting}>Создать</button>
                                        <button onClick={() => setIsCreatingMeeting(false)}>Отмена
                                        </button>
                                    </div>
                                )}
                                <div className="meeting-item-widget add"
                                     onClick={() => navigate(`/meeting/new?teamId=${id}&username=${username}`)}>+
                                </div>
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
                    <div
                        className={`dropdown-toggle ${isEditing ? 'editable' : ''}`}
                        onClick={() => isEditing && setShowNTI(!showNTI)}
                    >
                        {selectedMarket?.displayName || "Рынок НТИ"}
                    </div>
                    {isEditing && showNTI && (
                        <div className="dropdown-list">
                            {ntiMarkets.map((market) => (
                                <div
                                    key={market.id}
                                    className="dropdown-item"
                                    onClick={() => handleMarketSelect(market)}
                                >
                                    {market.displayName}
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                <div className="dropdown-block">
                    <div
                        className={`dropdown-toggle ${isEditing ? 'editable' : ''}`}
                        onClick={() => isEditing && setShowTRL(!showTRL)}
                    >
                        {selectedTRL?.label || "TRL"}
                    </div>
                    {isEditing && showTRL && (
                        <div className="dropdown-list">
                            {trlLevels.map((trl) => (
                                <div
                                    key={trl.id}
                                    className="dropdown-item"
                                    onClick={() => handleTRLSelect(trl)}
                                >
                                    {trl.label}
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>


            {(role === "ADMIN" || role === "SUPER_ADMIN") && isEditing ? (
                <div className="stream-attach-button-container">
                    <div className="dropdown-block">
                        <select
                            className="dropdown-toggle"
                            name="streamId"
                            value={editedData.streamId || ""}
                            onChange={handleChange}
                        >
                            <option value="" disabled>Выберите поток</option>
                            {streams.map((s) => (
                                <option key={s.id} value={s.id}>{s.name}</option>
                            ))}
                        </select>
                    </div>
                </div>
            ) : (
                <div className="stream-attach-button-container">
                    <div className="dropdown-block">
                        <div className="dropdown-toggle">
                            {teamData.stream ? teamData.stream.name : 'Не привязан к потоку'}
                        </div>
                    </div>
                </div>
            )}

            <div className="red-button-container">
                <button className="red-button-widget" onClick={handleDeactivate}>
                    <span>Деактивировать</span>
                </button>
            </div>
        </div>
    );
};

export default TeamCard;
