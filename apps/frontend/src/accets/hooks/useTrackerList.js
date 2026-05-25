import { useEffect, useCallback, useState, useMemo } from "react";
import { getCsrfConfigForFetch } from "../../utils/csrf-utils";
import { fetchUserTeams } from "../../services/requests";
import { isValidUsername } from "../../utils/validation";
 
// Allowed API paths whitelist for user operations
const ALLOWED_USER_API_PATHS = [
  '/api/v1/users/enable',
  '/api/v1/users/disable',
  '/api/v1/users/unlock',
  '/api/v1/users'
];
 
// Allowed API paths whitelist for fetching trackers
const ALLOWED_TRACKER_ENDPOINTS = [
  '/api/v1/users/search',
  '/api/v1/users/locked',
  '/api/v1/users/trackers',
  '/api/v1/users/administrators'
];
 
export function useTrackerList(endpoint) {
  const [allTrackers, setAllTrackers] = useState([]);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [hoveredTracker, setHoveredTracker] = useState(null);
  const [hoveredButton, setHoveredButton] = useState(null);
 
  // Пагинация
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const trackersPerPage = 16;
 
  // Новое состояние для фильтра заблокированных пользователей
  const [showLockedOnly, setShowLockedOnly] = useState(false);
 
  // Состояния для модальных окон
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [showTeamsWarning, setShowTeamsWarning] = useState(false);
  const [attachedTeams, setAttachedTeams] = useState([]);
  const [userToDelete, setUserToDelete] = useState(null);
 
  const ssoServiceUri = (process.env.REACT_APP_BACKEND_URI || "http://localhost:8080") + "/sso";
 
  /**
   * Создаёт безопасный URL с валидацией пути и параметров.
   * URLSearchParams автоматически кодирует параметры.
   * @param {string} path - API путь (должен быть в whitelist)
   * @param {Object} params - параметры (передаются в сыром виде, не кодированные)
   * @returns {string} Полный URL с закодированными параметрами
   */
  const createSafeUserUrl = (path, params = {}) => {
    if (!ALLOWED_USER_API_PATHS.includes(path)) {
      throw new Error(`Invalid API path: ${path}`);
    }
 
    const url = new URL(`${ssoServiceUri}${path}`);
 
    // URLSearchParams автоматически кодирует значения
    Object.entries(params).forEach(([key, value]) => {
      if (typeof value === 'string' && value.length > 0) {
        url.searchParams.set(key, value);
      }
    });
 
    return url.toString();
  };
 
  // Validate tracker endpoint
  const validateTrackerEndpoint = (endpoint) => {
    if (!endpoint || !ALLOWED_TRACKER_ENDPOINTS.includes(endpoint)) {
      throw new Error(`Invalid endpoint: ${endpoint}`);
    }
    return endpoint;
  };
 
  const fetchTrackers = useCallback(async (currentPage = 0, currentSize = 16, currentShowLocked = false, hasSearchQuery = false) => {
    try {
      // Validate endpoint
      const validEndpoint = validateTrackerEndpoint(endpoint);
 
      const filters = [];
 
      // Основной фильтр - всегда показываем активных пользователей
      if (!currentShowLocked) {
  filters.push({
    fieldName: "accountNonLocked",
    type: "EQ",
    value: true,
  });
} else {
  filters.push({
    fieldName: "accountNonLocked",
    type: "EQ",
    value: false,
  });
}
 
      // Добавляем сортировку по алфавиту на бэкенде
      const sortParams = "sort=fullName,asc";
 
      // Если активен поиск, загружаем все данные (без пагинации) чтобы найти везде
      const pageForSearch = hasSearchQuery ? 0 : currentPage;
      const sizeForSearch = hasSearchQuery ? 10000 : currentSize;
 
      const response = await fetch(`${ssoServiceUri}${validEndpoint}?page=${pageForSearch}&size=${sizeForSearch}&${sortParams}`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...getCsrfConfigForFetch()},
        credentials: "include",
        body: JSON.stringify({ filters }),
      });
 
      if (!response.ok) {
        if (response.status === 401) {
          throw new Error("Ошибка авторизации! Пожалуйста, выполните вход заново.");
        } else {
          throw new Error(`Ошибка при загрузке пользователей. Статус: ${response.status}`);
        }
      }
 
      const data = await response.json();
      // Безопасное логирование - объект вместо строки
      console.log("Backend response received", {
        itemCount: data?.content?.length ?? data?.length ?? 0,
        timestamp: new Date().toISOString()
      });
 
      // Функция для сортировки по активности на фронтенде
      const sortByActiveStatus = (trackers) => {
        return [...trackers].sort((a, b) => {
          // Сначала активные (enabled = true), потом неактивные
          if (a.enabled && !b.enabled) return -1;
          if (!a.enabled && b.enabled) return 1;
 
          // Если статус одинаковый, оставляем порядок из бэкенда (уже отсортировано по алфавиту)
          return 0;
        });
      };
 
      // Обновленная проверка ответа с учетом пагинации
      if (data.content && data.page) {
        const sortedContent = sortByActiveStatus(data.content);
        setAllTrackers(sortedContent);
        setTotalPages(data.page.totalPages || 1);
        setTotalElements(data.page.totalElements || data.content.length);
      } else if (Array.isArray(data)) {
        // Если бэкенд возвращает просто массив без пагинации
        const sortedData = sortByActiveStatus(data);
        setAllTrackers(sortedData);
        setTotalPages(1);
        setTotalElements(sortedData.length);
      } else {
        throw new Error("Неверный формат данных, полученных с сервера.");
      }
    } catch (err) {
      console.error("Ошибка при загрузке пользователей:", err);
      setError(err.message);
      setAllTrackers([]);
    }
  }, [ssoServiceUri, endpoint]);
 
  // Фильтрация на клиенте по поисковому запросу (регистронезависимая)
  const filteredTrackers = useMemo(() => {
    if (!searchQuery) return allTrackers;
    const query = searchQuery.toLowerCase();
    return allTrackers.filter(t =>
      t.fullName && t.fullName.toLowerCase().includes(query)
    );
  }, [allTrackers, searchQuery]);
 
  // Пагинация на клиенте (используем backend totalPages для серверной пагинации)
 
 
  useEffect(() => {
    setPage(0);
  }, [searchQuery, showLockedOnly]);
 
  useEffect(() => {
  fetchTrackers(0, trackersPerPage, showLockedOnly, searchQuery.length > 0);
}, [fetchTrackers, showLockedOnly, searchQuery]);
 
  // Функция для переключения отображения заблокированных пользователей
  const toggleShowLocked = () => {
    setShowLockedOnly(prev => !prev);
    setPage(0);
  };
 
  // передаём RAW username без предварительного кодирования
  const confirmUser = async (username) => {
    try {
      // Validate username before using
      if (!isValidUsername(username)) {
        throw new Error("Invalid username format");
      }
 
      // Передаём RAW username - createSafeUserUrl сама закодирует через URLSearchParams
      const url = createSafeUserUrl('/api/v1/users/enable', { username });
 
      const response = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json",  ...getCsrfConfigForFetch() },
        credentials: "include",
      });
 
      if (!response.ok) throw new Error(response.statusText);
 
      setAllTrackers(prev => 
        prev.map(t => t.username === username ? { ...t, enabled: true } : t)
      );
    } catch (err) {
      console.error("Ошибка при подтверждении пользователя:", err);
      setError(`Ошибка при подтверждении пользователя: ${err.message}`);
    }
  };
 
  // передаём RAW username
  const toggleUserLock = async (username) => {
    try {
      // Validate username before using
      if (!isValidUsername(username)) {
        throw new Error("Invalid username format");
      }
 
      if (showLockedOnly) {
        // Разблокировка заблокированного
        const url = createSafeUserUrl('/api/v1/users/unlock', { username });
        const response = await fetch(url, {
          method: "POST",
          headers: { "Content-Type": "application/json", ...getCsrfConfigForFetch() },
          credentials: "include",
        });
        if (!response.ok) throw new Error(response.statusText);
      } else {
        // Блокировка активного
        const url = createSafeUserUrl('/api/v1/users/disable', { username });
        const response = await fetch(url, {
          method: "POST",
          headers: { "Content-Type": "application/json", ...getCsrfConfigForFetch() },
          credentials: "include",
        });
        if (!response.ok) throw new Error(response.statusText);
      }
      await fetchTrackers(page, trackersPerPage, showLockedOnly, searchQuery.length > 0);
    } catch (err) {
      setError(`Ошибка: ${err.message}`);
    }
  };
 
  // передаём RAW username без предварительного кодирования
  const handleDeleteClick = async (username) => {
      try {
          // Validate username
          if (!isValidUsername(username)) {
            throw new Error("Invalid username format");
          }
 
          // Передаём RAW username
          const response = await fetchUserTeams(username);
 
          if (!response.ok) throw new Error("Failed to fetch teams");
 
          const teams = await response.json();
          // Безопасное логирование - объект вместо строки
          console.log("Teams fetched successfully", {
            teamCount: teams?.length ?? 0,
            timestamp: new Date().toISOString()
          });
 
          if (teams && teams.length > 0) {
              setAttachedTeams(teams.map(team => ({ id: team.id, name: team.name })));
              setUserToDelete(username);
              setShowTeamsWarning(true);
          } else {
              setUserToDelete(username);
              setAttachedTeams([]);
              setShowDeleteConfirm(true);
          }
      } catch (err) {
          console.error("Error fetching user teams:", err);
          setUserToDelete(username);
          setAttachedTeams([]);
          setShowDeleteConfirm(true);
      }
  };
 
  // userToDelete теперь содержит RAW username
  const confirmDeleteUser = async () => {
    if (!userToDelete) return;
    try {
      // Validate userToDelete (теперь это RAW username)
      if (!isValidUsername(userToDelete)) {
        throw new Error("Invalid username format");
      }
 
      // Передаём RAW username
      const url = createSafeUserUrl('/api/v1/users', { username: userToDelete });
 
      const response = await fetch(url, {
        method: "DELETE",
        headers: { "Content-Type": "application/json", ...getCsrfConfigForFetch() },
        credentials: "include",
      });
      if (!response.ok) throw new Error(response.statusText);
      setShowDeleteConfirm(false);
      setUserToDelete(null);
      await fetchTrackers(page, trackersPerPage, showLockedOnly, searchQuery.length > 0);
    } catch (err) {
      setError(`Ошибка при удалении: ${err.message}`);
      setShowDeleteConfirm(false);
      setUserToDelete(null);
    }
  };
 
  const closeTeamsWarning = () => {
    setShowTeamsWarning(false);
    setShowDeleteConfirm(true);
  };
 
  const cancelTeamsWarning = () => {
    setShowTeamsWarning(false);
    setUserToDelete(null);
    setAttachedTeams([]);
  };
 
 
  const changePage = (newPage) => {
  setPage(newPage);
  fetchTrackers(newPage, trackersPerPage, showLockedOnly, searchQuery.length > 0);
};
 
 
  return {
  trackers: filteredTrackers,
  error,
  searchQuery,
  setSearchQuery,
  hoveredTracker,
  setHoveredTracker,
  hoveredButton,
  setHoveredButton,
  confirmUser,
  toggleUserLock,
  handleDeleteClick,
  confirmDeleteUser,
  showDeleteConfirm,
  setShowDeleteConfirm,
  showTeamsWarning,
  attachedTeams,
  userToDelete,
  closeTeamsWarning,
  cancelTeamsWarning,
  page,
  totalPages,
  totalElements,
  setPage,
  fetchTrackers,
  showLockedOnly,
  toggleShowLocked,
  trackersPerPage,
  changePage, // Новая функция для смены страницы
  // Используем changePage в обработчиках
  handleFirstPage: () => changePage(0),
  handleLastPage: () => changePage(totalPages - 1),
  handleNextPage: () => changePage(page + 1),
  handlePrevPage: () => changePage(page - 1),
  handlePageJump: (jump) => changePage(page + jump),
};
}