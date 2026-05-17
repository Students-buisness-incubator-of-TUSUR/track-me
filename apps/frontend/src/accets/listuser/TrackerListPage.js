import { useState, useEffect, useRef } from "react";
import { Link } from "react-router-dom";
import { useTrackerList } from "../hooks/useTrackerList";
import "./TrackerList.css";
import trueIcon from "./true.png";
import falseIcon from "./false.png";
import editIcon from "./edit.png";
import trueIcon2 from "./true2.png";
import falseIcon2 from "./false2.png";
import Header from "../header/header";
import { useGetUserInfo } from "../../services/util";
import { isValidUsername } from "../../utils/validation";

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
    handleNextPage,
    handlePrevPage,
    setPage,
    handlePageJump,
    showLockedOnly,
    toggleShowLocked,
  } = useTrackerList(endpoint);
  
  const [userRole, setUserRole] = useState('');
    
  const [activeMobileMenu, setActiveMobileMenu] = useState(null);

  const lastTap = useRef(0);
  const tapTimeout = useRef(null);


  const user = useGetUserInfo();
  useEffect(() => {
    setUserRole(user.roles[0]);
  }, [user]);

  // Safe action wrapper with username validation
  const safeAction = (username, action) => {
    if (!isValidUsername(username)) {
      console.error("Invalid username for action", { username });
      return;
    }
    action(username);
  };

  const handleSearchChange = (event) => {
    setSearchQuery(event.target.value);
    setPage(0);
  };


  const handleDoubleTap = (username) => {
    // Validate username before using
    if (!isValidUsername(username)) {
      console.error("Invalid username for double tap", { username });
      return;
    }
    
    const now = Date.now();
    const timeDiff = now - lastTap.current;

    if (timeDiff < 300) {
      if (tapTimeout.current) {
        clearTimeout(tapTimeout.current);
      }

      /* istanbul ignore if */
      if (activeMobileMenu === username) {
        setActiveMobileMenu(null);
        setHoveredTracker(null);
      }

      window.location.href = `/profile/${encodeURIComponent(username)}`;
    } else {
      tapTimeout.current = setTimeout(() => {
        /* istanbul ignore if */
        if (activeMobileMenu === username) {
          setActiveMobileMenu(null);
          setHoveredTracker(null);
        } else {
          setActiveMobileMenu(username);
          setHoveredTracker(username);
        }
      }, 300);
    }

    lastTap.current = now;
  };

  const isMobile = () => {
    return window.innerWidth <= 768;
  };
  return (
    <div className="tracker-container">
      {/* Модальное окно - предупреждение о командах */}
      {showTeamsWarning && (
        <div className="tracker-delete-modal">
          <div className="tracker-delete-modal-content">
            <h3>К данному пользователю привязаны следующие команды:</h3>
            <ul className="tracker-attached-teams-list">
              {attachedTeams.map((team, index) => (
                <li key={team.id} style={{ textAlign: 'center' }}>
                  <Link
                    to={`/teamcard/${team.id}`}
                    className="tracker-team-hyperlink"
                  >
                    {team.name}
                  </Link>
                </li>
              ))}
            </ul>
            <p>Вы уверены, что хотите безвозвратно удалить пользователя?</p>
            <div className="tracker-delete-modal-buttons">
              <button
                className="tracker-delete-modal-yes"
                onClick={closeTeamsWarning}
              >
                Да, удалить
              </button>
              <button
                className="tracker-delete-modal-no"
                onClick={cancelTeamsWarning}
              >
                Отмена
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Модальное окно - подтверждение удаления (без команд) */}
      {showDeleteConfirm && (
        <div className="tracker-delete-modal">
          <div className="tracker-delete-modal-content">
            <h3>Вы уверены, что хотите безвозвратно удалить пользователя @{userToDelete}?</h3>
            <div className="tracker-delete-modal-buttons">
              <button
                className="tracker-delete-modal-yes"
                onClick={confirmDeleteUser}
              >
                Да
              </button>
              <button
                className="tracker-delete-modal-no"
                onClick={() => setShowDeleteConfirm(false)}
              >
                Нет
              </button>
            </div>
          </div>
        </div>
      )}

      <Header userRole={userRole}></Header>
      <div className="Users-header-bottom-cont">
        <div className="Stream-search-cont">
          <div className="Stream-search-contcont">
            <button className="Stream-settings-pic2"></button>
            <input
              type="search"
              placeholder="Найти"
              className="Stream-search"
              value={searchQuery}
              onChange={handleSearchChange}
              onKeyDown={(e) => {
                if (e.key === "Escape") {
                  setSearchQuery("");
                }
              }}
            />
          </div>
        </div>

        <div className="filter-toggle-container">
          <button
            className={`filter-toggle ${showLockedOnly ? 'active' : ''}`}
            onClick={toggleShowLocked}
            onMouseEnter={() => setHoveredButton("filter")}
            onMouseLeave={() => setHoveredButton(null)}
            aria-label={showLockedOnly ? "Показать активных пользователей" : "Показать заблокированных пользователей"}
          >
            <div className="filter-toggle-icon">
              {showLockedOnly ? (
                <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M10 18a8 8 0 100-16 8 8 0 000 16zM6.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-3.293 3.293a1 1 0 101.414 1.414L10 11.414l3.293 3.293a1 1 0 001.414-1.414L11.414 10l3.293-3.293a1 1 0 00-1.414-1.414L10 8.586 6.707 5.293z" />
                </svg>
              ) : (
                <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" />
                </svg>
              )}
            </div>
            {hoveredButton === "filter" && (
              <div className="filter-tooltip">
                {showLockedOnly ? "Показать активных пользователей" : "Показать заблокированных пользователей"}
              </div>
            )}
          </button>
        </div>
        </div>
     <main 
      className="tracker-list-content" 
      aria-label="Close mobile menu"
>
  {error && <div className="error-message oval2">{error}</div>}

  <div className="trackerlist-grid">
    {trackers.length > 0 ? (
      trackers.map((tracker, index) => {
        const isEnabled = tracker.enabled;
        const itemClass = isEnabled ? "trackerlist-item-true" : "trackerlist-item-edit";
        const showMenu = hoveredTracker === tracker.username || activeMobileMenu === tracker.username;

        return (
          <div
            className={itemClass}
            key={tracker.username || index}
            onMouseEnter={() => !isMobile() && setHoveredTracker(tracker.username)}
            onMouseLeave={() => !isMobile() && setHoveredTracker(null)}
            onTouchStart={(e) => {
              e.preventDefault();
              if (isMobile()) {
                handleDoubleTap(tracker.username);
              }
            }}
            onClick={(e) => {
              /* istanbul ignore if */
              if (!isMobile()) {
                if (!e.target.closest('.trackerlist-edit-panel12')) {
                  // Validate username before navigation
                  if (isValidUsername(tracker.username)) {
                    window.location.href = `/profile/${encodeURIComponent(tracker.username)}`;
                  }
                }
              }
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                if (!isMobile()) {
                  if (!e.target.closest('.trackerlist-edit-panel12')) {
                    // Validate username before navigation
                    if (isValidUsername(tracker.username)) {
                      window.location.href = `/profile/${encodeURIComponent(tracker.username)}`;
                    }
                  }
                }
              }
            }}
            role="button"
            tabIndex={0}
          >
            <Link
              to="#"
                className="trackerlist-profile-link"
                onClick={(e) => {
                  e.preventDefault();
                }}
              >
              <div className="trackerlist-avatar" aria-hidden="true">
                {isEnabled ? (
                  <span className="green-checkmark" title="Включён">
                    <img src={trueIcon} alt="Подтверждён" />
                  </span>
                ) : (
                  <div className="edit-icon" title="Редактировать">
                    <img src={editIcon} alt="Редактировать" />
                  </div>
                )}

                <div className="trackerlist-text">
                  <div className="trackerlist-fio">{tracker.fullName}</div>
                  <div className="trackerlist-nick">@{tracker.username}</div>
                </div>
              </div>
            </Link>

            {showMenu && (
              <div className="trackerlist-edit-panel12" 
                onClick={(e) => e.stopPropagation()}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    e.stopPropagation();
                  }
                }}
                role="presentation"
                aria-hidden="true">
                {isEnabled ? (
                  <>
                    <button
                      className="confirm-button"
                      onClick={(e) => {
                        e.stopPropagation();
                        setHoveredTracker(null);
                        setActiveMobileMenu(null);
                      }}
                      onMouseEnter={() => setHoveredButton("confirm")}
                      onMouseLeave={() => setHoveredButton(null)}
                    >
                      <img
                        src={hoveredButton === "cancel" ? trueIcon2 : trueIcon}
                        alt="Оставить"
                      />
                    </button>
                    {hoveredButton === "cancel" && (
                      <span className="tooltip tooltip-red">Отключить</span>
                    )}

                    <button
                      className="cancel-button"
                      onClick={(e) => {
                        e.stopPropagation();
                        if (!window.confirm("Вы точно хотите отключить этого пользователя?")) {
                          return;
                        }
                        safeAction(tracker.username, toggleUserLock);
                        setActiveMobileMenu(null);
                      }}
                      onMouseEnter={() => setHoveredButton("cancel")}
                      onMouseLeave={() => setHoveredButton(null)}
                    >
                      <img
                        src={hoveredButton === "confirm" ? falseIcon2 : falseIcon}
                        alt="Отключить"
                      />
                    </button>
                    {hoveredButton === "confirm" && (
                      <span className="tooltip tooltip-green">Оставить</span>
                    )}
                  </>
                ) : showLockedOnly ? (
                  <>
                    <button
                      className="confirm-button"
                      onClick={(e) => {
                        e.stopPropagation();
                        safeAction(tracker.username, toggleUserLock);
                        setActiveMobileMenu(null);
                      }}
                      onMouseEnter={() => setHoveredButton("confirm")}
                      onMouseLeave={() => setHoveredButton(null)}
                    >
                      <img
                        src={hoveredButton === "cancel" ? trueIcon2 : trueIcon}
                        alt="Разблокировать"
                      />
                    </button>
                    {hoveredButton === "cancel" && (
                      <span className="tooltip tooltip-red">Удалить</span>
                    )}

                    <button
                      className="cancel-button"
                      onClick={(e) => {
                        e.stopPropagation();
                        safeAction(tracker.username, handleDeleteClick);
                        setActiveMobileMenu(null);
                      }}
                      onMouseEnter={() => setHoveredButton("cancel")}
                      onMouseLeave={() => setHoveredButton(null)}
                    >
                      <img
                        src={hoveredButton === "confirm" ? falseIcon2 : falseIcon}
                        alt="Удалить"
                      />
                    </button>
                    {hoveredButton === "confirm" && (
                      <span className="tooltip tooltip-green">Разблокировать</span>
                    )}
                  </>
                ) : (
                  <>
                    <button
                      className="confirm-button"
                      onClick={(e) => {
                        e.stopPropagation();
                        safeAction(tracker.username, confirmUser);
                        setActiveMobileMenu(null);
                      }}
                      onMouseEnter={() => setHoveredButton("confirm")}
                      onMouseLeave={() => setHoveredButton(null)}
                    >
                      <img
                        src={hoveredButton === "cancel" ? trueIcon2 : trueIcon}
                        alt="Подтвердить"
                      />
                    </button>
                    {hoveredButton === "cancel" && (
                      <span className="tooltip tooltip-red">Заблокировать</span>
                    )}

                    <button
                      className="cancel-button"
                      onClick={(e) => {
                        e.stopPropagation();
                        if (!window.confirm("Вы точно хотите заблокировать этого пользователя?")) {
                          return;
                        }
                        safeAction(tracker.username, toggleUserLock);
                        setActiveMobileMenu(null);
                      }}
                      onMouseEnter={() => setHoveredButton("cancel")}
                      onMouseLeave={() => setHoveredButton(null)}
                    >
                      <img
                        src={hoveredButton === "confirm" ? falseIcon2 : falseIcon}
                        alt="Отклонить"
                      />
                    </button>
                    {hoveredButton === "confirm" && (
                      <span className="tooltip tooltip-green">Принять</span>
                    )}
                  </>
                )}
              </div>
            )}
          </div>
        );
      })
    ) : (
      <p className="error-message oval2">
        {showLockedOnly
          ? "Нет заблокированных пользователей для отображения"
          : "Нет активных пользователей для отображения"}
      </p>
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
                  onClick={handleNextPage}
                  className="Stream-footer-button-5"
                  aria-label="Следующая страница"
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