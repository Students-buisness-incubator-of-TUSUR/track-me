import React, { useState } from 'react';
import { Link } from "react-router-dom";
import './stream-page.css';

export default function Stream() {
  const [isVisible, setIsVisible] = useState(false);
  const [visibleCardsStart, setVisibleCardsStart] = useState(0);
  const [showCheckboxes, setShowCheckboxes] = useState(false); // Состояние для управления видимостью чекбоксов

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
    setShowCheckboxes(!showCheckboxes); // Переключаем видимость чекбоксов
  };

  const cards = Array.from({ length: 20 }, (_, index) => ({
    id: index + 1,
    title: `Карточка ${index + 1}`,
    content: `Содержимое карточки ${index + 1}`,
  }));

  const visibleCards = cards.slice(visibleCardsStart, visibleCardsStart + 9);

  // Пример данных для чекбоксов
  const checkboxesData = Array.from({ length: 9 }, (_, index) => ({
    id: `checkbox-${index + 1}`,
    label: `Чекбокс ${index + 1}`,
  }));

  return (
    <div className="Stream">
      <header className="Stream-header">
        <div className="Stream-header-cont">
          <h1 className="Stream-title">Название</h1>
          <div className="Stream-buttons">
            <button className="Stream-butt">Администраторы</button>
            <button className="Stream-butt">Трекеры</button>
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
          <button className="Stream-butt">+ Создать карточку</button>
        </div>
        {isVisible && (
          <div className="Stream-header-afterclick-cont">
            <div className="Stream-header-afterclick-left">
              <div className="Stream-header-afterclick-left-up">
                <button className="Stream-header-chose-butt">Год [0]</button>
                <button className="Stream-header-chose-butt">Рынок [0]</button>
              </div>
              <div className="Stream-header-chosefrom-cont">
                <div className="Stream-header-chosefrom-butt">
                  <div className="Stream-header-chosefrom-butt-cont" onClick={handleShowCheckboxes}>
                    <b className="Stream-header-chosefrom-butt-label">Год</b>
                    <div className="Stream-header-chosefrom-butt-pic"></div>
                  </div>
                  {showCheckboxes && (
                    <div className="Stream-header-checkboxes">
                      {checkboxesData.map((checkbox, index) => (
                        <div key={checkbox.id} className={`Stream-header-checkbox ${index < 5 ? 'first-row' : 'second-row'}`}>
                          <input type="checkbox" id={checkbox.id} />
                          <label htmlFor={checkbox.id}>{checkbox.label}</label>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
                <div className="Stream-header-chosefrom-butt2">
                  <div className="Stream-header-chosefrom-butt-cont">
                    <b className="Stream-header-chosefrom-butt-label">Рынок</b>
                    <div className="Stream-header-chosefrom-butt-pic"></div>
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
            {visibleCardsStart + 9 < cards.length && (
              <button onClick={handleShowMore} className="Stream-footer-button-4"></button>
            )}
          </div>
          <div className="Stream-footer-p-butt-5">
            {visibleCardsStart + 9 < cards.length && (
              <button onClick={handleShowMore} className="Stream-footer-button-5"></button>
            )}
          </div>
        </div>
      </footer>
    </div>
  );
}