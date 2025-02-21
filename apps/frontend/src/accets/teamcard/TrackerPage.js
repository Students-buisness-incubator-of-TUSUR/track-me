import React, { useState, useEffect } from "react";
import "./TrackerPage.css";
import { Link, useNavigate } from "react-router-dom";

function TrackerPage() {
  const [cards, setCards] = useState([]);
  const [visibleCardsStart, setVisibleCardsStart] = useState(0);
  const [streamName, setStreamName] = useState("");
  const [streamNTI, setNTIName] = useState("");
  const [streamreadinessLevel, setLevel] = useState("");
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem("accessToken");
    if (!token) {
      setError("Отсутствует токен авторизации. Пожалуйста, выполните вход.");
      // Если нужно — сразу делаем редирект:
      // navigate("/login");
      return;
    }

    // Запрос на список карточек
    fetch("http://127.0.0.1:8080/api/v1/team-cards?page=0&size=150", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ filters: [] }),
    })
      .then((response) => {
        if (!response.ok) {
          if (response.status === 401) {
            setError("Ошибка авторизации! Пожалуйста, выполните вход заново.");
          } else {
            setError("Ошибка при загрузке карточек. Статус: " + response.status);
          }
          throw new Error("Ошибка запроса");
        }
        return response.json();
      })
      .then((data) => {
        if (data && data.content) {
          setCards(data.content);
        } else {
          setError("Неверный формат данных, полученных с сервера (team-cards).");
        }
      })
      .catch((err) => {
        console.error("Ошибка при загрузке карточек:", err);
      });

    // Запрос на текущий поток
    fetch("http://127.0.0.1:8080/api/v1/streams/current", {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })
      .then((response) => {
        if (!response.ok) {
          if (response.status === 401) {
            setError(
              "Ошибка авторизации при получении потока! Выполните вход заново."
            );
          } else {
            setError("Ошибка при загрузке потока. Статус: " + response.status);
          }
          throw new Error("Ошибка запроса потока");
        }
        return response.json();
      })
      .then((data) => {
        if (data && data.name) {
          setStreamName(data.name);
          setNTIName(data.ntiMarkets);
          setLevel(data.readinessLevel);
        } else {
          setError("Неверный формат данных, полученных с сервера (streams).");
        }
      })
      .catch((err) => {
        console.error("Ошибка при загрузке потока:", err);
      });
  }, [navigate]);

  // Фильтрация карточек по названию (поисковой запрос)
  const filteredCards = cards.filter((card) =>
    card.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Обновляем пагинацию для отфильтрованных карточек
  const visibleCards = filteredCards.slice(visibleCardsStart, visibleCardsStart + 9);

  const handleShowMore = () => {
    setVisibleCardsStart((prev) => prev + 9);
  };

  const handleShowPrevious = () => {
    setVisibleCardsStart((prev) => Math.max(prev - 9, 0));
  };

  // При изменении поискового запроса сбрасываем пагинацию
  const handleSearchChange = (e) => {
    setSearchQuery(e.target.value);
    setVisibleCardsStart(0);
  };

  return (
    <div className="tracker-container">
      <header className="Stream-header">
        <div className="Stream-header-cont">
          <h1 className="Stream-title">
            {streamName ? streamName : "Название потока не получено"}
          </h1>
          <div className="Stream-buttons">
            <Link to="/profile" className="Stream-pic"></Link>
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
                value={searchQuery}
                onChange={handleSearchChange}
              />
            </div>
          </div>
          <button className="Stream-butt">+ Создать карточку</button>
        </div>
      </header>

      <div className="cards-wrapper">
        {error ? (
          <p className="error-message">{error}</p>
        ) : filteredCards.length > 0 ? (
          visibleCards.map((card) => (
            <div className="card" key={card.id}>
              <div className="card-image" />
              <span className="status-completed">Завершено</span>
              <div className="card-content">
                <div className="text-container project-title">
                  <h3>{card.name}</h3>
                </div>
                <div className="text-container project-description">
                  <p>{card.description}</p>
                </div>
                <div className="under-cont">
                  <div className="text-container project-markets">
                    <p>
                      Рынки НТИ:{" "}
                      {Array.isArray(streamNTI) && streamNTI.length > 0
                        ? streamNTI
                            .map(
                              (market) =>
                                market.name || "Неизвестное название"
                            )
                            .join(", ")
                        : "Неизвестен"}
                    </p>
                  </div>
                  <div className="text-container project-trl">
                    <p>TRL: {streamreadinessLevel || "Неизвестен"}</p>
                  </div>
                  <div className="text-container project-flow">
                    <p>Поток: {streamName || "Неизвестен"}</p>
                  </div>
                </div>
              </div>
              <button className="edit-button">Редактировать</button>
            </div>
          ))
        ) : (
          <p>{searchQuery ? "Ничего не найдено по запросу" : "Загрузка карточек..."}</p>
        )}
      </div>

      {filteredCards.length > 0 && (
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
              {visibleCardsStart + 9 < filteredCards.length && (
                <button onClick={handleShowMore} className="Stream-footer-button-4"></button>
              )}
            </div>
            <div className="Stream-footer-p-butt-5">
              {visibleCardsStart + 9 < filteredCards.length && (
                <button onClick={handleShowMore} className="Stream-footer-button-5"></button>
              )}
            </div>
          </div>
        </footer>
      )}
    </div>
  );
}

export default TrackerPage;
