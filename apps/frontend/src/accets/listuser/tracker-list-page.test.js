import React from 'react';
import { render, screen, fireEvent, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import { BrowserRouter } from 'react-router-dom';
import TrackerListPage from './TrackerListPage';

// Моки статических ресурсов (иконки и изображения)
jest.mock('./true.png', () => 'true.png');
jest.mock('./false.png', () => 'false.png');
jest.mock('./edit.png', () => 'edit.png');
jest.mock('./true2.png', () => 'true2.png');
jest.mock('./false2.png', () => 'false2.png');
jest.mock('./personal_account_1.png', () => 'personal_account_1.png');

// Мок useTrackerList
jest.mock('../hooks/useTrackerList', () => {
  const confirmUser = jest.fn();
  const deleteUser = jest.fn();
  const setHoveredTracker = jest.fn();
  const setHoveredButton = jest.fn();

  return {
    __esModule: true,
    confirmUser,
    deleteUser,
    setHoveredTracker,
    setHoveredButton,
    useTrackerList: () => ({
      trackers: [
        { username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true },
        { username: 'testuser2', fullName: 'Test User 2', telegramId: 'test2', enabled: false },
      ],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      visibleTrackersStart: 0,
      setVisibleTrackersStart: jest.fn(),
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

const { setHoveredTracker, confirmUser, deleteUser } = require('../hooks/useTrackerList');

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
    expect(screen.getByText('@test1')).toBeInTheDocument();
    expect(screen.getByText('Test User 2')).toBeInTheDocument();
    expect(screen.getByText('@test2')).toBeInTheDocument();
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
  const profileBtn = screen.getByAltText("Профиль").closest("button");
  fireEvent.click(profileBtn);
  expect(screen.getByText("Личный кабинет")).toBeInTheDocument();
  expect(screen.getByText("Выход")).toBeInTheDocument();
});
test('поиск трекеров вызывает setSearchQuery и сбрасывает видимость', () => {
  const setSearchQuery = jest.fn();
  const setVisibleTrackersStart = jest.fn();

  require('../hooks/useTrackerList').useTrackerList = () => ({
    trackers: [
      { username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true },
      { username: 'testuser2', fullName: 'Another User', telegramId: 'other', enabled: false },
    ],
    error: null,
    searchQuery: '',
    setSearchQuery,
    visibleTrackersStart: 0,
    setVisibleTrackersStart,
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

  const input = screen.getByPlaceholderText("Найти");
  fireEvent.change(input, { target: { value: "Another" } });

  expect(setSearchQuery).toHaveBeenCalledWith("Another");
  expect(setVisibleTrackersStart).toHaveBeenCalledWith(0);
});


test('рендер ошибки при наличии error', () => {
  require('../hooks/useTrackerList').useTrackerList = () => ({
    trackers: [],
    error: "Ошибка загрузки",
    searchQuery: "",
    setSearchQuery: jest.fn(),
    visibleTrackersStart: 0,
    setVisibleTrackersStart: jest.fn(),
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

  expect(screen.getByText("Ошибка загрузки")).toBeInTheDocument();
});

test('рендер сообщения при отсутствии трекеров', () => {
  require('../hooks/useTrackerList').useTrackerList = () => ({
    trackers: [],
    error: null,
    searchQuery: "",
    setSearchQuery: jest.fn(),
    visibleTrackersStart: 0,
    setVisibleTrackersStart: jest.fn(),
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

  expect(screen.getByText("Нет трекеров для отображения")).toBeInTheDocument();
});

test('кнопка показать больше вызывает setVisibleTrackersStart', () => {
  const setVisibleTrackersStart = jest.fn();

  require('../hooks/useTrackerList').useTrackerList = () => ({
    trackers: new Array(10).fill({ username: "u", fullName: "f", telegramId: "t", enabled: true }),
    error: null,
    searchQuery: "",
    setSearchQuery: jest.fn(),
    visibleTrackersStart: 0,
    setVisibleTrackersStart,
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

  const buttons = screen.getAllByRole("button");
  const lastButton = buttons[buttons.length - 1];
  fireEvent.click(lastButton);
  expect(setVisibleTrackersStart).toHaveBeenCalled();
});
test('handleLogout очищает localStorage', () => {
  const TrackerListPage = require('./TrackerListPage').default;
  Storage.prototype.removeItem = jest.fn();

  render(
    <BrowserRouter>
      <TrackerListPage endpoint="/trackers" />
    </BrowserRouter>
  );

  const profileBtn = screen.getByAltText("Профиль").closest("button");
  fireEvent.click(profileBtn);
  const logoutLink = screen.getByText("Выход");
  fireEvent.click(logoutLink);

  expect(localStorage.removeItem).toHaveBeenCalledWith("user");
  expect(localStorage.removeItem).toHaveBeenCalledWith("userRole");
  expect(localStorage.removeItem).toHaveBeenCalledWith("streamName");
  expect(localStorage.removeItem).toHaveBeenCalledWith("streamId");
  expect(localStorage.removeItem).toHaveBeenCalledWith("streamSDate");
  expect(localStorage.removeItem).toHaveBeenCalledWith("streamEDate");
});

test('клик по confirm (enabled) вызывает setHoveredTracker(null)', () => {
  const setHoveredTracker = jest.fn();
  require('../hooks/useTrackerList').useTrackerList = () => ({
    trackers: [{ username: "user1", fullName: "Test", telegramId: "test", enabled: true }],
    error: null,
    searchQuery: "",
    setSearchQuery: jest.fn(),
    visibleTrackersStart: 0,
    setVisibleTrackersStart: jest.fn(),
    hoveredTracker: "user1",
    setHoveredTracker,
    hoveredButton: null,
    setHoveredButton: jest.fn(),
    trackersPerPage: 5,
    confirmUser: jest.fn(),
    deleteUser: jest.fn(),
  });

  const TrackerListPage = require('./TrackerListPage').default;
  render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

  const confirmButton = screen.getByAltText("Оставить");
  fireEvent.click(confirmButton);
  expect(setHoveredTracker).toHaveBeenCalledWith(null);
});

test('клик по cancel (enabled) вызывает deleteUser', () => {
  const deleteUser = jest.fn();
  require('../hooks/useTrackerList').useTrackerList = () => ({
    trackers: [{ username: "user1", fullName: "Test", telegramId: "test", enabled: true }],
    error: null,
    searchQuery: "",
    setSearchQuery: jest.fn(),
    visibleTrackersStart: 0,
    setVisibleTrackersStart: jest.fn(),
    hoveredTracker: "user1",
    setHoveredTracker: jest.fn(),
    hoveredButton: null,
    setHoveredButton: jest.fn(),
    trackersPerPage: 5,
    confirmUser: jest.fn(),
    deleteUser,
  });

  const TrackerListPage = require('./TrackerListPage').default;
  render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

  const cancelButton = screen.getByAltText("Удалить");
  fireEvent.click(cancelButton);
  expect(deleteUser).toHaveBeenCalledWith("user1");
});

test('клик по confirm (not enabled) вызывает confirmUser', () => {
  const confirmUser = jest.fn();
  require('../hooks/useTrackerList').useTrackerList = () => ({
    trackers: [{ username: "user2", fullName: "User2", telegramId: "t2", enabled: false }],
    error: null,
    searchQuery: "",
    setSearchQuery: jest.fn(),
    visibleTrackersStart: 0,
    setVisibleTrackersStart: jest.fn(),
    hoveredTracker: "user2",
    setHoveredTracker: jest.fn(),
    hoveredButton: null,
    setHoveredButton: jest.fn(),
    trackersPerPage: 5,
    confirmUser,
    deleteUser: jest.fn(),
  });

  const TrackerListPage = require('./TrackerListPage').default;
  render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

  const confirmBtn = screen.getByAltText("Подтвердить");
  fireEvent.click(confirmBtn);
  expect(confirmUser).toHaveBeenCalledWith("user2");
});


test('клик по cancel (not enabled) вызывает deleteUser', () => {
  const deleteUser = jest.fn();
  require('../hooks/useTrackerList').useTrackerList = () => ({
    trackers: [{ username: "user2", fullName: "User2", telegramId: "t2", enabled: false }],
    error: null,
    searchQuery: "",
    setSearchQuery: jest.fn(),
    visibleTrackersStart: 0,
    setVisibleTrackersStart: jest.fn(),
    hoveredTracker: "user2",
    setHoveredTracker: jest.fn(),
    hoveredButton: null,
    setHoveredButton: jest.fn(),
    trackersPerPage: 5,
    confirmUser: jest.fn(),
    deleteUser,
  });

  const TrackerListPage = require('./TrackerListPage').default;
  render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

  const cancelBtn = screen.getByAltText("Отклонить");
  fireEvent.click(cancelBtn);
  expect(deleteUser).toHaveBeenCalledWith("user2");
});
test('наведение на кнопку подтверждения вызывает setHoveredButton', () => {
  const setHoveredButton = require('../hooks/useTrackerList').setHoveredButton;

  require('../hooks/useTrackerList').useTrackerList = () => ({
    trackers: [{ username: "hovered", fullName: "Hover User", telegramId: "hover", enabled: false }],
    error: null,
    searchQuery: "",
    setSearchQuery: jest.fn(),
    visibleTrackersStart: 0,
    setVisibleTrackersStart: jest.fn(),
    hoveredTracker: "hovered",
    setHoveredTracker: jest.fn(),
    hoveredButton: null,
    setHoveredButton,
    trackersPerPage: 5,
    confirmUser: jest.fn(),
    deleteUser: jest.fn(),
  });

  const TrackerListPage = require('./TrackerListPage').default;
  render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

  const confirmBtn = screen.getByAltText("Подтвердить");
  fireEvent.mouseEnter(confirmBtn);
  expect(setHoveredButton).toHaveBeenCalledWith("confirm");

  fireEvent.mouseLeave(confirmBtn);
  expect(setHoveredButton).toHaveBeenCalledWith(null);
});


test('наведение на кнопку удаления вызывает setHoveredButton', () => {
  const setHoveredButton = require('../hooks/useTrackerList').setHoveredButton;

  require('../hooks/useTrackerList').useTrackerList = () => ({
    trackers: [{ username: "hovered", fullName: "Hover User", telegramId: "hover", enabled: false }],
    error: null,
    searchQuery: "",
    setSearchQuery: jest.fn(),
    visibleTrackersStart: 0,
    setVisibleTrackersStart: jest.fn(),
    hoveredTracker: "hovered",
    setHoveredTracker: jest.fn(),
    hoveredButton: null,
    setHoveredButton,
    trackersPerPage: 5,
    confirmUser: jest.fn(),
    deleteUser: jest.fn(),
  });

  const TrackerListPage = require('./TrackerListPage').default;
  render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

  const cancelBtn = screen.getByAltText("Отклонить");
  fireEvent.mouseEnter(cancelBtn);
  expect(setHoveredButton).toHaveBeenCalledWith("cancel");

  fireEvent.mouseLeave(cancelBtn);
  expect(setHoveredButton).toHaveBeenCalledWith(null);
});


test('рендер тултипа "Удалить", когда hoveredButton === "cancel"', () => {
  require('../hooks/useTrackerList').useTrackerList = () => ({
    trackers: [{ username: "hovered", fullName: "Hover User", telegramId: "hover", enabled: true }],
    error: null,
    searchQuery: "",
    setSearchQuery: jest.fn(),
    visibleTrackersStart: 0,
    setVisibleTrackersStart: jest.fn(),
    hoveredTracker: "hovered",
    setHoveredTracker: jest.fn(),
    hoveredButton: "cancel",
    setHoveredButton: jest.fn(),
    trackersPerPage: 5,
    confirmUser: jest.fn(),
    deleteUser: jest.fn(),
  });

  const TrackerListPage = require('./TrackerListPage').default;
  render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

  expect(screen.getByText("Удалить")).toBeInTheDocument();
});



test('рендер тултипа "Оставить" или "Подтвердить" при наведении', () => {
  require('../hooks/useTrackerList').useTrackerList = () => ({
    trackers: [{ username: "hovered", fullName: "Hover User", telegramId: "hover", enabled: false }],
    error: null,
    searchQuery: "",
    setSearchQuery: jest.fn(),
    visibleTrackersStart: 0,
    setVisibleTrackersStart: jest.fn(),
    hoveredTracker: "hovered",
    setHoveredTracker: jest.fn(),
    hoveredButton: "confirm",
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

test('ввод в строку поиска вызывает setSearchQuery и сбрасывает visibleTrackersStart', () => {
  const setSearchQuery = jest.fn();
  const setVisibleTrackersStart = jest.fn();

  require('../hooks/useTrackerList').useTrackerList = () => ({
    trackers: [{ username: "x", fullName: "Alice", telegramId: "alice", enabled: true }],
    error: null,
    searchQuery: "",
    setSearchQuery,
    visibleTrackersStart: 0,
    setVisibleTrackersStart,
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
  fireEvent.change(screen.getByPlaceholderText("Найти"), { target: { value: "Al" } });

  expect(setSearchQuery).toHaveBeenCalledWith("Al");
  expect(setVisibleTrackersStart).toHaveBeenCalledWith(0);
});

test('нажатие Enter на активном трекере вызывает setHoveredTracker', () => {
  const setHoveredTracker = jest.fn();
  require('../hooks/useTrackerList').useTrackerList = () => ({
    trackers: [{ username: "active1", fullName: "Active", telegramId: "act", enabled: true }],
    error: null,
    searchQuery: "",
    setSearchQuery: jest.fn(),
    visibleTrackersStart: 0,
    setVisibleTrackersStart: jest.fn(),
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
  fireEvent.keyDown(screen.getByText("Active").closest('[role="button"]'), { key: 'Enter' });
  expect(setHoveredTracker).toHaveBeenCalledWith("active1");
});

test('клик по confirm у включенного трекера вызывает setHoveredTracker(null)', () => {
  const setHoveredTracker = jest.fn();
  require('../hooks/useTrackerList').useTrackerList = () => ({
    trackers: [{ username: "enabled1", fullName: "Enabled", telegramId: "tg", enabled: true }],
    error: null,
    searchQuery: "",
    setSearchQuery: jest.fn(),
    visibleTrackersStart: 0,
    setVisibleTrackersStart: jest.fn(),
    hoveredTracker: "enabled1",
    setHoveredTracker,
    hoveredButton: "cancel",
    setHoveredButton: jest.fn(),
    trackersPerPage: 5,
    confirmUser: jest.fn(),
    deleteUser: jest.fn(),
  });

  const TrackerListPage = require('./TrackerListPage').default;
  render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);
  fireEvent.click(screen.getByAltText("Оставить"));
  expect(setHoveredTracker).toHaveBeenCalledWith(null);
});

test('клик по confirm у неактивного трекера вызывает confirmUser', () => {
  const confirmUser = jest.fn();
  require('../hooks/useTrackerList').useTrackerList = () => ({
    trackers: [{ username: "disabled1", fullName: "Disabled", telegramId: "tg", enabled: false }],
    error: null,
    searchQuery: "",
    setSearchQuery: jest.fn(),
    visibleTrackersStart: 0,
    setVisibleTrackersStart: jest.fn(),
    hoveredTracker: "disabled1",
    setHoveredTracker: jest.fn(),
    hoveredButton: "cancel",
    setHoveredButton: jest.fn(),
    trackersPerPage: 5,
    confirmUser,
    deleteUser: jest.fn(),
  });

  const TrackerListPage = require('./TrackerListPage').default;
  render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);
  fireEvent.click(screen.getByAltText("Подтвердить"));
  expect(confirmUser).toHaveBeenCalledWith("disabled1");
});

test('клик по cancel у неактивного трекера вызывает deleteUser', () => {
  const deleteUser = jest.fn();
  require('../hooks/useTrackerList').useTrackerList = () => ({
    trackers: [{ username: "disabled2", fullName: "Disabled2", telegramId: "tg2", enabled: false }],
    error: null,
    searchQuery: "",
    setSearchQuery: jest.fn(),
    visibleTrackersStart: 0,
    setVisibleTrackersStart: jest.fn(),
    hoveredTracker: "disabled2",
    setHoveredTracker: jest.fn(),
    hoveredButton: "confirm",
    setHoveredButton: jest.fn(),
    trackersPerPage: 5,
    confirmUser: jest.fn(),
    deleteUser,
  });

  const TrackerListPage = require('./TrackerListPage').default;
  render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);
  fireEvent.click(screen.getByAltText("Отклонить"));
  expect(deleteUser).toHaveBeenCalledWith("disabled2");
});



});
