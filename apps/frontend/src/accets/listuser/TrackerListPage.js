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
    handlePageJump,
    showLockedOnly,
    toggleShowLocked,
  } = useTrackerList(endpoint);
  
  const [userRole, setUserRole] = useState('');
    
  const [activeMobileMenu, setActiveMobileMenu] = useState(null);

  const lastTap = useRef(0); // Будет хранить время последнего касания
  const tapTimeout = useRef(null); // Будет хранить таймер


  const user = useGetUserInfo();
  useEffect(() => {
    setUserRole(user.roles[0]);
  }, [user]);


  const handleSearchChange = (event) => {
    setSearchQuery(event.target.value);
    setPage(0); // Reset page to 0 on search
  };


  // Закрыть меню при клике вне элемента
  // const closeMobileMenu = () => {
  //   setActiveMobileMenu(null);
  //   setHoveredTracker(null);
  // };

  const handleDoubleTap = (username) => {
  const now = Date.now(); // Текущее время в миллисекундах
  const timeDiff = now - lastTap.current;

  // Если прошло меньше 300 мс — это двойное касание
  if (timeDiff < 300) {
    // Сбрасываем таймер, если он был
    if (tapTimeout.current) {
      clearTimeout(tapTimeout.current);
    }

    /* istanbul ignore if */
    if (activeMobileMenu === username) {
      setActiveMobileMenu(null);
      setHoveredTracker(null);
    }

    // В любом случае — переходим в профиль
    window.location.href = `/profile/${username}`;
  } else {
    // Это одиночное касание — ставим таймер
    tapTimeout.current = setTimeout(() => {
      // Через 300 мс проверяем: если не было второго касания — обрабатываем как одиночное
      /* istanbul ignore if */
      if (activeMobileMenu === username) {
        // Уже открыто — закрываем
        setActiveMobileMenu(null);
        setHoveredTracker(null);
      } else {
        // Закрыто — открываем
        setActiveMobileMenu(username);
        setHoveredTracker(username);
      }
    }, 300);
  }

  // Сохраняем время последнего касания
  lastTap.current = now;
};

  // Определяем, мобильное ли устройство
  const isMobile = () => {
    return window.innerWidth <= 768;
  };
  return (
    <div className="tracker-container">
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
            />
          </div>
        </div>

        {/* Добавляем кнопку переключения фильтра */}
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
                  {/* Иконка крестика (заблокированные) */}
                  <path d="M10 18a8 8 0 100-16 8 8 0 000 16zM6.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-3.293 3.293a1 1 0 101.414 1.414L10 11.414l3.293 3.293a1 1 0 001.414-1.414L11.414 10l3.293-3.293a1 1 0 00-1.414-1.414L10 8.586 6.707 5.293z" />
                </svg>
              ) : (
                <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                  {/* Иконка пользователя (активные) */}
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
      // onClick={closeMobileMenu}
      // onKeyDown={(e) => {
      //   if (e.key === 'Enter' || e.key === ' ') {
      //     e.preventDefault();
      //     closeMobileMenu();
      //   }
      //   if (e.key === 'Escape') {
      //     closeMobileMenu();
      //   }
      // }}
      // tabIndex={-1}
      // role="button"
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
              e.preventDefault(); // Важно: предотвращаем стандартное поведение
              if (isMobile()) {
                handleDoubleTap(tracker.username);
              }
            }}
            onClick={(e) => {
              /* istanbul ignore if */
              if (!isMobile()) {
                // Разрешаем переход, только если клик НЕ по панели
                if (!e.target.closest('.trackerlist-edit-panel12')) {
                  window.location.href = `/profile/${tracker.username}`;
                }
              }
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                if (!isMobile()) {
                  if (!e.target.closest('.trackerlist-edit-panel12')) {
                    window.location.href = `/profile/${tracker.username}`;
                  }
                }
              }
            }}
            role="button"
            tabIndex={0}
          >
            {/* ---------- КЛИКАБЕЛЬНАЯ ССЫЛКА НА ПРОФИЛЬ ---------- */}
            <Link
              to="#"
                className="trackerlist-profile-link"
                onClick={(e) => {
                  e.preventDefault(); // Блокируем переход
                  // e.stopPropagation();
                }}
                // onMouseEnter={(e) => e.stopPropagation()}
              >
              <div className="trackerlist-avatar" aria-hidden="true">
                {/* Иконка статуса */}
                {isEnabled ? (
                  <span className="green-checkmark" title="Включён">
                    <img src={trueIcon} alt="Подтверждён" />
                  </span>
                ) : (
                  <div className="edit-icon" title="Редактировать">
                    <img src={editIcon} alt="Редактировать" />
                  </div>
                )}

                {/* Текст */}
                <div className="trackerlist-text">
                  <div className="trackerlist-fio">{tracker.fullName}</div>
                  <div className="trackerlist-nick">@{tracker.username}</div>
                </div>
              </div>
            </Link>

            {/* ---------- ПАНЕЛЬ ДЕЙСТВИЙ ПРИ НАВЕДЕНИИ ИЛИ LONG PRESS ---------- */}
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
                    {/* Оставить (просто закрыть) */}
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
                      <span className="tooltip tooltip-red">Удалить</span>
                    )}

                    {/* Удалить */}
                    <button
                      className="cancel-button"
                      onClick={(e) => {
                        e.stopPropagation();
                        if (!window.confirm("Вы точно хотите удалить этого пользователя?")) {
                          return;
                        }
                        deleteUser(tracker.username);
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
                      <span className="tooltip tooltip-green">Оставить</span>
                    )}
                  </>
                ) : (
                  <>
                    {/* Подтвердить */}
                    <button
                      className="confirm-button"
                      onClick={(e) => {
                        e.stopPropagation();
                        confirmUser(tracker.username);
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
                      <span className="tooltip tooltip-red">Удалить</span>
                    )}

                    {/* Отклонить */}
                    <button
                      className="cancel-button"
                      onClick={(e) => {
                        e.stopPropagation();
                        if (!window.confirm("Вы точно хотите удалить этого пользователя?")) {
                          return;
                        }
                        deleteUser(tracker.username);
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
                      <span className="tooltip tooltip-green">Разблокировать</span>
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
