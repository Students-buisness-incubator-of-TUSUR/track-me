import React, { useState, useEffect, useCallback } from 'react';
import { Link } from "react-router-dom";
import './stream-page.css';

export default function Stream() {
  const [isVisible, setIsVisible] = useState(false);
  const [visibleCardsStart, setVisibleCardsStart] = useState(0);
  const [showCheckboxes, setShowCheckboxes] = useState(false);
  const [showCheckboxes2, setShowCheckboxes2] = useState(false);
  const [showCheckboxes3, setShowCheckboxes3] = useState(false);
  const [loading, setLoading] = useState(false); // Состояние для отслеживания загрузки
  const [error, setError] = useState(null); // Состояние для отслеживания ошибок
  const [checkboxesData2, setCheckboxesData2] = useState([]); // Состояние для данных чекбоксов
  const [data, setData] = useState({ content: [], page: {} }); // Состояние для хранения данных
  const backendHost = process.env.REACT_APP_BACKEND_HOST || 'http://localhost:8080';
  const numberOfCheckboxes = 12;

  // Функция для выполнения POST-запроса
  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    
    const token = localStorage.getItem("accessToken");
    if (!token) {
      setError("Ошибка: отсутствует токен авторизации. Выполните вход.");
      setLoading(false);
      return;
    }
  
    try {
      const response = await fetch(`${backendHost}/api/v1/admin/streams?page=0&size=10`, {
        method: 'POST',
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          filters: [
          ],
        }),
      });
  
      if (!response.ok) {
        throw new Error('Ошибка при загрузке данных');
      }
  
      const result = await response.json();
      setData(result);
    } catch (error) {
      setError(error.message);
    } finally {
      setLoading(false);
    }
  }, [backendHost]); // Добавляем зависимость backendHost
  
  useEffect(() => {
    fetchData();
  }, [fetchData]); // Теперь зависимость корректно указана
  
  const cardd = data.content.map((item, index) => ({
    id: item.id, // Используем уникальный id из данных
    title: item.name, // Генерируем заголовок на основе индекса
    content: item.description, // Генерируем содержимое на основе индекса
    startDate: item.startDate, // Добавляем startDate из данных
    endDate: item.endDate, // Добавляем endDate из данных
    readinessLevel: item.readinessLevel // Добавляем readinessLevel из данных
  }));

  const handleClick = () => {
    setIsVisible(!isVisible);
  };

  const handleShowMore = () => {
    setVisibleCardsStart(prev => prev + 9);
  };

  const handleShowPrevious = () => {
    setVisibleCardsStart(prev => Math.max(prev - 9, 0));
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


  const visibleCards = cardd.slice(visibleCardsStart, visibleCardsStart + 9);

  // Пример данных для чекбоксов

  const checkboxesData = Array.from({ length: numberOfCheckboxes }, (_, index) => ({
    id: `checkbox-${index + 1}`,
    label: `${index + 2014}`,
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

  const checkboxesData3 = Array.from({ length: numberOfCheckboxes }, (_, index) => ({
    id: `checkbox3-${index + 1}`,
    label: `${index + 1}`,
  }));

if (true){
  if (loading) {
    return <div>Загрузка...</div>;
  }

  if (error) {
    return <div>Ошибка: {error}</div>;
  }}



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
            <button className="Stream-settings-pic2"> </button>
            <input type="search" placeholder="Найти" className="Stream-search" />
          </div>
        </div>
        <Link to="/create-stream"><button className="Stream-butt">+ Создать карточку</button></Link>
      </div>
      {isVisible && (
        <div className="Stream-header-afterclick-cont">
          <div className="Stream-header-afterclick-left">
            <div className="Stream-header-afterclick-left-up">
              <button className="Stream-header-chose-butt">Год [0]</button>
              <button className="Stream-header-chose-butt">Рынок [0]</button>
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
                          <input type="checkbox" id={checkbox.id} class="custom-checkbox"/>
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
                        <input type="checkbox" id={checkbox.id} class="custom-checkbox" />
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
                        <input type="checkbox" id={checkbox.id} class="custom-checkbox" />
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
            <button className="Stream-header-chose-butt2">Сбросить</button>
            <button className="Stream-header-chose-butt">Применить</button>
          </div>
        </div>
      )}
    </header>
    <main className="Stream-main">
      {visibleCards.map(card => (
        <div key={card.id} className="Stream-card">
          <div className="Stream-card-pic"></div>
          <h1 className="Stream-card-headText">{card.title}</h1>
          <div className="Stream-card-bodyText">{card.content}</div>
        </div>
      ))}
    </main>
    <footer className="Stream-footer">
      <div className="Stream-footer-butts">
        <div className="Stream-footer-p-butt-1">
          {visibleCardsStart > 0 && (
            <button onClick={handleShowPrevious} className="Stream-footer-button-1"></button>
          )}
        </div>
        <div className="Stream-footer-p-butts">
          {visibleCardsStart > 0 && (
            <button onClick={handleShowPrevious} className="Stream-footer-button-2"></button>
          )}
          <button className="Stream-footer-button-3"></button>
          {visibleCardsStart + 9 < cardd.length && (
            <button onClick={handleShowMore} className="Stream-footer-button-4"></button>
          )}
        </div>
        <div className="Stream-footer-p-butt-5">
          {visibleCardsStart + 9 < cardd.length && (
            <button onClick={handleShowMore} className="Stream-footer-button-5"></button>
          )}
        </div>
      </div>
    </footer>
  </div>
);
}