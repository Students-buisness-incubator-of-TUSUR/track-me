import React, { useState, useEffect, useMemo } from "react";
import { Link } from "react-router-dom";
import "./TrackerList.css";
import trueIcon from "./true.png";
import editIcon from "./edit.png";

function TrackerList() {
  const [trackers, setTrackers] = useState([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [visibleTrackersStart, setVisibleTrackersStart] = useState(0);
  const [error, setError] = useState(null);
  const trackersPerPage = 20;

  // Получаем реальный токен из localStorage или контекста
  const token = localStorage.getItem("accessToken");

  // Если фильтры не изменяются, мемоизируем их
  const filters = useMemo(() => [], []);

  useEffect(() => {
    fetch("http://127.0.0.1:8080/api/v1/admin/users/trackers?page=0&size=10", {
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
  }, [token, filters]);

  const handleSearchChange = (event) => {
    setSearchQuery(event.target.value);
    setVisibleTrackersStart(0);
  };

  const filteredTrackers = trackers.filter((tracker) =>
    (tracker.name || "").toLowerCase().includes(searchQuery.toLowerCase())
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
        {error && <div className="error-message">{error}</div>}
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
                      <div className="tracker-nick">{tracker.telegramId}</div>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="tracker-item-edit" key={tracker.id || index}>
                  <div className="tracker-avatar">
                    <div className="edit-icon" title="Редактировать">
                      <img src={editIcon}  alt="Редактировать" />
                    </div>
                    <div className="tracker-text">
                      <div className="tracker-fio">{tracker.fullName}</div>
                      <div className="tracker-nick">{tracker.telegramId}</div>
                    </div>
                  </div>
                </div>
              )
            )
          ) : (
            <p>Нет трекеров для отображения</p>
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
