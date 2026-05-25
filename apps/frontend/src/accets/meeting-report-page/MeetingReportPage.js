import { useState, useEffect, useCallback, useRef, useMemo } from "react";
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

const getStatusInfo = (item) => {
  if (item.status === "SCHEDULED") {
    const teamStatusLabel = item.teamStatus ? COMBINED_STATUS_OPTIONS[item.teamStatus]?.label : null;
    return {
      text: teamStatusLabel ? `Запланирована (${teamStatusLabel})` : "Запланирована",
      className: "mrep-status-lavender"
    };
  }
  
  if (item.status === "COMPLETED_AS_NOT_HAPPENED") {
    return { text: "Не состоялась", className: "" };
  }

  const statusMap = {
    OK: "mrep-status-green",
    WITH_ISSUES: "mrep-status-yellow",
    MANY_ISSUES: "mrep-status-red"
  };

  return {
    text: COMBINED_STATUS_OPTIONS[item.teamStatus]?.label || "—",
    className: statusMap[item.teamStatus] || ""
  };
};

export default function MeetingReportPage() {
  const { streamId } = useParams();
  const navigate = useNavigate();
  const isFirstRun = useRef(true);

  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(false);
  const [availableData, setAvailableData] = useState({ trackers: [], teams: [] });
  const [openMenu, setOpenMenu] = useState({ tracker: false, team: false, status: false });
  const [filters, setFilters] = useState({ tracker: null, team: null, status: null });
  const [sortConfig, setSortConfig] = useState({ 
    teamNameDir: "asc", 
    secondary: { field: "startDate", direction: "desc" } 
  });

  const user = useGetUserInfo();
  const userRole = user?.roles?.[0] || "";

  const updateFilter = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
    setOpenMenu({ tracker: false, team: false, status: false });
  };

  const toggleMenu = (key) => {
    setOpenMenu(prev => ({ tracker: false, team: false, status: false, [key]: !prev[key] }));
  };

  const requestSort = (field) => {
    if (field === "teamName") {
      setSortConfig(prev => ({ ...prev, teamNameDir: prev.teamNameDir === "asc" ? "desc" : "asc" }));
    } else {
      setSortConfig(prev => ({
        ...prev,
        secondary: { field, direction: prev.secondary.field === field && prev.secondary.direction === "asc" ? "desc" : "asc" }
      }));
    }
  };

  const effectiveSortParams = useMemo(() => {
    const params = [`teamName,${sortConfig.teamNameDir}`];
    if (sortConfig.secondary.field) {
      params.push(`${sortConfig.secondary.field},${sortConfig.secondary.direction}`);
    }
    return params;
  }, [sortConfig]);

  const apiFilters = useMemo(() => {
    const result = [];
    if (filters.tracker) result.push({ fieldName: "trackerFullName", type: "EQ", value: filters.tracker.fullName });
    if (filters.team) result.push({ fieldName: "teamName", type: "EQ", value: filters.team });
    if (filters.status) {
      const config = COMBINED_STATUS_OPTIONS[filters.status];
      if (config.isTeamStatus) {
        result.push(
          { fieldName: "teamStatus", type: "EQ", value: filters.status },
          { fieldName: "status", type: "EQ", value: "COMPLETED" }
        );
      } else {
        result.push({ fieldName: "status", type: "EQ", value: filters.status });
      }
    }
    return result;
  }, [filters]);

  const loadReports = useCallback(async (isInitial = false) => {
    try {
      const response = await fetchMeetingReport({
        streamId, filters: apiFilters, page: 0, size: 10000, sort: effectiveSortParams,
      });
      if (!response.ok) throw new Error(`Ошибка HTTP: ${response.status}`);
      const data = await response.json();
      setReports(data.content);

      if (isInitial) {
        const trackersMap = new Map();
        data.content.forEach(item => {
          if (item.trackerName && !trackersMap.has(item.trackerName)) {
            trackersMap.set(item.trackerName, {
              fullName: item.trackerFullName || item.trackerName,
              username: item.trackerName
            });
          }
        });
        setAvailableData({
          trackers: Array.from(trackersMap.values()).sort((a, b) => a.fullName.localeCompare(b.fullName)),
          teams: [...new Set(data.content.map(i => i.teamName))]
            .filter(Boolean)
            .sort((a, b) => a.localeCompare(b, 'ru', { numeric: true }))
        });
      }
    } catch (error) {
      console.error(error);
    }
  }, [streamId, apiFilters, effectiveSortParams]);

  useEffect(() => {
    setLoading(true);
    loadReports(isFirstRun.current).finally(() => {
      setLoading(false);
      isFirstRun.current = false;
    });
  }, [loadReports]);

  const handleExportExcel = async () => {
    try {
      const response = await fetchMeetingReportExcel({ streamId, filters: apiFilters, sort: effectiveSortParams });
      if (!response.ok) throw new Error(`Ошибка HTTP: ${response.status}`);
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `отчёт-по-встречам.xlsx`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      console.error(error);
    }
  };

  const renderTableContent = () => {
    if (loading) return <tr><td colSpan="7">Загрузка...</td></tr>;
    if (reports.length === 0) return <tr><td colSpan="7">Нет данных</td></tr>;

    return reports.map((item, index) => {
      const isNotHappened = item.status === "COMPLETED_AS_NOT_HAPPENED";
      const isFirstInGroup = index === 0 || reports[index - 1].teamName !== item.teamName;
      const isLastInGroup = index === reports.length - 1 || reports[index + 1].teamName !== item.teamName;
      
      const rowClass = `${isNotHappened ? "mrep-row-not-happened" : ""} ${isFirstInGroup ? "mrep-group-start" : ""} ${isLastInGroup ? "mrep-group-end" : ""}`;
      const { text: displayStatus, className: statusCellClass } = getStatusInfo(item);
      const showTasks = item.status === "COMPLETED";

      return (
        <tr key={`${item.teamName}-${item.startDate}`} className={rowClass}>
          <td className="mrep-cell-left">{index + 1}</td>
          <td
            style={{ cursor: 'pointer', color: '#843AEB', textDecoration: 'underline' }}
            onClick={() => navigate(`/teamcard/${item.teamId}`)}
          >
            {item.teamName}
          </td>
          <td>{item.startDate ? new Date(item.startDate).toLocaleDateString("ru-RU") : "—"}</td>
          <td>{item.trackerFullName || item.trackerName || "—"}</td>
          <td className="mrep-text-wrap">{showTasks ? item.tasksNextMeeting || "—" : "—"}</td>
          <td className="mrep-text-wrap">{showTasks ? item.tasksCurrentMeeting || "—" : "—"}</td>
          <td className={`${statusCellClass} mrep-cell-right`}>{displayStatus}</td>
        </tr>
      );
    });
  };

  return (
    <div className="mrep-page">
      <Header userRole={userRole} />
      <main className="mrep-main">
        <div className="mrep-header">
          <button className="mrep-btn-back" onClick={() => navigate(-1)}>← Назад</button>
          
          <div className="mrep-filters-container">
            <Dropdown label={filters.team || "Команда"} isOpen={openMenu.team} onToggle={() => toggleMenu('team')}>
              <button className="mrep-dropdown-item" onClick={() => updateFilter('team', null)}>— Все —</button>
              {availableData.teams.map(name => (
                <button key={name} className="mrep-dropdown-item" title={name} onClick={() => updateFilter('team', name)}>{name}</button>
              ))}
            </Dropdown>

            <Dropdown label={filters.tracker?.fullName || "Трекеры"} isOpen={openMenu.tracker} onToggle={() => toggleMenu('tracker')}>
              <button className="mrep-dropdown-item" onClick={() => updateFilter('tracker', null)}>— Все —</button>
              {availableData.trackers.map(t => (
                <button key={t.username} className="mrep-dropdown-item" onClick={() => updateFilter('tracker', t)}>{`${t.fullName} (@${t.username})`}</button>
              ))}
            </Dropdown>

            <Dropdown label={filters.status ? COMBINED_STATUS_OPTIONS[filters.status].label : "Статус"} isOpen={openMenu.status} onToggle={() => toggleMenu('status')}>
              <button className="mrep-dropdown-item" onClick={() => updateFilter('status', null)}>— Все —</button>
              {Object.entries(COMBINED_STATUS_OPTIONS).map(([key, opt]) => (
                <button key={key} className="mrep-dropdown-item" onClick={() => updateFilter('status', key)}>{opt.label}</button>
              ))}
            </Dropdown>
          </div>

          <button className="mrep-btn-export" onClick={handleExportExcel}>Выгрузить отчет</button>
        </div>

        <div className="mrep-table-container">
          <table className="mrep-table">
            <thead>
              <tr>
                <th>№</th>
                <SortableHeader title="Название команды" dir={sortConfig.teamNameDir} onSort={() => requestSort("teamName")} />
                <SortableHeader title="Дата встречи" currentSort={sortConfig.secondary} field="startDate" onSort={() => requestSort("startDate")} />
                <th>Трекер</th>
                <th>Задачи к следующей встрече</th>
                <th>Выполнение задач / инфо по команде</th>
                <SortableHeader title="Статус команды" currentSort={sortConfig.secondary} field="teamStatusValue" onSort={() => requestSort("teamStatusValue")} />
              </tr>
            </thead>
            <tbody>
              {renderTableContent()}
            </tbody>
          </table>
        </div>
      </main>
    </div>
  );
}

function Dropdown({ label, isOpen, onToggle, children }) {
  return (
    <div className="mrep-dropdown">
      <button className={`mrep-dropdown-btn ${isOpen ? "open" : ""}`} onClick={onToggle}>
        {label}
        <img src={isOpen ? IconClose : IconOpen} alt="" className="mrep-dropdown-arrow" />
      </button>
      {isOpen && <div className="mrep-dropdown-menu">{children}</div>}
    </div>
  );
}

function SortableHeader({ title, dir, currentSort, field, onSort }) {
  const isActive = currentSort ? currentSort.field === field : true;
  const icon = currentSort 
    ? (currentSort.direction === "asc" ? "↑" : "↓") 
    : (dir === "asc" ? "А→Я" : "Я→А");

  return (
    <th onClick={onSort} className="mrep-th-sortable">
      {title} <span className={isActive ? "mrep-icon-active" : "mrep-icon-inactive"}>{isActive ? icon : "↕"}</span>
    </th>
  );
}
