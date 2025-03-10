import React, {useCallback, useEffect, useState} from 'react';
import {Link} from "react-router-dom";
import './stream-page.css';

export default function Stream() {
  const [isVisible, setIsVisible] = useState(false);
  const [visibleCardsStart] = useState(0);
  const [showCheckboxes, setShowCheckboxes] = useState(false);
  const [showCheckboxes2, setShowCheckboxes2] = useState(false);
  const [showCheckboxes3, setShowCheckboxes3] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [checkboxesData2, setCheckboxesData2] = useState([]);
  const [data, setData] = useState({ content: [], page: {} });
  // eslint-disable-next-line
  const [fdata, setfData] = useState({ fieldName: " ", type: " ", value: " " });
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
  const backendHost = process.env.REACT_APP_BACKEND_HOST || 'http://localhost:8080';
  const numberOfCheckboxes = year - 2015;



  const fetchData = useCallback(async (filters = {filters: [] }) => {
    setLoading(true);
    setError(null);
    console.log(page);
    console.log(filters);

    const token = localStorage.getItem("accessToken");
    if (!token) {
      setError("Ошибка: отсутствует токен авторизации. Выполните вход.");
      setLoading(false);
      return;
    }
    try {
      const response = await fetch(`${backendHost}/api/v1/admin/streams?page=${page}&size=9`, {
        method: 'POST',
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(filters),
      });

      if (!response.ok) {
        throw new Error('Ошибка при загрузке данных');
      }

      const result = await response.json();
      console.log(response);
      setData(result);
    } catch (error) {
      setError(error.message);
    } finally {
      setLoading(false);

    }
  }, [backendHost,page]);

  useEffect(() => {
    fetchData(); // Первоначальный запрос без фильтров
  }, [fetchData]);



  const cardd = data.content.map((item, index) => ({
    id: item.id,
    title: item.name,
    content: item.description,
    startDate: item.startDate,
    endDate: item.endDate,
    readinessLevel: item.readinessLevel,
  }));

  const fetchStreamImage = useCallback(async (streamId) => {
    if (!streamId) {
      console.error("Ошибка: streamId отсутствует или некорректен.");
      return null;
    }
  
    const token = localStorage.getItem("accessToken");
    if (!token) {
      console.error("Ошибка: отсутствует токен авторизации. Выполните вход.");
      return null;
    }
  
    try {
      const response = await fetch(`${backendHost}/api/v1/streams/${streamId}/image`, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json', // Если требуется
        },
      });
  
      if (!response.ok) {
        const errorData = await response.json();
        console.error("Ошибка при загрузке изображения:", errorData);
        return null; // Возвращаем null, если изображение отсутствует
      }
  
      const imageBlob = await response.blob();
      const imageUrl = URL.createObjectURL(imageBlob);
      return imageUrl;
    } catch (error) {
      console.error('Ошибка при загрузке изображения:', error);
      return null; // Возвращаем null в случае ошибки
    }
  }, [backendHost]);
  
  useEffect(() => {
    const fetchImages = async () => {
      const newImageUrls = {};
      for (const card of data.content) { // Используем data.content вместо cardd
        if (!imageUrls[card.id]) { // Проверяем, было ли изображение уже загружено
          const imageUrl = await fetchStreamImage(card.id);
          if (imageUrl) {
            newImageUrls[card.id] = imageUrl;
          }
        }
      }
      setImageUrls((prevImageUrls) => ({ ...prevImageUrls, ...newImageUrls })); // Обновляем состояние
    };
  
    fetchImages();
    // eslint-disable-next-line
  }, [data.content, fetchStreamImage]);
  const handleClick = () => {
    setIsVisible(!isVisible);
  };

  const handleShowMore = () => {
    console.log(data.page.totalPages);
    setpage(page+1);

  };

  const handleShowPrevious = () => {
    console.log(data.page.totalPages);
    setpage(page-1);
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
    setfData({ fieldName: "name", type: "LIKE", value: query });
  };

  const handleYearCheckboxChange = (id, label) => {
    setCheckedYears(prev => {
      const newCheckedYears = { ...prev, [id]: !prev[id] };
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
      const newCheckedMarkets = { ...prev, [id]: !prev[id] };
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
      const newCheckedTRLs = { ...prev, [id]: !prev[id] };
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

  const handleApplyFilters = () => {
    const newFilters = {
      filters: [
        {
          fieldName: "name",
          type: "LIKE",
          value: searchQuery,
        },
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

  const visibleCards = cardd.slice(visibleCardsStart, visibleCardsStart + 9);

  const checkboxesData = Array.from({ length: numberOfCheckboxes }, (_, index) => ({
    id: `checkbox-${index + 1}`,
    label: `${index + 2016}`,
  }));

  const fetchCheckboxesData = useCallback(async () => {
    const token = localStorage.getItem("accessToken");
    if (!token) {
      setError("Ошибка: отсутствует токен авторизации. Выполните вход.");
      return;
    }
    try {
      const response = await fetch(`${backendHost}/api/v1/streams/nti-markets`, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error('Ошибка при загрузке данных для чекбоксов');
      }

      const result = await response.json();
      const formattedData = result.map((item) => ({
        id: item.id,
        name: item.displayName,
        description: item.name,
      }));
      setCheckboxesData2(formattedData);
    } catch (error) {
      console.error('Ошибка при загрузке данных для чекбоксов:', error);
      setError('Не удалось загрузить данные для чекбоксов.');
    }
  }, [backendHost]);

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
          <h1 className="Stream-title">Название</h1>
          <div className="Stream-buttons">
            <Link to="/list-admins"><button className="Stream-butt">Администраторы</button></Link>
            <Link to="/list-trackers"><button className="Stream-butt">Трекеры</button></Link>
            <button className="Stream-butt">Все команды</button>
            <Link to="/profile" className="Stream-pic"></Link>
          </div>
        </div>
        <div className="Stream-header-bottom-cont">
          <div className="Stream-search-cont">
            <button onClick={handleClick} className="Stream-settings-pic"></button>
            <div className="Stream-search-contcont">
            <button className="Stream-settings-pic2" onClick={handleApplyFilters}> </button>
              <input
                type="search"
                placeholder="Найти"
                onChange={handleSearch}
                value={searchQuery}
                className="Stream-search"
              />
            </div>
          </div>
          <Link to="/create-stream"><button className="Stream-butt">+ Создать карточку</button></Link>
        </div>
        {isVisible && (
          <div className="Stream-header-afterclick-cont">
            <div className="Stream-header-afterclick-left">
              <div className="Stream-header-afterclick-left-up">
                <button className="Stream-header-chose-butt">Год [{selectedYears.size}]</button>
                <button className="Stream-header-chose-butt">Рынок [{selectedMarkets.size}]</button>
                <button className="Stream-header-chose-butt">TRL [{selectedTRLs.size}]</button>
              </div>
              <div className="Stream-header-chosefrom-cont">
                <div className="Stream-header-chosefrom-buttw">
                  <div className="Stream-header-chosefrom-butt">
                    <div className="Stream-header-chosefrom-butt-cont" onClick={handleShowCheckboxes}>
                      <b className="Stream-header-chosefrom-butt-label">Год</b>
                      <div className="Stream-header-chosefrom-butt-pic"></div>
                    </div>
                    {showCheckboxes && (
                      <div className="Stream-header-checkboxes">
                        {checkboxesData.map((checkbox, index) => (
                          <div key={checkbox.id} className={`Stream-header-checkbox ${index < 5 ? 'first-row' : 'second-row'}`}>
                            <input
                              type="checkbox"
                              id={checkbox.id}
                              className="custom-checkbox"
                              checked={!!checkedYears[checkbox.id]}
                              onChange={() => handleYearCheckboxChange(checkbox.id, checkbox.label)}
                            />
                            <label className='Stream-header-checkbox-label' htmlFor={checkbox.id}>{checkbox.label}</label>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
                <div className="Stream-header-chosefrom-buttw">
                  <div className="Stream-header-chosefrom-butt2">
                    <div className="Stream-header-chosefrom-butt-cont" onClick={handleShowCheckboxes2}>
                      <b className="Stream-header-chosefrom-butt-label">Рынок</b>
                      <div className="Stream-header-chosefrom-butt-pic"></div>
                    </div>
                    {showCheckboxes2 && (
                      <div className="Stream-header-checkboxes">
                        {checkboxesData2.map((checkbox, index) => (
                          <div key={checkbox.id} className={`Stream-header-checkbox ${index < 5 ? 'first-row' : 'second-row'}`}>
                            <input
                              type="checkbox"
                              id={checkbox.id}
                              className="custom-checkbox"
                              checked={!!checkedMarkets[checkbox.id]}
                              onChange={() => handleMarketCheckboxChange(checkbox.id, checkbox.description)}
                            />
                            <label className='Stream-header-checkbox-label' htmlFor={checkbox.id}>{checkbox.name}</label>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
                <div className="Stream-header-chosefrom-buttw">
                  <div className="Stream-header-chosefrom-butt2">
                    <div className="Stream-header-chosefrom-butt-cont" onClick={handleShowCheckboxes3}>
                      <b className="Stream-header-chosefrom-butt-label">TRL</b>
                      <div className="Stream-header-chosefrom-butt-pic"></div>
                    </div>
                    {showCheckboxes3 && (
                      <div className="Stream-header-checkboxes">
                        {checkboxesData3.map((checkbox, index) => (
                          <div key={checkbox.id} className={`Stream-header-checkbox ${index < 5 ? 'first-row' : 'second-row'}`}>
                            <input
                              type="checkbox"
                              id={checkbox.id}
                              className="custom-checkbox"
                              checked={!!checkedTRLs[checkbox.id]}
                              onChange={() => handleTRLCheckboxChange(checkbox.id, checkbox.label)}
                            />
                            <label className='Stream-header-checkbox-label' htmlFor={checkbox.id}>{checkbox.label}</label>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
            <div className="Stream-header-afterclick-right">
              <button className="Stream-header-chose-butt2" onClick={handleResetCheckboxes}>Сбросить</button>
              <button className="Stream-header-chose-butt" onClick={handleApplyFilters}>Применить</button>
            </div>
          </div>
        )}
      </header>
      <main className="Stream-main">
      {/* localStorage.setItem("streamName",card.title)   onClick={console.log(card.title)} */}
        {visibleCards.map(card => (

          <Link to="/team-cards" key={card.id} onClick={() => localStorage.setItem("streamName",card.title)} className="Stream-card">
<div className="Stream-card-pic">
  {imageUrls[card.id] ? (
    <img src={imageUrls[card.id]} alt={card.title} />
  ) : (
    <img src="путь_к_fallback_изображению" alt=" " />
  )}
</div>     
    <h1 className="Stream-card-headText">{card.title} </h1>
          </Link>
        ))}


      </main>
      <footer className="Stream-footer">
        <div className="Stream-footer-butts">
          <div className="Stream-footer-p-butt-1">
            {0 < page && (
              <button onClick={handleShowPrevious} className="Stream-footer-button-1"></button>
            )}
          </div>
          <div className="Stream-footer-p-butts">
            {0 < page && (
              <button onClick={handleShowPrevious} className="Stream-footer-button-2"></button>
            )}
            <button className="Stream-footer-button-3"></button>
            {data.page.totalPages > (page + 1) && (
              <button onClick={handleShowMore} className="Stream-footer-button-4"></button>
            )}
          </div>
          <div className="Stream-footer-p-butt-5">
            {data.page.totalPages > (page + 1) && (
              <button onClick={handleShowMore} className="Stream-footer-button-5"></button>
            )}
          </div>
        </div>
      </footer>
    </div>
  );
}