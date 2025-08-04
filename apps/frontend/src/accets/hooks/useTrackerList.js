import { useEffect, useCallback, useState } from "react";
import { getCsrfConfigForFetch } from "../../utils/csrf-utils";
export function useTrackerList(endpoint) {
  const [trackers, setTrackers] = useState([]);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [hoveredTracker, setHoveredTracker] = useState(null);
  const [hoveredButton, setHoveredButton] = useState(null);
  
  // Пагинация
  const [page, setPage] = useState(0);
  const [size] = useState(16);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const ssoServiceUri = (process.env.REACT_APP_BACKEND_URI || "http://localhost:8080") + "/sso";

  const fetchTrackers = useCallback(async (currentPage = 0, currentSize = 15, currentSearchQuery = "") => {
    try {
      const filters = [];
      if (currentSearchQuery) {
        filters.push({
          fieldName: "fullName",
          type: "LIKE",
          value: currentSearchQuery,
        });
      }

      const response = await fetch(`${ssoServiceUri}${endpoint}?page=${currentPage}&size=${currentSize}`, {
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

      // Обновленная проверка ответа с учетом пагинации
      if (data.content && data.page) {
        setTrackers(data.content);
        setTotalPages(data.page.totalPages);
        setTotalElements(data.page.totalElements);
      } else if (Array.isArray(data)) {
        // Если бэкенд возвращает просто массив без пагинации
        setTrackers(data);
        setTotalPages(1);
        setTotalElements(data.length);
      } else {
        throw new Error("Неверный формат данных, полученных с сервера.");
      }
    } catch (err) {
      console.error("Ошибка при загрузке пользователей:", err);
      setError(err.message);
      setTrackers([]);
      setTotalPages(1);
      setTotalElements(0);
    }
  }, [ssoServiceUri, endpoint]);

  useEffect(() => {
    fetchTrackers(page, size, searchQuery);
  }, [fetchTrackers, page, size, searchQuery]);

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

      setTrackers(prev => 
        prev.map(t => t.username === username ? { ...t, enabled: true } : t)
      );
      fetchTrackers(page, size, searchQuery);
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

      await fetchTrackers(page, size, searchQuery);
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
    trackers,
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
    size,
    handleFirstPage,
    handleLastPage,
    handleNextPage,
    handlePrevPage,
    handlePageJump,
    fetchTrackers,
  };
}