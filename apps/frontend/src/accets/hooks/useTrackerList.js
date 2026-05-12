import { useEffect, useCallback, useState, useMemo } from "react";
import { getCsrfConfigForFetch } from "../../utils/csrf-utils";

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
  const trackersPerPage = 15;

  // Новое состояние для фильтра заблокированных пользователей
  const [showLockedOnly, setShowLockedOnly] = useState(false);

  const ssoServiceUri = (process.env.REACT_APP_BACKEND_URI || "http://localhost:8080") + "/sso";

  const fetchTrackers = useCallback(async (currentPage = 0, currentSize = 15, showLocked = false) => {
    try {
      const filters = [];
      
      // Основной фильтр - всегда показываем активных пользователей
      if (!showLocked) {
        filters.push({
          fieldName: "accountNonLocked",
          type: "EQ",
          value: true,
        });
      } else {
        // Если нажата кнопка - показываем только заблокированных
        filters.push({
          fieldName: "accountNonLocked",
          type: "EQ",
          value: false,
        });
      }

      // Добавляем сортировку по алфавиту на бэкенде
      const sortParams = "sort=fullName,asc";

      const response = await fetch(`${ssoServiceUri}${endpoint}?page=${currentPage}&size=${currentSize}&${sortParams}`, {
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
      console.log("Backend response:", data);

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
  const clientPaginatedTrackers = filteredTrackers.slice(
    page * trackersPerPage,
    (page + 1) * trackersPerPage
  );

  useEffect(() => {
    setPage(0);
  }, [searchQuery, showLockedOnly]);

  useEffect(() => {
    fetchTrackers(0, 15, showLockedOnly);
  }, [fetchTrackers, showLockedOnly]);

  // Функция для переключения отображения заблокированных пользователей
  const toggleShowLocked = () => {
    setShowLockedOnly(prev => !prev);
    setPage(0);
  };

  // Остальные функции остаются без изменений
  const confirmUser = async (username) => {
    try {
      const url = `${ssoServiceUri}/api/v1/users/enable?username=${username}`;
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

  const deleteUser = async (username) => {
    try {
      const url = `${ssoServiceUri}/api/v1/users/disable?username=${username}`;
      const response = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json",  ...getCsrfConfigForFetch() },
        credentials: "include",
      });

      if (!response.ok) throw new Error(response.statusText);

      await fetchTrackers(0, 15, showLockedOnly);
    } catch (err) {
      console.error("Ошибка при удалении пользователя:", err);
      setError(`Ошибка при удалении пользователя: ${err.message}`);
    }
  };

  const handleFirstPage = () => setPage(0);
  const handleLastPage = () => setPage(totalPages - 1);
  const handleNextPage = () => {
    if (page < totalPages - 1) setPage(p => p + 1);
  };
  const handlePrevPage = () => {
    if (page > 0) setPage(p => p - 1);
  };
  const handlePageJump = (jump) => {
    const newPage = page + jump;
    if (newPage >= 0 && newPage < totalPages) setPage(newPage);
  };

  return {
    trackers: clientPaginatedTrackers,
    error,
    searchQuery,
    setSearchQuery,
    hoveredTracker,
    setHoveredTracker,
    hoveredButton,
    setHoveredButton,
    confirmUser,
    deleteUser,
    page,
    totalPages,
    totalElements,
    setPage,
    handleFirstPage,
    handleLastPage,
    handleNextPage,
    handlePrevPage,
    handlePageJump,
    fetchTrackers,
    showLockedOnly,
    toggleShowLocked,
    trackersPerPage,
  };
}
