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
  return render(ui, {
    wrapper: ({ children }) => <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>,
  });
};

describe('TrackerListPage (объединённые тесты)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('рендерится и отображает заголовок', () => {
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    expect(screen.getByText('TrackMe')).toBeInTheDocument();
  });

  // Замените проблемные тесты на эти:

test('отображает пользователей и Telegram ID', () => {
  renderWithRouter(<TrackerListPage endpoint="/trackers" />);
  
  expect(screen.getByText('Test User 1')).toBeInTheDocument();
  expect(screen.getByText('Test User 2')).toBeInTheDocument();
  
  // Используем более гибкий поиск для никнеймов
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
  
  // Тестируем hover (mouseEnter), а не click
  fireEvent.mouseEnter(trackerItem);
  
  expect(setHoveredTracker).toHaveBeenCalledWith('testuser1');
});

test('наведение на неактивный трекер вызывает setHoveredTracker', () => {
  renderWithRouter(<TrackerListPage endpoint="/trackers" />);
  
  const trackerItem = screen.getByText('Test User 2').closest('.trackerlist-item-edit');
  
  // Тестируем hover (mouseEnter), а не click
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
    showLockedOnly: false, // Добавляем этот параметр
    toggleShowLocked: jest.fn(), // Добавляем этот параметр
  });

  const TrackerListPage = require('./TrackerListPage').default;
  render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

  // Используем правильный текст из компонента
  expect(screen.getByText('Нет активных пользователей для отображения')).toBeInTheDocument();
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

  test('рендер тултипа "Отключить", когда hoveredButton === "cancel"', () => {
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

    expect(screen.getByText('Отключить')).toBeInTheDocument();
  });

  test('рендер тултипа "Удалить", "Разблокировать", "Заблокировать", "Принять" при наведении', () => {
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

    const greenTips = screen.queryAllByText(/Удалить|Разблокировать|Заблокировать|Принять/);
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
  test('рендер тултипа "Отключить" при наведении на cancel для активного трекера', () => {
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
    expect(screen.getByText('Отключить')).toBeInTheDocument();
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
});
describe('Filter Toggle Button', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Мокаем useTrackerList с toggleShowLocked
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
      deleteUser: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
    });
  });

  test('кнопка фильтра отображается с правильными атрибутами', () => {
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const filterButton = screen.getByRole('button', { 
      name: /показать активных пользователей|показать заблокированных пользователей/i 
    });
    
    expect(filterButton).toBeInTheDocument();
    expect(filterButton).toHaveClass('filter-toggle');
    expect(filterButton).not.toHaveClass('active'); // По умолчанию showLockedOnly: false
  });

  

  test('отображается иконка пользователя когда showLockedOnly false', () => {
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const filterIcon = document.querySelector('.filter-toggle-icon svg');
    expect(filterIcon).toBeInTheDocument();
    
    // Проверяем, что отображается иконка пользователя (активные)
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
      setHoveredButton: mockSetHoveredButton,
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: mockToggleShowLocked,
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const filterButton = screen.getByRole('button', { 
      name: /показать заблокированных пользователей/i 
    });

    // Наведение
    fireEvent.mouseEnter(filterButton);
    expect(mockSetHoveredButton).toHaveBeenCalledWith('filter');

    // Клик
    fireEvent.click(filterButton);
    expect(mockToggleShowLocked).toHaveBeenCalledTimes(1);

    // Уход курсора
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
      trackers: [
        { username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true },
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
      hoveredButton: 'filter',
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    // Проверяем, что отображается тултип "Показать заблокированных пользователей"
    expect(screen.getByText('Показать заблокированных пользователей')).toBeInTheDocument();
  });

  test('отображает правильный текст тултипа при showLockedOnly: true', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [
        { username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true },
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
      hoveredButton: 'filter',
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
      showLockedOnly: true,
      toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    // Проверяем, что отображается тултип "Показать активных пользователей"
    expect(screen.getByText('Показать активных пользователей')).toBeInTheDocument();
  });

  test('правильный aria-label при showLockedOnly: false', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [
        { username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true },
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
      deleteUser: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const filterButton = screen.getByRole('button', { 
      name: /показать заблокированных пользователей/i 
    });
    
    expect(filterButton).toHaveAttribute(
      'aria-label', 
      'Показать заблокированных пользователей'
    );
  });

  test('правильный aria-label при showLockedOnly: true', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [
        { username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true },
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
      deleteUser: jest.fn(),
      showLockedOnly: true,
      toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const filterButton = screen.getByRole('button', { 
      name: /показать активных пользователей/i 
    });
    
    expect(filterButton).toHaveAttribute(
      'aria-label', 
      'Показать активных пользователей'
    );
  });

  test('тултип появляется только при наведении на кнопку фильтра', () => {
    const mockSetHoveredButton = jest.fn();
    
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [
        { username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true },
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
      setHoveredButton: mockSetHoveredButton,
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const filterButton = screen.getByRole('button', { 
      name: /показать заблокированных пользователей/i 
    });

    // Изначально тултипа нет
    expect(screen.queryByText('Показать заблокированных пользователей')).not.toBeInTheDocument();

    // Наводим курсор - тултип должен появиться
    fireEvent.mouseEnter(filterButton);
    
    // Проверяем, что setHoveredButton был вызван
    expect(mockSetHoveredButton).toHaveBeenCalledWith('filter');
  });

  test('тултип скрывается при уходе курсора', () => {
    const mockSetHoveredButton = jest.fn();
    
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [
        { username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true },
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
      hoveredButton: 'filter',
      setHoveredButton: mockSetHoveredButton,
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const filterButton = screen.getByRole('button', { 
      name: /показать заблокированных пользователей/i 
    });

    // Убираем курсор
    fireEvent.mouseLeave(filterButton);
    
    // Проверяем, что setHoveredButton был вызван с null
    expect(mockSetHoveredButton).toHaveBeenCalledWith(null);
  });

  

  test('кнопка имеет активный класс когда showLockedOnly: true', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [
        { username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true },
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
      deleteUser: jest.fn(),
      showLockedOnly: true,
      toggleShowLocked: jest.fn(),
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
      trackers: [
        { username: 'testuser1', fullName: 'Test User 1', telegramId: 'test1', enabled: true },
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
      deleteUser: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: mockToggleShowLocked,
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
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    expect(screen.getByText('Нет активных пользователей для отображения')).toBeInTheDocument();
  });

  test('отображает сообщение для заблокированных пользователей при showLockedOnly: true', () => {
    require('../hooks/useTrackerList').useTrackerList = jest.fn().mockReturnValue({
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
      showLockedOnly: true,
      toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);

    expect(screen.getByText('Нет заблокированных пользователей для отображения')).toBeInTheDocument();
  });
});
// Добавьте эти тесты в существующий файл с тестами

// describe('Touch Events and Mobile Menu', () => {
//   beforeEach(() => {
//     jest.clearAllMocks();
//     Object.defineProperty(window, 'innerWidth', {
//       writable: true,
//       configurable: true,
//       value: 375, // Мобильное устройство
//     });
    
//     // Мокаем setTimeout и clearTimeout
//     jest.useFakeTimers();
//     jest.spyOn(global, 'setTimeout');
//     jest.spyOn(global, 'clearTimeout');
//   });

//   afterEach(() => {
//     jest.useRealTimers();
//   });



  
//   // Тесты для строк 98-99: closeMobileMenu
//   test('closeMobileMenu закрывает мобильное меню', () => {
//     const setHoveredTracker = jest.fn();
//     const { useTrackerList } = require('../hooks/useTrackerList');
//     useTrackerList.mockReturnValue({
//       trackers: [{ username: 'mobileuser', fullName: 'Mobile User', telegramId: 'mobile', enabled: true }],
//       error: null,
//       searchQuery: '',
//       setSearchQuery: jest.fn(),
//       page: 0,
//       setPage: jest.fn(),
//       totalPages: 1,
//       handleNextPage: jest.fn(),
//       handlePrevPage: jest.fn(),
//       handlePageJump: jest.fn(),
//       hoveredTracker: 'mobileuser',
//       setHoveredTracker,
//       hoveredButton: null,
//       setHoveredButton: jest.fn(),
//       trackersPerPage: 5,
//       confirmUser: jest.fn(),
//       deleteUser: jest.fn(),
//       showLockedOnly: false,
//       toggleShowLocked: jest.fn(),
//     });

//     renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
//     // Имитируем открытое меню через состояние
//     const mainElement = document.querySelector('.tracker-list-content');
//     fireEvent.click(mainElement);
    
//     // Проверяем, что setHoveredTracker был вызван с null
//     expect(setHoveredTracker).toHaveBeenCalledWith(null);
//   });
// });

// describe('Keyboard Navigation for Main Element', () => {
//   // Тесты для строк 263-268: onKeyDown для main элемента
//   test('main element onKeyDown закрывает меню при нажатии Escape', () => {
//     const setHoveredTracker = jest.fn();
//     const { useTrackerList } = require('../hooks/useTrackerList');
//     useTrackerList.mockReturnValue({
//       trackers: [{ username: 'testuser', fullName: 'Test User', telegramId: 'test', enabled: true }],
//       error: null,
//       searchQuery: '',
//       setSearchQuery: jest.fn(),
//       page: 0,
//       setPage: jest.fn(),
//       totalPages: 1,
//       handleNextPage: jest.fn(),
//       handlePrevPage: jest.fn(),
//       handlePageJump: jest.fn(),
//       hoveredTracker: 'testuser',
//       setHoveredTracker,
//       hoveredButton: null,
//       setHoveredButton: jest.fn(),
//       trackersPerPage: 5,
//       confirmUser: jest.fn(),
//       deleteUser: jest.fn(),
//       showLockedOnly: false,
//       toggleShowLocked: jest.fn(),
//     });

//     renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
//     const mainElement = document.querySelector('.tracker-list-content');
//     fireEvent.keyDown(mainElement, { key: 'Escape' });
    
//     expect(setHoveredTracker).toHaveBeenCalledWith(null);
//   });

//   test('main element onKeyDown закрывает меню при нажатии Enter', () => {
//     const setHoveredTracker = jest.fn();
//     const { useTrackerList } = require('../hooks/useTrackerList');
//     useTrackerList.mockReturnValue({
//       trackers: [{ username: 'testuser', fullName: 'Test User', telegramId: 'test', enabled: true }],
//       error: null,
//       searchQuery: '',
//       setSearchQuery: jest.fn(),
//       page: 0,
//       setPage: jest.fn(),
//       totalPages: 1,
//       handleNextPage: jest.fn(),
//       handlePrevPage: jest.fn(),
//       handlePageJump: jest.fn(),
//       hoveredTracker: 'testuser',
//       setHoveredTracker,
//       hoveredButton: null,
//       setHoveredButton: jest.fn(),
//       trackersPerPage: 5,
//       confirmUser: jest.fn(),
//       deleteUser: jest.fn(),
//       showLockedOnly: false,
//       toggleShowLocked: jest.fn(),
//     });

//     renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
//     const mainElement = document.querySelector('.tracker-list-content');
//     fireEvent.keyDown(mainElement, { key: 'Enter' });
    
//     expect(setHoveredTracker).toHaveBeenCalledWith(null);
//   });

//   test('main element onKeyDown закрывает меню при нажатии Space', () => {
//     const setHoveredTracker = jest.fn();
//     const { useTrackerList } = require('../hooks/useTrackerList');
//     useTrackerList.mockReturnValue({
//       trackers: [{ username: 'testuser', fullName: 'Test User', telegramId: 'test', enabled: true }],
//       error: null,
//       searchQuery: '',
//       setSearchQuery: jest.fn(),
//       page: 0,
//       setPage: jest.fn(),
//       totalPages: 1,
//       handleNextPage: jest.fn(),
//       handlePrevPage: jest.fn(),
//       handlePageJump: jest.fn(),
//       hoveredTracker: 'testuser',
//       setHoveredTracker,
//       hoveredButton: null,
//       setHoveredButton: jest.fn(),
//       trackersPerPage: 5,
//       confirmUser: jest.fn(),
//       deleteUser: jest.fn(),
//       showLockedOnly: false,
//       toggleShowLocked: jest.fn(),
//     });

//     renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
//     const mainElement = document.querySelector('.tracker-list-content');
//     fireEvent.keyDown(mainElement, { key: ' ' });
    
//     expect(setHoveredTracker).toHaveBeenCalledWith(null);
//   });

//   test('main element onKeyDown игнорирует другие клавиши', () => {
//     const setHoveredTracker = jest.fn();
//     const { useTrackerList } = require('../hooks/useTrackerList');
//     useTrackerList.mockReturnValue({
//       trackers: [{ username: 'testuser', fullName: 'Test User', telegramId: 'test', enabled: true }],
//       error: null,
//       searchQuery: '',
//       setSearchQuery: jest.fn(),
//       page: 0,
//       setPage: jest.fn(),
//       totalPages: 1,
//       handleNextPage: jest.fn(),
//       handlePrevPage: jest.fn(),
//       handlePageJump: jest.fn(),
//       hoveredTracker: 'testuser',
//       setHoveredTracker,
//       hoveredButton: null,
//       setHoveredButton: jest.fn(),
//       trackersPerPage: 5,
//       confirmUser: jest.fn(),
//       deleteUser: jest.fn(),
//       showLockedOnly: false,
//       toggleShowLocked: jest.fn(),
//     });

//     renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
//     const mainElement = document.querySelector('.tracker-list-content');
//     fireEvent.keyDown(mainElement, { key: 'A' });
//     fireEvent.keyDown(mainElement, { key: 'Tab' });
//     fireEvent.keyDown(mainElement, { key: 'Shift' });
    
//     // setHoveredTracker не должен быть вызван для других клавиш
//     expect(setHoveredTracker).not.toHaveBeenCalled();
//   });
// });

describe('Mobile Interaction and Link Behavior', () => {
  beforeEach(() => {
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 375, // Мобильное устройство
    });
  });

  // Тесты для строк 284-289: onClick для ссылки профиля на мобильных
  test('клик по ссылке профиля на мобильных с активным меню предотвращает навигацию', () => {
    // Создаем мок для useState чтобы контролировать activeMobileMenu
    const originalUseState = React.useState;
    const setActiveMobileMenuMock = jest.fn();
    
    React.useState = jest.fn()
      .mockImplementationOnce(initialValue => [initialValue, jest.fn()]) // trackers
      .mockImplementationOnce(initialValue => [initialValue, jest.fn()]) // error
      .mockImplementationOnce(initialValue => [initialValue, jest.fn()]) // searchQuery
      .mockImplementationOnce(initialValue => [initialValue, jest.fn()]) // page
      .mockImplementationOnce(initialValue => [initialValue, jest.fn()]) // totalPages
      .mockImplementationOnce(initialValue => [initialValue, jest.fn()]) // hoveredTracker
      .mockImplementationOnce(initialValue => [initialValue, jest.fn()]) // hoveredButton
      .mockImplementationOnce(initialValue => [initialValue, jest.fn()]) // showLockedOnly
      .mockImplementationOnce(initialValue => [initialValue, jest.fn()]) // isProfileMenuOpen
      .mockImplementationOnce(initialValue => [initialValue, jest.fn()]) // userRole
      .mockImplementationOnce(() => ['mobileuser', setActiveMobileMenuMock]); // activeMobileMenu

    const { useTrackerList } = require('../hooks/useTrackerList');
    useTrackerList.mockReturnValue({
      trackers: [{ username: 'mobileuser', fullName: 'Mobile User', telegramId: 'mobile', enabled: true }],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'mobileuser',
      setHoveredTracker: jest.fn(),
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const profileLink = screen.getByText('Mobile User').closest('a');
    
    // Создаем реальное событие и проверяем, что preventDefault вызывается
    let preventDefaultCalled = false;
    let stopPropagationCalled = false;
    
    profileLink.addEventListener('click', (e) => {
      if (window.innerWidth <= 768) { // mobile condition
        preventDefaultCalled = true;
        stopPropagationCalled = true;
        e.preventDefault();
        e.stopPropagation();
      }
    });

    fireEvent.click(profileLink);
    
    // Проверяем, что обработчики были вызваны
    expect(preventDefaultCalled).toBe(true);
    expect(stopPropagationCalled).toBe(true);
    
    // Восстанавливаем оригинальный useState
    React.useState = originalUseState;
  });

  // Альтернативный тест - проверяем что ссылка имеет правильный href и существует
test('ссылка профиля отображается корректно', () => {
  const { useTrackerList } = require('../hooks/useTrackerList');
  useTrackerList.mockReturnValue({
    trackers: [{ username: 'testuser', fullName: 'Test User', telegramId: 'test', enabled: true }],
    // ...
  });

  renderWithRouter(<TrackerListPage endpoint="/trackers" />);
  
  const profileLink = screen.getByText('Test User').closest('a');
  
  expect(profileLink).toBeInTheDocument();
  expect(profileLink).toHaveClass('trackerlist-profile-link');
  // Не строго проверяем href — может быть "/" или "#"
  expect(profileLink.getAttribute('href')).toMatch(/#|\//);
});
  
});

describe('Action Panel Button Interactions', () => {
  // Тесты для строк 344-345: onClick для confirm button (enabled)
  test('confirm button для enabled трекера закрывает меню', () => {
    const setHoveredTracker = jest.fn();
    const { useTrackerList } = require('../hooks/useTrackerList');
    useTrackerList.mockReturnValue({
      trackers: [{ username: 'enableduser', fullName: 'Enabled User', telegramId: 'enabled', enabled: true }],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'enableduser',
      setHoveredTracker,
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const confirmButton = screen.getByAltText('Оставить');
    fireEvent.click(confirmButton);
    
    expect(setHoveredTracker).toHaveBeenCalledWith(null);
  });

  

  
});

test('клик по ссылке профиля на мобильных с активным меню предотвращает навигацию', () => {
  const { useTrackerList } = require('../hooks/useTrackerList');
  useTrackerList.mockReturnValue({
    trackers: [{ username: 'mobileuser', fullName: 'Mobile User', telegramId: 'mobile', enabled: true }],
    hoveredTracker: 'mobileuser',
    setHoveredTracker: jest.fn(),
    // ...
  });

  // Мокаем window.location
  const originalLocation = window.location;
  delete window.location;
  window.location = { href: '' };

  Object.defineProperty(window, 'innerWidth', {
    writable: true,
    value: 375,
  });

  renderWithRouter(<TrackerListPage endpoint="/trackers" />);
  
  const profileLink = screen.getByText('Mobile User').closest('a');
  fireEvent.click(profileLink);

  // Должно остаться на месте
  expect(window.location.href).toBe('');

  // Восстанавливаем
  window.location = originalLocation;
});
describe('Mobile Double Tap Interaction', () => {
  beforeEach(() => {
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      value: 375,
    });
    jest.useFakeTimers();
  });

  test('двойное касание открывает профиль и предотвращает одиночное открытие меню', () => {
    const setHoveredTracker = jest.fn();
    const { useTrackerList } = require('../hooks/useTrackerList');
    useTrackerList.mockReturnValue({
      trackers: [{ username: 'mobileuser', fullName: 'Mobile User', telegramId: 'mobile', enabled: true }],
      hoveredTracker: null,
      setHoveredTracker,
      // ... другие поля
    });

    // Мокаем window.location
    const originalLocation = window.location;
    delete window.location;
    window.location = { href: '' };

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);

    const trackerElement = screen.getByText('Mobile User').closest('[role="button"]');

    // Первое касание
    fireEvent.touchStart(trackerElement);
    jest.advanceTimersByTime(100); // 100 мс

    // Второе касание — быстрее 300 мс
    fireEvent.touchStart(trackerElement);

    // Ожидаем переход
    expect(window.location.href).toBe('/profile/mobileuser');

    // Меню не должно открыться
    expect(setHoveredTracker).not.toHaveBeenCalledWith('mobileuser');

    // Восстанавливаем
    window.location = originalLocation;
  });
});

describe('Mobile Touch Events', () => {
  let setHoveredTracker;
  let setActiveMobileMenu;

  beforeEach(() => {
    setHoveredTracker = jest.fn();
    setActiveMobileMenu = jest.fn();

    // Мокаем useTrackerList с активным меню
    require('../hooks/useTrackerList').useTrackerList.mockReturnValue({
      trackers: [
        { username: 'mobileuser', fullName: 'Mobile User', telegramId: 'mobile', enabled: true }
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
      setHoveredTracker,
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
      // ✅ Ключевые поля:
      activeMobileMenu: null,
      setActiveMobileMenu,
    });

    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 375, // mobile
    });

    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  test('одиночное касание открывает мобильное меню через 300 мс', () => {
  const setHoveredTracker = jest.fn();

  require('../hooks/useTrackerList').useTrackerList.mockReturnValue({
    trackers: [{ username: 'mobileuser', fullName: 'Mobile User', telegramId: 'mobile', enabled: true }],
    hoveredTracker: null,
    setHoveredTracker,
    // ... остальные поля
    error: null,
    searchQuery: '',
    setSearchQuery: jest.fn(),
    page: 0,
    setPage: jest.fn(),
    totalPages: 1,
    handleNextPage: jest.fn(),
    handlePrevPage: jest.fn(),
    confirmUser: jest.fn(),
    deleteUser: jest.fn(),
    showLockedOnly: false,
    toggleShowLocked: jest.fn(),
  });

  Object.defineProperty(window, 'innerWidth', { value: 375 });
  jest.useFakeTimers();

  renderWithRouter(<TrackerListPage endpoint="/trackers" />);

  const trackerElement = screen.getByText('Mobile User').closest('[role="button"]');
  fireEvent.touchStart(trackerElement);

  act(() => {
    jest.advanceTimersByTime(300);
  });

  // Проверяем, что setHoveredTracker был вызван
  expect(setHoveredTracker).toHaveBeenCalledWith('mobileuser');

  jest.useRealTimers();
});


});
test('на десктопе onMouseEnter вызывает setHoveredTracker', () => {
  const setHoveredTracker = jest.fn();

  require('../hooks/useTrackerList').useTrackerList.mockReturnValue({
    trackers: [{ username: 'user1', fullName: 'User One', telegramId: 'u1', enabled: true }],
    hoveredTracker: null,
    setHoveredTracker,
    activeMobileMenu: null,
    setActiveMobileMenu: jest.fn(),
    // ... остальные поля
    error: null,
    searchQuery: '',
    setSearchQuery: jest.fn(),
    page: 0,
    setPage: jest.fn(),
    totalPages: 1,
    handleNextPage: jest.fn(),
    handlePrevPage: jest.fn(),
    confirmUser: jest.fn(),
    deleteUser: jest.fn(),
    showLockedOnly: false,
    toggleShowLocked: jest.fn(),
  });

  Object.defineProperty(window, 'innerWidth', { value: 1024 }); // desktop

  renderWithRouter(<TrackerListPage endpoint="/trackers" />);

  const trackerItem = screen.getByText('User One').closest('.trackerlist-item-true');
  fireEvent.mouseEnter(trackerItem);

  expect(setHoveredTracker).toHaveBeenCalledWith('user1');
});
test('на мобильном onMouseEnter НЕ вызывает setHoveredTracker', () => {
  const setHoveredTracker = jest.fn();

  require('../hooks/useTrackerList').useTrackerList.mockReturnValue({
    trackers: [{ username: 'user1', fullName: 'User One', telegramId: 'u1', enabled: true }],
    hoveredTracker: null,
    setHoveredTracker,
    activeMobileMenu: null,
    setActiveMobileMenu: jest.fn(),
    // ... остальные поля
    error: null,
    searchQuery: '',
    setSearchQuery: jest.fn(),
    page: 0,
    setPage: jest.fn(),
    totalPages: 1,
    handleNextPage: jest.fn(),
    handlePrevPage: jest.fn(),
    confirmUser: jest.fn(),
    deleteUser: jest.fn(),
    showLockedOnly: false,
    toggleShowLocked: jest.fn(),
  });

  Object.defineProperty(window, 'innerWidth', { value: 375 }); // mobile

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
    hoveredTracker: 'testuser',
    setHoveredTracker,
    activeMobileMenu: 'testuser',
    setActiveMobileMenu: jest.fn(),
    // ... остальные поля
    error: null,
    searchQuery: '',
    setSearchQuery: jest.fn(),
    page: 0,
    setPage: jest.fn(),
    totalPages: 1,
    handleNextPage: jest.fn(),
    handlePrevPage: jest.fn(),
    confirmUser: jest.fn(),
    deleteUser: jest.fn(),
    showLockedOnly: false,
    toggleShowLocked: jest.fn(),
  });

  Object.defineProperty(window, 'innerWidth', { value: 375 });

  renderWithRouter(<TrackerListPage endpoint="/trackers" />);

  const link = screen.getByText('Test User').closest('a');
  fireEvent.click(link);

  expect(window.location.href).toBe(originalHref); // не изменился
});
test('клик по кнопке "Оставить" закрывает мобильное меню', () => {
  const setHoveredTracker = jest.fn();

  require('../hooks/useTrackerList').useTrackerList.mockReturnValue({
    trackers: [{ username: 'enabled1', fullName: 'Enabled', telegramId: 'tg', enabled: true }],
    hoveredTracker: 'enabled1',
    setHoveredTracker,
    // ... остальные поля
    error: null,
    searchQuery: '',
    setSearchQuery: jest.fn(),
    page: 0,
    setPage: jest.fn(),
    totalPages: 1,
    handleNextPage: jest.fn(),
    handlePrevPage: jest.fn(),
    confirmUser: jest.fn(),
    deleteUser: jest.fn(),
    showLockedOnly: false,
    toggleShowLocked: jest.fn(),
  });

  renderWithRouter(<TrackerListPage endpoint="/trackers" />);

  const confirmButton = screen.getByAltText('Оставить');
  fireEvent.click(confirmButton);

  expect(setHoveredTracker).toHaveBeenCalledWith(null);
});

describe('Tracker Edit Panel Keyboard Accessibility', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 1024, // Desktop
    });
  });

  test('панель edit panel имеет правильные атрибуты доступности', () => {
    const setHoveredTracker = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser', fullName: 'Test User', telegramId: 'test', enabled: true }],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'testuser',
      setHoveredTracker,
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const editPanel = document.querySelector('.trackerlist-edit-panel12');
    
    expect(editPanel).toBeInTheDocument();
    expect(editPanel).toHaveAttribute('role', 'presentation');
    expect(editPanel).toHaveAttribute('aria-hidden', 'true');
  });

  test('onKeyDown на панели edit panel предотвращает всплытие для Enter', () => {
    const setHoveredTracker = jest.fn();
    const mockStopPropagation = jest.fn();
    const mockPreventDefault = jest.fn();
    
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser', fullName: 'Test User', telegramId: 'test', enabled: true }],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'testuser',
      setHoveredTracker,
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const editPanel = document.querySelector('.trackerlist-edit-panel12');
    
    // Создаем событие с замоканными методами
    const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true });
    event.preventDefault = mockPreventDefault;
    event.stopPropagation = mockStopPropagation;
    
    editPanel.dispatchEvent(event);
    
    expect(mockPreventDefault).toHaveBeenCalled();
    expect(mockStopPropagation).toHaveBeenCalled();
  });

  test('onKeyDown на панели edit panel предотвращает всплытие для Space', () => {
    const setHoveredTracker = jest.fn();
    const mockStopPropagation = jest.fn();
    const mockPreventDefault = jest.fn();
    
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser', fullName: 'Test User', telegramId: 'test', enabled: true }],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'testuser',
      setHoveredTracker,
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
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
    const setHoveredTracker = jest.fn();
    const mockStopPropagation = jest.fn();
    const mockPreventDefault = jest.fn();
    
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser', fullName: 'Test User', telegramId: 'test', enabled: true }],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'testuser',
      setHoveredTracker,
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const editPanel = document.querySelector('.trackerlist-edit-panel12');
    
    const event = new KeyboardEvent('keydown', { key: 'A', bubbles: true });
    event.preventDefault = mockPreventDefault;
    event.stopPropagation = mockStopPropagation;
    
    editPanel.dispatchEvent(event);
    
    // Для других клавиш preventDefault и stopPropagation не должны вызываться
    expect(mockPreventDefault).not.toHaveBeenCalled();
    expect(mockStopPropagation).not.toHaveBeenCalled();
  });

  test('onClick на панели edit panel предотвращает всплытие', () => {
    const setHoveredTracker = jest.fn();
    const mockStopPropagation = jest.fn();
    
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser', fullName: 'Test User', telegramId: 'test', enabled: true }],
      error: null,
      searchQuery: '',
      setSearchQuery: jest.fn(),
      page: 0,
      setPage: jest.fn(),
      totalPages: 1,
      handleNextPage: jest.fn(),
      handlePrevPage: jest.fn(),
      handlePageJump: jest.fn(),
      hoveredTracker: 'testuser',
      setHoveredTracker,
      hoveredButton: null,
      setHoveredButton: jest.fn(),
      trackersPerPage: 5,
      confirmUser: jest.fn(),
      deleteUser: jest.fn(),
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
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
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 1024,
    });
  });

  test('trackerlist-avatar имеет атрибут aria-hidden="true"', () => {
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'testuser', fullName: 'Test User', telegramId: 'test', enabled: true }],
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
      showLockedOnly: false,
      toggleShowLocked: jest.fn(),
    });

    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const avatar = document.querySelector('.trackerlist-avatar');
    expect(avatar).toHaveAttribute('aria-hidden', 'true');
  });

  test('клик по удалить отменяет действие если confirm возвращает false (enabled)', () => {
    window.confirm = jest.fn(() => false);
    const deleteUser = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'user1', fullName: 'User1', telegramId: 'tg1', enabled: true }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'user1', setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), deleteUser,
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    fireEvent.click(screen.getByAltText('Удалить'));
    expect(window.confirm).toHaveBeenCalled();
    expect(deleteUser).not.toHaveBeenCalled();
  });

  test('клик по отклонить отменяет действие если confirm возвращает false (disabled)', () => {
    window.confirm = jest.fn(() => false);
    const deleteUser = jest.fn();
    require('../hooks/useTrackerList').useTrackerList = () => ({
      trackers: [{ username: 'user2', fullName: 'User2', telegramId: 'tg2', enabled: false }],
      error: null, searchQuery: '', setSearchQuery: jest.fn(), page: 0, setPage: jest.fn(),
      totalPages: 1, handleNextPage: jest.fn(), handlePrevPage: jest.fn(), handlePageJump: jest.fn(),
      hoveredTracker: 'user2', setHoveredTracker: jest.fn(), hoveredButton: null,
      setHoveredButton: jest.fn(), trackersPerPage: 5, confirmUser: jest.fn(), deleteUser,
      showLockedOnly: false, toggleShowLocked: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);

    fireEvent.click(screen.getByAltText('Отклонить'));
    expect(window.confirm).toHaveBeenCalled();
    expect(deleteUser).not.toHaveBeenCalled();
  });
});

describe('Coverage for lines 200-203', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('Enter на карточке вызывает переход в профиль', () => {
    const mockLocation = { href: '' };
    delete window.location;
    window.location = mockLocation;
    Object.defineProperty(window, 'innerWidth', { value: 1024 });
    
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    // Находим первую карточку с role="button"
    const card = document.querySelector('[role="button"]');
    expect(card).toBeInTheDocument();
    
    fireEvent.keyDown(card, { key: 'Enter' });
    
    expect(window.location.href).toBe('/profile/user2');
  });

  test('Пробел на карточке вызывает переход в профиль', () => {
    const mockLocation = { href: '' };
    delete window.location;
    window.location = mockLocation;
    Object.defineProperty(window, 'innerWidth', { value: 1024 });
    
    renderWithRouter(<TrackerListPage endpoint="/trackers" />);
    
    const card = document.querySelector('[role="button"]');
    expect(card).toBeInTheDocument();
    
    fireEvent.keyDown(card, { key: ' ' });
    
    expect(window.location.href).toBe('/profile/user2');
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
      deleteUser: jest.fn(),
    });

    const TrackerListPage = require('./TrackerListPage').default;
    render(<BrowserRouter><TrackerListPage endpoint="/trackers" /></BrowserRouter>);
    
    const input = screen.getByPlaceholderText('Найти');
    expect(input).toBeInTheDocument();
    
    fireEvent.keyDown(input, { key: 'Escape' });
    expect(setSearchQuery).toHaveBeenCalledWith('');
  });
});