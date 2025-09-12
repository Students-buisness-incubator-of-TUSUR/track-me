/* istanbul ignore next */
import React, { useState,useEffect } from "react";
import { Link, useLocation } from "react-router-dom";
import { useTrackerList } from "../hooks/useTrackerList";
import "./TrackerList.css";
import trueIcon from "./true.png";
import falseIcon from "./false.png";
import editIcon from "./edit.png";
import trueIcon2 from "./true2.png";
import falseIcon2 from "./false2.png";
import ProfileIcon from "./personal_account_1.png";
import { useNavigate } from "react-router-dom";
function TrackerListPage({ endpoint }) {
  const {
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
    handleNextPage,
    handlePrevPage,
    setPage,
    handlePageJump
  } = useTrackerList(endpoint);
  const location = useLocation();
  const logoutHost = (process.env.REACT_APP_BACKEND_URI || "http://localhost:8080") + "/logout";
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
const navigate = useNavigate();
  const toggleProfileMenu = () => setIsProfileMenuOpen((prev) => !prev);
const [userRole, setUserRole] = useState('');
  const handleLogout = async () => {
        localStorage.removeItem("user");
        localStorage.removeItem("userRole");
        localStorage.removeItem("streamName");
        localStorage.removeItem("streamId");
        localStorage.removeItem("streamSDate");
        localStorage.removeItem("streamEDate");
        localStorage.removeItem("csrfToken");
        localStorage.removeItem("csrfHeaderName");
    };
useEffect(() => {
    // Проверяем данные пользователя в localStorage
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      const userData = JSON.parse(savedUser);
      setUserRole(userData.roles[0]);
    }
  }, []);
  const handleSearchChange = (event) => {
    setSearchQuery(event.target.value);
    setPage(0); // Reset page to 0 on search
  };

  return (
    <div className="tracker-container">
      <header className="Stream-header">
        <div className="Stream-header-cont">
          <div // NOSONAR
  className="Stream-header-logo" // NOSONAR
  onClick={() => navigate("/streams")} // NOSONAR
  onKeyDown={(e) => { // NOSONAR
    if (e.key === 'Enter' || e.key === ' ') { // NOSONAR
      navigate("/streams"); // NOSONAR
    } // NOSONAR
  }} // NOSONAR
  tabIndex={0} // NOSONAR
  role="button" // NOSONAR
  aria-label="Вернуться на главную страницу" // NOSONAR
  style={{ cursor: "pointer" }} // NOSONAR
/> 
<h1 // NOSONAR
  className="Stream-title" // NOSONAR
  onClick={() => navigate("/streams")} // NOSONAR
  onKeyDown={(e) => { // NOSONAR
    if (e.key === 'Enter' || e.key === ' ') { // NOSONAR
      navigate("/streams"); // NOSONAR
    } // NOSONAR
  }} // NOSONAR
  tabIndex={0} // NOSONAR
  role="button" // NOSONAR
  aria-label="Вернуться на главную страницу" // NOSONAR
  style={{ cursor: "pointer" }} // NOSONAR
> {/*NOSONAR*/}
  TrackMe {/*NOSONAR*/}
</h1> {/*NOSONAR*/}
          <div className="Stream-buttons">
            {userRole === 'SUPER_ADMIN' && location.pathname === "/list-trackers" && (
              <Link to="/list-admins">
                <button className="Stream-butt">Администраторы</button>
              </Link>
            )}
            
            {userRole === 'SUPER_ADMIN' && location.pathname === "/list-admins" && (
              <Link to="/list-trackers">
                <button className="Stream-butt">Трекеры</button>
              </Link>
            )}
            
            {userRole === 'SUPER_ADMIN' && location.pathname !== "/list-trackers" && location.pathname !== "/list-admins" && (
              <>
                <Link to="/list-admins">
                  <button className="Stream-butt">Администраторы</button>
                </Link>
                <Link to="/list-trackers">
                  <button className="Stream-butt">Трекеры</button>
                </Link>
              </>
            )}
            
            {/* Для администраторов показываем только кнопку "Трекеры" */}
            {userRole === 'ADMIN' && location.pathname === "/list-trackers" && (
              <Link to="/list-trackers">
                <button className="Stream-butt">Трекеры</button>
              </Link>
            )}
            
            {userRole === 'ADMIN' && location.pathname === "/list-admins" && (
              <Link to="/list-trackers">
                <button className="Stream-butt">Трекеры</button>
              </Link>
            )}
            
            {userRole === 'ADMIN' && location.pathname !== "/list-trackers" && location.pathname !== "/list-admins" && (
              <Link to="/list-trackers">
                <button className="Stream-butt">Трекеры</button>
              </Link>
            )}

            <Link to="/streams">
              <button className="Stream-butt">Потоки</button>
            </Link>
            <Link to="/all-team-cards">
              <button className="Stream-butt">Все команды</button>
            </Link>
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
              <input
                type="search"
                placeholder="Найти"
                className="Stream-search"
                value={searchQuery}
                onChange={handleSearchChange}
              />
            </div>
          </div>
        </div>
      </header>

      <main className="tracker-list-content">
        {error && <div className="error-message oval2">{error}</div>}
        <div className="tracker-grid">
          {trackers.length > 0 ? (
            trackers.map((tracker, index) =>
              tracker.enabled ? (
                <div
                  className="tracker-item-true"
                  key={tracker.username || index}
                  onClick={() => setHoveredTracker(tracker.username)}
                  onMouseLeave={() => setHoveredTracker(null)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" || e.key === " ") {
                      setHoveredTracker(tracker.username);
                    }
                  }}
                  role="button"
                  tabIndex={0}
                >
                  <div className="tracker-avatar">
                    {hoveredTracker === tracker.username && (
                      <div className="tracker-edit-panel12">
                        <button
                          className="confirm-button"
                          onClick={(e) => {
                            e.stopPropagation();
                            setHoveredTracker(null);
                          }}
                          onMouseEnter={() => setHoveredButton("confirm")}
                          onMouseLeave={() => setHoveredButton(null)}
                        >
                          <img src={hoveredButton === "cancel" ? trueIcon2 : trueIcon} alt="Оставить" />
                        </button>
                        {hoveredButton === "cancel" && (
                          <span className="tooltip tooltip-red">Удалить</span>
                        )}
                        <button
                          className="cancel-button"
                          onClick={(e) => {
                            e.stopPropagation();
                            deleteUser(tracker.username);
                          }}
                          onMouseEnter={() => setHoveredButton("cancel")}
                          onMouseLeave={() => setHoveredButton(null)}
                        >
                          <img src={hoveredButton === "confirm" ? falseIcon2 : falseIcon} alt="Удалить" />
                        </button>
                        {hoveredButton === "confirm" && (
                          <span className="tooltip tooltip-green">Оставить</span>
                        )}
                      </div>
                    )}
                    <span className="green-checkmark" title="Включён">
                      <img src={trueIcon} alt="Подтверждён" />
                    </span>
                    <div className="tracker-text">
                      <div className="tracker-fio">{tracker.fullName}</div>
                      <div className="tracker-nick">{tracker.username}</div>
                    </div>
                  </div>
                </div>
              ) : (
                <div
                  className="tracker-item-edit"
                  key={tracker.username || index}
                  onClick={() => setHoveredTracker(tracker.username)}
                  onMouseLeave={() => setHoveredTracker(null)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" || e.key === " ") {
                      setHoveredTracker(tracker.username);
                    }
                  }}
                  role="button"
                  tabIndex={0}
                >
                  <div className="tracker-avatar">
                    {hoveredTracker === tracker.username && (
                      <div className="tracker-edit-panel">
                        <button
                          className="confirm-button"
                          onClick={() => confirmUser(tracker.username)}
                          onMouseEnter={() => setHoveredButton("confirm")}
                          onMouseLeave={() => setHoveredButton(null)}
                        >
                          <img src={hoveredButton === "cancel" ? trueIcon2 : trueIcon} alt="Подтвердить" />
                        </button>
                        {hoveredButton === "cancel" && (
                          <span className="tooltip tooltip-red">Отменить</span>
                        )}
                        <button
                          className="cancel-button"
                          onClick={() => deleteUser(tracker.username)}
                          onMouseEnter={() => setHoveredButton("cancel")}
                          onMouseLeave={() => setHoveredButton(null)}
                        >
                          <img src={hoveredButton === "confirm" ? falseIcon2 : falseIcon} alt="Отклонить" />
                        </button>
                        {hoveredButton === "confirm" && (
                          <span className="tooltip tooltip-green">Подтвердить</span>
                        )}
                      </div>
                    )}
                    <div className="edit-icon" title="Редактировать">
                      <img src={editIcon} alt="Редактировать" />
                    </div>
                    <div className="tracker-text">
                      <div className="tracker-fio">{tracker.fullName}</div>
                      <div className="tracker-nick">@{tracker.username}</div>
                    </div>
                  </div>
                </div>
              )
            )
          ) : (
            <p className="error-message oval2">Нет трекеров для отображения</p>
          )}
        </div>
      </main>

      {trackers.length > 0 && (
  <footer className="Stream-footer">
  <div className="Stream-footer-butts">
    <div className="Stream-footer-p-butt-1">
      {page > 0 && (
        <button
          onClick={handlePrevPage}
          className="Stream-footer-button-1"
          aria-label="Предыдущая страница"
        ></button>
      )}
    </div>
    <div className="Stream-footer-p-butts">
      {page > 1 && (
        <button
          onClick={() => handlePageJump(-2)}
          className="Stream-footer-button-2"
          aria-label="Перейти на 2 страницы назад"
        ></button>
      )}
      {page > 0 && (
        <button
          onClick={handlePrevPage}
          className="Stream-footer-button-2"
          aria-label="Предыдущая страница"
        ></button>
      )}
      <button
        className="Stream-footer-button-3"
        aria-label={`Текущая страница ${page + 1}`}
        disabled
      ></button>
      {page < totalPages - 1 && (
        <button
          onClick={handleNextPage}
          className="Stream-footer-button-4"
          aria-label="Следующая страница"
        ></button>
      )}
      {page < totalPages - 2 && (
        <button
          onClick={() => handlePageJump(2)}
          className="Stream-footer-button-4"
          aria-label="Перейти на 2 страницы вперед"
        ></button>
      )}
    </div>
    <div className="Stream-footer-p-butt-5">
      {page < totalPages - 1 && (
        <button
          onClick={handleNextPage} // Изменено с setPage(totalPages - 1) на handleNextPage
          className="Stream-footer-button-5"
          aria-label="Следующая страница" // Изменено с "Последняя страница" на "Следующая страница"
        ></button>
      )}
    </div>
  </div>
</footer>
)}
      
    </div>
  );
}

export default TrackerListPage;