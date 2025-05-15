import React, {useEffect, useMemo, useState} from "react";
import {useLocation, useNavigate, useParams} from "react-router-dom";
import "./team-card.css";
import {useSelector} from "react-redux";
import penIcon from "./pen.png";

const backendHost = (process.env.REACT_APP_BACKEND_URI || "https://localhost:8080") + '/backend';

const TeamCard = () => {
    const navigate = useNavigate();
    const {id} = useParams();
    const location = useLocation();
    const query = new URLSearchParams(location.search);
    
    const [role, setRole] = useState(null);
const [username, setUsername] = useState(null);
const reduxUser = useSelector(state => state.user?.user);




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

    const [isLoading, setIsLoading] = useState(false);
    const [apiError, setApiError] = useState(null);

    const trlLevels = useMemo(() => [
        {id: 1, label: "0-2"},
        {id: 2, label: "3-5"},
        {id: 3, label: "6-8"},
        {id: 4, label: "9-10"},
    ], []);
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
        if (!role || !username) return; // Wait until user data is loaded
        
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
            <button className="close-button-widget" onClick={() => navigate(`/team-cards`)}>×</button>

            <button
                className="edit-button-widget"
                // onClick={isEditing ? handleSave : () => setIsEditing(true)}
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
                                        <option key={tracker.id} value={tracker.id}>
                                            {tracker.fullName}
                                        </option>
                                    ))}
                                </select>
                            </>
                        ) : (
                            <>
                                <input
                                    className="team-input-widget"
                                    value={teamData.user?.fullName || ""}
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
                    <div className="dropdown-block">
                        <div
                            className={`dropdown-toggle ${isEditing ? 'editable' : ''}`}
                        >
                            {"Поток"}
                        </div>                                  
                    </div>
                ) : (null)}

                {isEditing ? (
                    <div className={`dropdown-block${showNTI ? " open" : ""}`}>
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
                ) : (
                        <div className="team-card-info">
                            <span className="team-label-widget">Рынки НТИ:</span>
                            <div className="team-input-list">
                                <div className="team-input-item">Рынок НТИ</div>
                                <div className="team-input-item">Рынок НТИ</div>
                                
                            </div>
                            {isEditing && (
                                    <img src={penIcon} alt="edit" className="team-edit-icon"/>
                            )}
                        </div>
                    )}

                {isEditing ? (
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
                ) : (
                    <div className="team-card-info">
                    <span className="team-label-widget">TRL:</span>
                    <div className="team-input-list">
                        <div className="team-input-item">TRL</div>
                        
                    </div>
                    {isEditing && (
                            <img src={penIcon} alt="edit" className="team-edit-icon"/>
                    )}
                </div>
                )}

                <div className="team-description">
                    <span className="team-description-label">Описание:
                        {isEditing && (
                                <img src={penIcon} alt="edit" className="team-edit-icon"/>
                        )}    
                    </span>
                    
                    <div className="team-description-wrapper">
                        <textarea
                            className="team-description-input"
                            name="description"
                            placeholder="Описание карточки команды"
                        />
                    </div>
                </div>

                {isEditing ? (null) : (
                    <div className="team-stream-block">
                        Название потока 12 команд 01.01.2025 - 10.10.2025
                    </div>
                )}
   
            </div>

            <div className="right-panel">
            <div className="team-meetings-block">
                    <div className="team-meetings-exist">
                        <div className="team-meeting">
                            <span class="meeting-date">25.04</span>
                            <span class="meeting-title">Встреча 1</span> 
                        </div>
                        <div className="team-meeting">  
                            <span class="meeting-date">25.04</span>
                            <span class="meeting-title">Встреча 1</span>  
                        </div>
                    </div>
                        <button className="team-meeting-add">
                            Запланировать   
                        </button>
                    <div className="fake-scrollbar"></div>
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
