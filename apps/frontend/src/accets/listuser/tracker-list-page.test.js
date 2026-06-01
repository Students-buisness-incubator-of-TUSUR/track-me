import React from 'react';
import { render, screen, fireEvent, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import { BrowserRouter } from 'react-router-dom';
import { MemoryRouter } from "react-router-dom";
import TrackerListPage from './TrackerListPage';
import { useSelector } from 'react-redux';

const mockUseGetUserInfo = jest.fn();
jest.mock('../../services/util', () => ({
  useGetUserInfo: () => mockUseGetUserInfo(),
}));

jest.mock('react-redux', () => ({
  ...jest.requireActual('react-redux'),
  useSelector: jest.fn(),
}));

// Мок валидации username
jest.mock('../../utils/validation', () => ({
  isValidUsername: jest.fn(() => true),
}));

// Моки статических ресурсов
jest.mock('./true.png', () => 'true.png');
jest.mock('./false.png', () => 'false.png');
jest.mock('./edit.png', () => 'edit.png');
jest.mock('./true2.png', () => 'true2.png');
jest.mock('./false2.png', () => 'false2.png');
jest.mock('./personal_account_1.png', () => 'personal_account_1.png');

beforeEach(() => {
  window.confirm = jest.fn(() => true);
  mockUseGetUserInfo.mockReturnValue({
    roles: ['SUPER_ADMIN'],
    username: "username12",
  });
  Object.defineProperty(window, 'innerWidth', {
    writable: true,
    configurable: true,
    value: 1024,
  });
});

// Мок useTrackerList
jest.mock('../hooks/useTrackerList', () => {
  const confirmUser = jest.fn();
  const toggleUserLock = jest.fn();
  const handleDeleteClick = jest.fn();
  const confirmDeleteUser = jest.fn();
  const setShowDeleteConfirm = jest.fn();
  const closeTeamsWarning = jest.fn();
  const cancelTeamsWarning = jest.fn();
  const setHoveredTracker = jest.fn();
  const setHoveredButton = jest.fn();
  const setSearchQuery = jest.fn();
  const setPage = jest.fn();

  return {
    __esModule: true,
    confirmUser,
    toggleUserLock,
    handleDeleteClick,
    confirmDeleteUser,
    setShowDeleteConfirm,
    closeTeamsWarning,
    cancelTeamsWarning,
    setHoveredTracker,
    setHoveredButton,
    setSearchQuery,
    setPage,
    useTrackerList: () => ({
      trackers: [
        { username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true },
        { username: 'testuser2', fullName: 'Test User 2', telegramId: 'test2', enabled: false },
      ],
      error: null,
      searchQuery: '',
      setSearchQuery,
      page: 0,
      setPage,
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: null,
      setHoveredTracker,
      hoveredButton: null,
      setHoveredButton,
      trackersPerPage: 5,
      confirmUser,
      toggleUserLock,
      handleDeleteClick,
      confirmDeleteUser,
      showDeleteConfirm: false,
      setShowDeleteConfirm,
      showTeamsWarning: false,
      attachedTeams: [],
      userToDelete: null,
      closeTeamsWarning,
      cancelTeamsWarning,
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
    }),
  };
});

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

const { setHoveredTracker, confirmUser, toggleUserLock, setSearchQuery, setPage } = require('../hooks/useTrackerList');
const { isValidUsername } = require('../../utils/validation');

const renderWithRouter = (ui, { route = '/' } = {}) => {
  window.history.pushState({}, 'Test page', route);
  return render(ui, {
    wrapper: ({ children }) => <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>,
  });
};

describe('TrackerListPage (объединённые тесты)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    isValidUsername.mockReturnValue(true);
  });

  test('рендерится и отображает заголовок', () => {
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    expect(screen.getByText('TrackMe')).toBeInTheDocument();
  });

  test('отображает пользователей и Telegram ID', () => {
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    expect(screen.getByText('Test User 1')).toBeInTheDocument();
    expect(screen.getByText('Test User 2')).toBeInTheDocument();
    
    const nick1 = screen.getByText((content, element) => {
      return element.className === 'trackerlist-nick' && content.includes('testuser1');
    });
    expect(nick1).toBeInTheDocument();
    
    const nick2 = screen.getByText((content, element) => {
      return element.className === 'trackerlist-nick' && content.includes('testuser2');
    });
    expect(nick2).toBeInTheDocument();
  });

  test('наведение на активный трекер вызывает setHoveredTracker', () => {
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const trackerItem = screen.getByText('Test User 1').closest('.trackerlist-item-true');
    fireEvent.mouseEnter(trackerItem);
    
    expect(setHoveredTracker).toHaveBeenCalledWith('testuser1');
  });

  test('наведение на неактивный трекер вызывает setHoveredTracker', () => {
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const trackerItem = screen.getByText('Test User 2').closest('.trackerlist-item-edit');
    fireEvent.mouseEnter(trackerItem);
    
    expect(setHoveredTracker).toHaveBeenCalledWith('testuser2');
  });

  test('поиск трекеров вызывает setSearchQuery и сбрасывает видимость', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [
        { username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true },
        { username: 'testuser2', fullName: 'Another User', telegramId: 'other', enabled: false },
      ],
      error: null,
      searchQuery: '',
      setSearchQuery,
      page: 0,
      setPage,
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: null,
      setHoveredTracker: jest.fn(),
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(),
      confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false,
      setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false,
      attachedTeams: [],
      userToDelete: null,
      closeTeamsWarning: jest.fn(),
      cancelTeamsWarning: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    const input = screen.getByPlaceholderText('Найти');
    fireEvent.change(input, { target: { value: 'Another' } });

    expect(setSearchQuery).toHaveBeenCalledWith('Another');
    expect(setPage).toHaveBeenCalledWith(0);
  });

  test('рендер ошибки при наличии error', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [],
      error: 'Ошибка загрузки',
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: null,
      setHoveredTracker: jest.fn(),
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(),
      confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false,
      setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false,
      attachedTeams: [],
      userToDelete: null,
      closeTeamsWarning: jest.fn(),
      cancelTeamsWarning: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    expect(screen.getByText('Ошибка загрузки')).toBeInTheDocument();
  });

  test('рендер сообщения при отсутствии трекеров', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: null,
      setHoveredTracker: jest.fn(),
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(),
      confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false,
      setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false,
      attachedTeams: [],
      userToDelete: null,
      closeTeamsWarning: jest.fn(),
      cancelTeamsWarning: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    expect(screen.getByText('Нет активных пользователей для отображения')).toBeInTheDocument();
  });

  test('кнопка показать больше вызывает setPage', () => {
    const setPage = jest.fn();
    const handleNextPage = jest.fn(() => setPage(page => page + 1));
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: Array.from({ length: 10 }, (_, i) => ({
        username: `user${i}`,
        fullName: `User ${i}`,
        telegramId: `tg${i}`,
        enabled: true,
      })),
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage,
      totalPages: 2,
      handleNextPage,
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: null,
      setHoveredTracker: jest.fn(),
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(),
      confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false,
      setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false,
      attachedTeams: [],
      userToDelete: null,
      closeTeamsWarning: jest.fn(),
      cancelTeamsWarning: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    const nextButtons = screen.getAllByRole('button', { name: 'Следующая страница' });
    const nextButton = nextButtons.find(button => button.classList.contains('Stream-footer-button-4'));
    fireEvent.click(nextButton);
    expect(handleNextPage).toHaveBeenCalled();
    expect(setPage).toHaveBeenCalledWith(expect.any(Function));
    const setPageArg = setPage.mock.calls[0][0];
    expect(setPageArg(0)).toBe(1);
  });

  test('клик по cancel (enabled) вызывает toggleUserLock', () => {
    const toggleUserLock = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'user1', fullName: 'Test', telegramId: 'test', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'user1', setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock,
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    const cancelButton = screen.getByAltText('Отключить');
    fireEvent.click(cancelButton);
    expect(isValidUsername).toHaveBeenCalledWith('user1');
    expect(toggleUserLock).toHaveBeenCalledWith('user1');
  });

  test('клик по confirm (not enabled) вызывает confirmUser', () => {
    const confirmUser = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'user2', fullName: 'User2', telegramId: 't2', enabled: false }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'user2', setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser, toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    const confirmBtn = screen.getByAltText('Подтвердить');
    fireEvent.click(confirmBtn);
    expect(isValidUsername).toHaveBeenCalledWith('user2');
    expect(confirmUser).toHaveBeenCalledWith('user2');
  });

  test('клик по cancel (not enabled) вызывает toggleUserLock', () => {
    const toggleUserLock = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'user2', fullName: 'User2', telegramId: 't2', enabled: false }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'user2', setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock,
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    const cancelBtn = screen.getByAltText('Отклонить');
    fireEvent.click(cancelBtn);
    expect(isValidUsername).toHaveBeenCalledWith('user2');
    expect(toggleUserLock).toHaveBeenCalledWith('user2');
  });

  test('наведение на кнопку подтверждения вызывает setHoveredButton', () => {
    const setHoveredButton = require('../hooks/useTrackerList').setHoveredButton;
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'hovered', fullName: 'Hover User', telegramId: 'hover', enabled: false }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'hovered', setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton, trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    const confirmBtn = screen.getByAltText('Подтвердить');
    fireEvent.mouseEnter(confirmBtn);
    expect(setHoveredButton).toHaveBeenCalledWith('confirm');

    fireEvent.mouseLeave(confirmBtn);
    expect(setHoveredButton).toHaveBeenCalledWith(null);
  });

  test('наведение на кнопку удаления вызывает setHoveredButton', () => {
    const setHoveredButton = require('../hooks/useTrackerList').setHoveredButton;
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'hovered', fullName: 'Hover User', telegramId: 'hover', enabled: false }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'hovered', setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton, trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    const cancelBtn = screen.getByAltText('Отклонить');
    fireEvent.mouseEnter(cancelBtn);
    expect(setHoveredButton).toHaveBeenCalledWith('cancel');

    fireEvent.mouseLeave(cancelBtn);
    expect(setHoveredButton).toHaveBeenCalledWith(null);
  });

  test('рендер тултипа "Отключить", когда hoveredButton === "cancel"', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'hovered', fullName: 'Hover User', telegramId: 'hover', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'hovered', setHoveredTracker: jest.fn(), hoveredButton: 'cancel',
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    expect(screen.getByText('Отключить')).toBeInTheDocument();
  });

  test('рендер тултипа "Удалить", "Разблокировать", "Заблокировать", "Принять" при наведении', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'hovered', fullName: 'Hover User', telegramId: 'hover', enabled: false }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'hovered', setHoveredTracker: jest.fn(), hoveredButton: 'confirm',
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    const greenTips = screen.queryAllByText(/Удалить|Разблокировать|Заблокировать|Принять/);
    expect(greenTips.length).toBeGreaterThan(0);
  });

  test('ввод в строку поиска вызывает setSearchQuery и сбрасывает page', () => {
    const setSearchQuery = jest.fn();
    const setPage = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'x', fullName: 'Alice', telegramId: 'alice', enabled: true }],
      error: null, searchQuery: '', setSearchQuery, page: 0, setPage,
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);
    fireEvent.change(screen.getByPlaceholderText('Найти'), { target: { value: 'Al' } });

    expect(setSearchQuery).toHaveBeenCalledWith('Al');
    expect(setPage).toHaveBeenCalledWith(0);
  });

  test('клик по confirm у включенного трекера вызывает setHoveredTracker(null)', () => {
    const setHoveredTracker = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'enabled1', fullName: 'Enabled', telegramId: 'tg', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'enabled1', setHoveredTracker, hoveredButton: 'cancel',
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);
    fireEvent.click(screen.getByAltText('Оставить'));
    expect(setHoveredTracker).toHaveBeenCalledWith(null);
  });

  test('клик по confirm у неактивного трекера вызывает confirmUser', () => {
    const confirmUser = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'disabled1', fullName: 'Disabled', telegramId: 'tg', enabled: false }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'disabled1', setHoveredTracker: jest.fn(), hoveredButton: 'cancel',
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser, toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);
    fireEvent.click(screen.getByAltText('Подтвердить'));
    expect(isValidUsername).toHaveBeenCalledWith('disabled1');
    expect(confirmUser).toHaveBeenCalledWith('disabled1');
  });

  test('клик по cancel у неактивного трекера вызывает toggleUserLock', () => {
    const toggleUserLock = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'disabled2', fullName: 'Disabled2', telegramId: 'tg2', enabled: false }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'disabled2', setHoveredTracker: jest.fn(), hoveredButton: 'confirm',
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock,
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);
    fireEvent.click(screen.getByAltText('Отклонить'));
    expect(isValidUsername).toHaveBeenCalledWith('disabled2');
    expect(toggleUserLock).toHaveBeenCalledWith('disabled2');
  });
});

test('клик по кнопке "Следующая страница" вызывает handleNextPage', () => {
  const setPage = jest.fn();
  const handleNextPage = jest.fn(() => setPage((page) => page + 1));
  require('../hooks/useTrackerList').useTrackerList = () => ({
    trackers: Array.from({ length: 10 }, (_, i) => ({
      username: `user${i}`,
      fullName: `User ${i}`,
      telegramId: `tg${i}`,
      enabled: true,
    })),
    error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage,
    totalPages: 2, handleNextPage, handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
    hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: null,
    setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
    handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
    showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
    showTeamsWarning: false, attachedTeams: [], userToDelete: null,
    closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
    showLockedOnly: false, toggleShowLocked: jest.fn(),
  });

  renderWithRouter(<TrackerListPage endpoint="/trackers" />);
  const nextButton = screen.getAllByRole('button', { name: 'Следующая страница' })[0];
  fireEvent.click(nextButton);
  expect(handleNextPage).toHaveBeenCalled();
  expect(setPage).toHaveBeenCalledWith(expect.any(Function));
  expect(setPage.mock.calls[0][0](0)).toBe(1);
});

describe('TrackerListPage - Coverage for Lines 121, 139-140, 153-154, 179-180, 245-271', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    isValidUsername.mockReturnValue(true);
  });

  test('рендер тултипа "Оставить" при наведении на confirm для активного трекера', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'enabled1', fullName: 'Enabled', telegramId: 'tg', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'enabled1', setHoveredTracker: jest.fn(), hoveredButton: 'confirm',
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    expect(screen.getByText('Оставить')).toBeInTheDocument();
  });

  test('рендер тултипа "Отключить" при наведении на cancel для активного трекера', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'enabled1', fullName: 'Enabled', telegramId: 'tg', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'enabled1', setHoveredTracker: jest.fn(), hoveredButton: 'cancel',
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    expect(screen.getByText('Отключить')).toBeInTheDocument();
  });

  test('пагинация не рендерится при отсутствии трекеров', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [], error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0,
      setPage: jest.fn(), totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(), hoveredTracker: null, setHoveredTracker: jest.fn(),
      hoveredButton: null, setHoveredButton: jest.fn(), trackersPerPage: 5,
      confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    expect(screen.queryByRole('button', { name: 'Следующая страница' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Предыдущая страница' })).not.toBeInTheDocument();
  });

  test('на первой странице не отображаются кнопки "Предыдущая" и "На 2 назад"', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: Array.from({ length: 15 }, (_, i) => ({
        username: `user${i}`, fullName: `User ${i}`, telegramId: `tg${i}`, enabled: true,
      })),
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 3, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    expect(screen.queryByRole('button', { name: 'Предыдущая страница' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Перейти на 2 страницы назад' })).not.toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Следующая страница' })).toHaveLength(2);
    expect(screen.getByRole('button', { name: 'Перейти на 2 страницы вперед' })).toBeInTheDocument();
  });

  test('на последней странице не отображаются кнопки "Следующая" и "На 2 вперед"', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: Array.from({ length: 15 }, (_, i) => ({
        username: `user${i}`, fullName: `User ${i}`, telegramId: `tg${i}`, enabled: true,
      })),
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 2, setPage: jest.fn(),
      totalPages: 3, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    expect(screen.getAllByRole('button', { name: 'Предыдущая страница' })).toHaveLength(2);
    expect(screen.getByRole('button', { name: 'Перейти на 2 страницы назад' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Следующая страница' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Перейти на 2 страницы вперед' })).not.toBeInTheDocument();
  });
});

describe("TrackerListPage userRole from localStorage", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  test("устанавливает роль SUPER_ADMIN из localStorage", () => {
    localStorage.setItem(
      "user",
      JSON.stringify({ roles: ["SUPER_ADMIN"] })
    );

    render(
      <MemoryRouter initialEntries={["/list-trackers"]}>
        <TrackerListPage endpoint="/test" />
      </MemoryRouter>
    );

    expect(screen.getByRole("button", { name: /Администраторы/i })).toBeInTheDocument();
  });
});

describe('Filter Toggle Button', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [
        { username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true },
        { username: 'testuser2', fullName: 'Test User 2', telegramId: 'test2', enabled: false },
      ],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });
  });

  test('кнопка фильтра отображается с правильными атрибутами', () => {
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const filterButton = screen.getByRole('button', { 
      name: /показать активных пользователей|показать заблокированных пользователей/i 
    });
    
    expect(filterButton).toBeInTheDocument();
    expect(filterButton).toHaveClass('filter-toggle');
    expect(filterButton).not.toHaveClass('active');
  });

  test('отображается иконка пользователя когда showLockedOnly false', () => {
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const filterIcon = document.querySelector('.filter-toggle-icon svg');
    expect(filterIcon).toBeInTheDocument();
    
    const svgPath = filterIcon.querySelector('path');
    expect(svgPath).toHaveAttribute('d', 'M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z');
  });

  test('контейнер кнопки фильтра имеет правильный класс', () => {
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const filterContainer = document.querySelector('.filter-toggle-container');
    expect(filterContainer).toBeInTheDocument();
  });
});

describe('Filter Toggle Button Integration', () => {
  test('полный цикл взаимодействия с кнопкой фильтра', () => {
    const mockToggleShowLocked = jest.fn();
    const mockSetHoveredButton = jest.fn();
    
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [
        { username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true },
      ],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: mockSetHoveredButton, trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: mockToggleShowLocked,
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const filterButton = screen.getByRole('button', { 
      name: /показать заблокированных пользователей/i 
    });

    fireEvent.mouseEnter(filterButton);
    expect(mockSetHoveredButton).toHaveBeenCalledWith('filter');

    fireEvent.click(filterButton);
    expect(mockToggleShowLocked).toHaveBeenCalledTimes(1);

    fireEvent.mouseLeave(filterButton);
    expect(mockSetHoveredButton).toHaveBeenCalledWith(null);
  });
});

describe('Filter Toggle Button Text and Functionality', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('отображает правильный текст тултипа при showLockedOnly: false', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: 'filter',
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    expect(screen.getByText('Показать заблокированных пользователей')).toBeInTheDocument();
  });

  test('отображает правильный текст тултипа при showLockedOnly: true', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: 'filter',
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: true, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    expect(screen.getByText('Показать активных пользователей')).toBeInTheDocument();
  });

  test('правильный aria-label при showLockedOnly: false', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const filterButton = screen.getByRole('button', { 
      name: /показать заблокированных пользователей/i 
    });
    
    expect(filterButton).toHaveAttribute('aria-label', 'Показать заблокированных пользователей');
  });

  test('правильный aria-label при showLockedOnly: true', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: true, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const filterButton = screen.getByRole('button', { 
      name: /показать активных пользователей/i 
    });
    
    expect(filterButton).toHaveAttribute('aria-label', 'Показать активных пользователей');
  });

  test('тултип появляется только при наведении на кнопку фильтра', () => {
    const mockSetHoveredButton = jest.fn();
    
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: mockSetHoveredButton, trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const filterButton = screen.getByRole('button', { 
      name: /показать заблокированных пользователей/i 
    });

    expect(screen.queryByText('Показать заблокированных пользователей')).not.toBeInTheDocument();

    fireEvent.mouseEnter(filterButton);
    expect(mockSetHoveredButton).toHaveBeenCalledWith('filter');
  });

  test('тултип скрывается при уходе курсора', () => {
    const mockSetHoveredButton = jest.fn();
    
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: 'filter',
      setHoveredButton: mockSetHoveredButton, trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const filterButton = screen.getByRole('button', { 
      name: /показать заблокированных пользователей/i 
    });

    fireEvent.mouseLeave(filterButton);
    expect(mockSetHoveredButton).toHaveBeenCalledWith(null);
  });

  test('кнопка имеет активный класс когда showLockedOnly: true', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: true, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const filterButton = screen.getByRole('button', { 
      name: /показать активных пользователей/i 
    });
    
    expect(filterButton).toHaveClass('filter-toggle', 'active');
  });

  test('клик по кнопке вызывает toggleShowLocked', () => {
    const mockToggleShowLocked = jest.fn();
    
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: mockToggleShowLocked,
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const filterButton = screen.getByRole('button', { 
      name: /показать заблокированных пользователей/i 
    });

    fireEvent.click(filterButton);
    expect(mockToggleShowLocked).toHaveBeenCalledTimes(1);
  });
});

describe('Filter messages', () => {
  test('отображает сообщение для активных пользователей при showLockedOnly: false', () => {
    require('../hooks/useTrackerList').useTrackerList = jest.fn().mockReturnValue({
      trackers: [], error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    expect(screen.getByText('Нет активных пользователей для отображения')).toBeInTheDocument();
  });

  test('отображает сообщение для заблокированных пользователей при showLockedOnly: true', () => {
    require('../hooks/useTrackerList').useTrackerList = jest.fn().mockReturnValue({
      trackers: [], error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: true, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    expect(screen.getByText('Нет заблокированных пользователей для отображения')).toBeInTheDocument();
  });
});

describe('Mobile Double Tap Interaction', () => {
  beforeEach(() => {
    Object.defineProperty(window, 'innerWidth', { writable: true, value: 375 });
    jest.useFakeTimers();
    isValidUsername.mockReturnValue(true);
  });

  test('двойное касание открывает профиль и предотвращает одиночное открытие меню', () => {
    const setHoveredTracker = jest.fn();
    const { useTrackerList } = require('../hooks/useTrackerList');
    useTrackerList.mockReturnValue({
      trackers: [{ username: 'mobileuser', fullName: 'Mobile User', telegramId: 'mobile', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker, hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    const originalLocation = window.location;
    delete window.location;
    window.location = { href: '' };

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    const trackerElement = screen.getByText('Mobile User').closest('[role="button"]');

    fireEvent.touchStart(trackerElement);
    jest.advanceTimersByTime(100);
    fireEvent.touchStart(trackerElement);

    expect(isValidUsername).toHaveBeenCalledWith('mobileuser');
    expect(window.location.href).toBe('/profile/mobileuser');
    expect(setHoveredTracker).not.toHaveBeenCalledWith('mobileuser');

    window.location = originalLocation;
  });
});

describe('Mobile Touch Events', () => {
  afterEach(() => {
    jest.useRealTimers();
  });

  test('одиночное касание открывает мобильное меню через 300 мс', () => {
    isValidUsername.mockReturnValue(true);
    const setHoveredTracker = jest.fn();
    require('../hooks/useTrackerList').useTrackerList.mockReturnValue({
      trackers: [{ username: 'mobileuser', fullName: 'Mobile User', telegramId: 'mobile', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker, hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    Object.defineProperty(window, 'innerWidth', { value: 375 });
    jest.useFakeTimers();

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    const trackerElement = screen.getByText('Mobile User').closest('[role="button"]');
    fireEvent.touchStart(trackerElement);

    act(() => {
      jest.advanceTimersByTime(300);
    });

    expect(setHoveredTracker).toHaveBeenCalledWith('mobileuser');
    jest.useRealTimers();
  });
});

test('на десктопе onMouseEnter вызывает setHoveredTracker', () => {
  const setHoveredTracker = jest.fn();
  require('../hooks/useTrackerList').useTrackerList.mockReturnValue({
    trackers: [{ username: 'user1', fullName: 'User One', telegramId: 'u1', enabled: true }],
    error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
    totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
    hoveredTracker: null, setHoveredTracker, hoveredButton: null,
    setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
    handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
    showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
    showTeamsWarning: false, attachedTeams: [], userToDelete: null,
    closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
    showLockedOnly: false, toggleShowLocked: jest.fn(),
  });

  Object.defineProperty(window, 'innerWidth', { value: 1024 });
  renderWithRouter(<TrackerListPage endpoint="/trackers" />);

  const trackerItem = screen.getByText('User One').closest('.trackerlist-item-true');
  fireEvent.mouseEnter(trackerItem);
  expect(setHoveredTracker).toHaveBeenCalledWith('user1');
});

test('на мобильном onMouseEnter НЕ вызывает setHoveredTracker', () => {
  const setHoveredTracker = jest.fn();
  require('../hooks/useTrackerList').useTrackerList.mockReturnValue({
    trackers: [{ username: 'user1', fullName: 'User One', telegramId: 'u1', enabled: true }],
    error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
    totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
    hoveredTracker: null, setHoveredTracker, hoveredButton: null,
    setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
    handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
    showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
    showTeamsWarning: false, attachedTeams: [], userToDelete: null,
    closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
    showLockedOnly: false, toggleShowLocked: jest.fn(),
  });

  Object.defineProperty(window, 'innerWidth', { value: 375 });
  renderWithRouter(<TrackerListPage endpoint="/trackers" />);

  const trackerItem = screen.getByText('User One').closest('.trackerlist-item-true');
  fireEvent.mouseEnter(trackerItem);
  expect(setHoveredTracker).not.toHaveBeenCalled();
});

test('клик по ссылке профиля предотвращает переход', () => {
  const originalHref = window.location.href;
  const setHoveredTracker = jest.fn();
  require('../hooks/useTrackerList').useTrackerList.mockReturnValue({
    trackers: [{ username: 'testuser', fullName: 'Test User', telegramId: 'test', enabled: true }],
    error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
    totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
    hoveredTracker: 'testuser', setHoveredTracker, hoveredButton: null,
    setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
    handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
    showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
    showTeamsWarning: false, attachedTeams: [], userToDelete: null,
    closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
    showLockedOnly: false, toggleShowLocked: jest.fn(),
  });

  Object.defineProperty(window, 'innerWidth', { value: 375 });
  renderWithRouter(<TrackerListPage endpoint="/trackers" />);

  const link = screen.getByText('Test User').closest('a');
  fireEvent.click(link);
  expect(window.location.href).toBe(originalHref);
});

test('клик по кнопке "Оставить" закрывает мобильное меню', () => {
  const setHoveredTracker = jest.fn();
  require('../hooks/useTrackerList').useTrackerList.mockReturnValue({
    trackers: [{ username: 'enabled1', fullName: 'Enabled', telegramId: 'tg', enabled: true }],
    error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
    totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
    hoveredTracker: 'enabled1', setHoveredTracker, hoveredButton: null,
    setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
    handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
    showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
    showTeamsWarning: false, attachedTeams: [], userToDelete: null,
    closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
    showLockedOnly: false, toggleShowLocked: jest.fn(),
  });

  renderWithRouter(<TrackerListPage endpoint="/trackers" />);
  const confirmButton = screen.getByAltText('Оставить');
  fireEvent.click(confirmButton);
  expect(setHoveredTracker).toHaveBeenCalledWith(null);
});

describe('Tracker Edit Panel Keyboard Accessibility', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 1024 });
  });

  test('панель edit panel имеет правильные атрибуты доступности', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser', fullName: 'Test User', telegramId: 'test', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'testuser', setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    const editPanel = document.querySelector('.trackerlist-edit-panel12');
    expect(editPanel).toBeInTheDocument();
    expect(editPanel).toHaveAttribute('role', 'presentation');
    expect(editPanel).toHaveAttribute('aria-hidden', 'true');
  });

  test('onKeyDown на панели edit panel предотвращает всплытие для Enter', () => {
    const mockStopPropagation = jest.fn();
    const mockPreventDefault = jest.fn();
    
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser', fullName: 'Test User', telegramId: 'test', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'testuser', setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    const editPanel = document.querySelector('.trackerlist-edit-panel12');
    const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true });
    event.preventDefault = mockPreventDefault;
    event.stopPropagation = mockStopPropagation;
    editPanel.dispatchEvent(event);
    expect(mockPreventDefault).toHaveBeenCalled();
    expect(mockStopPropagation).toHaveBeenCalled();
  });

  test('onKeyDown на панели edit panel предотвращает всплытие для Space', () => {
    const mockStopPropagation = jest.fn();
    const mockPreventDefault = jest.fn();
    
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser', fullName: 'Test User', telegramId: 'test', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'testuser', setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    const editPanel = document.querySelector('.trackerlist-edit-panel12');
    const event = new KeyboardEvent('keydown', { key: ' ', bubbles: true });
    event.preventDefault = mockPreventDefault;
    event.stopPropagation = mockStopPropagation;
    editPanel.dispatchEvent(event);
    expect(mockPreventDefault).toHaveBeenCalled();
    expect(mockStopPropagation).toHaveBeenCalled();
  });

  test('onKeyDown на панели edit panel игнорирует другие клавиши', () => {
    const mockStopPropagation = jest.fn();
    const mockPreventDefault = jest.fn();
    
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser', fullName: 'Test User', telegramId: 'test', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'testuser', setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    const editPanel = document.querySelector('.trackerlist-edit-panel12');
    const event = new KeyboardEvent('keydown', { key: 'A', bubbles: true });
    event.preventDefault = mockPreventDefault;
    event.stopPropagation = mockStopPropagation;
    editPanel.dispatchEvent(event);
    expect(mockPreventDefault).not.toHaveBeenCalled();
    expect(mockStopPropagation).not.toHaveBeenCalled();
  });

  test('onClick на панели edit panel предотвращает всплытие', () => {
    const mockStopPropagation = jest.fn();
    
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser', fullName: 'Test User', telegramId: 'test', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'testuser', setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    const editPanel = document.querySelector('.trackerlist-edit-panel12');
    const event = new MouseEvent('click', { bubbles: true });
    event.stopPropagation = mockStopPropagation;
    editPanel.dispatchEvent(event);
    expect(mockStopPropagation).toHaveBeenCalled();
  });
});

describe('Tracker Avatar Accessibility', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 1024 });
  });

  test('trackerlist-avatar имеет атрибут aria-hidden="true"', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser', fullName: 'Test User', telegramId: 'test', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    const avatar = document.querySelector('.trackerlist-avatar');
    expect(avatar).toHaveAttribute('aria-hidden', 'true');
  });

  test('клик по отключить отменяет действие если confirm возвращает false (enabled)', () => {
    window.confirm = jest.fn(() => false);
    const toggleUserLock = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'user1', fullName: 'User1', telegramId: 'tg1', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'user1', setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock,
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    fireEvent.click(screen.getByAltText('Отключить'));
    expect(window.confirm).toHaveBeenCalled();
    expect(toggleUserLock).not.toHaveBeenCalled();
  });

  test('клик по отклонить отменяет действие если confirm возвращает false (disabled)', () => {
    window.confirm = jest.fn(() => false);
    const toggleUserLock = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'user2', fullName: 'User2', telegramId: 'tg2', enabled: false }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'user2', setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock,
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    fireEvent.click(screen.getByAltText('Отклонить'));
    expect(window.confirm).toHaveBeenCalled();
    expect(toggleUserLock).not.toHaveBeenCalled();
  });
});

describe('Coverage for lines 200-203', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    isValidUsername.mockReturnValue(true);
    // Принудительно переопределяем мок для этого блока
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [
        { username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true },
        { username: 'testuser2', fullName: 'Test User 2', telegramId: 'test2', enabled: false },
      ],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: null,
      setHoveredTracker: jest.fn(),
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(),
      confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false,
      setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false,
      attachedTeams: [],
      userToDelete: null,
      closeTeamsWarning: jest.fn(),
      cancelTeamsWarning: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
    });
  });

  test('Enter на карточке вызывает переход в профиль', () => {
    const mockLocation = { href: '' };
    delete window.location;
    window.location = mockLocation;
    Object.defineProperty(window, 'innerWidth', { value: 1024 });
    
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const cards = document.querySelectorAll('[role="button"]');
    expect(cards.length).toBeGreaterThan(1);
    
    fireEvent.keyDown(cards[0], { key: 'Enter' });
    expect(isValidUsername).toHaveBeenCalledWith('testuser1');
    expect(window.location.href).toBe('/profile/testuser1');
  });

  test('Пробел на карточке вызывает переход в профиль', () => {
    const mockLocation = { href: '' };
    delete window.location;
    window.location = mockLocation;
    Object.defineProperty(window, 'innerWidth', { value: 1024 });
    
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const cards = document.querySelectorAll('[role="button"]');
    expect(cards.length).toBeGreaterThan(1);
    
    fireEvent.keyDown(cards[0], { key: ' ' });
    expect(isValidUsername).toHaveBeenCalledWith('testuser1');
    expect(window.location.href).toBe('/profile/testuser1');
  });

  test('Enter на мобильном НЕ вызывает переход', () => {
    const mockLocation = { href: '' };
    delete window.location;
    window.location = mockLocation;
    Object.defineProperty(window, 'innerWidth', { value: 375 });
    
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const card = document.querySelector('[role="button"]');
    expect(card).toBeInTheDocument();
    
    fireEvent.keyDown(card, { key: 'Enter' });
    expect(window.location.href).toBe('');
  });
});

describe('Delete confirmation modals', () => {
  test('отображает модальное окно с командами при showTeamsWarning: true', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser', fullName: 'Test User', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: true,
      attachedTeams: [{ id: '1', name: 'Team Alpha' }, { id: '2', name: 'Team Beta' }],
      userToDelete: 'testuser',
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);

    expect(screen.getByText(/К данному пользователю привязаны следующие команды/)).toBeInTheDocument();
    expect(screen.getByText('Team Alpha')).toBeInTheDocument();
    expect(screen.getByText('Team Beta')).toBeInTheDocument();
    expect(screen.getByText('Да, удалить')).toBeInTheDocument();
    expect(screen.getByText('Отмена')).toBeInTheDocument();
  });

  test('отображает модальное окно подтверждения удаления при showDeleteConfirm: true', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser', fullName: 'Test User', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: true, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: 'todelete',
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);

    expect(screen.getByText(/Вы уверены, что хотите безвозвратно удалить пользователя @todelete/)).toBeInTheDocument();
    expect(screen.getByText('Да')).toBeInTheDocument();
    expect(screen.getByText('Нет')).toBeInTheDocument();
  });

  test('кнопка "Нет" в модальном окне подтверждения вызывает setShowDeleteConfirm(false)', () => {
    const setShowDeleteConfirm = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser', fullName: 'Test User', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: true, setShowDeleteConfirm,
      showTeamsWarning: false, attachedTeams: [], userToDelete: 'todelete',
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    fireEvent.click(screen.getByText('Нет'));
    expect(setShowDeleteConfirm).toHaveBeenCalledWith(false);
  });

  test('кнопка "Да" в модальном окне подтверждения вызывает confirmDeleteUser', () => {
    const confirmDeleteUser = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser', fullName: 'Test User', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: null, setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(), confirmDeleteUser,
      showDeleteConfirm: true, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: 'todelete',
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    fireEvent.click(screen.getByText('Да'));
    expect(confirmDeleteUser).toHaveBeenCalled();
  });
});

describe('Locked mode delete button', () => {
  beforeEach(() => {
    isValidUsername.mockReturnValue(true);
  });

  test('кнопка "Удалить" вызывает handleDeleteClick в режиме showLockedOnly', () => {
    const handleDeleteClick = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'disabled1', fullName: 'Disabled User', enabled: false }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'disabled1', setHoveredTracker: jest.fn(), hoveredButton: 'cancel',
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick, confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: true, toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const deleteButton = screen.getByAltText('Удалить');
    fireEvent.click(deleteButton);
    expect(isValidUsername).toHaveBeenCalledWith('disabled1');
    expect(handleDeleteClick).toHaveBeenCalledWith('disabled1');
  });
});

describe('safeAction validation - invalid username', () => {
  test('не вызывает toggleUserLock если username невалидный', () => {
    isValidUsername.mockReturnValueOnce(false);
    
    const toggleUserLock = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'invalid<script>', fullName: 'Test', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'invalid<script>', setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock,
      handleDeleteClick: jest.fn(), confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    const cancelButton = screen.getByAltText('Отключить');
    fireEvent.click(cancelButton);
    
    expect(isValidUsername).toHaveBeenCalledWith('invalid<script>');
    expect(toggleUserLock).not.toHaveBeenCalled();
  });

  test('не вызывает handleDeleteClick если username невалидный', () => {
    isValidUsername.mockReturnValueOnce(false);
    
    const handleDeleteClick = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'invalid\nuser', fullName: 'Test', enabled: false }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'invalid\nuser', setHoveredTracker: jest.fn(), hoveredButton: 'cancel',
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), toggleUserLock: jest.fn(),
      handleDeleteClick, confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false, setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false, attachedTeams: [], userToDelete: null,
      closeTeamsWarning: jest.fn(), cancelTeamsWarning: jest.fn(),
      showLockedOnly: true, toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    const deleteButton = screen.getByAltText('Удалить');
    fireEvent.click(deleteButton);
    
    expect(isValidUsername).toHaveBeenCalledWith('invalid\nuser');
    expect(handleDeleteClick).not.toHaveBeenCalled();
  });

  test('Escape очищает поисковый запрос (setSearchQuery called with empty string)', () => {
    const setSearchQuery = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [],
      error: null,
      searchQuery: 'test',
      setSearchQuery,
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: null,
      setHoveredTracker: jest.fn(),
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      toggleUserLock: jest.fn(),
      handleDeleteClick: jest.fn(),
      confirmDeleteUser: jest.fn(),
      showDeleteConfirm: false,
      setShowDeleteConfirm: jest.fn(),
      showTeamsWarning: false,
      attachedTeams: [],
      userToDelete: null,
      closeTeamsWarning: jest.fn(),
      cancelTeamsWarning: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    const input = screen.getByPlaceholderText('Найти');
    expect(input).toBeInTheDocument();
    
    fireEvent.keyDown(input, { key: 'Escape' });
    expect(setSearchQuery).toHaveBeenCalledWith('');
  });
}); 
