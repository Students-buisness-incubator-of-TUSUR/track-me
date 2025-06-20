import { useEffect, useMemo, useState } from "react";

export function useTrackerList(endpoint) {
  const [trackers, setTrackers] = useState([]);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [visibleTrackersStart, setVisibleTrackersStart] = useState(0);
  const [hoveredTracker, setHoveredTracker] = useState(null);
  const [hoveredButton, setHoveredButton] = useState(null);
  const trackersPerPage = 20;

  const filters = useMemo(() => [], []);
  const ssoServiceUri = (process.env.REACT_APP_BACKEND_URI || "http://localhost:8080") + "/sso";

  useEffect(() => {
    fetch(`${ssoServiceUri}${endpoint}?page=0&size=100`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ filters }),
    })
      .then((response) => {
        if (!response.ok) {
          if (response.status === 401) {
            setError("Ошибка авторизации! Пожалуйста, выполните вход заново.");
          } else {
            setError("Ошибка при загрузке пользователей. Статус: " + response.status);
          }
          throw new Error("Ошибка запроса");
        }
        return response.json();
      })
      .then((data) => {
        if (data && data.content) {
          setTrackers(data.content);
          setVisibleTrackersStart(0);
        } else {
          setError("Неверный формат данных, полученных с сервера.");
        }
      })
      .catch((err) => {
        console.error("Ошибка при загрузке пользователей:", err);
      });
  }, [filters, ssoServiceUri, endpoint]);

  const confirmUser = (username) => {
    const url = `${ssoServiceUri}/api/v1/users/enable?username=${username}`;
    fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
    })
      .then((res) => res.ok ? res.text() : Promise.reject(res.statusText))
      .then(() => {
        setTrackers((prev) =>
          prev.map((t) => (t.username === username ? { ...t, enabled: true } : t))
        );
      })
      .catch((err) => {
        console.error("Ошибка при подтверждении пользователя:", err);
        setError(`Ошибка при подтверждении пользователя: ${err.message}`);
      });
  };

  const deleteUser = (username) => {
    const url = `${ssoServiceUri}/api/v1/users/disable?username=${username}`;
    fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
    })
      .then((res) => res.ok ? res.text() : Promise.reject(res.statusText))
      .then(() => {
        setTrackers((prev) => prev.filter((t) => t.username !== username));
      })
      .catch((err) => {
        console.error("Ошибка при удалении пользователя:", err);
        setError(`Ошибка при удалении пользователя: ${err.message}`);
      });
  };

  return {
    trackers,
    error,
    searchQuery,
    setSearchQuery,
    visibleTrackersStart,
    setVisibleTrackersStart,
    hoveredTracker,
    setHoveredTracker,
    hoveredButton,
    setHoveredButton,
    trackersPerPage,
    confirmUser,
    deleteUser,
  };
}
