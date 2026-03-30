// accets/report/ReportPage.js
import { useState, useEffect, useCallback } from "react";
import "./ReportPage.css";

import IconOpen from "./icon-open.png";
import IconClose from "./icon-close.png";

import { fetchReportExcel, fetchReports, fetchStreams, fetchTrackers } from "../../services/requests";
import { useGetUserInfo } from "../../services/util";

import Header from "../header/header";
import PropTypes from "prop-types";

export default function ReportPage({ defaultIsActive = true }) {
const [reports, setReports] = useState([]);
  const [trackers, setTrackers] = useState([]);
  const [streams, setStreams] = useState([]);
const [page] = useState(0);
const [size] = useState(10000);
const [loading, setLoading] = useState(false);
const [userRole, setUserRole] = useState('');

  // фильтры (заглушки)
  const [isActive, setIsActive] = useState(defaultIsActive);
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
  
  const buildFilters = useCallback(() => {
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
    if (isActive) {
      const todayDate = new Date().toISOString().split('T')[0];
      filters.push({
        fieldName: "streams.startDate",
        type: "LTE",
        value: todayDate,
      }, {
        fieldName: "streams.endDate",
        type: "GTE",
        value: todayDate,
      });
    }
    return filters;
  }, [filterTrackers, filterStreams, isActive]);

  const handleExportExcel = async () => {
    try {
      const response = await fetchReportExcel({ filters: buildFilters() });
      if (!response.ok) throw new Error(`Ошибка HTTP: ${response.status}`);

      const disposition = response.headers.get("Content-Disposition");
      let filename = "отчёт-по-командам.xlsx";
      if (disposition) {
        const match = disposition.match(/filename\*=UTF-8''(.+)/);
        if (match) {
          filename = decodeURIComponent(match[1]);
        }
      }

      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      setTimeout(() => URL.revokeObjectURL(url), 75);
    } catch (error) {
      console.error("Ошибка выгрузки отчёта", error);
    }
  };

  const loadReports = useCallback(async () => {
    try {
      const response = await fetchReports({ page, size, filters: buildFilters() });
      if (!response.ok) {
        throw new Error(`Ошибка HTTP: ${response.status}`);
      }
      const data = await response.json();
      setReports(data.content);
    } catch (error) {
      console.error("Ошибка загрузки отчётов", error);
    }
  // eslint-disable-next-line
  }, [page, size, filterTrackers, filterStreams, isActive]);

  const loadStreams = useCallback(async () => {
    try {
      const response = await fetchStreams({ page: page, size: size });
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
      const response = await fetchTrackers({ page: page, size: size });
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

  const user = useGetUserInfo();
  useEffect(() => {
    setUserRole(user.roles[0]);
  }, [user]);

  return (
    <div className="Report">
      <Header userRole={userRole}></Header>

      {/* Контент */}
      <main className="Report-main">
        <div className="report-header">
          {/* Кнопка выгрузки отчета */}
          <button className="report-btn" onClick={handleExportExcel}>
            Выгрузить отчет
          </button>

          {/* фильтры */}
          <div className="report-filters">
            <button
              data-testid="button-isactive"
              className="report-page_btn-isactive"
              onClick={() => setIsActive((prev) => !prev)}
            >
              <input
                id="isActive"
                type="checkbox"
                checked={!isActive}
                onClick={(e) => {
                  e.stopPropagation();
                  setIsActive((prev) => !prev)
                }}
              />
              <label>Показывать неактивные</label>
            </button>
           <div className="report-dropdown1">
  <button
    data-testid="trackers-btn"
    className={`report-dropdown-btn ${trackerFilterOpen ? 'open' : ''}`}
    onClick={() => setTrackerFilterOpen(!trackerFilterOpen)}
  >
    Трекеры
    <span className="report-dropdown-icon1">
  <img
    src={trackerFilterOpen ? IconClose : IconOpen}
    alt={trackerFilterOpen ? "Закрыто" : "Открыто"}
    className="report-dropdown-icon-img"
  />
</span>

  </button>
  {trackerFilterOpen && (
    <div data-testid="trackers-dropdown-menu" className="report-dropdown-menu">
      <button
        key={0}
        className="report-dropdown-item"
        onClick={() => setFilterTrackers(null)}
      >—</button>
      {trackers.map((t, i) => (
        <button
          key={i}
          className="report-dropdown-item"
          onClick={() => setFilterTrackers(t.username)}
        >{`${t.fullName} (${t.username})`}</button>
      ))}
    </div>
  )}
</div>

<div className="report-dropdown2">
  <button
    className={`report-dropdown-btn ${streamFilterOpen ? 'open' : ''}`}
    onClick={() => setStreamFilterOpen(!streamFilterOpen)}
  >
    Потоки
    <span className="report-dropdown-icon2">
  <img
    src={streamFilterOpen ? IconClose : IconOpen}
    alt={streamFilterOpen ? "Закрыто" : "Открыто"}
    className="report-dropdown-icon-img"
  />
</span>

  </button>
  {streamFilterOpen && (
    <div data-testid="streams-dropdown-menu" className="report-dropdown-menu">
      <button
        key={0}
        className="report-dropdown-item"
        onClick={() => setFilterStreams(null)}
      >—</button>
      {streams.map((t, i) => (
        <button
          key={i}
          className="report-dropdown-item"
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
                <th>Трекшн-митинг (факт/план)</th>
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

        <td>{`${trackers?.filter((tracker) => tracker.username === item.username)[0]?.fullName} (${item.username})`}</td>

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

ReportPage.propTypes = {
  defaultIsActive: PropTypes.bool,
};
