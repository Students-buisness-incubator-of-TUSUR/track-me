import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { BrowserRouter as Router } from 'react-router-dom';
import ReportPage from '../report-page/ReportPage.js';
import '@testing-library/jest-dom';

// Мокаем useNavigate
const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

// Мокаем MobileHeader
jest.mock('../adaptive-accets/MobileHeader', () => {
  return function MockMobileHeader({ onNavigate }) {
    return (
      <div data-testid="mobile-header">
        Mock Mobile Header
      </div>
    );
  };
});

// Мокаем getCsrfConfigForFetch
jest.mock('../../utils/csrf-utils', () => ({
  getCsrfConfigForFetch: jest.fn(() => ({})),
}));

// Создаем мок для localStorage
const localStorageMock = {
  getItem: jest.fn(),
  setItem: jest.fn(),
  clear: jest.fn(),
  removeItem: jest.fn(),
  length: 0,
  key: jest.fn(),
};

// Переопределяем window.location для теста выхода
const originalLocation = window.location;
delete window.location;
window.location = {
  ...originalLocation,
  href: '',
};

// Мок данных для отчетов
const mockReports = Array.from({ length: 30 }, (_, i) => ({
  streamName: 'Поток называется вот так',
  startDate: '01.09.2025',
  endDate: '12.12.2025',
  teamCardName: 'Название команды очень длинное',
  username: 'Александров Александр Александрович',
  averageTeamGrade: 5,
  averageUserGrade: 5,
  meetingsCountFact: 1,
  meetingsCountPlan: 3,
  ntiMarkets: ['HealthNet'],
  readinessLevel: '0-2'
}));

describe('ReportPage Component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    
    // Настраиваем мок localStorage
    Object.defineProperty(window, 'localStorage', {
      value: localStorageMock,
      writable: true,
    });
    
    window.location.href = '';
    
    // Мокаем fetch
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: mockReports
        }),
      })
    );
  });

  afterAll(() => {
    window.location = originalLocation;
  });

  test('рендерит компонент без ошибок', async () => {
    render(
      <Router>
        <ReportPage />
      </Router>
    );
    
    // Ждем пока данные загрузятся
    await waitFor(() => {
      expect(screen.getByText('TrackMe')).toBeInTheDocument();
    });
    
    expect(screen.getByText('Выгрузить отчет')).toBeInTheDocument();
  });

  test('открывает и закрывает меню профиля', async () => {
    render(
      <Router>
        <ReportPage />
      </Router>
    );
    
    // Ждем загрузки
    await waitFor(() => {
      expect(screen.getByText('TrackMe')).toBeInTheDocument();
    });
    
    const profileButton = screen.getByAltText('Профиль');
    fireEvent.click(profileButton);
    
    expect(screen.getByText('Личный кабинет')).toBeInTheDocument();
    expect(screen.getByText('Выход')).toBeInTheDocument();
    
    fireEvent.click(profileButton);
    expect(screen.queryByText('Личный кабинет')).not.toBeInTheDocument();
  });

  test('обрабатывает выход из системы', async () => {
    render(
      <Router>
        <ReportPage />
      </Router>
    );
    
    // Ждем загрузки
    await waitFor(() => {
      expect(screen.getByText('TrackMe')).toBeInTheDocument();
    });
    
    const profileButton = screen.getByAltText('Профиль');
    fireEvent.click(profileButton);
    
    const logoutLink = screen.getByText('Выход');
    fireEvent.click(logoutLink);
    
    expect(localStorageMock.clear).toHaveBeenCalled();
    expect(window.location.href).toBe('/');
  });

  test('открывает и закрывает фильтр трекеров', async () => {
    render(
      <Router>
        <ReportPage />
      </Router>
    );
    
    // Ждем загрузки
    await waitFor(() => {
      expect(screen.getByText('TrackMe')).toBeInTheDocument();
    });
    
    const trackerFilterButton = screen.getByText('Трекеры');
    fireEvent.click(trackerFilterButton);
    
    // Проверяем, что dropdown-menu появился
    const dropdownMenu = document.querySelector('.dropdown-menu');
    expect(dropdownMenu).toBeInTheDocument();
    
    // Проверяем, что в dropdown есть элементы
    const dropdownItems = dropdownMenu.querySelectorAll('.dropdown-item');
    expect(dropdownItems.length).toBeGreaterThan(0);
    
    fireEvent.click(trackerFilterButton);
    expect(document.querySelector('.dropdown-menu')).not.toBeInTheDocument();
  });

  test('открывает и закрывает фильтр потоков', async () => {
    render(
      <Router>
        <ReportPage />
      </Router>
    );
    
    // Ждем загрузки
    await waitFor(() => {
      expect(screen.getByText('TrackMe')).toBeInTheDocument();
    });
    
    const streamFilterButton = screen.getByText('Потоки');
    fireEvent.click(streamFilterButton);
    
    // Проверяем, что dropdown-menu появился
    const dropdownMenu = document.querySelector('.dropdown-menu');
    expect(dropdownMenu).toBeInTheDocument();
    
    // Проверяем, что в dropdown есть элементы
    const dropdownItems = dropdownMenu.querySelectorAll('.dropdown-item');
    expect(dropdownItems.length).toBeGreaterThan(0);
    
    fireEvent.click(streamFilterButton);
    expect(document.querySelector('.dropdown-menu')).not.toBeInTheDocument();
  });

  test('рендерит таблицу с данными', async () => {
    render(
      <Router>
        <ReportPage />
      </Router>
    );

    // Ждем пока данные загрузятся и таблица отобразится
    await waitFor(() => {
      expect(screen.queryByText('Загрузка...')).not.toBeInTheDocument();
    });

    const table = screen.getByRole('table');
    const rows = within(table).getAllByRole('row');

    // Пропускаем заголовок и берем первую строку данных
    const firstDataRow = rows[1];
    const cells = within(firstDataRow).getAllByRole('cell');

    expect(cells[0]).toHaveTextContent('1'); // №
    expect(cells[1]).toHaveTextContent('Поток называется вот так'); // Название потока
    expect(cells[2]).toHaveTextContent('01.09.2025 – 12.12.2025'); // Сроки потока
    expect(cells[3]).toHaveTextContent('Название команды очень длинное'); // Название команды
    expect(cells[4]).toHaveTextContent('Александров Александр Александрович'); // Имя трекера
    expect(cells[5]).toHaveTextContent('5'); // Средняя оценка команды
    expect(cells[6]).toHaveTextContent('5'); // Средняя оценка трекера
    expect(cells[7]).toHaveTextContent('1/3'); // Трекшн-митинг
    expect(cells[8]).toHaveTextContent('HealthNet'); // Рынки НТИ
    expect(cells[9]).toHaveTextContent('0-2'); // Уровень TRL
  });

  test('навигация по клику на логотип и заголовок', async () => {
    render(
      <Router>
        <ReportPage />
      </Router>
    );
    
    // Ждем загрузки
    await waitFor(() => {
      expect(screen.getByText('TrackMe')).toBeInTheDocument();
    });
    
    const logo = document.querySelector('.Stream-header-logo');
    fireEvent.click(logo);
    expect(mockNavigate).toHaveBeenCalledWith('/streams');
    
    const title = screen.getByText('TrackMe');
    fireEvent.click(title);
    expect(mockNavigate).toHaveBeenCalledWith('/streams');
  });

  test('навигация по клавиатуре на логотип и заголовок', async () => {
    render(
      <Router>
        <ReportPage />
      </Router>
    );
    
    // Ждем загрузки
    await waitFor(() => {
      expect(screen.getByText('TrackMe')).toBeInTheDocument();
    });
    
    const logo = document.querySelector('.Stream-header-logo');
    fireEvent.keyDown(logo, { key: 'Enter' });
    expect(mockNavigate).toHaveBeenCalledWith('/streams');
    
    const title = screen.getByText('TrackMe');
    fireEvent.keyDown(title, { key: ' ' });
    expect(mockNavigate).toHaveBeenCalledWith('/streams');
  });

  test('отображает правильные иконки в фильтрах', async () => {
    render(
      <Router>
        <ReportPage />
      </Router>
    );
    
    // Ждем загрузки
    await waitFor(() => {
      expect(screen.getByText('TrackMe')).toBeInTheDocument();
    });
    
    // Проверяем начальное состояние иконок
    const trackerIcons = document.querySelectorAll('.dropdown-icon-img');
    expect(trackerIcons[0]).toHaveAttribute('alt', 'Открыто');
    
    // Открываем фильтр и проверяем изменение иконки
    const trackerFilterButton = screen.getByText('Трекеры');
    fireEvent.click(trackerFilterButton);
    
    const updatedIcons = document.querySelectorAll('.dropdown-icon-img');
    expect(updatedIcons[0]).toHaveAttribute('alt', 'Закрыто');
  });

  test('рендерит правильное количество строк в таблице', async () => {
    render(
      <Router>
        <ReportPage />
      </Router>
    );
    
    // Ждем пока данные загрузятся
    await waitFor(() => {
      expect(screen.queryByText('Загрузка...')).not.toBeInTheDocument();
    });
    
    const tableRows = screen.getAllByRole('row');
    // 1 строка заголовка + 30 строк данных
    expect(tableRows).toHaveLength(31);
  });

  test('фильтры содержат правильное количество элементов', async () => {
    render(
      <Router>
        <ReportPage />
      </Router>
    );
    
    // Ждем загрузки
    await waitFor(() => {
      expect(screen.getByText('TrackMe')).toBeInTheDocument();
    });
    
    // Открываем фильтр трекеров
    const trackerFilterButton = screen.getByText('Трекеры');
    fireEvent.click(trackerFilterButton);
    
    await waitFor(() => {
      const dropdownMenu = document.querySelector('.dropdown-menu');
      const trackerItems = dropdownMenu.querySelectorAll('.dropdown-item');
      expect(trackerItems).toHaveLength(12);
    });
    
    // Закрываем фильтр трекеров
    fireEvent.click(trackerFilterButton);
    
    // Открываем фильтр потоков
    const streamFilterButton = screen.getByText('Потоки');
    fireEvent.click(streamFilterButton);
    
    await waitFor(() => {
      const dropdownMenu = document.querySelector('.dropdown-menu');
      const streamItems = dropdownMenu.querySelectorAll('.dropdown-item');
      expect(streamItems).toHaveLength(18);
    });
  });

  test('кнопка "Выгрузить отчет" отображается и кликабельна', async () => {
    render(
      <Router>
        <ReportPage />
      </Router>
    );
    
    // Ждем загрузки
    await waitFor(() => {
      expect(screen.getByText('TrackMe')).toBeInTheDocument();
    });
    
    const exportButton = screen.getByText('Выгрузить отчет');
    expect(exportButton).toBeInTheDocument();
    expect(exportButton).toBeEnabled();
    
    // Проверяем, что кнопка кликабельна
    fireEvent.click(exportButton);
  });

  test('меню профиля содержит правильные ссылки', async () => {
    render(
      <Router>
        <ReportPage />
      </Router>
    );
    
    // Ждем загрузки
    await waitFor(() => {
      expect(screen.getByText('TrackMe')).toBeInTheDocument();
    });
    
    const profileButton = screen.getByAltText('Профиль');
    fireEvent.click(profileButton);
    
    const profileLink = screen.getByText('Личный кабинет');
    const logoutLink = screen.getByText('Выход');
    
    expect(profileLink).toBeInTheDocument();
    expect(logoutLink).toBeInTheDocument();
    expect(profileLink.closest('a')).toHaveAttribute('href', '/profile');
  });

  test('переключение фильтров изменяет иконки', async () => {
    render(
      <Router>
        <ReportPage />
      </Router>
    );
    
    // Ждем загрузки
    await waitFor(() => {
      expect(screen.getByText('TrackMe')).toBeInTheDocument();
    });
    
    const trackerFilterButton = screen.getByText('Трекеры');
    const streamFilterButton = screen.getByText('Потоки');
    
    // Проверяем начальное состояние
    const initialIcons = document.querySelectorAll('.dropdown-icon-img');
    expect(initialIcons[0]).toHaveAttribute('alt', 'Открыто');
    expect(initialIcons[1]).toHaveAttribute('alt', 'Открыто');
    
    // Открываем фильтр трекеров
    fireEvent.click(trackerFilterButton);
    const afterTrackerOpenIcons = document.querySelectorAll('.dropdown-icon-img');
    expect(afterTrackerOpenIcons[0]).toHaveAttribute('alt', 'Закрыто');
    expect(afterTrackerOpenIcons[1]).toHaveAttribute('alt', 'Открыто');
    
    // Закрываем фильтр трекеров
    fireEvent.click(trackerFilterButton);
    const afterTrackerCloseIcons = document.querySelectorAll('.dropdown-icon-img');
    expect(afterTrackerCloseIcons[0]).toHaveAttribute('alt', 'Открыто');
    expect(afterTrackerCloseIcons[1]).toHaveAttribute('alt', 'Открыто');
  });

  test('показывает состояние загрузки', async () => {
    // Мокаем медленный fetch
    global.fetch = jest.fn(() => 
      new Promise(resolve => 
        setTimeout(() => 
          resolve({
            ok: true,
            json: () => Promise.resolve({ content: [] })
          }), 100
        )
      )
    );

    render(
      <Router>
        <ReportPage />
      </Router>
    );

    // Должен отображаться текст "Загрузка..."
    expect(screen.getByText('Загрузка...')).toBeInTheDocument();

    // Ждем завершения загрузки
    await waitFor(() => {
      expect(screen.queryByText('Загрузка...')).not.toBeInTheDocument();
    });
  });

  test('показывает "Нет данных" при пустом ответе', async () => {
    // Мокаем пустой ответ
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [] }),
      })
    );

    render(
      <Router>
        <ReportPage />
      </Router>
    );

    // Ждем пока компонент перестанет показывать "Загрузка..."
    await waitFor(() => {
      expect(screen.getByText('Нет данных')).toBeInTheDocument();
    });
  });
});