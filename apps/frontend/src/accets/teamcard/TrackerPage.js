import React, {useCallback, useEffect, useState} from "react";
import "./TrackerPage.css";

import {Link, useNavigate} from "react-router-dom";
import {useSelector} from "react-redux";

function TrackerPage() {
    const [cards, setCards] = useState([]);
    const [visibleCardsStart, setVisibleCardsStart] = useState(0);
    const [streamName, setStreamName] = useState("");
    // eslint-disable-next-line
    const [streamId, setStreamId] = useState("");
    const [streamSDate, setStreamSDate] = useState("");
    const [streamEDate, setStreamEDate] = useState("");
    const [error, setError] = useState(null);
    const [searchQuery, setSearchQuery] = useState("");
    const [ntiMarkets, setNtiMarkets] = useState([]);
    const [selectedStreams, setSelectedStreams] = useState([]);
    const [streams, setStreams] = useState([]);
    const [userRole, setUserRole] = useState(null);
    const [username, setusername] = useState(null);

    const backendHost = (process.env.REACT_APP_BACKEND_URI || 'http://localhost:8080') + '/backend';
    const logoutHost = (process.env.REACT_APP_BACKEND_URI || 'http://localhost:8080') + '/logout';
    // Состояния для отображения панели фильтров и групп чекбоксов
    const [isVisible, setIsVisible] = useState(false);
    const [showCheckboxesStream, setShowCheckboxesStream] = useState(false); // Для "Все потоки"
    const [showCheckboxes2, setShowCheckboxes2] = useState(false); // Для "рынки Нти"
    const [showCheckboxes3, setShowCheckboxes3] = useState(false); // Для "TRL"
    const [showCheckboxes, setShowCheckboxes] = useState(false); // Для "год"

    // Состояние для выбранного диапазона TRL
    const [selectedTrl, setSelectedTrl] = useState([]);
    const [selectedNtiMarkets, setSelectedNtiMarkets] = useState([]);

    let today = new Date();
    let year = today.getFullYear();
    const navigate = useNavigate();
    // const numberOfCheckboxes = 9;
    const numberOfCheckboxes1 = year - 2015;
    const user = useSelector((state) => state.user);

    // Данные для TRL – используем реальные диапазоны
    const trlRanges = [
        {id: "trl-0-2", label: "0-2"},
        {id: "trl-3-5", label: "3-5"},
        {id: "trl-6-8", label: "6-8"},
        {id: "trl-9-10", label: "9-10"},
    ];

    // Обновленный обработчик выбора TRL
    const handleTrlChange = (trlValue) => {
        setSelectedTrl((prev) =>
            prev.includes(trlValue)
                ? prev.filter((value) => value !== trlValue) // Удаляем, если уже выбран
                : [...prev, trlValue] // Добавляем, если не выбран
        );
    };

    const handleNtiMarketChange = (market) => {
        setSelectedNtiMarkets((prev) =>
            prev.includes(market.name)
                ? prev.filter((name) => name !== market.name) // Remove if already selected
                : [...prev, market.name] // Add if not selected
        );
    };

    const handleStreamChange = (streamName) => {
        setSelectedStreams((prev) =>
            prev.includes(streamName)
                ? prev.filter((name) => name !== streamName)
                : [...prev, streamName]
        );
    };

    // Данные для чекбоксов "год"
    const checkboxesData = Array.from({length: numberOfCheckboxes1}, (_, index) => ({
        id: `checkbox-${index + 1}`,
        label: `${index + 2016}`,
    }));

    // Функция для запроса карточек с заданными фильтрами
    const fetchCards = useCallback((filters = []) => {

        try {
            setUserRole(user.roles[0]);
            setusername(user.username);
        } catch (e) {
            console.error("Error decoding token:", e);
        }

        const allFilters = [...filters];

        // Для админа добавляем фильтр по потоку, для трекера - по его ID
        if (userRole === "ADMIN" || userRole === "SUPER_ADMIN") {
            allFilters.push({
                fieldName: "streams.name",
                type: "EQ",
                value: localStorage.getItem("streamName")
            });
        } else if (userRole === "TRACKER" && username) {
            allFilters.push({
                fieldName: "username",
                type: "EQ",
                value: username
            });
        }

        const endpoint = (userRole === "ADMIN" || userRole === "SUPER_ADMIN")
            ? `${backendHost}/api/v1/admin/team-cards`
            : `${backendHost}/api/v1/team-cards`;

        console.log("Using role:", userRole);
        console.log("Using filters:", allFilters);

        fetch(`${endpoint}?page=0&size=150`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            credentials: "include",
            body: JSON.stringify({filters: allFilters}),
        })
            .then((response) => {
                console.log("Response status:", response.status); // Проверяем статус ответа
                if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
                return response.json();
            })
            .then((data) => {
                console.log("Response data:", data); // Смотрим что приходит
                if (data && data.content) {
                    setCards(data.content);
                    setVisibleCardsStart(0);
                }
            })
            .catch((err) => {
                console.error("Error fetching cards:", err);
                setError(`Ошибка при загрузке карточек: ${err.message}`);
            });
    }, [userRole, username, backendHost, user.roles, user.username]);

    useEffect(() => {
        // Проверяем роль при загрузке компонента
        const role = localStorage.getItem("userRole");
        console.log("Initial role check:", role);

        // Загружаем карточки без фильтров при первом рендере
        fetchCards([]);

        fetch(`${backendHost}/api/v1/streams/nti-markets`, {
            method: "GET",
            credentials: "include"
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Ошибка при загрузке рынков НТИ");
                }
                return response.json();
            })
            .then((data) => {
                setNtiMarkets(data);
            })
            .catch((error) => {
                console.error(error);
            });
    }, [navigate, backendHost, fetchCards]);

    useEffect(() => {

        fetch(`${backendHost}/api/v1/streams?page=0&size=150`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            credentials: "include",
            // Передаём именно массив фильтров, как ожидает сервер
            body: JSON.stringify({filters: []}),
        })
            .then((response) => {
                setStreamName(localStorage.getItem("streamName"));
                setStreamId(localStorage.getItem("streamId"));
                setStreamSDate(localStorage.getItem("streamSDate"));
                setStreamEDate(localStorage.getItem("streamEDate"));

                if (!response.ok) {
                    throw new Error("Ошибка при загрузке потоков");
                }
                return response.json();
            })
            .then((data) => {
                // Если нужно, извлекаем только id и name
                const streamsWithNames = data.content.map((stream) => ({
                    id: stream.id,
                    name: stream.name,
                }));
                setStreams(streamsWithNames);
            })
            .catch((error) => {
                console.error(error);
            });
    }, [backendHost]);

    // Фильтрация карточек по поисковому запросу
    const filteredCards = cards.filter((card) =>
        card.name.toLowerCase().includes(searchQuery.toLowerCase())
    );
    const visibleCards = filteredCards.slice(visibleCardsStart, visibleCardsStart + 9);

    const handleShowMore = () => {
        setVisibleCardsStart((prev) => prev + 9);
    };

    const handleShowPrevious = () => {
        setVisibleCardsStart((prev) => Math.max(prev - 9, 0));
    };

    const handleSearchChange = (e) => {
        setSearchQuery(e.target.value);
        setVisibleCardsStart(0);
    };

    // Переключение отображения панели фильтров
    const handleClick = () => {
        setIsVisible(!isVisible);
    };

    // Обработчик применения фильтров
    const applyFilters = () => {
        const filters = [];

        if (selectedTrl.length > 0) {
            filters.push({
                fieldName: "readinessLevel",
                type: "EQ",
                values: selectedTrl,
            });
        }

        if (selectedNtiMarkets.length > 0) {
            filters.push({
                fieldName: "ntiMarket.name",
                type: "EQ",
                values: selectedNtiMarkets,
            });
        }

        if (selectedStreams.length > 0) {
            filters.push({
                fieldName: "streams.name",
                type: "EQ",
                values: selectedStreams,
            });
        }

        console.log("Applying filters:", filters);
        fetchCards(filters);
        setIsVisible(false);
    };

    // Обработчик сброса фильтров
    const resetFilters = () => {
        setSelectedTrl("");
        setSelectedNtiMarkets([]);
        setSelectedStreams([]);
        // Сбросить другие фильтры, если они будут добавлены
        fetchCards([]);
        setIsVisible(false);
    };

    return (
        <div className="tracker-container">
            <header className="Stream-header">
                <div className="Stream-header-cont">
                    <div className="Stream-header-cont-cont">
                        <h1 className="Stream-title11">
                            {(userRole === "ADMIN" || userRole === "SUPER_ADMIN")
                                ? (streamName ? streamName : "Название потока не получено")
                                : "Track-me"
                            }
                        </h1>
                        {(userRole === "ADMIN" || userRole === "SUPER_ADMIN") && (
                            <h1 className="Stream-title11">
                                {streamName ? "Сроки акселератора: " + streamSDate + " -- " + streamEDate : "Название потока не получено"}
                            </h1>
                        )}
                    </div>

                    <div className="Stream-buttons">
                        <Link to={logoutHost}>
                            <button className="Stream-butt">Выход</button>
                        </Link>
                        <Link to="/profile" className="Stream-pic"></Link>
                    </div>
                </div>
                <div className="Stream-header-bottom-cont">
                    <div className="Stream-search-cont">
                        {/* При клике открывается панель фильтров */}
                        <button onClick={handleClick} className="Stream-settings-pic"></button>
                        <div className="Stream-search-contcont">
                            <button className="Stream-settings-pic2"></button>
                            <input
                                type="search"
                                placeholder="Найти"
                                className="Stream-search"
                                value={searchQuery}
                                onChange={handleSearchChange}
                            />
                        </div>
                    </div>
                    <button
                        className="Stream-butt"
                        onClick={() => {
                            navigate(`/teamcard/create`);
                        }}>
                        + Создать карточку
                    </button>
                </div>
                {/* Панель фильтров */}
                {isVisible && (
                    <div className="Stream-header-afterclick-cont">
                        <div className="Stream-header-afterclick-left">
                            {/* Верхний ряд – заголовки фильтров */}
                            <div className="Stream-header-afterclick-left-up">
                                <button className="Stream-header-chose-butt">Поток
                                    [{selectedStreams.length}]
                                </button>
                                <button className="Stream-header-chose-butt">Рынки
                                    [{selectedNtiMarkets.length}]
                                </button>
                                <button className="Stream-header-chose-butt">
                                    TRL [{selectedTrl.length}]
                                </button>
                                <button className="Stream-header-chose-butt">Год [0]</button>
                            </div>
                            {/* Нижний ряд – группы чекбоксов */}
                            <div className="Stream-header-chosefrom-cont">
                                <div className="Stream-header-chosefrom-buttw">
                                    <div className="Stream-header-chosefrom-butt2"
                                         onClick={() => setShowCheckboxesStream(!showCheckboxesStream)}>
                                        <div className="Stream-header-chosefrom-butt-cont">
                                            <b className="Stream-header-chosefrom-butt-label">Все
                                                потоки</b>
                                            <div className="Stream-header-chosefrom-butt-pic"></div>
                                        </div>
                                    </div>
                                    {showCheckboxesStream && (
                                        <div className="Stream-header-checkboxes">
                                            {streams.map((stream) => (
                                                <div key={stream.id}
                                                     className="Stream-header-checkbox">
                                                    <input
                                                        type="checkbox"
                                                        id={stream.id}
                                                        checked={selectedStreams.includes(stream.name)}
                                                        onChange={() => handleStreamChange(stream.name)}
                                                    />
                                                    <label className="Stream-header-checkbox-label"
                                                           htmlFor={stream.id}>
                                                        {stream.name}
                                                    </label>
                                                </div>
                                            ))}

                                        </div>
                                    )}
                                </div>

                                <div className="Stream-header-chosefrom-buttw">
                                    <div
                                        className="Stream-header-chosefrom-butt2"
                                        onClick={() => setShowCheckboxes2(!showCheckboxes2)}
                                    >
                                        <div className="Stream-header-chosefrom-butt-cont">
                                            <b className="Stream-header-chosefrom-butt-label">
                                                Рынки Нти
                                            </b>
                                            <div className="Stream-header-chosefrom-butt-pic"></div>
                                        </div>
                                    </div>
                                    {showCheckboxes2 && (
                                        <div className="Stream-header-checkboxes">
                                            {ntiMarkets.map((market) => (
                                                <div key={market.id}
                                                     className="Stream-header-checkbox">
                                                    <input
                                                        type="checkbox"
                                                        id={market.id}
                                                        checked={selectedNtiMarkets.includes(market.name)}
                                                        onChange={() => handleNtiMarketChange(market)} // Pass the entire market object
                                                    />
                                                    <label class="Stream-header-checkbox-label"
                                                           htmlFor={market.id}>{market.displayName}</label>
                                                </div>
                                            ))}
                                        </div>
                                    )}

                                </div>
                                <div className="Stream-header-chosefrom-buttw">
                                    <div
                                        className="Stream-header-chosefrom-butt2"
                                        onClick={() => setShowCheckboxes3(!showCheckboxes3)}
                                    >
                                        <div className="Stream-header-chosefrom-butt-cont">
                                            <b className="Stream-header-chosefrom-butt-label">
                                                TRL
                                            </b>
                                            <div className="Stream-header-chosefrom-butt-pic"></div>
                                        </div>
                                    </div>
                                    {showCheckboxes3 && (
                                        <div className="Stream-header-checkboxes">
                                            {trlRanges.map((option) => (
                                                <div key={option.id}
                                                     className="Stream-header-checkbox">
                                                    <input
                                                        type="checkbox"
                                                        id={option.id}
                                                        value={option.label}
                                                        checked={selectedTrl.includes(option.label)}
                                                        onChange={() => handleTrlChange(option.label)}
                                                    />
                                                    <label class="Stream-header-checkbox-label"
                                                           htmlFor={option.id}>{option.label}</label>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                                <div className="Stream-header-chosefrom-buttw">
                                    <div
                                        className="Stream-header-chosefrom-butt2"
                                        onClick={() => setShowCheckboxes(!showCheckboxes)}
                                    >
                                        <div class="Stream-header-chosefrom-butt-cont">
                                            <b className="Stream-header-chosefrom-butt-label">
                                                Год
                                            </b>
                                            <div className="Stream-header-chosefrom-butt-pic"></div>
                                        </div>
                                    </div>
                                    {showCheckboxes && (
                                        <div className="Stream-header-checkboxes">
                                            {checkboxesData.map((checkbox, index) => (
                                                <div
                                                    key={checkbox.id}
                                                    className={`Stream-header-checkbox ${
                                                        index < 5 ? "first-row" : "second-row"
                                                    }`}
                                                >
                                                    <input type="checkbox" id={checkbox.id}/>
                                                    <label class="Stream-header-checkbox-label"
                                                           htmlFor={checkbox.id}>
                                                        {checkbox.label}
                                                    </label>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                        <div className="Stream-header-afterclick-right">
                            <button onClick={resetFilters} className="Stream-header-chose-butt2">
                                Сбросить
                            </button>
                            <button onClick={applyFilters} className="Stream-header-chose-butt">
                                Применить
                            </button>
                        </div>
                    </div>
                )}
            </header>

            <div className="cards-wrapper">
                {error ? (
                    <p className="error-message">{error}</p>
                ) : filteredCards.length > 0 ? (
                    visibleCards.map((card) => (
                        <div
                            className="card"
                            key={card.id}
                            onClick={() => navigate(`/teamcard/${card.id}?username=${card.username}`)}
                            style={{cursor: "pointer"}}
                        >
                            <div className="card-image"/>
                            <span className="status-completed">Завершено</span>

                            <div className="card-content">
                                <div className="text-container project-title">
                                    <h3>{card.name}</h3>
                                </div>
                                <div className="text-container project-description">
                                    <p>{card.description}</p>
                                </div>
                                <div className="under-cont">
                                    <div className="text-container project-markets">
                                        <p>Рынки
                                            НТИ: {card.ntiMarket ? card.ntiMarket.displayName : "Неизвестен"}</p>
                                    </div>
                                    <div className="text-container project-trl">
                                        <p>TRL: {card.readinessLevel || "Неизвестен"}</p>
                                    </div>
                                    <div className="text-container project-flow">
                                        <p>Поток: {streamName || "Неизвестен"}</p>
                                    </div>
                                </div>
                            </div>

                            <button
                                className="edit-button"
                                onClick={(e) => {
                                    e.stopPropagation(); // чтобы не срабатывал переход по карточке
                                    navigate(`/teamcard/${card.id}?username=${card.username}&edit=true`);
                                }}
                            >
                                Редактировать
                            </button>
                        </div>
                    ))
                ) : (
                    <p>
                        {searchQuery
                            ? "Ничего не найдено по запросу"
                            : "Ничего не найдено по запросу"}
                    </p>
                )}
            </div>

            {filteredCards.length > 0 && (
                <footer className="Stream-footer">
                    <div className="Stream-footer-butts">
                        <div className="Stream-footer-p-butt-1">
                            {visibleCardsStart > 0 && (
                                <button
                                    onClick={handleShowPrevious}
                                    className="Stream-footer-button-1"
                                ></button>
                            )}
                        </div>
                        <div className="Stream-footer-p-butts">
                            {visibleCardsStart > 0 && (
                                <button
                                    onClick={handleShowPrevious}
                                    className="Stream-footer-button-2"
                                ></button>
                            )}
                            <button className="Stream-footer-button-3"></button>
                            {visibleCardsStart + 9 < filteredCards.length && (
                                <button
                                    onClick={handleShowMore}
                                    className="Stream-footer-button-4"
                                ></button>
                            )}
                        </div>
                        <div className="Stream-footer-p-butt-5">
                            {visibleCardsStart + 9 < filteredCards.length && (
                                <button
                                    onClick={handleShowMore}
                                    className="Stream-footer-button-5"
                                ></button>
                            )}
                        </div>
                    </div>
                </footer>
            )}
        </div>
    );
}

export default TrackerPage;
