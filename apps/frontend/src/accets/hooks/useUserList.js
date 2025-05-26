import { useState, useEffect, useMemo } from "react";

export function useUserList(type = "trackers") {
    const [trackers, setTrackers] = useState([]);
    const [error, setError] = useState(null);
    const [visibleStart, setVisibleStart] = useState(0);
    const [searchQuery, setSearchQuery] = useState("");
    const trackersPerPage = 20;

    const filters = useMemo(() => [], []);
    const ssoServiceUri = (process.env.REACT_APP_BACKEND_URI || "http://localhost:8080") + "/sso";

    useEffect(() => {
        fetch(`${ssoServiceUri}/api/v1/users/${type}?page=0&size=10`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify({ filters })
        })
        .then(res => {
            if (!res.ok) throw new Error(`Status: ${res.status}`);
            return res.json();
        })
        //////
        .then(data => {
            if (data?.content) {
                setTrackers(data.content);
                setVisibleStart(0);
            } else {
                setError("Неверный формат данных");
            }
        })
        .catch(err => {
            console.error(err);
            setError("Ошибка при загрузке: " + err.message);
        });
    }, [type, filters, ssoServiceUri]);

    const confirmUser = username => {
        fetch(`${ssoServiceUri}/api/v1/users/enable?username=${username}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include"
        })
        .then(res => {
            if (!res.ok) throw new Error("Ошибка при подтверждении");
            return res.text();
        })
        .then(() => {
            setTrackers(prev =>
                prev.map(t => (t.username === username ? { ...t, enabled: true } : t))
            );
        })
        .catch(err => setError("Ошибка подтверждения: " + err.message));
    };

    const deleteUser = username => {
        fetch(`${ssoServiceUri}/api/v1/users/disable?username=${username}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include"
        })
        .then(res => {
            if (!res.ok) throw new Error("Ошибка при удалении");
            return res.text();
        })
        .then(() => {
            setTrackers(prev => prev.filter(t => t.username !== username));
        })
        .catch(err => setError("Ошибка удаления: " + err.message));
    };

    const handleSearchChange = e => {
        setSearchQuery(e.target.value);
        setVisibleStart(0);
    };

    const filtered = trackers.filter(t =>
        (t.fullName || "").toLowerCase().includes(searchQuery.toLowerCase())
    );
    const visible = filtered.slice(visibleStart, visibleStart + trackersPerPage);

    const showMore = () => setVisibleStart(prev => prev + trackersPerPage);
    const showPrevious = () => setVisibleStart(prev => Math.max(prev - trackersPerPage, 0));

    return {
        trackers,
        error,
        confirmUser,
        deleteUser,
        visible,
        searchQuery,
        handleSearchChange,
        visibleStart,
        trackersPerPage,
        filtered,
        showMore,
        showPrevious
    };
}
