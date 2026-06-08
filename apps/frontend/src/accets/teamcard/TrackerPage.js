import {useCallback, useEffect, useState} from "react";
import "./TrackerPage.css";
import { useLocation, useSearchParams } from "react-router-dom";
import {useNavigate} from "react-router-dom";
import StreamPlaceholder from './Заглушка для потока в TrackMe.png';
import { getCsrfConfigForFetch } from "../../utils/csrf-utils";
import Header from "../header/header";
import { useGetUserInfo } from "../../services/util";

function TrackerPage() {
    const [cards, setCards] = useState([]);
    const [streamImages, setStreamImages] = useState({});
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
    const location = useLocation();
    const [searchParams] = useSearchParams();
const showAllCards = location.pathname === "/all-team-cards";
const [showMyTeamsOnly, setShowMyTeamsOnly] = useState(false);
const [page, setPage] = useState(0);
const pageSize = 18; // или 10, если хочешь другой размер
const [totalPages, setTotalPages] = useState(1);
const [currentFilters, setCurrentFilters] = useState([]);
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
    
    const user = useGetUserInfo();
    useEffect(() => {
        setUserRole(user.roles[0]);
        setusername(user.username);
    }, [user]);

    
    

    // Данные для чекбоксов "год"
    const checkboxesData = Array.from({length: numberOfCheckboxes1}, (_, index) => ({
        id: `checkbox-${index + 1}`,
        label: `${index + 2016}`,
    }));

    const buildTeamCardFilters = useCallback((filters = [], searchParams) => {
        const allFilters = [...filters];

        if (searchQuery?.trim()) {
                allFilters.push({
                    fieldName: "name",
                    type: "LIKE",
                    value: searchQuery.trim(),
                });
            }

        if (userRole === "ADMIN" || userRole === "SUPER_ADMIN") {
            if (!showAllCards && streamName) {
                allFilters.push({
                    fieldName: "streams.name",
                    type: "EQ",
                    value: streamName,
                });
            }

            const searchUsername = searchParams?.get("username");
            if (searchUsername) {
                allFilters.push({
                    fieldName: "username",
                    type: "EQ",
                    value: searchUsername,
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

        return allFilters;
    }, [userRole, username, streamName, showAllCards, showMyTeamsOnly, searchQuery]);

    const getTeamCardsEndpoint = useCallback(() => {
        return (userRole === "ADMIN" || userRole === "SUPER_ADMIN")
            ? `${backendHost}/api/v1/admin/team-cards`
            : `${backendHost}/api/v1/team-cards`;
    }, [userRole, backendHost]);

    const sortParams = "sort=enabled,desc&sort=streams.startDate,desc&sort=averageGrade,desc&sort=name,asc";

    // Функция для запроса карточек с заданными фильтрами
   const fetchCards = useCallback((filters = [], searchParams) => {
    if (!userRole || !username) return;

    const allFilters = buildTeamCardFilters(filters, searchParams);
    const endpoint = getTeamCardsEndpoint();

    fetch(`${endpoint}?page=${page}&size=${pageSize}&${sortParams}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            ...getCsrfConfigForFetch()
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
                setCards(cardsArray.map(card => ({ ...card, _showFull: false })));
                setTotalPages(data?.page?.totalPages || 1);
            }
        })
        .catch((err) => {
            console.error("Error fetching cards:", err);
            setError(`Ошибка при загрузке карточек: ${err.message}`);
        });
}, [userRole, username, buildTeamCardFilters, getTeamCardsEndpoint, page]);

 // ✅ streamName в зависимости

    const fetchStreamImage = useCallback(async (streamId) => {
  try {
    const response = await fetch(`${backendHost}/api/v1/streams/${streamId}/image`, {
      credentials: 'include'
    });
    
    if (!response.ok) {
      throw new Error('Image not found');
    }
    
    const blob = await response.blob();
    return URL.createObjectURL(blob);
  } catch (error) {
    console.error('Error fetching stream image:', error);
    return null; // или URL дефолтного изображения
  }
}, [backendHost]);
useEffect(() => {
  if (!userRole) return;

  const isTracker = userRole === "TRACKER";
  const url = isTracker
    ? `${backendHost}/api/v1/streams?page=0&size=150`
    : `${backendHost}/api/v1/admin/streams?page=0&size=150`;

  const options = {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...getCsrfConfigForFetch()
    },
    credentials: "include",
    body: JSON.stringify({ filters: [] }),
  };

  fetch(url, options)
    .then((response) => {
      if (!response.ok) throw new Error("Ошибка при загрузке потоков");
      return response.json();
    })
    .then(async (data) => {
      const list = data?.content ?? [];
      const streamsWithNames = await Promise.all(list.map(async (stream) => {
        const imageUrl = await fetchStreamImage(stream.id);
        return {
          id: stream.id,
          name: stream.name,
          startDate: stream.startDate,
          endDate: stream.endDate,
          imageUrl: imageUrl
        };
      }));

      setStreams(streamsWithNames);

      // Обновляем изображения в состоянии
      const imagesMap = {};
      streamsWithNames.forEach(stream => {
        if (stream.imageUrl) {
          imagesMap[stream.id] = stream.imageUrl;
        }
      });
      setStreamImages(imagesMap);

      // Остальная логика...
    })
    .catch((error) => {
      console.error("Ошибка при получении потоков:", error);
    });
}, [backendHost, userRole, fetchStreamImage]);
    
useEffect(() => {
  return () => {
    // Очищаем URL объектов при размонтировании
    Object.values(streamImages).forEach(url => {
      if (url) URL.revokeObjectURL(url);
    });
  };
}, [streamImages]);
    useEffect(() => {
        // Проверяем роль при загрузке компонента
        const role = localStorage.getItem("userRole");
        console.log("Initial role check:", role);

        

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
    }, [backendHost]);
    


    useEffect(() => {
    if (!userRole) return;

    const isTracker = userRole === "TRACKER";
const url = isTracker
    ? `${backendHost}/api/v1/streams?page=0&size=150`
    : `${backendHost}/api/v1/admin/streams?page=0&size=150`;

const options = {
    method: "POST", // Всегда POST
    headers: {
        "Content-Type": "application/json",
        ...getCsrfConfigForFetch()
    },
    credentials: "include",
    body: JSON.stringify({ filters: [] }), // Отправляем пустые фильтры для всех
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


    const filteredCards = cards;
    const totalPagesToUse = totalPages;
    const visibleCards = cards;

    const handleShowMore = () => {
        if (page + 1 < totalPagesToUse) {
            setPage((prev) => prev + 1);
        }
    };

    const handleShowPrevious = () => {
        if (page > 0) {
            setPage((prev) => prev - 1);
        }
    };

    const handleSearchChange = (e) => {
        setSearchQuery(e.target.value);
        setPage(0);
    };

    // Переключение отображения панели фильтров
    const handleClick = () => {
        setIsVisible(!isVisible);
    };

    // Обработчик применения фильтров
    const applyFilters = () => {
        const filters = [];

        if (searchQuery?.trim()) {
                filters.push({
                    fieldName: "name",
                    type: "LIKE",
                    value: searchQuery.trim(),
                });
            }

        // Поиск по name теперь на клиенте, не отправляем на бэкенд

        if (selectedTrl.length > 0) {
            filters.push({
                fieldName: "readinessLevel",
                type: "IN",
                values: selectedTrl,
            });
        }

        if (selectedNtiMarkets.length > 0) {
            filters.push({
                fieldName: "ntiMarkets.name",
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
        setCurrentFilters(filters);
        setCards([]);     // сброс перед новым поиском/фильтром
        setPage(0);
        setIsVisible(false);

        fetchCards(filters, searchParams);
    };

    // Обработчик сброса фильтров
    const resetFilters = () => {
        setCurrentFilters([]);
        setSelectedTrl([]);
        setSelectedNtiMarkets([]);
        setSelectedStreams([]);
        setSearchQuery("");
        setPage(0);
        setIsVisible(false);
        setSelectedYears([]);

    };
    useEffect(() => {
    if (userRole && username && streamName) {
        fetchCards([], searchParams);
    }
}, [userRole, username, streamName, fetchCards, searchParams]);

    useEffect(() => {
    const handleVisibilityChange = () => {
        if (document.visibilityState === 'visible' && userRole && username && streamName) {
            fetchCards([], searchParams);
        }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    
    return () => {
        document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
}, [userRole, username, streamName, fetchCards, searchParams]);
    // Добавить этот useEffect для перезагрузки карточек при изменении страницы
    useEffect(() => {
        if (userRole && username && streamName) {
            fetchCards(currentFilters, searchParams);
        }
    }, [page]);

    return (
        <div className="tracker-container">
            <Header userRole={userRole}></Header>
            <h1
                className="Stream-title"
                onClick={() => {
                    const targetPath = (userRole === "ADMIN" || userRole === "SUPER_ADMIN")
                        ? "/streams"
                        : "/team-cards";
                    navigate(targetPath);
                }}
                onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                        const targetPath = (userRole === "ADMIN" || userRole === "SUPER_ADMIN")
                            ? "/streams"
                            : "/team-cards";
                        navigate(targetPath);
                    }
                }}
                tabIndex={0}
                role="button"
                aria-label="Вернуться на главную страницу"
                style={{ cursor: "pointer" }}
            >
            </h1>
            <div className="Stream-header-cont-cont">
                {(userRole === "ADMIN" || userRole === "SUPER_ADMIN") && !showAllCards ? (
                    <h1 className="Stream-title11">
                        {streamName
                            ? `${streamName}: сроки акселератора: ${formatDateToYMD(streamSDate)} - ${formatDateToYMD(streamEDate)}`
                            : "Название потока не получено"}
                    </h1>
                ) : (
                    <h1 className="Stream-title11">Все команды</h1>
                )}
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
                            onKeyDown={(e) => {
                                if (e.key === "Enter")
                                    applyFilters();
                                if (e.key === "Escape") {
                                    setSearchQuery("");
                                    applyFilters();
                                }
                            }}
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
            {isVisible && (
                <div className="Teams-header-afterclick-cont">
                    <div className="Teams-header-afterclick-left">
                        <div className="Teams-header-afterclick-left-up">
                            <button className="Teams-header-chose-butt">Поток
                                [{selectedStreams.length}]
                            </button>
                            <button className="Teams-header-chose-butt">Рынок
                                [{selectedNtiMarkets.length}]
                            </button>
                            <button className="Teams-header-chose-butt">
                                TRL [{selectedTrl.length}]
                            </button>
                            <button className="Teams-header-chose-butt">Год [{selectedYears.length}]</button>
                        </div>
                        <div className="Teams-header-chosefrom-cont">
                            <div className={`Teams-header-chosefrom-buttw ${showCheckboxesStream ? "Stream-checkboxes_remove-below-border-radius" : ""}`}>
                                <div className="Teams-header-chosefrom-butt2"
                                    onClick={() => setShowCheckboxesStream(!showCheckboxesStream)}
                                    onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && setShowCheckboxesStream(!showCheckboxesStream)}
                                >
                                    <div className="Teams-header-chosefrom-butt-cont">
                                        <b className="Teams-header-chosefrom-butt-label">Поток</b>
                                        <div className="Teams-header-chosefrom-butt-pic"></div>
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

                            <div className={`Teams-header-chosefrom-buttw ${showCheckboxes2 ? "Stream-checkboxes_remove-below-border-radius" : ""}`}>
                                <div
                                    className="Teams-header-chosefrom-butt2"
                                    onClick={() => setShowCheckboxes2(!showCheckboxes2)}
                                >
                                    <div className="Teams-header-chosefrom-butt-cont">
                                        <b className="Teams-header-chosefrom-butt-label">
                                            Рынок
                                        </b>
                                        <div className="Teams-header-chosefrom-butt-pic"></div>
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
                            <div className={`Teams-header-chosefrom-buttw ${showCheckboxes3 ? "Stream-checkboxes_remove-below-border-radius" : ""}`}>
                                <div
                                    className="Teams-header-chosefrom-butt2"
                                    onClick={() => setShowCheckboxes3(!showCheckboxes3)}
                                >
                                    <div className="Teams-header-chosefrom-butt-cont">
                                        <b className="Teams-header-chosefrom-butt-label">
                                            TRL
                                        </b>
                                        <div className="Teams-header-chosefrom-butt-pic"></div>
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
                            <div className={`Teams-header-chosefrom-buttw ${showCheckboxes ? "Stream-checkboxes_remove-below-border-radius" : ""}`}>
                                <div
                                    className="Teams-header-chosefrom-butt2"
                                    onClick={() => setShowCheckboxes(!showCheckboxes)}
                                >
                                    <div class="Teams-header-chosefrom-butt-cont">
                                        <b className="Teams-header-chosefrom-butt-label">
                                            Год
                                        </b>
                                        <div className="Teams-header-chosefrom-butt-pic"></div>
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
                    {(userRole === "ADMIN" || userRole === "SUPER_ADMIN") &&
                        (location.pathname === "/all-team-cards" || location.pathname.startsWith("/team-cards")) ? (
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
                                <span className="slider-text"> Показать карточки, где вы назначены трекером </span>
                            </div>
                        </div>
                    ) : null}



                    <div className="Teams-header-afterclick-right">
                        <button onClick={resetFilters} className="Teams-header-chose-butt2">
                            Сбросить
                        </button>
                        <button onClick={applyFilters} className="Teams-header-chose-butt">
                            Применить
                        </button>
                    </div>
                </div>
            )}
            <div className="cards-wrapper">
                {error ? (
                    <p className="error-message">{error}</p>
                ) : filteredCards.length > 0 ? (
                    visibleCards.map((card) => (
                        <div 
  className="card" 
  key={card.id} 
  onClick={() => navigate(`/teamcard/${card.id}`)}
  onKeyDown={(e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      navigate(`/teamcard/${card.id}`);
    }
  }}
  tabIndex={0}
  role="button"
  aria-label={`Перейти к карточке команды ${card.name}`}
>
  <div className="card-image">
    {card?.averageGrade !== undefined && card?.averageGrade !== null && (
      <div className={`card-image-rating ${
        card.averageGrade >= 0.51 ? 'rating-green' :
        card.averageGrade >= 0.26 ? 'rating-yellow' :
        'rating-red'
      }`}>
        {card.averageGrade.toLocaleString('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
      </div>
    )}
    <img 
      src={card.streams?.[0]?.id && streamImages[card.streams[0].id] ? streamImages[card.streams[0].id] : StreamPlaceholder}
      alt=""
      onError={(e) => {
        e.target.src = StreamPlaceholder;
        e.target.onerror = null;
      }}
      className="stream-image"
    />
  </div>
                            <span className={`status ${!card.enabled ? "inactive" : ""}`}>
                                {card.enabled ? "Активно" : "Завершено"}
                            </span>

                            <div className="card-content">
                                <div className="text-container project-title">
                                    <h3>{card.name}</h3>
                                </div>
                                <div className="text-container">
  <div className={`project-description ${card._showFull ? "expanded" : ""}`}>
    <p>{card.description}</p>
  </div>

  {card.description.length > 100 && (
    <div
  className="show-more-text"
  onClick={(e) => {
    e.stopPropagation();
    setCards((prev) =>
      prev.map((c) =>
        c.id === card.id ? { ...c, _showFull: !c._showFull } : c
      )
    );
  }}
  onKeyDown={(e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault(); // Предотвращаем прокрутку страницы при нажатии пробела
      setCards((prev) =>
        prev.map((c) =>
          c.id === card.id ? { ...c, _showFull: !c._showFull } : c
        )
      );
    }
  }}
  tabIndex={0} // Делаем элемент фокусируемым
  role="button" // Указываем роль кнопки для семантики
  aria-label={card._showFull ? "Свернуть описание" : "Показать полное описание"} // Улучшаем доступность
>
  {card._showFull ? "Свернуть" : "Подробнее"}
</div>
  )}
</div>



                                <div className="under-cont">
                                    <div className="text-container project-markets">
                                        <p>
  Рынки НТИ:{" "}
  {card.ntiMarkets?.length > 0
    ? card.ntiMarkets.map((market) => market.displayName).join(", ")
    : "Неизвестны"}
</p>


                                    </div>
                                    <div className="text-container project-trl">
                                        <p>TRL: {card.readinessLevel || "Неизвестен"}</p>
                                    </div>
                                    <div className="text-container project-flow">
                                        <p>
  Поток: {card.streams?.[0]?.name || "Неизвестен"}: {card.streams?.[0]?.startDate || "?"} - {card.streams?.[0]?.endDate || "?"}
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
                    <p>Ничего не найдено по запросу</p>
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
                {page + 1 < totalPagesToUse && (
                    <button
                        onClick={handleShowMore}
                        className="Stream-footer-button-4"
                    ></button>
                )}
            </div>
            <div className="Stream-footer-p-butt-5">
                {page + 1 < totalPagesToUse && (
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
