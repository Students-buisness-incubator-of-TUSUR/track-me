// accets/report/ReportPage.js
import React, { useState, useEffect, useCallback } from "react";

import { getCsrfConfigForFetch } from "../../utils/csrf-utils";
import { Link, useNavigate } from "react-router-dom";
import ProfileIcon from "../stream-page/personal_account_1.png";
import "./ReportPage.css";
import IconOpen from "./icon-open.png";
import IconClose from "./icon-close.png";
import MobileHeader from "../adaptive-accets/MobileHeader";
export default function ReportPage() {
  const navigate = useNavigate();
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
const [reports, setReports] = useState([]);
const [page] = useState(0);
const [size] = useState(10);
const [loading, setLoading] = useState(false);

  const toggleProfileMenu = () => setIsProfileMenuOpen(prev => !prev);

  const handleLogout = () => {
    localStorage.clear();
    window.location.href = "/";
  };

  // фильтры (заглушки)
  const [trackerFilterOpen, setTrackerFilterOpen] = useState(false);
  const [streamFilterOpen, setStreamFilterOpen] = useState(false);
  
  const loadReports = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetch(
        `https://api.trackme.test.startup-poligon.com/backend/api/v1/team-cards/reports?page=${page}&size=${size}`,
        {
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer `,
            ...getCsrfConfigForFetch(),
          },
        }
      );

      const data = await response.json();
      setReports(data.content);
    } catch (error) {
      console.error("Ошибка загрузки отчётов", error);
    } finally {
      setLoading(false);
    }
  }, [page, size]);
  useEffect(() => {
    loadReports();
  }, [loadReports]);



  const dummyTrackers = [
    "Александров Александр Александрович",
    "Иванов Иван Иванович",
    "Петров Пётр Петрович",
    "Сидоров Сидор Сидорович",
    "Александров Александр Александрович",
    "Иванов Иван Иванович",
    "Петров Пётр Петрович",
    "Сидоров Сидор Сидорович",
    "Александров Александр Александрович",
    "Иванов Иван Иванович",
    "Петров Пётр Петрович",
    "Сидоров Сидор Сидорович",
  ];

  // заглушка потоков как на картинке
  const dummyStreams = [
    "Поток называется вот так",
    "Поток называется вот так",
    "Поток называется вот так",
    "Поток называется вот так",
    "Поток называется вот так",
    "Поток называется вот так",
    "Поток называется вот так",
    "Поток называется вот так",
    "Поток называется вот так",
    "Поток называется вот так",
    "Поток называется вот так",
    "Поток называется вот так",
    "Поток называется вот так",
    "Поток называется вот так",
    "Поток называется вот так",
    "Поток называется вот так",
    "Поток называется вот так",
    "Поток называется вот так",
  ];

  return (
    <div className="Report">
      <MobileHeader onNavigate={navigate} />
      {/* Хеддер */}
      <header className="Stream-header">
        <div className="Stream-header-cont">
          <div
  className="Stream-header-logo"
  onClick={() => navigate("/streams")}
  onKeyDown={(e) => {
    if (e.key === "Enter" || e.key === " ") {
      navigate("/streams");
    }
  }}
  tabIndex={0}
  role="button"
  style={{ cursor: "pointer" }}
/>

<h1
  className="Stream-title"
  onClick={() => navigate("/streams")}
  onKeyDown={(e) => {
    if (e.key === "Enter" || e.key === " ") {
      navigate("/streams");
    }
  }}
  tabIndex={0}
  role="button"
  style={{ cursor: "pointer" }}
>
  TrackMe
</h1>


          <div className="Stream-buttons">
            <button className="Stream-pic" onClick={toggleProfileMenu}>
              <img src={ProfileIcon} alt="Профиль" className="Stream-pic-img" />
            </button>
            {isProfileMenuOpen && (
              <div className="ProfileDropdown">
                <Link to="/profile" className="ProfileDropdown-item">Личный кабинет</Link>
                <Link onClick={handleLogout} to="/" className="ProfileDropdown-item logout">Выход</Link>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Контент */}
      <main className="Report-main">
        <div className="report-header">
          <button className="report-btn">Выгрузить отчет</button>

          {/* фильтры */}
          <div className="report-filters">
           <div className="dropdown1">
  <button
    className={`dropdown-btn ${trackerFilterOpen ? 'open' : ''}`}
    onClick={() => setTrackerFilterOpen(!trackerFilterOpen)}
  >
    Трекеры
    <span className="dropdown-icon1">
  <img
    src={trackerFilterOpen ? IconClose : IconOpen}
    alt={trackerFilterOpen ? "Закрыто" : "Открыто"}
    className="dropdown-icon-img"
  />
</span>

  </button>
  {trackerFilterOpen && (
    <div className="dropdown-menu">
      {dummyTrackers.map((t, i) => (
        <div key={i} className="dropdown-item">{t}</div>
      ))}
    </div>
  )}
</div>

<div className="dropdown2">
  <button
    className={`dropdown-btn ${streamFilterOpen ? 'open' : ''}`}
    onClick={() => setStreamFilterOpen(!streamFilterOpen)}
  >
    Потоки
    <span className="dropdown-icon2">
  <img
    src={streamFilterOpen ? IconClose : IconOpen}
    alt={streamFilterOpen ? "Закрыто" : "Открыто"}
    className="dropdown-icon-img"
  />
</span>

  </button>
  {streamFilterOpen && (
    <div className="dropdown-menu">
      {dummyStreams.map((s, i) => (
        <div key={i} className="dropdown-item">{s}</div>
      ))}
    </div>
  )}
</div>

          </div>
        </div>

        {/* таблица */}
        <div className="report-table-container">
          <table className="report-table">
            <thead>
              <tr>
                <th>№</th>
                <th>Название потока</th>
                <th>Сроки потока</th>
                <th>Название команды</th>
                <th>Имя трекера</th>
                <th>Средняя оценка команды</th>
                <th>Средняя оценка трекера</th>
                <th>Трекшн-митинг (план/факт)</th>
                <th>Рынки НТИ</th>
                <th>Уровень TRL</th>
              </tr>
            </thead>
            <tbody>
  {loading && (
    <tr>
      <td colSpan="10">Загрузка...</td>
    </tr>
  )}

  {!loading && reports.length === 0 && (
    <tr>
      <td colSpan="10">Нет данных</td>
    </tr>
  )}

  {!loading &&
    reports.map((item, index) => (
      <tr key={index}>
        <td>{index + 1 + page * size}</td>

        <td>{item.streamName}</td>

        <td>
          {item.startDate} – {item.endDate}
        </td>

        <td>{item.teamCardName}</td>

        <td>{item.username}</td>

        <td>{item.averageTeamGrade ?? "—"}</td>

        <td>{item.averageUserGrade ?? "—"}</td>

        <td>
          {item.meetingsCountFact}/{item.meetingsCountPlan}
        </td>

        <td>{item.ntiMarkets.join(", ")}</td>

        <td>{item.readinessLevel}</td>
      </tr>
    ))}
</tbody>

          </table>
        </div>
      </main>
    </div>
  );
}
