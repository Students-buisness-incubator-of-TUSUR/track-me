import React, {useCallback, useEffect, useState} from "react";
import "./TrackerPage.css";
import { useLocation } from "react-router-dom";
import {Link, useNavigate} from "react-router-dom";
import {useSelector} from "react-redux";
import ProfileIcon from "./personal_account_1.png";

function TrackerPage() {
    const [cards, setCards] = useState([]);
    
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
    const [selectedYears, setSelectedYears] = useState([]);
    const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
    const location = useLocation();
const showAllCards = location.pathname === "/all-team-cards";
const [showMyTeamsOnly, setShowMyTeamsOnly] = useState(false);
const [page, setPage] = useState(0);
const pageSize = 9; // или 10, если хочешь другой размер
const [totalPages, setTotalPages] = useState(1);



    
    const toggleProfileMenu = () => {
        setIsProfileMenuOpen(prev => !prev);
    };
    const formatDateToYMD = (dateString) => {
    if (!dateString) return "";
  
    try {
        const date = new Date(dateString);
        const year = date.getFullYear();
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        const day = date.getDate().toString().padStart(2, '0');
        
        return `${year}.${month}.${day}`;
    } catch (e) {
        console.error("Error formatting date:", e);
        return dateString; // возвращаем как есть, если не удалось преобразовать
    }
    };


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
    const handleYearChange = (year) => {
        setSelectedYears((prev) =>
            prev.includes(year)
                ? prev.filter((y) => y !== year)
                : [...prev, year]
        );
    };
    
    // В начале компонента TrackerPage
useEffect(() => {
    // Проверяем данные пользователя в localStorage при загрузке
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
        const userData = JSON.parse(savedUser);
        setUserRole(userData.roles[0]);
        setusername(userData.username);
    } else if (user?.user) {
        // Сохраняем данные пользователя в localStorage
        localStorage.setItem('user', JSON.stringify(user.user));
        setUserRole(user.user.roles[0]);
        setusername(user.user.username);
    }
}, [user]);
    useEffect(() => {
        if (user?.roles?.length && user?.username) {
            const role = user.roles[0];
            const uname = user.username;
            setUserRole(role);
            setusername(uname);
        }
    }, [user]);
    

    // Данные для чекбоксов "год"
    const checkboxesData = Array.from({length: numberOfCheckboxes1}, (_, index) => ({
        id: `checkbox-${index + 1}`,
        label: `${index + 2016}`,
    }));

    // Функция для запроса карточек с заданными фильтрами
    const fetchCards = useCallback((filters = []) => {
    if (!userRole || !username) return;

    const allFilters = [...filters];

    if ((userRole === "ADMIN" || userRole === "SUPER_ADMIN")) {
        if (!showAllCards) {
            allFilters.push({
                fieldName: "streams.name",
                type: "EQ",
                value: streamName,
            });
        } else if (showMyTeamsOnly) {
            allFilters.push({
                fieldName: "username",
                type: "EQ",
                value: username,
            });
        }
    } else if (userRole === "TRACKER") {
        allFilters.push({
            fieldName: "username",
            type: "EQ",
            value: username,
        });
    }

    const endpoint = (userRole === "ADMIN" || userRole === "SUPER_ADMIN")
        ? `${backendHost}/api/v1/admin/team-cards`
        : `${backendHost}/api/v1/team-cards`;

    fetch(`${endpoint}?page=${page}&size=${pageSize}&sort=enabled%2Cdesc&sort=streams.startDate&sort=averageGrade%2Cdesc`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify({ filters: allFilters }),
    })
        .then((response) => {
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return response.json();
        })
        .then((data) => {
            if (data?.content) {
                const cardsArray = Array.isArray(data.content) ? data.content : [];
                
                setCards(cardsArray);
                setTotalPages(data?.page?.totalPages || 1);
            }
        })
        .catch((err) => {
            console.error("Error fetching cards:", err);
            setError(`Ошибка при загрузке карточек: ${err.message}`);
        });
}, [userRole, username, streamName, backendHost, showAllCards, showMyTeamsOnly, page]);
 // ✅ streamName в зависимости

    
    useEffect(() => {
        if (userRole && username) {
            fetchCards([]);
        }
    }, [userRole, username, fetchCards]);
    

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
    if (!userRole) return;

    const isTracker = userRole === "TRACKER";
    const url = isTracker
        ? `${backendHost}/api/v1/streams/active?page=0&size=150`
        : `${backendHost}/api/v1/admin/streams?page=0&size=150`;

    const options = {
        method: isTracker ? "GET" : "POST",
        headers: {
            "Content-Type": "application/json",
        },
        credentials: "include",
        ...(isTracker ? {} : { body: JSON.stringify({ filters: [] }) }),
    };

    fetch(url, options)
        .then((response) => {
            if (!response.ok) {
                throw new Error("Ошибка при загрузке потоков");
            }
            return response.json();
        })
        .then((data) => {
            const list = data?.content ?? [];

            const streamsWithNames = list.map((stream) => ({
                id: stream.id,
                name: stream.name,
                startDate: stream.startDate,
                endDate: stream.endDate,
            }));

            setStreams(streamsWithNames);

            // Запоминаем в localStorage первый поток, если он есть
            const firstStream = streamsWithNames[0];
            if (firstStream) {
    if (!localStorage.getItem("streamName")) {
        localStorage.setItem("streamName", firstStream.name);
        localStorage.setItem("streamId", firstStream.id);
        localStorage.setItem("streamSDate", firstStream.startDate);
        localStorage.setItem("streamEDate", firstStream.endDate);
    }

    setStreamName(localStorage.getItem("streamName") || firstStream.name);
    setStreamId(localStorage.getItem("streamId") || firstStream.id);
    setStreamSDate(localStorage.getItem("streamSDate") || firstStream.startDate);
    setStreamEDate(localStorage.getItem("streamEDate") || firstStream.endDate);
}

        })
        .catch((error) => {
            console.error("Ошибка при получении потоков:", error);
        });
}, [backendHost, userRole]);


    // Фильтрация карточек по поисковому запросу
    const filteredCards = cards.filter((card) =>
        card.name.toLowerCase().includes(searchQuery.toLowerCase())
    );
    const visibleCards = filteredCards;


    const handleShowMore = () => {
        setPage((prev) => prev + 1)
    };

    const handleShowPrevious = () => {
        setPage((prev) => Math.max(prev - 1, 0))
    };

    const handleSearchChange = (e) => {
        setSearchQuery(e.target.value);
        setPage(0)
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
        if (selectedYears.length > 0) {
            filters.push({
                fieldName: "streams.year", // имя поля уточни при необходимости
                type: "EQ",
                values: selectedYears,
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
        setSelectedYears([]);

    };
    const handleLogout = async () => {
    // 1. Удаление данных из localStorage
    localStorage.removeItem("user");
    localStorage.removeItem("userRole");
    localStorage.removeItem("streamName");
    localStorage.removeItem("streamId");
    localStorage.removeItem("streamSDate");
    localStorage.removeItem("streamEDate");
    
    }
    useEffect(() => {
    if (userRole && username && streamName) {
        fetchCards([]);
    }
}, [userRole, username, streamName, fetchCards]);


    return (
        <div className="tracker-container">
            <header className="Stream-header">
                <div className="Stream-header-cont">
                    <div className='Stream-header-logo'/>
                    <h1 className="Stream-title">TrackMe</h1>
                    <div className="Stream-header-cont-cont">
                        {showAllCards ? (
  <h1 className="Stream-title11">Все карточки команд</h1>
) : (
  <>
    <h1 className="Stream-title11">
      {(userRole === "ADMIN" || userRole === "SUPER_ADMIN" || userRole === "TRACKER")
        ? (streamName ? streamName : "Название потока не получено")
        : ""}
    </h1>
    {(userRole === "ADMIN" || userRole === "SUPER_ADMIN" || userRole === "TRACKER") && (
      <h1 className="Stream-title11">
        {streamName ? ": cроки акселератора: " + formatDateToYMD(streamSDate) + " - " + formatDateToYMD(streamEDate) : ""}
      </h1>
    )}
  </>
)}

                    </div>

                    <div className="Stream-buttons">
                        <button className="Stream-pic" onClick={toggleProfileMenu}>
                          <img src={ProfileIcon} alt="Профиль" className="Stream-pic-img" />
                        </button>
                        
                        
                        {isProfileMenuOpen && (
                          <div className="ProfileDropdown">
                            <Link to="/profile" className="ProfileDropdown-item">
                              Личный кабинет
                            </Link>
                            <Link onClick={handleLogout} to={logoutHost} className="ProfileDropdown-item logout">
                              Выход
                            </Link>
                          </div>
                        )}
                           
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
                                <button className="Stream-header-chose-butt">Год [{selectedYears.length}]</button>
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
        className={`Stream-header-checkbox ${index < 5 ? "first-row" : "second-row"}`}
    >
        <input
            type="checkbox"
            id={checkbox.id}
            checked={selectedYears.includes(parseInt(checkbox.label))}
            onChange={() => handleYearChange(parseInt(checkbox.label))}
        />
        <label className="Stream-header-checkbox-label" htmlFor={checkbox.id}>
            {checkbox.label}
        </label>
    </div>
))}
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                        <div className="switch-wrapper">
  <div className="tooltip-wrapper">
    <label className="ios-switch">
      <input
        type="checkbox"
        checked={showMyTeamsOnly}
        onChange={() => setShowMyTeamsOnly(prev => !prev)}
      />
      <span className="slider"></span>
    </label>
    <span className="tooltip-text">Показать карточки, где вы назначены трекером</span>
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
                            onClick={() => navigate(`/teamcard/${card.id}`, { 
  state: { 
    userId: card.userId,
    streamId: streamId, 
    from: location.pathname 
  }
})}
                            style={{cursor: "pointer"}}
                        >
                            <div className="card-image"/>
                            <span className="status">
                                {card.enabled ? "Активно" : "Завершено"}
                            </span>

                            <div className="card-content">
                                <div className="text-container project-title">
                                    <h3>{card.name}</h3>
                                </div>
                                <div className="text-container project-description">
                                    <p>{card.description}</p>
                                </div>
                                <div className="under-cont">
                                    <div className="text-container project-markets">
                                        <p>Рынки НТИ: {card.ntiMarket ? card.ntiMarket.displayName : "Неизвестен"}</p>
                                    </div>
                                    <div className="text-container project-trl">
                                        <p>TRL: {card.readinessLevel || "Неизвестен"}</p>
                                    </div>
                                    <div className="text-container project-flow">
                                        <p>Поток: {streamName || "Неизвестен"}
                                        {streamName ? ": " + streamSDate + " - " + streamEDate : "Название потока не получено"}
                                        </p>
                                    </div>
                                </div>
                            </div>

                            <button
                                className="edit-button"
                                onClick={(e) => {
                                    e.stopPropagation(); // чтобы не срабатывал переход по карточке
                                    navigate(`/teamcard/${card.id}?userId=${card.userId}&edit=true`, {
  state: {
    userId: card.userId,
    streamId: streamId,
    from: location.pathname
  }
});

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
                {page > 0 && (
                    <button
                        onClick={handleShowPrevious}
                        className="Stream-footer-button-1"
                    ></button>
                )}
            </div>
            <div className="Stream-footer-p-butts">
                {page > 0 && (
                    <button
                        onClick={handleShowPrevious}
                        className="Stream-footer-button-2"
                    ></button>
                )}
                <button className="Stream-footer-button-3"></button>
                {page + 1 < totalPages && (
                    <button
                        onClick={handleShowMore}
                        className="Stream-footer-button-4"
                    ></button>
                )}
            </div>
            <div className="Stream-footer-p-butt-5">
                {page + 1 < totalPages && (
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