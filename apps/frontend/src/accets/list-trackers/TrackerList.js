import React, { useState } from "react";
import { Link } from "react-router-dom";
import { useUserList } from "../hooks/useUserList";
import "./TrackerList.css";
import trueIcon from "./true.png";
import falseIcon from "./false.png";
import editIcon from "./edit.png";
import trueIcon2 from "./true2.png";
import falseIcon2 from "./false2.png";
import ProfileIcon from "./personal_account_1.png";

function TrackerList() {
    const {
        confirmUser, deleteUser, error,
        visible, searchQuery, handleSearchChange,
        visibleStart, trackersPerPage, filtered,
        showMore, showPrevious
    } = useUserList("trackers");

    const [hoveredTracker, setHoveredTracker] = useState(null);
    const [hoveredButton, setHoveredButton] = useState(null);
    const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
    const logoutHost = (process.env.REACT_APP_BACKEND_URI || 'http://localhost:8080') + '/logout';

    const toggleProfileMenu = () => {
        setIsProfileMenuOpen(prev => !prev);
    };

    const handleLogout = () => {
        localStorage.removeItem("user");
        localStorage.removeItem("userRole");
        localStorage.removeItem("streamName");
        localStorage.removeItem("streamId");
        localStorage.removeItem("streamSDate");
        localStorage.removeItem("streamEDate");
    };

    return (
        <div className="tracker-container">
            <header className="Stream-header">
                <div className="Stream-header-cont">
                    <div className='Stream-header-logo' />
                    <h1 className="Stream-title">TrackMe</h1>
                    <div className="Stream-buttons">
                        <Link to="/list-admins"><button className="Stream-butt">Администраторы</button></Link>
                        <Link to="/streams"><button className="Stream-butt">Потоки</button></Link>
                        <Link to="/team-cards"><button className="Stream-butt">Все команды</button></Link>
                        <button className="Stream-pic" onClick={toggleProfileMenu}>
                            <img src={ProfileIcon} alt="Профиль" className="Stream-pic-img" />
                        </button>
                        {isProfileMenuOpen && (
                            <div className="ProfileDropdown">
                                <Link to="/profile" className="ProfileDropdown-item">Личный кабинет</Link>
                                <Link onClick={handleLogout} to={logoutHost} className="ProfileDropdown-item logout">Выход</Link>
                            </div>
                        )}
                    </div>
                </div>
                <div className="Stream-header-bottom-cont">
                    <div className="Stream-search-cont">
                        <div className="Stream-search-contcont">
                            <button className="Stream-settings-pic2"></button>
                            <input type="search" placeholder="Найти" className="Stream-search"
                                value={searchQuery} onChange={handleSearchChange} />
                        </div>
                    </div>
                </div>
            </header>
            <main className="tracker-list-content">
                {error && <div className="error-message oval2">{error}</div>}
                <div className="tracker-grid">
                    {visible.map((tracker, index) =>
                        tracker.enabled ? (
                            <div className="tracker-item-true" key={tracker.username || index}
                                onClick={() => setHoveredTracker(tracker.username)}
                                onMouseLeave={() => setHoveredTracker(null)}>
                                <div className="tracker-avatar">
                                    {hoveredTracker === tracker.username && (
                                        <div className="tracker-edit-panel12">
                                            <button className="confirm-button" onClick={(e) => {
                                                e.stopPropagation(); setHoveredTracker(null);
                                            }}
                                                onMouseEnter={() => setHoveredButton("confirm")}
                                                onMouseLeave={() => setHoveredButton(null)}>
                                                <img src={hoveredButton === "cancel" ? trueIcon2 : trueIcon} alt="Ничего не происходит" />
                                            </button>
                                            <button className="cancel-button" onClick={(e) => {
                                                e.stopPropagation(); deleteUser(tracker.username);
                                            }}
                                                onMouseEnter={() => setHoveredButton("cancel")}
                                                onMouseLeave={() => setHoveredButton(null)}>
                                                <img src={hoveredButton === "confirm" ? falseIcon2 : falseIcon} alt="Удалить" />
                                            </button>
                                        </div>
                                    )}
                                    <span className="green-checkmark" title="Включён">
                                        <img src={trueIcon} alt="Подтвержден" />
                                    </span>
                                    <div className="tracker-text">
                                        <div className="tracker-fio">{tracker.fullName}</div>
                                        <div className="tracker-nick">@{tracker.telegramId}</div>
                                    </div>
                                </div>
                            </div>
                        ) : (
                            <div className="tracker-item-edit" key={tracker.username || index}
                                onClick={() => setHoveredTracker(tracker.username)}
                                onMouseLeave={() => setHoveredTracker(null)}>
                                <div className="tracker-avatar">
                                    {hoveredTracker === tracker.username && (
                                        <div className="tracker-edit-panel">
                                            <button className="confirm-button" onClick={() => confirmUser(tracker.username)}
                                                onMouseEnter={() => setHoveredButton("confirm")}
                                                onMouseLeave={() => setHoveredButton(null)}>
                                                <img src={hoveredButton === "cancel" ? trueIcon2 : trueIcon} alt="Подтвердить" />
                                            </button>
                                            <button className="cancel-button" onClick={() => deleteUser(tracker.username)}
                                                onMouseEnter={() => setHoveredButton("cancel")}
                                                onMouseLeave={() => setHoveredButton(null)}>
                                                <img src={hoveredButton === "confirm" ? falseIcon2 : falseIcon} alt="Отклонить" />
                                            </button>
                                        </div>
                                    )}
                                    <div className="edit-icon" title="Редактировать">
                                        <img src={editIcon} alt="Редактировать" />
                                    </div>
                                    <div className="tracker-text">
                                        <div className="tracker-fio">{tracker.fullName}</div>
                                        <div className="tracker-nick">@{tracker.telegramId}</div>
                                    </div>
                                </div>
                            </div>
                        )
                    )}
                </div>
            </main>
            {filtered.length > 0 && (
                <footer className="Stream-footer">
                    <div className="Stream-footer-butts">
                        <div className="Stream-footer-p-butt-1">
                            {visibleStart > 0 && <button onClick={showPrevious} className="Stream-footer-button-1"></button>}
                        </div>
                        <div className="Stream-footer-p-butts">
                            {visibleStart > 0 && <button onClick={showPrevious} className="Stream-footer-button-2"></button>}
                            <button className="Stream-footer-button-3"></button>
                            {visibleStart + trackersPerPage < filtered.length && <button onClick={showMore} className="Stream-footer-button-4"></button>}
                        </div>
                        <div className="Stream-footer-p-butt-5">
                            {visibleStart + trackersPerPage < filtered.length && <button onClick={showMore} className="Stream-footer-button-5"></button>}
                        </div>
                    </div>
                </footer>
            )}
        </div>
    );
}

export default TrackerList;
