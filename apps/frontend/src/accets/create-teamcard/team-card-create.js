import React, {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import "./team-card-create.css";
import penIcon from "./pen.png";

const backendHost = process.env.REACT_APP_BACKEND_URI + '/backend';

const TeamCard = () => {
    const navigate = useNavigate();

    const [error, setError] = useState("");
    const [streams, setStreams] = useState([]);
    const [markets, setMarkets] = useState([]);
    const [trackers, setTrackers] = useState([]);
    const [currentUser, setCurrentUser] = useState(null);
    const [showStreams, setShowStreams] = useState(false);
    const [showNTI, setShowNTI] = useState(false);
    const [showTRL, setShowTRL] = useState(false);
    const [showTrackers, setShowTrackers] = useState(false);
    const [selectedMarket, setSelectedMarket] = useState(null);
    const [selectedTRL, setSelectedTRL] = useState(null);
    const [selectedTracker, setSelectedTracker] = useState(null);
    const [formData, setFormData] = useState({
        name: "",
        description: "",
        tracker: "",
        streamId: null
    });
    const [isLoading, setIsLoading] = useState(false);

    // Close dropdowns when clicking outside
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (!event.target.closest('.create-dropdown-block') && !event.target.closest('.tracker-select-container')) {
                setShowNTI(false);
                setShowTRL(false);
                setShowStreams(false);
                setShowTrackers(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    // Получаем информацию о текущем пользователе
    useEffect(() => {
        fetch(`${backendHost}/api/v1/users/current/info`, {
            credentials: "include",
        })
            .then((response) => response.json())
            .then((userData) => {
                setCurrentUser(userData);
                const isAdmin = userData.role === "ADMIN" || userData.role === "SUPER_ADMIN";

                // Если пользователь не админ, устанавливаем его имя в поле трекера
                if (!isAdmin) {
                    setFormData(prev => ({...prev, tracker: userData.fullName}));
                }
            })
            .catch((error) => {
                console.error("Ошибка при получении данных пользователя:", error);
                setError("Ошибка при получении данных пользователя");
            });
    }, []);

    const trlLevels = [
        {id: 1, label: "0-2"},
        {id: 2, label: "3-5"},
        {id: 3, label: "6-8"},
        {id: 4, label: "9-10"},
    ];

    // Загрузка потоков
    useEffect(() => {
        fetch(`${backendHost}/api/v1/streams?page=0&size=150`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({filters: []}),
            credentials: "include"
        })
            .then((res) => res.ok ? res.json() : null)
            .then((data) => {
                if (data?.content) setStreams(data.content);
            })
            .catch(() => setError("Ошибка при загрузке потоков"));
    }, []);

    // Загрузка рынков НТИ
    useEffect(() => {
        fetch(`${backendHost}/api/v1/streams/nti-markets`, {
            method: "GET",
            credentials: "include",
        })
            .then((res) => res.ok ? res.json() : null)
            .then((data) => {
                if (data) setMarkets(data);
            })
            .catch(() => setError("Ошибка при загрузке рынков НТИ"));
    }, []);

    // Загрузка списка трекеров для админа
    useEffect(() => {
        if (currentUser && (currentUser.role === "ADMIN" || currentUser.role === "SUPER_ADMIN")) {
            fetch(`${backendHost}/api/v1/admin/users/trackers?page=0&size=150`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                credentials: "include",
                body: JSON.stringify({filters: []}),
            })
                .then((res) => res.ok ? res.json() : null)
                .then((data) => {
                    if (data?.content) {
                        setTrackers(data.content);
                    }
                })
                .catch(() => setError("Ошибка при загрузке списка трекеров"));
        }
    }, [currentUser]);

    const handleChange = (e) => {
        const {name, value} = e.target;
        setFormData(prev => ({...prev, [name]: value}));
    };

    const handleMarketSelect = (market) => {
        setSelectedMarket(market);
        setShowNTI(false);
    };

    const handleTRLSelect = (trl) => {
        setSelectedTRL(trl);
        setShowTRL(false);
    };

    const handleStreamSelect = (streamId) => {
        setFormData(prev => ({...prev, streamId}));
        setShowStreams(false);
    };

    const handleTrackerSelect = (tracker) => {
        setSelectedTracker(tracker);
        setFormData(prev => ({
            ...prev,
            tracker: tracker.fullName,
            trackerId: tracker.id // Добавляем ID трекера в formData
        }));
        setShowTrackers(false);
    };

    const validateForm = () => {
        const errors = [];
        if (!formData.name?.trim()) errors.push("Название команды обязательно");
        if (!selectedMarket) errors.push("Выберите рынок НТИ");
        if (!selectedTRL) errors.push("Выберите уровень TRL");
        if (!formData.streamId) errors.push("Привяжите к потоку");
        if (currentUser?.role === "ADMIN" && !selectedTracker) {
            errors.push("Выберите трекера");
        }
        return errors;
    };

    const handleCreate = async () => {
        const errors = validateForm();
        if (errors.length > 0) {
            setError(errors.join("\n"));
            return;
        }

        setIsLoading(true);
        try {
            const payload = {
                name: formData.name,
                description: formData.description || "Описание карточки команды",
                ntiMarketId: selectedMarket.id,
                readinessLevel: selectedTRL.label
            };

            let url = `${backendHost}`;

            // Добавляем username в URL если выбран трекер
            if (currentUser && (currentUser.role === "ADMIN" || currentUser.role === "SUPER_ADMIN")) {
                url += `/api/v1/admin/team-card?streamId=${formData.streamId}&username=${selectedTracker?.username}`;
            } else {
                url += `/api/v1/team-card?streamId=${formData.streamId}`;
            }

            const response = await fetch(url, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                credentials: "include",
                body: JSON.stringify(payload),
            });

            if (!response.ok) {
                throw new Error(response.status === 401 ? "Ошибка авторизации" : "Ошибка создания команды");
            }

            const data = await response.json();
            // После успешного создания переходим на страницу карточки
            navigate(`/teamcard/${data.id}`);
        } catch (error) {
            setError(error.message);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="create-card-container">
            <button className="create-card-close" onClick={() => navigate(-1)}>×</button>

            <div className="create-card-left">
                <div className="create-card-info">
                    <span className="create-card-label">Трекер:</span>
                    <div className="create-input-wrapper">
                        {/* {currentUser && (currentUser.role === "ADMIN" || currentUser.role === "SUPER_ADMIN") ? ( */}
                            <div className="tracker-select-container">
                                <input
                                    className="create-input"
                                    name="tracker"
                                    value={formData.tracker}
                                    onClick={() => setShowTrackers(!showTrackers)}
                                    readOnly
                                    placeholder="Выберите трекера"
                                />
                                {showTrackers && (
                                    <div className="trackers-dropdown">
                                        {trackers.map((tracker) => (
                                            <div
                                                key={tracker.id}
                                                className="tracker-option"
                                                onClick={() => handleTrackerSelect(tracker)}
                                            >
                                                {tracker.fullName}
                                            </div>
                                        ))}
                                    </div>
                                )}
                                <img src={penIcon} alt="edit" className="create-edit-icon"
                                     onClick={() => setShowTrackers(!showTrackers)}/>
                            </div>
                        {/* ) : (
                            <input
                                className="create-input"
                                name="tracker"
                                value={formData.tracker}
                                readOnly
                            />
                        )} */}
                    </div>
                </div>

                <div className="create-card-info">
                    <span className="create-card-label">Название команды:</span>
                    <div className="create-input-wrapper">
                        <input
                            className="create-input"
                            name="name"
                            value={formData.name}
                            onChange={handleChange}
                            placeholder="Введите название команды"
                        />
                        {/* <img src={penIcon} alt="edit" className="create-edit-icon"/> */}
                    </div>
                </div>

                <div className="create-dropdown-block">
                    <div className="create-dropdown-toggle" onClick={() => setShowNTI(!showNTI)}>
                        {selectedMarket ? selectedMarket.displayName : "Рынок НТИ"}
                    </div>
                    {showNTI && (
                        <div className="create-checkbox-list">
                            {markets.map((market) => (
                                <div
                                    key={market.id}
                                    className="create-checkbox-item"
                                    onClick={() => handleMarketSelect(market)}
                                >
                                    {market.displayName}
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                <div className="create-dropdown-block">
                    <div className="create-dropdown-toggle" onClick={() => setShowTRL(!showTRL)}>
                        {selectedTRL ? selectedTRL.label : "TRL"}
                    </div>
                    {showTRL && (
                        <div className="create-checkbox-list">
                            {trlLevels.map((trl) => (
                                <div
                                    key={trl.id}
                                    className="create-checkbox-item"
                                    onClick={() => handleTRLSelect(trl)}
                                >
                                    {trl.label}
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                <div className="create-dropdown-block">
                    <div className="create-dropdown-toggle" onClick={() => setShowStreams(!showStreams)}>
                        {selectedTRL ? selectedTRL.label : "Все потоки"}
                    </div>
                    {showStreams && (
                            <div className="create-checkbox-list">
                                {streams.map((stream) => (
                                    <div
                                        key={stream.id}
                                        className="create-checkbox-item"
                                        onClick={() => handleStreamSelect(stream.id)}
                                    >
                                        {stream.name}
                                    </div>
                                ))}
                            </div>
                        )}
                </div>

                <div className="create-team-description">
                    <span className="create-team-description-label">Описание:</span>
                    <div className="create-team-description-wrapper">
                        <textarea
                            className="create-description-input"
                            name="description"
                            placeholder="Введите описание карточки команды"
                        />
                    </div>
                </div>
            </div>

            <div className="create-right-panel">
                <div className="create-meetings-block">
                    <div className="create-meetings-exist">
                        <div className="create-meeting">
                            <span class="meeting-date">25.04</span>
                            <span class="meeting-title">Встреча 1</span> 
                        </div>
                        <div className="create-meeting">   
                        </div>
                    </div>
                    <button
                        className="create-meeting-add"
                    >
                        Запланировать   
                    </button>
                </div>
            </div>

            

            {error && (
                <div className="error-message" style={{whiteSpace: 'pre-line'}}>
                    {error}
                </div>
            )}

            <div className="create-button-container">
                <button
                    className="create-button"
                    onClick={handleCreate}
                    disabled={isLoading}
                >
                    {isLoading ? "Создание..." : "Создать"}
                </button>
            </div>
        </div>
    );
};

export default TeamCard;
