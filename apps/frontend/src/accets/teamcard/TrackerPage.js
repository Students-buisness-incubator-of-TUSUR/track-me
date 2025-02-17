import React from "react";
import "./TrackerPage.css";

function TrackerPage() {
  return (
    <div className="tracker-container">
      <header className="Stream-header">
        <div className="Stream-header-cont">
          <h1 className="Stream-title">Название</h1>
          <div className="Stream-buttons">
            <div className="Stream-pic"></div>
          </div>
        </div>
        <div className="Stream-header-bottom-cont">
          <div className="Stream-search-cont">
            <button className="Stream-settings-pic"> </button>
            <div className="Stream-search-contcont">
              <button className="Stream-settings-pic2"> </button>
              <input
                type="search"
                placeholder="Найти"
                className="Stream-search"
              />
            </div>
          </div>
          <button className="Stream-butt">+ Создать карточку</button>
        </div>
      </header>
      {/* Обертка карточек */}
      <div className="cards-wrapper">
        {/* Пример одной карточки */}
        <div className="card">
          <div className="card-image" />
          <span className="status-completed">Завершён</span>
          <div className="card-content">
            <div className="text-container project-title">
              <h3>Название проекта</h3>
            </div>
            <div className="text-container project-description">
              <p>
                Этот текст не несет никакого смысла, здесь будет описание проекта.
              </p>
            </div>
            <div className="under-cont">
              <div className="text-container project-markets">
                <p>Рынки НТИ: рынок 1, рынок 2 и тд</p>
              </div>
              <div className="text-container project-trl">
                <p>TRL: TRL 1, TRL 2 и тд</p>
              </div>
              <div className="text-container project-flow">
                <p>Поток: Поток</p>
              </div>
            </div>
          </div>
          <button className="edit-button">Редактировать</button>
        </div>
        {/* Если нужно отобразить несколько карточек, дублируйте блок .card */}
      
      <div className="card">
          <div className="card-image" />
          <span className="status-completed">Завершён</span>
          <div className="card-content">
            <div className="text-container project-title">
              <h3>Название проекта</h3>
            </div>
            <div className="text-container project-description">
              <p>
                Этот текст не несет никакого смысла, здесь будет описание проекта.
              </p>
            </div>
            <div className="under-cont">
              <div className="text-container project-markets">
                <p>Рынки НТИ: рынок 1, рынок 2 и тд</p>
              </div>
              <div className="text-container project-trl">
                <p>TRL: TRL 1, TRL 2 и тд</p>
              </div>
              <div className="text-container project-flow">
                <p>Поток: Поток</p>
              </div>
            </div>
          </div>
          <button className="edit-button">Редактировать</button>
        </div>
        <div className="card">
          <div className="card-image" />
          <span className="status-completed">Завершён</span>
          <div className="card-content">
            <div className="text-container project-title">
              <h3>Название проекта</h3>
            </div>
            <div className="text-container project-description">
              <p>
                Этот текст не несет никакого смысла, здесь будет описание проекта.
              </p>
            </div>
            <div className="under-cont">
              <div className="text-container project-markets">
                <p>Рынки НТИ: рынок 1, рынок 2 и тд</p>
              </div>
              <div className="text-container project-trl">
                <p>TRL: TRL 1, TRL 2 и тд</p>
              </div>
              <div className="text-container project-flow">
                <p>Поток: Поток</p>
              </div>
            </div>
          </div>
          <button className="edit-button">Редактировать</button>
        </div>
        </div>
      
      
        
    </div>
    
    
  );
}

export default TrackerPage;
