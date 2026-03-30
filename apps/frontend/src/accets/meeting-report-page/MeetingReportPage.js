import { useState, useEffect, useCallback, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import "./MeetingReportPage.css";

import IconOpen from "./icon-open.png";
import IconClose from "./icon-close.png";

import { fetchMeetingReport, fetchMeetingReportExcel } from "../../services/requests";
import { useGetUserInfo } from "../../services/util";

import Header from "../header/header";

const COMBINED_STATUS_OPTIONS = {
  OK: { label: "Всё ок", isTeamStatus: true },
  WITH_ISSUES: { label: "Есть проблемы", isTeamStatus: true },
  MANY_ISSUES: { label: "Есть большие проблемы", isTeamStatus: true },
  SCHEDULED: { label: "Запланирована", isTeamStatus: false },
  COMPLETED_AS_NOT_HAPPENED: { label: "Не состоялась", isTeamStatus: false },
};

export default function MeetingReportPage() {
  const { streamId } = useParams();
  const navigate = useNavigate();
  const isFirstRun = useRef(true);

  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(false);
  const [userRole, setUserRole] = useState("");

  const [availableTrackers, setAvailableTrackers] = useState([]);
  const [availableTeams, setAvailableTeams] = useState([]);

  const [trackerFilterOpen, setTrackerFilterOpen] = useState(false);
  const [teamFilterOpen, setTeamFilterOpen] = useState(false);
  const [statusFilterOpen, setStatusFilterOpen] = useState(false);

  const [filterTracker, _setFilterTracker] = useState(null);
  const [filterTeam, _setFilterTeam] = useState(null);
  const [filterStatus, _setFilterStatus] = useState(null);

  const [teamNameDir, setTeamNameDir] = useState("asc");
  const [secondarySort, setSecondarySort] = useState({ field: "startDate", direction: "desc" });

  const setFilterTracker = (v) => { _setFilterTracker(v); setTrackerFilterOpen(false); };
  const setFilterTeam = (v) => { _setFilterTeam(v); setTeamFilterOpen(false); };
  const setFilterStatus = (v) => { _setFilterStatus(v); setStatusFilterOpen(false); };

  const requestSort = (field) => {
    if (field === "teamName") {
      setTeamNameDir(prev => prev === "asc" ? "desc" : "asc");
    } else {
      setSecondarySort(prev => ({
        field: field,
        direction: prev.field === field && prev.direction === "asc" ? "desc" : "asc"
      }));
    }
  };

  const getEffectiveSortParams = useCallback(() => {
    const params = [`teamName,${teamNameDir}`];
    if (secondarySort.field) {
      params.push(`${secondarySort.field},${secondarySort.direction}`);
    }
    return params;
  }, [teamNameDir, secondarySort]);

  const buildFilters = useCallback(() => {
    const filters = [];
    if (filterTracker) {
      filters.push({ fieldName: "trackerFullName", type: "EQ", value: filterTracker.fullName });
    }
    if (filterTeam) {
      filters.push({ fieldName: "teamName", type: "EQ", value: filterTeam });
    }
    if (filterStatus) {
      const config = COMBINED_STATUS_OPTIONS[filterStatus];
      if (config.isTeamStatus) {
        filters.push({ fieldName: "teamStatus", type: "EQ", value: filterStatus });
        filters.push({ fieldName: "status", type: "EQ", value: "COMPLETED" });
      } else {
        filters.push({ fieldName: "status", type: "EQ", value: filterStatus });
      }
    }
    return filters;
  }, [filterTracker, filterTeam, filterStatus]);

  const loadReports = useCallback(async (isInitialLoad = false) => {
    try {
      const response = await fetchMeetingReport({
        streamId,
        filters: buildFilters(),
        page: 0,
        size: 10000,
        sort: getEffectiveSortParams(),
      });
      if (!response.ok) throw new Error(`Ошибка HTTP: ${response.status}`);
      const data = await response.json();
      setReports(data.content);

      if (isInitialLoad) {
        const trackersMap = new Map();
        data.content.forEach(item => {
          const username = item.trackerName; 
          if (username) {
            if (!trackersMap.has(username)) {
              trackersMap.set(username, {
                fullName: item.trackerFullName || username,
                username: username
              });
            }
          }
        });
        const sortedTrackers = Array.from(trackersMap.values()).sort((a, b) => a.fullName.localeCompare(b.fullName));
        const teams = [...new Set(data.content.map(i => i.teamName))].filter(Boolean).sort();
        setAvailableTrackers(sortedTrackers);
        setAvailableTeams(teams);
      }
    } catch (error) {
      console.error("Ошибка загрузки", error);
    }
  }, [streamId, buildFilters, getEffectiveSortParams]);

  useEffect(() => {
    if (isFirstRun.current) {
      setLoading(true);
      loadReports(true).finally(() => {
        setLoading(false);
        isFirstRun.current = false;
      });
    } else {
      loadReports(false);
    }
  }, [loadReports]);

  const user = useGetUserInfo();
  useEffect(() => {
    if (user && user.roles) setUserRole(user.roles[0]);
  }, [user]);

  const handleExportExcel = async () => {
    try {
      const response = await fetchMeetingReportExcel({ 
        streamId, 
        filters: buildFilters(),
        sort: getEffectiveSortParams() 
      });
      if (!response.ok) throw new Error(`Ошибка HTTP: ${response.status}`);
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `отчёт-по-встречам.xlsx`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      console.error("Ошибка выгрузки отчёта", error);
    }
  };

  return (
    <div className="mrep-page">
      <Header userRole={userRole} />
      <main className="mrep-main">
        <div className="mrep-header">
          <button className="mrep-btn-back" onClick={() => navigate(-1)}>← Назад</button>
          <div className="mrep-filters-container">
            <div className="mrep-dropdown">
              <button className={`mrep-dropdown-btn ${teamFilterOpen ? "open" : ""}`} onClick={() => { setTeamFilterOpen(!teamFilterOpen); setTrackerFilterOpen(false); setStatusFilterOpen(false); }}>
                {filterTeam || "Команды"}
                <img src={teamFilterOpen ? IconClose : IconOpen} alt="" className="mrep-dropdown-arrow" />
              </button>
              {teamFilterOpen && (
                <div className="mrep-dropdown-menu">
                  <button className="mrep-dropdown-item" onClick={() => setFilterTeam(null)}>— Все —</button>
                  {availableTeams.map((name, i) => (
                    <button key={i} className="mrep-dropdown-item" onClick={() => setFilterTeam(name)}>{name}</button>
                  ))}
                </div>
              )}
            </div>
            <div className="mrep-dropdown">
              <button className={`mrep-dropdown-btn ${trackerFilterOpen ? "open" : ""}`} onClick={() => { setTrackerFilterOpen(!trackerFilterOpen); setTeamFilterOpen(false); setStatusFilterOpen(false); }}>
                {filterTracker ? filterTracker.fullName : "Трекеры"}
                <img src={trackerFilterOpen ? IconClose : IconOpen} alt="" className="mrep-dropdown-arrow" />
              </button>
              {trackerFilterOpen && (
                <div className="mrep-dropdown-menu">
                  <button className="mrep-dropdown-item" onClick={() => setFilterTracker(null)}>— Все —</button>
                  {availableTrackers.map((t, i) => (
                    <button key={i} className="mrep-dropdown-item" onClick={() => setFilterTracker(t)}>{`${t.fullName} (@${t.username})`}</button>
                  ))}
                </div>
              )}
            </div>
            <div className="mrep-dropdown">
              <button className={`mrep-dropdown-btn ${statusFilterOpen ? "open" : ""}`} onClick={() => { setStatusFilterOpen(!statusFilterOpen); setTeamFilterOpen(false); setTrackerFilterOpen(false); }}>
                {filterStatus ? COMBINED_STATUS_OPTIONS[filterStatus].label : "Статус"}
                <img src={statusFilterOpen ? IconClose : IconOpen} alt="" className="mrep-dropdown-arrow" />
              </button>
              {statusFilterOpen && (
                <div className="mrep-dropdown-menu">
                  <button className="mrep-dropdown-item" onClick={() => setFilterStatus(null)}>— Все —</button>
                  {Object.entries(COMBINED_STATUS_OPTIONS).map(([key, opt]) => (
                    <button key={key} className="mrep-dropdown-item" onClick={() => setFilterStatus(key)}>{opt.label}</button>
                  ))}
                </div>
              )}
            </div>
          </div>
          <button className="mrep-btn-export" onClick={handleExportExcel}>Выгрузить отчет</button>
        </div>
        <div className="mrep-table-container">
          <table className="mrep-table">
            <thead>
              <tr>
                <th>№</th>
                <th onClick={() => requestSort("teamName")} className="mrep-th-sortable">
                  Название команды <span className="mrep-icon-active">{teamNameDir === "asc" ? "↑" : "↓"}</span>
                </th>
                <th onClick={() => requestSort("startDate")} className="mrep-th-sortable">
                  Дата встречи {secondarySort.field === "startDate" ? <span className="mrep-icon-active">{secondarySort.direction === "asc" ? "↑" : "↓"}</span> : <span className="mrep-icon-inactive">↕</span>}
                </th>
                <th>Трекер</th>
                <th>Задачи к следующей встрече</th>
                <th>Выполнение задач / инфо по команде</th>
                <th onClick={() => requestSort("teamStatusValue")} className="mrep-th-sortable">
                  Статус команды {secondarySort.field === "teamStatusValue" ? <span className="mrep-icon-active">{secondarySort.direction === "asc" ? "↑" : "↓"}</span> : <span className="mrep-icon-inactive">↕</span>}
                </th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan="7">Загрузка...</td></tr>
              ) : reports.length === 0 ? (
                <tr><td colSpan="7">Нет данных</td></tr>
              ) : (
                reports.map((item, index) => {
                  const isScheduled = item.status === "SCHEDULED";
                  const isNotHappened = item.status === "COMPLETED_AS_NOT_HAPPENED";
                  const isFirstInGroup = index === 0 || reports[index - 1].teamName !== item.teamName;
                  const isLastInGroup = index === reports.length - 1 || reports[index + 1].teamName !== item.teamName;
                  let rowClass = isNotHappened ? "mrep-row-not-happened" : "";
                  if (isFirstInGroup) rowClass += " mrep-group-start";
                  if (isLastInGroup) rowClass += " mrep-group-end";
                  let displayStatus = "—";
                  let statusCellClass = ""; 
                  if (isScheduled) {
                    const teamStatusLabel = item.teamStatus ? COMBINED_STATUS_OPTIONS[item.teamStatus]?.label : null;
                    displayStatus = teamStatusLabel ? `Запланирована (${teamStatusLabel})` : "Запланирована";
                    statusCellClass = "mrep-status-lavender"; 
                  } else if (isNotHappened) {
                    displayStatus = "Не состоялась";
                  } else if (item.teamStatus) {
                    displayStatus = COMBINED_STATUS_OPTIONS[item.teamStatus]?.label || "—";
                    if (item.teamStatus === "OK") statusCellClass = "mrep-status-green";
                    if (item.teamStatus === "WITH_ISSUES") statusCellClass = "mrep-status-yellow";
                    if (item.teamStatus === "MANY_ISSUES") statusCellClass = "mrep-status-red";
                  }
                  return (
                    <tr key={index} className={rowClass}>
                      <td className="mrep-cell-left">{index + 1}</td>
                      <td>{item.teamName}</td>
                      <td>{item.startDate ? new Date(item.startDate).toLocaleDateString("ru-RU") : "—"}</td>
                      <td>{item.trackerFullName || item.trackerName || "—"}</td>
                      <td className="mrep-text-wrap">{isScheduled || isNotHappened ? "—" : item.tasksNextMeeting || "—"}</td>
                      <td className="mrep-text-wrap">{isScheduled || isNotHappened ? "—" : item.tasksCurrentMeeting || "—"}</td>
                      <td className={`${statusCellClass} mrep-cell-right`}>{displayStatus}</td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </main>
    </div>
  );
}