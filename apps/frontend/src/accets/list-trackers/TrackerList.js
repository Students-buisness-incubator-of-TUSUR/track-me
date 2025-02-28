import React, { useState, useEffect, useMemo } from "react";
import { Link } from "react-router-dom";
import "./TrackerList.css";
import trueIcon from "./true.png";
import falseIcon from "./false.png";
import editIcon from "./edit.png";
import trueIcon2 from "./true2.png";
import falseIcon2 from "./false2.png";

function TrackerList() {
  const [trackers, setTrackers] = useState([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [visibleTrackersStart, setVisibleTrackersStart] = useState(0);
  const [error, setError] = useState(null);
  const trackersPerPage = 20;
  const [hoveredTracker, setHoveredTracker] = useState(null);
  const [hoveredButton, setHoveredButton] = useState(null);


  // Получаем реальный токен из localStorage или контекста
  const token = localStorage.getItem("accessToken");

  // Если фильтры не изменяются, мемоизируем их
  const filters = useMemo(() => [], []);
  const backendHost = process.env.REACT_APP_BACKEND_HOST || 'http://localhost:8080';
  useEffect(() => {
    fetch(`${backendHost}/api/v1/admin/users/trackers?page=0&size=10`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`,
      },
      body: JSON.stringify({ filters: filters }),
    })
      .then((response) => {
        if (!response.ok) {
          if (response.status === 401) {
            setError("Ошибка авторизации! Пожалуйста, выполните вход заново.");
          } else {
            setError("Ошибка при загрузке трекеров. Статус: " + response.status);
          }
          throw new Error("Ошибка запроса");
        }
        return response.json();
      })
      .then((data) => {
        console.log("Полученные данные:", data);
        if (data && data.content) {
          setTrackers(data.content);
          setVisibleTrackersStart(0);
        } else {
          setError("Неверный формат данных, полученных с сервера (trackers).");
        }
      })
      .catch((err) => {
        console.error("Ошибка при загрузке трекеров:", err);
      });
  }, [token, filters, backendHost]);

  const confirmUser = (userId) => {
  const url = `${backendHost}/api/v1/admin/users/confirm?userId=${userId}`;
  
  fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${token}`,
    }
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error(`Ошибка при подтверждении пользователя: ${response.statusText}`);
      }

      // Try to parse JSON response, handle empty or invalid JSON body
      return response.text() // Read response body as text first
        .then((text) => {
          if (text) {
            try {
              return JSON.parse(text); // Parse as JSON
            } catch (e) {
              throw new Error('Ответ не является валидным JSON');
            }
          } else {
            return {}; // Return empty object if the body is empty
          }
        });
    })
    .then(() => {
      // Update the UI after successful confirmation
      setTrackers((prevTrackers) =>
        prevTrackers.map((tracker) =>
          tracker.id === userId ? { ...tracker, enabled: true } : tracker
        )
      );
    })
    .catch((err) => {
      console.error("Ошибка при подтверждении пользователя:", err);
      setError(`Ошибка при подтверждении пользователя: ${err.message}`);
    });
};

const deleteUser = (userId) => {
  const url = `${backendHost}/api/v1/admin/users/delete?userId=${userId}`;

  fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${token}`,
    },
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error(`Ошибка при удалении пользователя: ${response.statusText}`);
      }
      return response.text(); // Проверяем, есть ли ответ от сервера
    })
    .then(() => {
      // Убираем удалённого пользователя из списка
      setTrackers((prevTrackers) => prevTrackers.filter((tracker) => tracker.id !== userId));
    })
    .catch((err) => {
      console.error("Ошибка при удалении пользователя:", err);
      setError(`Ошибка при удалении пользователя: ${err.message}`);
    });
};

  
  const handleSearchChange = (event) => {
    setSearchQuery(event.target.value);
    setVisibleTrackersStart(0);
  };

  const filteredTrackers = trackers.filter((tracker) =>
    (tracker.fullName || "").toLowerCase().includes(searchQuery.toLowerCase())
  );

  const visibleTrackers = filteredTrackers.slice(
    visibleTrackersStart,
    visibleTrackersStart + trackersPerPage
  );

  const handleShowMore = () => {
    setVisibleTrackersStart((prev) => prev + trackersPerPage);
  };

  const handleShowPrevious = () => {
    setVisibleTrackersStart((prev) => Math.max(prev - trackersPerPage, 0));
  };

  return (
    <div className="tracker-container">
      {/* --- ШАПКА (Header) --- */}
      <header className="Stream-header">
        <div className="Stream-header-cont">
          <h1 className="Stream-title">Название</h1>
          <div className="Stream-buttons">
            <button className="Stream-butt">Администраторы</button>
            <Link to="/list-trackers">
              <button className="Stream-butt">Трекеры</button>
            </Link>
            <button className="Stream-butt">Все команды</button>
            <Link to="/profile" className="Stream-pic"></Link>
          </div>
        </div>
        {/* Поле поиска */}
        <div className="Stream-header-bottom-cont">
          <div className="Stream-search-cont">
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
        </div>
      </header>

      {/* --- ОСНОВНОЕ СОДЕРЖИМОЕ --- */}
      <main className="tracker-list-content">
        {error && <div className="error-message oval2">{error}</div>}
        <div className="tracker-grid">
          {visibleTrackers.length > 0 ? (
            visibleTrackers.map((tracker, index) =>
              tracker.enabled ? (
                <div className="tracker-item-true" key={tracker.id || index}>
                  <div className="tracker-avatar">
                    <span className="green-checkmark" title="Включён">
                      <img src={trueIcon}  alt="Подтвержден" />
                    </span>
                    <div className="tracker-text">
                      <div className="tracker-fio">{tracker.fullName}</div>
                      <div className="tracker-nick">@{tracker.telegramId}</div>
                    </div>
                  </div>
                </div>
              ) : (
                <div
  className="tracker-item-edit"
  key={tracker.id || index}
  onClick={() => setHoveredTracker(tracker.id)}
  onMouseLeave={() => setHoveredTracker(null)}
>
  <div className="tracker-avatar">
    {/* Панель с кнопками, показывается только при наведении */}
    {hoveredTracker === tracker.id && (
       <div className="edit-panel">
       <button 
  className="confirm-button"
  onClick={() => confirmUser(tracker.id)}  // Call the confirmUser function when clicked
  onMouseEnter={() => setHoveredButton("confirm")}
  onMouseLeave={() => setHoveredButton(null)}
>
  <img src={hoveredButton === "cancel" ? trueIcon2 : trueIcon} alt="Подтвердить" />
</button>

       {hoveredButton === "cancel" && <span className="tooltip tooltip-red">Отменить</span>}
   
       <button 
  className="cancel-button"
  onClick={() => deleteUser(tracker.id)} // Вызываем deleteUser при клике
  onMouseEnter={() => setHoveredButton("cancel")}
  onMouseLeave={() => setHoveredButton(null)}
>
  <img src={hoveredButton === "confirm" ? falseIcon2 : falseIcon} alt="Отклонить" />
</button>

       {hoveredButton === "confirm" && <span className="tooltip tooltip-green">Подтвердить</span>}
     </div>
    
    
    )}
    <div className="edit-icon" title="Редактировать">
      <img src={editIcon} alt="Редактировать" />
    </div>
    <div className="tracker-text">
      <div className="tracker-fio">{tracker.fullName}</div>
      <div className="tracker-nick">@{tracker.telegramId}</div>
    </div>
  </div>
</div>
              )
            )
          ) : (
            <p className="error-message oval2">Нет трекеров для отображения</p>
          )}
        </div>
      </main>

      {/* --- ФУТЕР (Footer) с пагинацией --- */}
      {filteredTrackers.length > 0 && (
        <footer className="Stream-footer">
          <div className="Stream-footer-butts">
            <div className="Stream-footer-p-butt-1">
              {visibleTrackersStart > 0 && (
                <button
                  onClick={handleShowPrevious}
                  className="Stream-footer-button-1"
                ></button>
              )}
            </div>
            <div className="Stream-footer-p-butts">
              {visibleTrackersStart > 0 && (
                <button
                  onClick={handleShowPrevious}
                  className="Stream-footer-button-2"
                ></button>
              )}
              <button className="Stream-footer-button-3"></button>
              {visibleTrackersStart + trackersPerPage < filteredTrackers.length && (
                <button
                  onClick={handleShowMore}
                  className="Stream-footer-button-4"
                ></button>
              )}
            </div>
            <div className="Stream-footer-p-butt-5">
              {visibleTrackersStart + trackersPerPage < filteredTrackers.length && (
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

export default TrackerList;
