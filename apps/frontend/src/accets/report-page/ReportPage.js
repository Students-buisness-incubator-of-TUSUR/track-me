// accets/report/ReportPage.js
import React, { useState, useEffect, useCallback } from "react";

import { Link, useNavigate } from "react-router-dom";
import ProfileIcon from "../stream-page/personal_account_1.png";
import "./ReportPage.css";
import IconOpen from "./icon-open.png";
import IconClose from "./icon-close.png";
import MobileHeader from "../adaptive-accets/MobileHeader";
import { fetchReports, fetchStreams, fetchTrackers } from "../../services/requests";
export default function ReportPage() {
  const navigate = useNavigate();
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
const [reports, setReports] = useState([]);
  const [trackers, setTrackers] = useState([]);
  const [streams, setStreams] = useState([]);
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
  const [filterTrackers, _setFilterTrackers] = useState(null);
  const [filterStreams, _setFilterStreams] = useState(null);
  const setFilterTrackers = (newTracker) => {
    _setFilterTrackers(newTracker);
    setTrackerFilterOpen(false);
  }
  const setFilterStreams = (newStreams) => {
    _setFilterStreams(newStreams);
    setStreamFilterOpen(false);
  }
  
  const loadReports = useCallback(async () => {
    try {
      const filters = [];
      if (filterTrackers) filters.push({
          fieldName: "username",
          type: "EQ",
          value: filterTrackers,
        });
      if (filterStreams) filters.push({
          fieldName: "streams.name",
          type: "EQ",
          value: filterStreams,
        });
      const response = await fetchReports(page, size, filters);
      if (!response.ok) {
        throw new Error(`Ошибка HTTP: ${response.status}`);
      }
      const data = await response.json();
      setReports(data.content);
    } catch (error) {
      console.error("Ошибка загрузки отчётов", error);
    }
  }, [page, size, filterTrackers, filterStreams]);

  const loadStreams = useCallback(async () => {
    try {
      const response = await fetchStreams(page, size);
      if (!response.ok) {
        throw new Error(`Ошибка HTTP: ${response.status}`);
      }
      const data = await response.json();
      setStreams(data.content);
    } catch (error) {
      console.error("Ошибка загрузки отчётов", error);
    }
  }, [page, size]);

  const loadTrackers = useCallback(async () => {
    try {
      const response = await fetchTrackers(page, size);
      if (!response.ok) {
        throw new Error(`Ошибка HTTP: ${response.status}`);
      }
      const data = await response.json();
      setTrackers(data.content);
    } catch (error) {
      console.error("Ошибка загрузки отчётов", error);
    }
  }, [page, size]);

  useEffect(() => {
    setLoading(true);
    Promise.all([loadReports(), loadStreams(), loadTrackers()]).finally(() => 
      setLoading(false)
    );
  }, [loadReports, loadStreams, loadTrackers]);

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
    <div data-testid="trackers-dropdown-menu" className="dropdown-menu">
      <button
        key={0}
        className="dropdown-item"
        onClick={() => setFilterTrackers(null)}
      >—</button>
      {trackers.map((t, i) => (
        <button
          key={i}
          className="dropdown-item"
          onClick={() => setFilterTrackers(t.username)}
        >{`${t.fullName} (${t.username})`}</button>
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
    <div data-testid="streams-dropdown-menu" className="dropdown-menu">
      <button
        key={0}
        className="dropdown-item"
        onClick={() => setFilterStreams(null)}
      >—</button>
      {streams.map((t, i) => (
        <button
          key={i}
          className="dropdown-item"
          onClick={() => setFilterStreams(t.name)}
        >{t.name}</button>
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
