import React from 'react';
import { render, screen, fireEvent, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import { BrowserRouter } from 'react-router-dom';
import { MemoryRouter } from "react-router-dom";
import TrackerListPage from './TrackerListPage';

// Моки статических ресурсов
jest.mock('./true.png', () => 'true.png');
jest.mock('./false.png', () => 'false.png');
jest.mock('./edit.png', () => 'edit.png');
jest.mock('./true2.png', () => 'true2.png');
jest.mock('./false2.png', () => 'false2.png');
jest.mock('./personal_account_1.png', () => 'personal_account_1.png');

// Мок useTrackerListв
jest.mock('../hooks/useTrackerList', () => {
  const confirmUser = jest.fn();
  const deleteUser = jest.fn();
  const setHoveredTracker = jest.fn();
  const setHoveredButton = jest.fn();
  const setSearchQuery = jest.fn();
  const setPage = jest.fn();

  return {
    __esModule: true,
    confirmUser,
    deleteUser,
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
      deleteUser,
    }),
  };
});
const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));
const { setHoveredTracker, confirmUser, deleteUser, setSearchQuery, setPage } = require('../hooks/useTrackerList');

const renderWithRouter = (ui, { route = '/' } = {}) => {
  window.history.pushState({}, 'Test page', route);
  return render(ui, { wrapper: BrowserRouter });
};

describe('TrackerListPage (объединённые тесты)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('рендерится и отображает заголовок', () => {
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    expect(screen.getByText('TrackMe')).toBeInTheDocument();
  });

  test('отображает пользователей и Telegram ID', () => {
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    expect(screen.getByText('Test User 1')).toBeInTheDocument();
    expect(screen.getByText('testuser1')).toBeInTheDocument();
    expect(screen.getByText('Test User 2')).toBeInTheDocument();
    expect(screen.getByText('@testuser2')).toBeInTheDocument();
  });

  test('клик по активному трекеру вызывает setHoveredTracker', () => {
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    fireEvent.click(screen.getByText('Test User 1'));
    expect(setHoveredTracker).toHaveBeenCalledWith('testuser1');
  });

  test('клик по неактивному трекеру вызывает setHoveredTracker', () => {
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    fireEvent.click(screen.getByText('Test User 2'));
    expect(setHoveredTracker).toHaveBeenCalledWith('testuser2');
  });

  test('открывает меню профиля', () => {
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    const profileBtn = screen.getByAltText('Профиль').closest('button');
    fireEvent.click(profileBtn);
    expect(screen.getByText('Личный кабинет')).toBeInTheDocument();
    expect(screen.getByText('Выход')).toBeInTheDocument();
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
      deleteUser: jest.fn(),
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
      deleteUser: jest.fn(),
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
      deleteUser: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    expect(screen.getByText('Нет трекеров для отображения')).toBeInTheDocument();
  });

  test('кнопка показать больше вызывает setPage', () => {
  const setPage = jest.fn();
  const handleNextPage = jest.fn(() => setPage(page => page + 1)); // Mock handleNextPage
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
    deleteUser: jest.fn(),
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

  test('handleLogout очищает localStorage', () => {
    const TrackerListPage = require('./TrackerListPage').default;
    Storage.prototype.removeItem = jest.fn();

    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    const profileBtn = screen.getByAltText('Профиль').closest('button');
    fireEvent.click(profileBtn);
    const logoutLink = screen.getByText('Выход');
    fireEvent.click(logoutLink);

    expect(localStorage.removeItem).toHaveBeenCalledWith('user');
    expect(localStorage.removeItem).toHaveBeenCalledWith('userRole');
    expect(localStorage.removeItem).toHaveBeenCalledWith('streamName');
    expect(localStorage.removeItem).toHaveBeenCalledWith('streamId');
    expect(localStorage.removeItem).toHaveBeenCalledWith('streamSDate');
    expect(localStorage.removeItem).toHaveBeenCalledWith('streamEDate');
  });

  test('клик по confirm (enabled) вызывает setHoveredTracker(null)', () => {
    const setHoveredTracker = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'user1', fullName: 'Test', telegramId: 'test', enabled: true }],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'user1',
      setHoveredTracker,
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    const confirmButton = screen.getByAltText('Оставить');
    fireEvent.click(confirmButton);
    expect(setHoveredTracker).toHaveBeenCalledWith(null);
  });

  test('клик по cancel (enabled) вызывает deleteUser', () => {
    const deleteUser = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'user1', fullName: 'Test', telegramId: 'test', enabled: true }],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'user1',
      setHoveredTracker: jest.fn(),
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser,
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    const cancelButton = screen.getByAltText('Удалить');
    fireEvent.click(cancelButton);
    expect(deleteUser).toHaveBeenCalledWith('user1');
  });

  test('клик по confirm (not enabled) вызывает confirmUser', () => {
    const confirmUser = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'user2', fullName: 'User2', telegramId: 't2', enabled: false }],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'user2',
      setHoveredTracker: jest.fn(),
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser,
      deleteUser: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    const confirmBtn = screen.getByAltText('Подтвердить');
    fireEvent.click(confirmBtn);
    expect(confirmUser).toHaveBeenCalledWith('user2');
  });

  test('клик по cancel (not enabled) вызывает deleteUser', () => {
    const deleteUser = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'user2', fullName: 'User2', telegramId: 't2', enabled: false }],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'user2',
      setHoveredTracker: jest.fn(),
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser,
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    const cancelBtn = screen.getByAltText('Отклонить');
    fireEvent.click(cancelBtn);
    expect(deleteUser).toHaveBeenCalledWith('user2');
  });

  test('наведение на кнопку подтверждения вызывает setHoveredButton', () => {
    const setHoveredButton = require('../hooks/useTrackerList').setHoveredButton;
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'hovered', fullName: 'Hover User', telegramId: 'hover', enabled: false }],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'hovered',
      setHoveredTracker: jest.fn(),
      hoveredButton: null,
      setHoveredButton,
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
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
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'hovered',
      setHoveredTracker: jest.fn(),
      hoveredButton: null,
      setHoveredButton,
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    const cancelBtn = screen.getByAltText('Отклонить');
    fireEvent.mouseEnter(cancelBtn);
    expect(setHoveredButton).toHaveBeenCalledWith('cancel');

    fireEvent.mouseLeave(cancelBtn);
    expect(setHoveredButton).toHaveBeenCalledWith(null);
  });

  test('рендер тултипа "Удалить", когда hoveredButton === "cancel"', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'hovered', fullName: 'Hover User', telegramId: 'hover', enabled: true }],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'hovered',
      setHoveredTracker: jest.fn(),
      hoveredButton: 'cancel',
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    expect(screen.getByText('Удалить')).toBeInTheDocument();
  });

  test('рендер тултипа "Оставить" или "Подтвердить" при наведении', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'hovered', fullName: 'Hover User', telegramId: 'hover', enabled: false }],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'hovered',
      setHoveredTracker: jest.fn(),
      hoveredButton: 'confirm',
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    const greenTips = screen.queryAllByText(/Оставить|Подтвердить/);
    expect(greenTips.length).toBeGreaterThan(0);
  });

  test('ввод в строку поиска вызывает setSearchQuery и сбрасывает page', () => {
    const setSearchQuery = jest.fn();
    const setPage = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'x', fullName: 'Alice', telegramId: 'alice', enabled: true }],
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
      deleteUser: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);
    fireEvent.change(screen.getByPlaceholderText('Найти'), { target: { value: 'Al' } });

    expect(setSearchQuery).toHaveBeenCalledWith('Al');
    expect(setPage).toHaveBeenCalledWith(0);
  });

  test('нажатие Enter на активном трекере вызывает setHoveredTracker', () => {
    const setHoveredTracker = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'active1', fullName: 'Active', telegramId: 'act', enabled: true }],
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
      setHoveredTracker,
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);
    fireEvent.keyDown(screen.getByText('Active').closest('[role="button"]'), { key: 'Enter' });
    expect(setHoveredTracker).toHaveBeenCalledWith('active1');
  });

  test('клик по confirm у включенного трекера вызывает setHoveredTracker(null)', () => {
    const setHoveredTracker = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'enabled1', fullName: 'Enabled', telegramId: 'tg', enabled: true }],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'enabled1',
      setHoveredTracker,
      hoveredButton: 'cancel',
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
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
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'disabled1',
      setHoveredTracker: jest.fn(),
      hoveredButton: 'cancel',
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser,
      deleteUser: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);
    fireEvent.click(screen.getByAltText('Подтвердить'));
    expect(confirmUser).toHaveBeenCalledWith('disabled1');
  });

  test('клик по cancel у неактивного трекера вызывает deleteUser', () => {
    const deleteUser = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'disabled2', fullName: 'Disabled2', telegramId: 'tg2', enabled: false }],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'disabled2',
      setHoveredTracker: jest.fn(),
      hoveredButton: 'confirm',
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser,
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);
    fireEvent.click(screen.getByAltText('Отклонить'));
    expect(deleteUser).toHaveBeenCalledWith('disabled2');
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
    deleteUser: jest.fn(),
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
  });

  // Test for Line 121: onKeyDown with Space for enabled tracker
  test('нажатие Space на активном трекере вызывает setHoveredTracker', () => {
    const setHoveredTracker = require('../hooks/useTrackerList').setHoveredTracker;
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'active1', fullName: 'Active', telegramId: 'act', enabled: true }],
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
      setHoveredTracker,
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    const trackerElement = screen.getByText('Active').closest('[role="button"]');
    fireEvent.keyDown(trackerElement, { key: ' ' });
    expect(setHoveredTracker).toHaveBeenCalledWith('active1');
  });

  // Test for Lines 139-140: Confirm button hover tooltip for enabled tracker
  test('рендер тултипа "Оставить" при наведении на confirm для активного трекера', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'enabled1', fullName: 'Enabled', telegramId: 'tg', enabled: true }],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'enabled1',
      setHoveredTracker: jest.fn(),
      hoveredButton: 'confirm',
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    expect(screen.getByText('Оставить')).toBeInTheDocument();
  });

  // Test for Lines 153-154: Cancel button hover tooltip for enabled tracker
  test('рендер тултипа "Удалить" при наведении на cancel для активного трекера', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'enabled1', fullName: 'Enabled', telegramId: 'tg', enabled: true }],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'enabled1',
      setHoveredTracker: jest.fn(),
      hoveredButton: 'cancel',
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    expect(screen.getByText('Удалить')).toBeInTheDocument();
  });

  // Test for Lines 179-180: onKeyDown with Space for disabled tracker
  test('нажатие Space на неактивном трекере вызывает setHoveredTracker', () => {
    const setHoveredTracker = require('../hooks/useTrackerList').setHoveredTracker;
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'disabled1', fullName: 'Disabled', telegramId: 'tg', enabled: false }],
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
      setHoveredTracker,
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    const trackerElement = screen.getByText('Disabled').closest('[role="button"]');
    fireEvent.keyDown(trackerElement, { key: ' ' });
    expect(setHoveredTracker).toHaveBeenCalledWith('disabled1');
  });

 

  // Test conditional rendering of pagination buttons
  test('пагинация не рендерится при отсутствии трекеров', () => {
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
      deleteUser: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    expect(screen.queryByRole('button', { name: 'Следующая страница' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Предыдущая страница' })).not.toBeInTheDocument();
  });

  // Test pagination buttons visibility on first page
  test('на первой странице не отображаются кнопки "Предыдущая" и "На 2 назад"', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: Array.from({ length: 15 }, (_, i) => ({
        username: `user${i}`,
        fullName: `User ${i}`,
        telegramId: `tg${i}`,
        enabled: true,
      })),
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0, // First page
      setPage: jest.fn(),
      totalPages: 3,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: null,
      setHoveredTracker: jest.fn(),
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    expect(screen.queryByRole('button', { name: 'Предыдущая страница' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Перейти на 2 страницы назад' })).not.toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Следующая страница' })).toHaveLength(2);
    expect(screen.getByRole('button', { name: 'Перейти на 2 страницы вперед' })).toBeInTheDocument();
  });

  // Test pagination buttons visibility on last page
  test('на последней странице не отображаются кнопки "Следующая" и "На 2 вперед"', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: Array.from({ length: 15 }, (_, i) => ({
        username: `user${i}`,
        fullName: `User ${i}`,
        telegramId: `tg${i}`,
        enabled: true,
      })),
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 2, // Last page
      setPage: jest.fn(),
      totalPages: 3,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: null,
      setHoveredTracker: jest.fn(),
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    expect(screen.getAllByRole('button', { name: 'Предыдущая страница' })).toHaveLength(2);
    expect(screen.getByRole('button', { name: 'Перейти на 2 страницы назад' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Следующая страница' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Перейти на 2 страницы вперед' })).not.toBeInTheDocument();
  });
});
describe('Header Logo and Title Navigation', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('логотип отображается и имеет правильные атрибуты', () => {
    render(
      <BrowserRouter>
        <TrackerListPage endpoint="/trackers" />
      </BrowserRouter>
    );

    const logoElement = document.querySelector('.Stream-header-logo');
    expect(logoElement).toBeInTheDocument();
    expect(logoElement).toHaveAttribute('role', 'button');
    expect(logoElement).toHaveAttribute('tabindex', '0');
    expect(logoElement).toHaveAttribute('aria-label', 'Вернуться на главную страницу');
    expect(logoElement).toHaveStyle('cursor: pointer');
  });

  test('заголовок отображается и имеет правильные атрибуты', () => {
    render(
      <BrowserRouter>
        <TrackerListPage endpoint="/trackers" />
      </BrowserRouter>
    );

    const titleElement = screen.getByText('TrackMe');
    expect(titleElement).toBeInTheDocument();
    expect(titleElement).toHaveClass('Stream-title');
    expect(titleElement).toHaveAttribute('role', 'button');
    expect(titleElement).toHaveAttribute('tabindex', '0');
    expect(titleElement).toHaveAttribute('aria-label', 'Вернуться на главную страницу');
    expect(titleElement).toHaveStyle('cursor: pointer');
  });

  test('клик по логотипу вызывает navigate с путем "/streams"', () => {
    render(
      <BrowserRouter>
        <TrackerListPage endpoint="/trackers" />
      </BrowserRouter>
    );

    const logoElement = document.querySelector('.Stream-header-logo');
    fireEvent.click(logoElement);
    
    expect(mockNavigate).toHaveBeenCalledWith('/streams');
  });

  test('клик по заголовку вызывает navigate с путем "/streams"', () => {
    render(
      <BrowserRouter>
        <TrackerListPage endpoint="/trackers" />
      </BrowserRouter>
    );

    const titleElement = screen.getByText('TrackMe');
    fireEvent.click(titleElement);
    
    expect(mockNavigate).toHaveBeenCalledWith('/streams');
  });

  test('нажатие Enter на логотипе вызывает navigate', () => {
    render(
      <BrowserRouter>
        <TrackerListPage endpoint="/trackers" />
      </BrowserRouter>
    );

    const logoElement = document.querySelector('.Stream-header-logo');
    fireEvent.keyDown(logoElement, { key: 'Enter' });
    
    expect(mockNavigate).toHaveBeenCalledWith('/streams');
  });

  test('нажатие Space на логотипе вызывает navigate', () => {
    render(
      <BrowserRouter>
        <TrackerListPage endpoint="/trackers" />
      </BrowserRouter>
    );

    const logoElement = document.querySelector('.Stream-header-logo');
    fireEvent.keyDown(logoElement, { key: ' ' });
    
    expect(mockNavigate).toHaveBeenCalledWith('/streams');
  });

  test('нажатие Enter на заголовке вызывает navigate', () => {
    render(
      <BrowserRouter>
        <TrackerListPage endpoint="/trackers" />
      </BrowserRouter>
    );

    const titleElement = screen.getByText('TrackMe');
    fireEvent.keyDown(titleElement, { key: 'Enter' });
    
    expect(mockNavigate).toHaveBeenCalledWith('/streams');
  });

  test('нажатие Space на заголовке вызывает navigate', () => {
    render(
      <BrowserRouter>
        <TrackerListPage endpoint="/trackers" />
      </BrowserRouter>
    );

    const titleElement = screen.getByText('TrackMe');
    fireEvent.keyDown(titleElement, { key: ' ' });
    
    expect(mockNavigate).toHaveBeenCalledWith('/streams');
  });

  test('нажатие других клавиш на логотипе не вызывает navigate', () => {
    render(
      <BrowserRouter>
        <TrackerListPage endpoint="/trackers" />
      </BrowserRouter>
    );

    const logoElement = document.querySelector('.Stream-header-logo');
    fireEvent.keyDown(logoElement, { key: 'Escape' });
    fireEvent.keyDown(logoElement, { key: 'Tab' });
    fireEvent.keyDown(logoElement, { key: 'a' });
    
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  test('нажатие других клавиш на заголовке не вызывает navigate', () => {
    render(
      <BrowserRouter>
        <TrackerListPage endpoint="/trackers" />
      </BrowserRouter>
    );

    const titleElement = screen.getByText('TrackMe');
    fireEvent.keyDown(titleElement, { key: 'Escape' });
    fireEvent.keyDown(titleElement, { key: 'Tab' });
    fireEvent.keyDown(titleElement, { key: 'a' });
    
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  test('логотип и заголовок доступны для фокусировки', () => {
    render(
      <BrowserRouter>
        <TrackerListPage endpoint="/trackers" />
      </BrowserRouter>
    );

    const logoElement = document.querySelector('.Stream-header-logo');
    const titleElement = screen.getByText('TrackMe');
    
    expect(logoElement).toHaveAttribute('tabindex', '0');
    expect(titleElement).toHaveAttribute('tabindex', '0');
  });

  test('элементы имеют семантическую роль button', () => {
    render(
      <BrowserRouter>
        <TrackerListPage endpoint="/trackers" />
      </BrowserRouter>
    );

    const logoElement = document.querySelector('.Stream-header-logo');
    const titleElement = screen.getByText('TrackMe');
    
    expect(logoElement).toHaveAttribute('role', 'button');
    expect(titleElement).toHaveAttribute('role', 'button');
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

    // проверяем, что появилась кнопка "Администраторы"
    expect(screen.getByRole("button", { name: /Администраторы/i })).toBeInTheDocument();
  });

  test("не устанавливает роль, если localStorage пустой", () => {
    render(
      <MemoryRouter initialEntries={["/list-trackers"]}>
        <TrackerListPage endpoint="/test" />
      </MemoryRouter>
    );

    // не должно быть кнопки "Администраторы", т.к. userRole не установлен
    expect(screen.queryByRole("button", { name: /Администраторы/i })).not.toBeInTheDocument();
  });
});