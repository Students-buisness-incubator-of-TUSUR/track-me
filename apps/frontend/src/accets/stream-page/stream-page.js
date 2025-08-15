import React, {useCallback, useEffect, useState} from 'react';
import {Link} from "react-router-dom";
import axios from 'axios';
import './stream-page.css';
import ProfileIcon from "./personal_account_1.png";
import { getCsrfConfig } from '../../utils/csrf-utils'; // Импортируем функцию для CSRF конфигурации
// import LoginService from '../../services/login-service'; // Импортируем сервис для логина
export default function Stream() {
    const [isVisible, setIsVisible] = useState(false);
    const [visibleCardsStart] = useState(0);
    const [showCheckboxes, setShowCheckboxes] = useState(false);
    const [showCheckboxes2, setShowCheckboxes2] = useState(false);
    const [showCheckboxes3, setShowCheckboxes3] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [checkboxesData2, setCheckboxesData2] = useState([]);
    const [data, setData] = useState({content: [], page: {}});
    // eslint-disable-next-line
    const [fdata, setfData] = useState({fieldName: " ", type: " ", value: " "});
    const [checkedYears, setCheckedYears] = useState({});
    const [checkedMarkets, setCheckedMarkets] = useState({});
    const [checkedTRLs, setCheckedTRLs] = useState({});
    const [searchQuery, setSearchQuery] = useState('');
    // eslint-disable-next-line
    const [filters, setFilters] = useState([]); // Состояние для фильтров
    const [page, setpage] = useState(0);
    const [selectedYears, setSelectedYears] = useState(new Set()); // Выбранные годы (Set)
    const [selectedMarkets, setSelectedMarkets] = useState(new Set()); // Выбранные рынки (Set)
    const [selectedTRLs, setSelectedTRLs] = useState(new Set()); // Выбранные TRL (Set)
    let today = new Date();
    const [imageUrls, setImageUrls] = useState({});
    let year = today.getFullYear();
    const backendHost = (process.env.REACT_APP_BACKEND_URI || 'http://localhost:8080') + '/backend';
    const logoutHost = (process.env.REACT_APP_BACKEND_URI || 'http://localhost:8080') + '/logout';
    const numberOfCheckboxes = year - 2015;
    const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);

    // const { logout } = LoginService();
const toggleProfileMenu = () => {
    setIsProfileMenuOpen(prev => !prev);
};
    // Убираем использование и проверку токена
    const fetchData = useCallback(async (filters = {filters: []}) => {
        setLoading(true);
        setError(null);
        try {
            const response = await axios.post(`${backendHost}/api/v1/admin/streams?page=${page}&size=6`,
                filters,
                {   
                    ...getCsrfConfig(),
                    headers: {
                        "Content-Type": "application/json",
                        ...getCsrfConfig().headers
                    },
                    withCredentials: true
                }
            );
            setData(response.data);
        } catch (error) {
            setError(error.message);
        } finally {
            setLoading(false);
        }
    }, [backendHost, page]);

    useEffect(() => {
        const newFilters = {
            filters: [
                
                ...Array.from(selectedYears).map(year => ({
                    fieldName: "year",
                    type: 'EQ',
                    value: year,
                })),
                ...Array.from(selectedMarkets).map(market => ({
                    fieldName: "ntiMarkets.name",
                    type: "EQ",
                    value: market,
                })),
                ...Array.from(selectedTRLs).map(trl => ({
                    fieldName: "teamCards.readinessLevel",
                    type: "EQ",
                    value: trl,
                })),
            ],
        };
        fetchData(newFilters);
        // eslint-disable-next-line
    }, [fetchData]);

    const cardd = data.content.map((item, index) => ({
        id: item.id,
        title: item.name,
        content: item.description,
        startDate: item.startDate,
        endDate: item.endDate,
        readinessLevel: item.readinessLevel,
    }));

    // Убираем использование токена для получения изображений
    const fetchStreamImage = useCallback(async (streamId) => {
        if (!streamId) {
            console.error("Ошибка: streamId отсутствует или некорректен.");
            return null;
        }
        try {
            const response = await axios.get(`${backendHost}/api/v1/streams/${streamId}/image`, {
                headers: {
                    'Content-Type': 'application/json',
                },
                withCredentials: true,
                responseType: 'blob'
            });

            const imageUrl = URL.createObjectURL(response.data);
            return imageUrl;
        } catch (error) {
            console.error('Ошибка при загрузке изображения:', error);
            return null;
        }
    }, [backendHost]);

    useEffect(() => {
        const fetchImages = async () => {
            const newImageUrls = {};
            for (const card of data.content) {
                if (!imageUrls[card.id]) {
                    const imageUrl = await fetchStreamImage(card.id);
                    if (imageUrl) {
                        newImageUrls[card.id] = imageUrl;
                    }
                }
            }
            setImageUrls((prevImageUrls) => ({...prevImageUrls, ...newImageUrls}));
        };
        fetchImages();
        // eslint-disable-next-line
    }, [data.content, fetchStreamImage]);

    // Остальной код компонента остается предыдущим, кроме fetchCheckboxesData
    const fetchCheckboxesData = useCallback(async () => {
        try {
            const response = await axios.get(`${backendHost}/api/v1/streams/nti-markets`, {
                withCredentials: true
            });
            setCheckboxesData2(response.data);
        } catch (error) {
            setError("Ошибка загрузки чекбоксов рынков");
        }
    }, [backendHost]);

    // ...
    // Остальной код компонента остается прежним

    const handleClick = () => {
        setIsVisible(!isVisible);
    };

    const handleShowFirst = () => {
        console.log(data.page.totalPages);
        setpage(0);

    };

    const handleShowLast = () => {
        console.log(data.page.totalPages);
        setpage(data.page.totalPages - 1);

    };
    const handleShowMore = () => {
        console.log(data.page.totalPages);
        setpage(page + 1);
    };
    const handleShowEvenMore = () => {
        console.log(data.page.totalPages);
        setpage(page + 2);
    };

    const handleShowEvenPrevious = () => {
        console.log(data.page.totalPages);
        setpage(page - 2);
    };

    const handleShowPrevious = () => {
        console.log(data.page.totalPages);
        setpage(page - 1);
    };

    const handleShowCheckboxes = () => {
        setShowCheckboxes(!showCheckboxes);
    };

    const handleShowCheckboxes2 = () => {
        setShowCheckboxes2(!showCheckboxes2);
    };

    const handleShowCheckboxes3 = () => {
        setShowCheckboxes3(!showCheckboxes3);
    };

    const perehod = (cardd) => {
        localStorage.setItem("streamName", cardd.title)
        localStorage.setItem("streamId", cardd.id)
        localStorage.setItem("streamSDate", cardd.startDate)
        localStorage.setItem("streamEDate", cardd.endDate)
    };

    const handleResetCheckboxes = () => {
        setCheckedYears({});
        setCheckedMarkets({});
        setCheckedTRLs({});
        setSelectedYears(new Set()); // Сбрасываем выбранные годы
        setSelectedMarkets(new Set()); // Сбрасываем выбранные рынки
        setSelectedTRLs(new Set()); // Сбрасываем выбранные TRL
        setpage(0);
        fetchData();
    };

    const handleSearch = (e) => {
        const query = e.target.value;
        setSearchQuery(query);
        setfData({fieldName: "name", type: "LIKE", value: query});
    };

    const handleYearCheckboxChange = (id, label) => {
        setCheckedYears(prev => {
            const newCheckedYears = {...prev, [id]: !prev[id]};
            const newSelectedYears = new Set(selectedYears); // Создаем копию Set

            if (newCheckedYears[id]) {
                newSelectedYears.add(label); // Добавляем год в Set
            } else {
                newSelectedYears.delete(label); // Удаляем год из Set
            }

            setSelectedYears(newSelectedYears); // Обновляем состояние
            return newCheckedYears;
        });
    };

    const handleMarketCheckboxChange = (id, name) => {
        setCheckedMarkets(prev => {
            const newCheckedMarkets = {...prev, [id]: !prev[id]};
            const newSelectedMarkets = new Set(selectedMarkets); // Создаем копию Set

            if (newCheckedMarkets[id]) {
                newSelectedMarkets.add(name); // Добавляем рынок в Set
            } else {
                newSelectedMarkets.delete(name); // Удаляем рынок из Set
            }

            setSelectedMarkets(newSelectedMarkets); // Обновляем состояние
            return newCheckedMarkets;
        });
    };

    const handleTRLCheckboxChange = (id, label) => {
        setCheckedTRLs(prev => {
            const newCheckedTRLs = {...prev, [id]: !prev[id]};
            const newSelectedTRLs = new Set(selectedTRLs); // Создаем копию Set

            if (newCheckedTRLs[id]) {
                newSelectedTRLs.add(label); // Добавляем TRL в Set
            } else {
                newSelectedTRLs.delete(label); // Удаляем TRL из Set
            }

            setSelectedTRLs(newSelectedTRLs); // Обновляем состояние
            return newCheckedTRLs;
        });
    };
    
    const handleLogout = async () => {
    
        // await logout();
        
        // Очистка localStorage
        localStorage.removeItem("user");
        localStorage.removeItem("userRole");
        localStorage.removeItem("streamName");
        localStorage.removeItem("streamId");
        localStorage.removeItem("streamSDate");
        localStorage.removeItem("streamEDate");
        localStorage.removeItem("csrfToken");
        localStorage.removeItem("csrfHeaderName");
        
        // Перенаправление на главную страницу
        // window.location.href = '/';
    // } catch (error) {
        // console.error("Logout failed:", error);
    
};
    const handleApplyFilters = () => {
        setpage(0);
        const newFilters = {
            filters: [
                
                ...Array.from(selectedYears).map(year => ({
                    fieldName: "year",
                    type: 'EQ',
                    value: year,
                })),
                ...Array.from(selectedMarkets).map(market => ({
                    fieldName: "ntiMarkets.name",
                    type: "EQ",
                    value: market,
                })),
                ...Array.from(selectedTRLs).map(trl => ({
                    fieldName: "teamCards.readinessLevel",
                    type: "EQ",
                    value: trl,
                })),
            ],
        };

        setFilters(newFilters); // Обновляем состояние фильтров
        fetchData(newFilters); // Выполняем запрос с новыми фильтрами
    };

    const visibleCards = cardd.slice(visibleCardsStart, visibleCardsStart + 6);

    const checkboxesData = Array.from({length: numberOfCheckboxes}, (_, index) => ({
        id: `checkbox-${index + 1}`,
        label: `${index + 2016}`,
    }));

    useEffect(() => {
        fetchCheckboxesData();
    }, [fetchCheckboxesData]);

    const checkboxesData3 = [
        {
            id: 1,
            label: "0-2"
        },
        {
            id: 2,
            label: "3-5"
        },
        {
            id: 3,
            label: "6-8"
        },
        {
            id: 4,
            label: "9-10"
        },
    ];

    if (loading) {
        return <div>Загрузка...</div>;
    }

    if (error) {
        return <div>Ошибка: {error}</div>;
    }

    return (
        <div className="Stream">
            <header className="Stream-header">
                <div className="Stream-header-cont">
                    <div className='Stream-header-logo'/>
                    <h1 className="Stream-title">TrackMe</h1>

                    <div className="Stream-buttons">
                        <Link to="/list-admins">
                            <button className="Stream-butt">Администраторы</button>
                        </Link>
                        <Link to="/list-trackers">
                            <button className="Stream-butt">Трекеры</button>
                        </Link>
                        
  <Link to="/all-team-cards">
    <button className="Stream-butt">Все команды</button>
  </Link>


                        <button className="Stream-pic" onClick={toggleProfileMenu}>
  <img src={ProfileIcon} alt="Профиль" className="Stream-pic-img" />
</button>


{isProfileMenuOpen && (
  <div className="ProfileDropdown">
    <Link to="/profile" className="ProfileDropdown-item">
      Личный кабинет
    </Link>
    <Link onClick={handleLogout} to={logoutHost} /*to="/"*/ className="ProfileDropdown-item logout">
  Выход
</Link>
  </div>
)}
                    </div>
                </div>
                <div className="Stream-header-bottom-cont">
                    <div className="Stream-search-cont">
                        <button onClick={handleClick} className="Stream-settings-pic"></button>
                        <div className="Stream-search-contcont">
                            <button className="Stream-settings-pic2"
                                    onClick={handleApplyFilters}></button>
                            <input
                                type="search"
                                placeholder="Найти"
                                onChange={handleSearch}
                                value={searchQuery}
                                className="Stream-search"
                            />
                        </div>
                    </div>
                    <Link to="/create-stream">
                        <button className="Stream-butt">+ Создать карточку</button>
                    </Link>
                </div>
                {isVisible && (
                    <div className="Stream-header-afterclick-cont">
                        <div className="Stream-header-afterclick-left">
                            <div className="Stream-header-afterclick-left-up">
                                <button className="Stream-header-chose-butt">Год
                                    [{selectedYears.size}]
                                </button>
                                <button className="Stream-header-chose-butt">Рынок
                                    [{selectedMarkets.size}]
                                </button>
                                <button className="Stream-header-chose-butt">TRL
                                    [{selectedTRLs.size}]
                                </button>
                            </div>
                            <div className="Stream-header-chosefrom-cont">
                                <div className="Stream-header-chosefrom-buttw">
                                    <div className="Stream-header-chosefrom-butt">
                                        <div className="Stream-header-chosefrom-butt-cont"
                                             onClick={handleShowCheckboxes}>
                                            <b className="Stream-header-chosefrom-butt-label">Год</b>
                                            <div className="Stream-header-chosefrom-butt-pic"></div>
                                        </div>
                                        {showCheckboxes && (
                                            <div className="Stream-header-checkboxes">
                                                {checkboxesData.map((checkbox, index) => (
                                                    <div key={checkbox.id}
                                                         className={`Stream-header-checkbox ${index < 5 ? 'first-row' : 'second-row'}`}>
                                                        <input
                                                            type="checkbox"
                                                            id={checkbox.id}
                                                            className="custom-checkbox"
                                                            checked={!!checkedYears[checkbox.id]}
                                                            onChange={() => handleYearCheckboxChange(checkbox.id, checkbox.label)}
                                                        />
                                                        <label
                                                            className='Stream-header-checkbox-label'
                                                            htmlFor={checkbox.id}>{checkbox.label}</label>
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                </div>
                                <div className="Stream-header-chosefrom-buttw">
                                    <div className="Stream-header-chosefrom-butt2">
                                        <div className="Stream-header-chosefrom-butt-cont"
                                             onClick={handleShowCheckboxes2}>
                                            <b className="Stream-header-chosefrom-butt-label">Рынок</b>
                                            <div className="Stream-header-chosefrom-butt-pic"></div>
                                        </div>
                                        {showCheckboxes2 && (
                                            <div className="Stream-header-checkboxes">
                                                {checkboxesData2.map((checkbox, index) => (
                                                    <div key={checkbox.id}
                                                         className={`Stream-header-checkbox ${index < 5 ? 'first-row' : 'second-row'}`}>
                                                        <input
                                                            type="checkbox"
                                                            id={checkbox.id}
                                                            className="custom-checkbox"
                                                            checked={!!checkedMarkets[checkbox.id]}
                                                            onChange={() => handleMarketCheckboxChange(checkbox.id, checkbox.description)}
                                                        />
                                                        <label
                                                            className='Stream-header-checkbox-label'
                                                            htmlFor={checkbox.id}>{checkbox.name}</label>
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                </div>
                                <div className="Stream-header-chosefrom-buttw">
                                    <div className="Stream-header-chosefrom-butt2">
                                        <div className="Stream-header-chosefrom-butt-cont"
                                             onClick={handleShowCheckboxes3}>
                                            <b className="Stream-header-chosefrom-butt-label">TRL</b>
                                            <div className="Stream-header-chosefrom-butt-pic"></div>
                                        </div>
                                        {showCheckboxes3 && (
                                            <div className="Stream-header-checkboxes">
                                                {checkboxesData3.map((checkbox, index) => (
                                                    <div key={checkbox.id}
                                                         className={`Stream-header-checkbox ${index < 5 ? 'first-row' : 'second-row'}`}>
                                                        <input
                                                            type="checkbox"
                                                            id={checkbox.id}
                                                            className="custom-checkbox"
                                                            checked={!!checkedTRLs[checkbox.id]}
                                                            onChange={() => handleTRLCheckboxChange(checkbox.id, checkbox.label)}
                                                        />
                                                        <label
                                                            className='Stream-header-checkbox-label'
                                                            htmlFor={checkbox.id}>{checkbox.label}</label>
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div className="Stream-header-afterclick-right">
                            <button className="Stream-header-chose-butt2"
                                    onClick={handleResetCheckboxes}>Сбросить
                            </button>
                            <button className="Stream-header-chose-butt"
                                    onClick={handleApplyFilters}>Применить
                            </button>
                        </div>
                    </div>
                )}
            </header>
            <main className="Stream-main">
                {visibleCards.map(card => (
                    <Link to="/team-cards" key={card.id} onClick={() => perehod(card)}
                          className="Stream-card">
                        <div className="Stream-card-pic">
                            {imageUrls[card.id] ? (
                                <img src={imageUrls[card.id]} alt={card.title}/>
                            ) : (
                                <img src="rabbit.png" alt=" "/>
                            )}
                        </div>
                        <h1 className="Stream-card-headText">{card.title} </h1>
                        <Link to={`/edit-stream/${card.id}`} key={card.id}
                              onClick={() => perehod(card)}
                              className="Stream-edit-link">редактировать</Link>
                    </Link>

                ))}
            </main>
            <footer className="Stream-footer">
                <div className="Stream-footer-butts">
                    <div className="Stream-footer-p-butt-1">
                        {0 < page && (
                            <button onClick={handleShowFirst}
                                    className="Stream-footer-button-1"></button>
                        )}
                    </div>
                    <div className="Stream-footer-p-butts">
                        {1 < page && (
                            <button onClick={handleShowEvenPrevious}
                                    className="Stream-footer-button-2"></button>
                        )}
                        {0 < page && (
                            <button onClick={handleShowPrevious}
                                    className="Stream-footer-button-2"></button>
                        )}
                        <button className="Stream-footer-button-3"></button>
                        {data.page.totalPages > (page + 1) && (
                            <button onClick={handleShowMore}
                                    className="Stream-footer-button-4"></button>
                        )}
                        {data.page.totalPages > (page + 2) && (
                            <button onClick={handleShowEvenMore}
                                    className="Stream-footer-button-4"></button>
                        )}
                    </div>
                    <div className="Stream-footer-p-butt-5">
                        {data.page.totalPages > (page + 1) && (
                            <button onClick={handleShowLast}
                                    className="Stream-footer-button-5"></button>
                        )}
                    </div>
                </div>
            </footer>
        </div>
    );
}