import React, { useState } from 'react'; // Импортируем useState

export default function Stream() {
  // Состояние для управления видимостью элементов
  const [isVisible, setIsVisible] = useState(false);

  // Функция для обработки клика
  const handleClick = () => {
    setIsVisible(!isVisible); // Переключаем состояние
  };

  return (
    <div className="Stream">
      <header className="Stream-header">
        <div className="Stream-header-cont">
          <h1 className="Stream-title">Название</h1>
          <div className="Stream-buttons">
            <button className="Stream-butt">Администраторы</button>
            <button className="Stream-butt">Трекеры</button>
            <button className="Stream-butt">Все команды</button>
            <div className="Stream-pic"></div>
          </div>
        </div>
        <div className="Stream-header-bottom-cont">
          <div className="Stream-search-cont">
            {/* Кнопка с обработчиком onClick */}
            <button onClick={handleClick} className="Stream-settings-pic">
            </button>
            <div className="Stream-search-contcont">
              <button className="Stream-settings-pic2"> </button>
              <input type="search" placeholder="Найти" className="Stream-search" />
            </div>
          </div>
          <button className="Stream-butt">+ Создать карточку</button>
        </div>
        {/* Элементы, которые будут появляться и скрываться */}
        {isVisible && (
          <div className="Stream-header-afterclick-cont">
            <div className="Stream-header-afterclick-left">
              <div className="Stream-header-afterclick-left-up">
                <button className="Stream-header-chose-butt">Год [0]</button>
                <button className="Stream-header-chose-butt">Рынок [0]</button>
              </div>
              <div className="">
                <button className="Stream-header-chosefrom-butt">Год</button>
                <button className="Stream-header-chosefrom-butt">Рынок НТИ</button>
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
        <div className="Stream-card">
          <div className="Stream-card-pic"></div>
          <h1 className="Stream-card-headText">asdas</h1>
          <div className="Stream-card-bodyText">xasx</div>
        </div>
      </main>
      <footer className="Stream-footer">
        <div className="Stream-footer-butts">
          <button className="Stream-footer-button-1"></button>
          <div className="Stream-footer-p-butts">
            <button className="Stream-footer-button-2"></button>
            <button className="Stream-footer-button-3"></button>
            <button className="Stream-footer-button-4"></button>
          </div>
          <button className="Stream-footer-button-5"></button>
        </div>
      </footer>
    </div>
  );
}