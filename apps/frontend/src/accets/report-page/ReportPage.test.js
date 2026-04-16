import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { BrowserRouter as Router } from 'react-router-dom';
import ReportPage from '../report-page/ReportPage.js';
import '@testing-library/jest-dom';
import { fetchReports, fetchStreams, fetchTrackers } from '../../services/requests';
import { useSelector } from 'react-redux';
const mockUseGetUserInfo = jest.fn();
jest.mock('../../services/util', () => ({
  useGetUserInfo: () => mockUseGetUserInfo(),
}));
jest.mock('react-redux', () => ({
  ...jest.requireActual('react-redux'),
  useSelector: jest.fn(),
}));

beforeEach(() => {
  mockUseGetUserInfo.mockReturnValue({
    roles: ['SUPER_ADMIN'],
    username: "username12",
  });
});

const size = 10000;

// Мокаем useNavigate
const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

// Мокаем getCsrfConfigForFetch
jest.mock('../../utils/csrf-utils', () => ({
  getCsrfConfigForFetch: jest.fn(() => ({})),
}));

// Мокаем модуль requests (уже сделано выше)
jest.mock('../../services/requests');

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

  test('isActive checkbox toggles correctly and triggers report reload', async () => {
  render(
    <Router>
      <ReportPage defaultIsActive={false} />
    </Router>
  );

  await waitFor(() => {
    expect(fetchReports).toHaveBeenCalledWith({ page: 0, size: size, filters: [] });
  });

  const checkboxButton = screen.getByTestId('button-isactive');
  expect(checkboxButton).toBeInTheDocument();

  fireEvent.click(checkboxButton);

  await waitFor(() => {
    const todayDate = new Date().toISOString().split('T')[0];
    expect(fetchReports).toHaveBeenCalledWith({
      page: 0,
      size: size,
      filters: expect.arrayContaining([
        {
          fieldName: "streams.startDate",
          type: "LTE",
          value: todayDate,
        },
        {
          fieldName: "streams.endDate", 
          type: "GTE",
          value: todayDate,
        }
      ])
    });
  });

  fireEvent.click(checkboxButton);

  await waitFor(() => {
    expect(fetchReports).toHaveBeenCalledWith({ page: 0, size: size, filters: [] });
  });
});
  test('successful fetch sets reports and displays them', async () => {
    const mockReportsData = {
      content: [
        {
          streamName: 'Stream 1',
          startDate: '2023-01-01',
          endDate: '2023-01-31',
          teamCardName: 'Team A',
          username: 'User1',
          averageTeamGrade: 4.5,
          averageUserGrade: 4.0,
          meetingsCountFact: 5,
          meetingsCountPlan: 5,
          ntiMarkets: ['Market1'],
          readinessLevel: 'TRL5',
        },
        {
          streamName: 'Stream 2',
          startDate: '2023-02-01',
          endDate: '2023-02-28',
          teamCardName: 'Team B',
          username: 'User2',
          averageTeamGrade: null,
          averageUserGrade: null,
          meetingsCountFact: 3,
          meetingsCountPlan: 4,
          ntiMarkets: ['Market2', 'Market3'],
          readinessLevel: 'TRL4',
        },
      ],
    };

    const mockResponse = {
      ok: true,
      json: jest.fn().mockResolvedValue(mockReportsData),
    };
    fetchReports.mockResolvedValue(mockResponse);

    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    expect(screen.getByText('Загрузка...')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.queryByText('Загрузка...')).not.toBeInTheDocument();
    });

    expect(screen.getByText('Stream 1')).toBeInTheDocument();
    expect(screen.getByText('Stream 2')).toBeInTheDocument();
    expect(screen.getByText('4.5')).toBeInTheDocument();
    expect(screen.getAllByText('—')).toHaveLength(2);

    expect(fetchReports).toHaveBeenCalledWith({ page: 0, size: size, filters: [] });
  });

  test('unsuccessful fetch (response not ok) logs error and shows no data', async () => {
    const mockResponse = {
      ok: false,
      status: 500,
      json: jest.fn(),
    };
    fetchReports.mockResolvedValue(mockResponse);

    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    expect(screen.getByText('Загрузка...')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.queryByText('Загрузка...')).not.toBeInTheDocument();
    });

    expect(screen.getByText('Нет данных')).toBeInTheDocument();

    expect(console.error).toHaveBeenCalledWith(
      'Ошибка загрузки отчётов',
      expect.objectContaining({
        message: expect.stringMatching(/Ошибка HTTP: 500/),
      })
    );

    expect(fetchReports).toHaveBeenCalledWith({ page: 0, size: size, filters: [] });
  });
  beforeEach(() => {
    jest.clearAllMocks();
    jest.spyOn(console, 'error').mockImplementation(() => {});
    
    // Настраиваем мок localStorage
    Object.defineProperty(window, 'localStorage', {
      value: localStorageMock,
      writable: true,
    });
    
    window.location.href = '';
    
    // Мокаем fetchReports для успешного ответа с данными
    fetchReports.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: mockReports }),
    });
  });

  afterAll(() => {
    window.location = originalLocation;
  });

  afterEach(() => {
    console.error.mockRestore();
  });

  test('рендерит компонент без ошибок', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );
    
    // Ждем пока данные загрузятся
    await waitFor(() => {
      expect(screen.getByText('TrackMe')).toBeInTheDocument();
    });
    
    expect(screen.getByText('Выгрузить отчет')).toBeInTheDocument();
  });

  test('открывает и закрывает фильтр трекеров', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );
    
    // Ждем загрузки
    await waitFor(() => {
      expect(screen.getByText('TrackMe')).toBeInTheDocument();
    });
    
    const trackerFilterButton = screen.getByTestId("trackers-btn");
    fireEvent.click(trackerFilterButton);
    
    // Проверяем, что dropdown-menu появился
    const dropdownMenu = document.querySelector('.report-dropdown-menu');
    expect(dropdownMenu).toBeInTheDocument();
    
    // Проверяем, что в dropdown есть элементы
    const dropdownItems = dropdownMenu.querySelectorAll('.report-dropdown-item');
    expect(dropdownItems.length).toBeGreaterThan(0);
    
    fireEvent.click(trackerFilterButton);
    expect(document.querySelector('.dropdown-menu')).not.toBeInTheDocument();
  });

  test('открывает и закрывает фильтр потоков', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );
    
    // Ждем загрузки
    await waitFor(() => {
      expect(screen.getByText('TrackMe')).toBeInTheDocument();
    });
    
    const streamFilterButton = screen.getByText('Потоки');
    fireEvent.click(streamFilterButton);
    
    // Проверяем, что dropdown-menu появился
    const dropdownMenu = document.querySelector('.report-dropdown-menu');
    expect(dropdownMenu).toBeInTheDocument();
    
    // Проверяем, что в dropdown есть элементы
    const dropdownItems = dropdownMenu.querySelectorAll('.report-dropdown-item');
    expect(dropdownItems.length).toBeGreaterThan(0);
    
    fireEvent.click(streamFilterButton);
    expect(document.querySelector('.report-dropdown-menu')).not.toBeInTheDocument();
  });

  test('рендерит таблицу с данными', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
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

  test('отображает правильные иконки в фильтрах', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );
    
    // Ждем загрузки
    await waitFor(() => {
      expect(screen.getByText('TrackMe')).toBeInTheDocument();
    });
    
    // Проверяем начальное состояние иконок
    const trackerIcons = document.querySelectorAll('.report-dropdown-icon-img');
    expect(trackerIcons[0]).toHaveAttribute('alt', 'Открыто');
    
    // Открываем фильтр и проверяем изменение иконки
    const trackerFilterButton = screen.getByTestId("trackers-btn");
    fireEvent.click(trackerFilterButton);
    
    const updatedIcons = document.querySelectorAll('.report-dropdown-icon-img');
    expect(updatedIcons[0]).toHaveAttribute('alt', 'Закрыто');
  });

  test('рендерит правильное количество строк в таблице', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
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
        <ReportPage defaultIsActive={false} />
      </Router>
    );
    
    // Ждем загрузки
    await waitFor(() => {
      expect(screen.getByText('TrackMe')).toBeInTheDocument();
    });
    
    // Открываем фильтр трекеров
    const trackerFilterButton = screen.getByTestId("trackers-btn");
    fireEvent.click(trackerFilterButton);
    
    await waitFor(() => {
      const dropdownMenu = screen.getByTestId('trackers-dropdown-menu');
      expect(dropdownMenu.children).toHaveLength(2);
    });
    
    // Закрываем фильтр трекеров
    fireEvent.click(trackerFilterButton);
    
    // Открываем фильтр потоков
    const streamFilterButton = screen.getByText('Потоки');
    fireEvent.click(streamFilterButton);
    
    await waitFor(() => {
      const dropdownMenu = screen.getByTestId('streams-dropdown-menu');
      expect(dropdownMenu.children).toHaveLength(1);
    });
  });

  test('кнопка "Выгрузить отчет" отображается и кликабельна', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
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

  test('переключение фильтров изменяет иконки', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );
    
    // Ждем загрузки
    await waitFor(() => {
      expect(screen.getByText('TrackMe')).toBeInTheDocument();
    });
    
    const trackerFilterButton = screen.getByTestId("trackers-btn");
    const streamFilterButton = screen.getByText('Потоки');
    
    // Проверяем начальное состояние
    const initialIcons = document.querySelectorAll('.report-dropdown-icon-img');
    expect(initialIcons[0]).toHaveAttribute('alt', 'Открыто');
    expect(initialIcons[1]).toHaveAttribute('alt', 'Открыто');
    
    // Открываем фильтр трекеров
    fireEvent.click(trackerFilterButton);
    const afterTrackerOpenIcons = document.querySelectorAll('.report-dropdown-icon-img');
    expect(afterTrackerOpenIcons[0]).toHaveAttribute('alt', 'Закрыто');
    expect(afterTrackerOpenIcons[1]).toHaveAttribute('alt', 'Открыто');
    
    // Закрываем фильтр трекеров
    fireEvent.click(trackerFilterButton);
    const afterTrackerCloseIcons = document.querySelectorAll('.report-dropdown-icon-img');
    expect(afterTrackerCloseIcons[0]).toHaveAttribute('alt', 'Открыто');
    expect(afterTrackerCloseIcons[1]).toHaveAttribute('alt', 'Открыто');
  });

  test('показывает состояние загрузки', async () => {
    // Мокаем медленный ответ
    fetchReports.mockImplementation(() => 
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
        <ReportPage defaultIsActive={false} />
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
    fetchReports.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: [] }),
    });

    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    // Ждем пока компонент перестанет показывать "Загрузка..."
    await waitFor(() => {
      expect(screen.getByText('Нет данных')).toBeInTheDocument();
    });
  });
});

describe('loadStreams and loadTrackers', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Default successful responses for streams and trackers
    fetchStreams.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [
          { name: 'Stream A' },
          { name: 'Stream B' },
        ]
      })
    });
    fetchTrackers.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [
          { username: 'tracker1', fullName: 'Tracker One' },
          { username: 'tracker2', fullName: 'Tracker Two' },
        ]
      })
    });
    fetchReports.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: [
        { 
          id: '1',
          username: 'tracker1', 
          streamName: 'Stream 1',
          startDate: '2024-01-01',
          endDate: '2024-01-31',
          teamCardName: 'Team A',
          averageTeamGrade: 4.5,
          averageUserGrade: 4.2,
          meetingsCountFact: 15,
          meetingsCountPlan: 16,
          ntiMarkets: ['MarketX'],
          readinessLevel: 'High'
        },
        { 
          id: '2',
          username: 'tracker2', 
          streamName: 'Stream 2',
          startDate: '2024-01-02',
          endDate: '2024-01-31',
          teamCardName: 'Team B',
          averageTeamGrade: 4.8,
          averageUserGrade: 4.6,
          meetingsCountFact: 20,
          meetingsCountPlan: 20,
          ntiMarkets: ['MarketY'],
          readinessLevel: 'Medium'
        }
      ] })
    });
  });

  test('loadStreams successfully fetches and sets streams', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(fetchStreams).toHaveBeenCalledWith({ page: 0, size: size });
    });

    // Open streams dropdown to verify the data appears
    const streamFilterButton = screen.getByText('Потоки');
    fireEvent.click(streamFilterButton);

    const dropdownMenu = await screen.findByTestId('streams-dropdown-menu');
    const items = within(dropdownMenu).getAllByRole('button'); // .dropdown-item divs
    expect(items).toHaveLength(3); // "—" + 2 streams
    expect(items[1]).toHaveTextContent('Stream A');
    expect(items[2]).toHaveTextContent('Stream B');
  });

  test('loadTrackers successfully fetches and sets trackers', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

await waitFor(() => {
      expect(fetchTrackers).toHaveBeenCalledWith({ page: 0, size: size });
    });

    const trackerFilterButton = screen.getByTestId("trackers-btn");
    fireEvent.click(trackerFilterButton);

    const dropdownMenu = await screen.findByTestId('trackers-dropdown-menu');
    const items = within(dropdownMenu).getAllByRole('button');
    expect(items).toHaveLength(3); // "—" + 2 trackers
    expect(items[1]).toHaveTextContent('Tracker One (tracker1)');
    expect(items[2]).toHaveTextContent('Tracker Two (tracker2)');
  });

  test('loadStreams handles HTTP error', async () => {
    fetchStreams.mockResolvedValue({
      ok: false,
      status: 500,
    });
    console.error = jest.fn();

    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(console.error).toHaveBeenCalledWith(
        'Ошибка загрузки отчётов',
        expect.objectContaining({ message: 'Ошибка HTTP: 500' })
      );
    });

    // Streams dropdown should only have the "—" item
    const streamFilterButton = screen.getByText('Потоки');
    fireEvent.click(streamFilterButton);
    const dropdownMenu = await screen.findByTestId('streams-dropdown-menu');
    const items = within(dropdownMenu).getAllByRole('button');
    expect(items).toHaveLength(1);
    expect(items[0]).toHaveTextContent('—');
  });

  test('loadTrackers handles HTTP error', async () => {
    fetchTrackers.mockResolvedValue({
      ok: false,
      status: 500,
    });
    console.error = jest.fn();

    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(console.error).toHaveBeenCalledWith(
        'Ошибка загрузки отчётов',
        expect.objectContaining({ message: 'Ошибка HTTP: 500' })
      );
    });

    const trackerFilterButton = screen.getByTestId("trackers-btn");
    fireEvent.click(trackerFilterButton);
    const dropdownMenu = await screen.findByTestId('trackers-dropdown-menu');
    const items = within(dropdownMenu).getAllByRole('button');
    expect(items).toHaveLength(1);
    expect(items[0]).toHaveTextContent('—');
  });

  test('loadStreams handles network error', async () => {
    const networkError = new Error('Network error');
    fetchStreams.mockRejectedValue(networkError);
    console.error = jest.fn();

    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(console.error).toHaveBeenCalledWith(
        'Ошибка загрузки отчётов',
        networkError
      );
    });
  });

  test('loadTrackers handles network error', async () => {
    const networkError = new Error('Network error');
    fetchTrackers.mockRejectedValue(networkError);
    console.error = jest.fn();

    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(console.error).toHaveBeenCalledWith(
        'Ошибка загрузки отчётов',
        networkError
      );
    });
  });
});

describe('filter selection', () => {
  beforeEach(() => {
    jest.clearAllMocks();
        fetchReports.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: [
        { 
          id: '1',
          username: 'tracker1', 
          streamName: 'Stream 1',
          startDate: '2024-01-01',
          endDate: '2024-01-31',
          teamCardName: 'Team A',
          averageTeamGrade: 4.5,
          averageUserGrade: 4.2,
          meetingsCountFact: 15,
          meetingsCountPlan: 16,
          ntiMarkets: ['MarketX'],
          readinessLevel: 'High'
        },
        { 
          id: '2',
          username: 'tracker2', 
          streamName: 'Stream 2',
          startDate: '2024-01-02',
          endDate: '2024-01-31',
          teamCardName: 'Team B',
          averageTeamGrade: 4.8,
          averageUserGrade: 4.6,
          meetingsCountFact: 20,
          meetingsCountPlan: 20,
          ntiMarkets: ['MarketY'],
          readinessLevel: 'Medium'
        }
      ] })
    });
    fetchStreams.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [{ name: 'Stream A' }, { name: 'Stream B' }]
      })
    });
    fetchTrackers.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [{ username: 'tracker1', fullName: 'Tracker One' }]
      })
    });
  });

  test('selecting a tracker filter triggers fetchReports with filter', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    // Wait for initial load
    await waitFor(() => {
      expect(fetchReports).toHaveBeenCalledWith({ page: 0, size: size, filters: [] });
    });

    const trackerFilterButton = screen.getByTestId("trackers-btn");
    fireEvent.click(trackerFilterButton);

    const dropdownMenu = await screen.findByTestId('trackers-dropdown-menu');
    const trackerItem = within(dropdownMenu).getByText('Tracker One (tracker1)');
    fireEvent.click(trackerItem);

    // Dropdown should close
    expect(screen.queryByTestId('trackers-dropdown-menu')).not.toBeInTheDocument();

    // fetchReports should be called again with the filter
    await waitFor(() => {
      expect(fetchReports).toHaveBeenCalledWith({ page: 0, size: size, filters: [
        { fieldName: "username", type: "EQ", value: "tracker1" }
      ] });
    });
  });

  test('selecting a stream filter triggers fetchReports with filter', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(fetchReports).toHaveBeenCalledWith({ page: 0, size: size, filters: [] });
    });

    const streamFilterButton = screen.getByText('Потоки');
    fireEvent.click(streamFilterButton);

    const dropdownMenu = await screen.findByTestId('streams-dropdown-menu');
    const streamItem = within(dropdownMenu).getByText('Stream A');
    fireEvent.click(streamItem);

    expect(screen.queryByTestId('streams-dropdown-menu')).not.toBeInTheDocument();

    await waitFor(() => {
      expect(fetchReports).toHaveBeenCalledWith({ page: 0, size: size, filters: [
        { fieldName: "streams.name", type: "EQ", value: "Stream A" }
      ] });
    });
  });

  test('selecting "—" resets tracker filter', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    // First set a filter
    const trackerFilterButton = screen.getByTestId("trackers-btn");
    fireEvent.click(trackerFilterButton);
    const dropdownMenu = await screen.findByTestId('trackers-dropdown-menu');
    const trackerItem = within(dropdownMenu).getByText('Tracker One (tracker1)');
    fireEvent.click(trackerItem);

    await waitFor(() => {
      expect(fetchReports).toHaveBeenCalledWith({ page: 0, size: size, filters: [
        { fieldName: "username", type: "EQ", value: "tracker1" }
      ] });
    });

    // Now reset using "—"
    fireEvent.click(trackerFilterButton);
    const resetItem = within(await screen.findByTestId('trackers-dropdown-menu')).getByText('—');
    fireEvent.click(resetItem);

    await waitFor(() => {
      expect(fetchReports).toHaveBeenCalledWith({ page: 0, size: size, filters: [] });
    });
  });

  test('selecting "—" resets stream filter', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    const streamFilterButton = screen.getByText('Потоки');
    fireEvent.click(streamFilterButton);
    const dropdownMenu = await screen.findByTestId('streams-dropdown-menu');
    const streamItem = within(dropdownMenu).getByText('Stream A');
    fireEvent.click(streamItem);

    await waitFor(() => {
      expect(fetchReports).toHaveBeenCalledWith({ page: 0, size: size, filters: [
        { fieldName: "streams.name", type: "EQ", value: "Stream A" }
      ] });
    });

    fireEvent.click(streamFilterButton);
    const resetItem = within(await screen.findByTestId('streams-dropdown-menu')).getByText('—');
    fireEvent.click(resetItem);

    await waitFor(() => {
      expect(fetchReports).toHaveBeenCalledWith({ page: 0, size: size, filters: [] });
    });
  });
});

describe('handleExportExcel', () => {
  let createObjectURLMock;
  let revokeObjectURLMock;
  let appendChildSpy;
  let removeChildSpy;
  let clickMock;

  beforeEach(() => {
    jest.clearAllMocks();

    fetchReports.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: [
        { 
          id: '1',
          username: 'tracker1', 
          streamName: 'Stream 1',
          startDate: '2024-01-01',
          endDate: '2024-01-31',
          teamCardName: 'Team A',
          averageTeamGrade: 4.5,
          averageUserGrade: 4.2,
          meetingsCountFact: 15,
          meetingsCountPlan: 16,
          ntiMarkets: ['MarketX'],
          readinessLevel: 'High'
        },
        { 
          id: '2',
          username: 'tracker2', 
          streamName: 'Stream 2',
          startDate: '2024-01-02',
          endDate: '2024-01-31',
          teamCardName: 'Team B',
          averageTeamGrade: 4.8,
          averageUserGrade: 4.6,
          meetingsCountFact: 20,
          meetingsCountPlan: 20,
          ntiMarkets: ['MarketY'],
          readinessLevel: 'Medium'
        }
      ] })
    });
    fetchStreams.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: [] }),
    });
    fetchTrackers.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: [] }),
    });

    clickMock = jest.fn();
    createObjectURLMock = jest.fn(() => 'blob:http://localhost/fake-url');
    revokeObjectURLMock = jest.fn();
    global.URL.createObjectURL = createObjectURLMock;
    global.URL.revokeObjectURL = revokeObjectURLMock;

    const originalAppendChild = document.body.appendChild.bind(document.body);
    appendChildSpy = jest.spyOn(document.body, 'appendChild').mockImplementation((el) => {
      if (el.tagName === 'A') {
        el.click = clickMock;
        return el;
      }
      return originalAppendChild(el);
    });
    removeChildSpy = jest.spyOn(document.body, 'removeChild').mockImplementation(() => {});
  });

  afterEach(() => {
    appendChildSpy.mockRestore();
    removeChildSpy.mockRestore();
  });

  test('вызывает fetchReportExcel без фильтров и скачивает файл', async () => {
    jest.useFakeTimers();
    
    const { fetchReportExcel } = require('../../services/requests');
    fetchReportExcel.mockResolvedValue({
      ok: true,
      headers: {
        get: () => "attachment; filename*=UTF-8''%D0%BE%D1%82%D1%87%D1%91%D1%82-%D0%BF%D0%BE-%D0%BA%D0%BE%D0%BC%D0%B0%D0%BD%D0%B4%D0%B0%D0%BC-2026-03-10.xlsx",
      },
      blob: jest.fn().mockResolvedValue(new Blob(['fake-xlsx'])),
    });

    render(<Router><ReportPage defaultIsActive={false} /></Router>);
    await waitFor(() => expect(screen.getByText('TrackMe')).toBeInTheDocument());

    fireEvent.click(screen.getByText('Выгрузить отчет'));

    await waitFor(() => {
      expect(fetchReportExcel).toHaveBeenCalledWith({ filters: [] });
    });

    await waitFor(() => {
      expect(createObjectURLMock).toHaveBeenCalled();
      expect(clickMock).toHaveBeenCalled();
      expect(removeChildSpy).toHaveBeenCalled();
    });

    expect(revokeObjectURLMock).not.toHaveBeenCalled();
    jest.runAllTimers();
    expect(revokeObjectURLMock).toHaveBeenCalledWith('blob:http://localhost/fake-url');

    jest.useRealTimers();
  });

  test('использует имя файла из Content-Disposition', async () => {
    const { fetchReportExcel } = require('../../services/requests');
    fetchReportExcel.mockResolvedValue({
      ok: true,
      headers: {
        get: () => "attachment; filename*=UTF-8''%D0%BE%D1%82%D1%87%D1%91%D1%82-%D0%BF%D0%BE-%D0%BA%D0%BE%D0%BC%D0%B0%D0%BD%D0%B4%D0%B0%D0%BC-2026-03-10.xlsx",
      },
      blob: jest.fn().mockResolvedValue(new Blob(['fake-xlsx'])),
    });

    render(<Router><ReportPage /></Router>);
    await waitFor(() => expect(screen.getByText('TrackMe')).toBeInTheDocument());

    fireEvent.click(screen.getByText('Выгрузить отчет'));

    await waitFor(() => {
      expect(appendChildSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          download: 'отчёт-по-командам-2026-03-10.xlsx',
          href: 'blob:http://localhost/fake-url',
        })
      );
    });
  });

  test('использует дефолтное имя если Content-Disposition отсутствует', async () => {
    const { fetchReportExcel } = require('../../services/requests');
    fetchReportExcel.mockResolvedValue({
      ok: true,
      headers: { get: () => null },
      blob: jest.fn().mockResolvedValue(new Blob(['fake-xlsx'])),
    });

    render(<Router><ReportPage /></Router>);
    await waitFor(() => expect(screen.getByText('TrackMe')).toBeInTheDocument());

    fireEvent.click(screen.getByText('Выгрузить отчет'));

    await waitFor(() => {
      expect(appendChildSpy).toHaveBeenCalledWith(
        expect.objectContaining({ download: 'отчёт-по-командам.xlsx' })
      );
    });
  });

  test('вызывает fetchReportExcel с фильтром трекера', async () => {
    const { fetchReportExcel } = require('../../services/requests');
    fetchReportExcel.mockResolvedValue({
      ok: true,
      headers: { get: () => null },
      blob: jest.fn().mockResolvedValue(new Blob(['fake-xlsx'])),
    });
    fetchTrackers.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [{ username: 'tracker1', fullName: 'Tracker One' }],
      }),
    });

    render(<Router><ReportPage defaultIsActive={false} /></Router>);
    await waitFor(() => expect(screen.getByText('TrackMe')).toBeInTheDocument());

    fireEvent.click(screen.getByTestId('trackers-btn'));
    const dropdown = await screen.findByTestId('trackers-dropdown-menu');
    fireEvent.click(within(dropdown).getByText('Tracker One (tracker1)'));

    fireEvent.click(screen.getByText('Выгрузить отчет'));

    await waitFor(() => {
      expect(fetchReportExcel).toHaveBeenCalledWith({
        filters: [{ fieldName: 'username', type: 'EQ', value: 'tracker1' }],
      });
    });
  });

  test('вызывает fetchReportExcel с фильтром потока', async () => {
    const { fetchReportExcel } = require('../../services/requests');
    fetchReportExcel.mockResolvedValue({
      ok: true,
      headers: { get: () => null },
      blob: jest.fn().mockResolvedValue(new Blob(['fake-xlsx'])),
    });
    fetchStreams.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [{ name: 'Stream A' }],
      }),
    });

    render(<Router><ReportPage defaultIsActive={false} /></Router>);
    await waitFor(() => expect(screen.getByText('TrackMe')).toBeInTheDocument());

    fireEvent.click(screen.getByText('Потоки'));
    const dropdown = await screen.findByTestId('streams-dropdown-menu');
    fireEvent.click(within(dropdown).getByText('Stream A'));

    fireEvent.click(screen.getByText('Выгрузить отчет'));

    await waitFor(() => {
      expect(fetchReportExcel).toHaveBeenCalledWith({
        filters: [{ fieldName: 'streams.name', type: 'EQ', value: 'Stream A' }],
      });
    });
  });
  
});
test("клик по названию команды вызывает navigate", async () => {
  fetchReports.mockResolvedValueOnce({
    ok: true,
    json: jest.fn().mockResolvedValue({
      content: [
        {
          teamId: "123",
          teamCardName: "Название команды очень длинное",
          teamName: "Название команды очень длинное",
          streamName: "Поток",
          startDate: "01.01.2025",
          endDate: "02.01.2025",
          username: "Тест",
          averageTeamGrade: 5,
          averageUserGrade: 5,
          meetingsCountFact: 1,
          meetingsCountPlan: 1,
          ntiMarkets: ["HealthNet"],
          readinessLevel: "1",
        },
      ],
    }),
  });

  render(
    <Router>
      <ReportPage defaultIsActive={false} />
    </Router>
  );

  await waitFor(() => {
    expect(screen.getByText("Название команды очень длинное")).toBeInTheDocument();
  });

  fireEvent.click(screen.getByText("Название команды очень длинное"));

  expect(mockNavigate).toHaveBeenCalledWith("/teamcard/123");
});

describe('Сортировка колонок таблицы', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    fetchReports.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [
          {
            streamName: 'Stream A',
            startDate: '2020-01-01',
            endDate: '2020-06-30',
            teamCardName: 'Team A',
            username: 'User1',
            averageTeamGrade: 3.5,
            averageUserGrade: 4.0,
            meetingsCountFact: 5,
            meetingsCountPlan: 10,
            ntiMarkets: ['Market1'],
            readinessLevel: '5'
          },
          {
            streamName: 'Stream A',
            startDate: '2020-01-01',
            endDate: '2020-06-30',
            teamCardName: 'Team B',
            username: 'User2',
            averageTeamGrade: 4.8,
            averageUserGrade: 4.5,
            meetingsCountFact: 8,
            meetingsCountPlan: 10,
            ntiMarkets: ['Market2'],
            readinessLevel: '7'
          },
          {
            streamName: 'Stream A',
            startDate: '2020-01-01',
            endDate: '2020-06-30',
            teamCardName: 'Team C',
            username: 'User3',
            averageTeamGrade: 4.2,
            averageUserGrade: 4.2,
            meetingsCountFact: 6,
            meetingsCountPlan: 10,
            ntiMarkets: ['Market3'],
            readinessLevel: '6'
          }
        ]
      }),
    });
    fetchStreams.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: [] }),
    });
    fetchTrackers.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: [] }),
    });
  });

  test('сортировка по средней оценке команды (desc)', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(screen.getByText('Team A')).toBeInTheDocument();
    });

    const gradeHeader = screen.getByText('Средняя оценка команды');
    fireEvent.click(gradeHeader);

    await waitFor(() => {
      const rows = screen.getAllByRole('row');
      expect(within(rows[1]).getByText('Team B')).toBeInTheDocument();
      expect(within(rows[2]).getByText('Team C')).toBeInTheDocument();
      expect(within(rows[3]).getByText('Team A')).toBeInTheDocument();
    });
  });

  test('сортировка по средней оценке команды (asc) при повторном клике', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(screen.getByText('Team A')).toBeInTheDocument();
    });

    const gradeHeader = screen.getByText('Средняя оценка команды');
    fireEvent.click(gradeHeader);
    fireEvent.click(gradeHeader);

    await waitFor(() => {
      const rows = screen.getAllByRole('row');
      expect(within(rows[1]).getByText('Team A')).toBeInTheDocument();
      expect(within(rows[2]).getByText('Team C')).toBeInTheDocument();
      expect(within(rows[3]).getByText('Team B')).toBeInTheDocument();
    });
  });

  test('сортировка по названию команды (А-Я)', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(screen.getByText(/Название команды/)).toBeInTheDocument();
    });

    const teamHeader = screen.getByText(/Название команды/);
    fireEvent.click(teamHeader);

    await waitFor(() => {
      const rows = screen.getAllByRole('row');
      const firstRowText = rows[1].textContent;
      expect(firstRowText).toContain('Team C');
    });

    fireEvent.click(teamHeader);

    await waitFor(() => {
      const rows = screen.getAllByRole('row');
      const firstRowText = rows[1].textContent;
      expect(firstRowText).toContain('Team A');
    });
  });

  test('сортировка по уровню TRL', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(screen.getByText('Team A')).toBeInTheDocument();
    });

    const trlHeader = screen.getByText('Уровень TRL');
    fireEvent.click(trlHeader);

    await waitFor(() => {
      const rows = screen.getAllByRole('row');
      expect(within(rows[1]).getByText('7')).toBeInTheDocument();
      expect(within(rows[2]).getByText('6')).toBeInTheDocument();
      expect(within(rows[3]).getByText('5')).toBeInTheDocument();
    });
  });

  test('сортировка по средней оценке трекера (desc)', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(screen.getByText('Team A')).toBeInTheDocument();
    });

    const gradeHeader = screen.getByText('Средняя оценка трекера');
    fireEvent.click(gradeHeader);

    await waitFor(() => {
      const rows = screen.getAllByRole('row');
      expect(within(rows[1]).getByText('Team B')).toBeInTheDocument();
      expect(within(rows[2]).getByText('Team C')).toBeInTheDocument();
      expect(within(rows[3]).getByText('Team A')).toBeInTheDocument();
    });
  });

  test('надпись сортировки для названия команды отображается корректно', async () => {
    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(screen.getByText('Team A')).toBeInTheDocument();
    });

    expect(screen.getByText('Название команды А→Я')).toBeInTheDocument();

    const teamHeader = screen.getByText('Название команды А→Я');
    fireEvent.click(teamHeader);

    await waitFor(() => {
      expect(screen.getByText('Название команды Я→А')).toBeInTheDocument();
    });
  });
});

describe('Фильтр потоков - активные и неактивные', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    fetchReports.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: [] }),
    });
  });

  test('активные потоки показываются сверху с зелёным кружком', async () => {
    const today = new Date();
    const formatDate = (offsetDays) => {
      const d = new Date(today);
      d.setDate(d.getDate() + offsetDays);
      return d.toISOString().split('T')[0];
    };

    fetchStreams.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [
          { name: 'Inactive Stream', startDate: '2020-01-01', endDate: '2020-12-31' },
          { name: 'Active Stream 1', startDate: formatDate(-30), endDate: formatDate(30) },
          { name: 'Active Stream 2', startDate: formatDate(-60), endDate: formatDate(60) },
        ]
      }),
    });
    fetchTrackers.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: [] }),
    });

    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(screen.getByText('Потоки')).toBeInTheDocument();
    });

    const streamFilterButton = screen.getByText('Потоки');
    fireEvent.click(streamFilterButton);

    await waitFor(() => {
      const dropdown = screen.getByTestId('streams-dropdown-menu');
      const items = within(dropdown).getAllByRole('button');
      expect(items).toHaveLength(4);

      expect(items[1]).toHaveTextContent('Active Stream 2');
      expect(items[1].querySelector('.active-stream-dot')).toBeInTheDocument();

      expect(items[2]).toHaveTextContent('Active Stream 1');
      expect(items[2].querySelector('.active-stream-dot')).toBeInTheDocument();

      expect(items[3]).toHaveTextContent('Inactive Stream');
      expect(items[3].querySelector('.active-stream-dot')).not.toBeInTheDocument();
    });
  });

  test('активные потоки сортируются по дате окончания (выше - позже)', async () => {
    const today = new Date();
    const formatDate = (offsetDays) => {
      const d = new Date(today);
      d.setDate(d.getDate() + offsetDays);
      return d.toISOString().split('T')[0];
    };

    fetchStreams.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [
          { name: 'Active Later', startDate: formatDate(-30), endDate: formatDate(60) },
          { name: 'Active Sooner', startDate: formatDate(-60), endDate: formatDate(30) },
        ]
      }),
    });
    fetchTrackers.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: [] }),
    });

    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(screen.getByText('Потоки')).toBeInTheDocument();
    });

    const streamFilterButton = screen.getByText('Потоки');
    fireEvent.click(streamFilterButton);

    await waitFor(() => {
      const dropdown = screen.getByTestId('streams-dropdown-menu');
      const items = within(dropdown).getAllByRole('button');
      expect(items[1]).toHaveTextContent('Active Later');
      expect(items[2]).toHaveTextContent('Active Sooner');
    });
  });

  test('активные потоки сортируются по алфавиту при равных датах', async () => {
    const today = new Date();
    const formatDate = (offsetDays) => {
      const d = new Date(today);
      d.setDate(d.getDate() + offsetDays);
      return d.toISOString().split('T')[0];
    };

    fetchStreams.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [
          { name: 'Stream Z', startDate: formatDate(-30), endDate: formatDate(30) },
          { name: 'Stream A', startDate: formatDate(-30), endDate: formatDate(30) },
        ]
      }),
    });
    fetchTrackers.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: [] }),
    });

    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(screen.getByText('Потоки')).toBeInTheDocument();
    });

    const streamFilterButton = screen.getByText('Потоки');
    fireEvent.click(streamFilterButton);

    await waitFor(() => {
      const dropdown = screen.getByTestId('streams-dropdown-menu');
      const items = within(dropdown).getAllByRole('button');
      expect(items[1]).toHaveTextContent('Stream A');
      expect(items[2]).toHaveTextContent('Stream Z');
    });
  });

  test('неактивные потоки сортируются по дате окончания (выше - позже)', async () => {
    fetchStreams.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [
          { name: 'Inactive Later', startDate: '2020-01-01', endDate: '2020-12-31' },
          { name: 'Inactive Sooner', startDate: '2020-01-01', endDate: '2020-06-30' },
        ]
      }),
    });
    fetchTrackers.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: [] }),
    });

    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(screen.getByText('Потоки')).toBeInTheDocument();
    });

    const streamFilterButton = screen.getByText('Потоки');
    fireEvent.click(streamFilterButton);

    await waitFor(() => {
      const dropdown = screen.getByTestId('streams-dropdown-menu');
      const items = within(dropdown).getAllByRole('button');
      expect(items[1]).toHaveTextContent('Inactive Later');
      expect(items[2]).toHaveTextContent('Inactive Sooner');
    });
  });
});

describe('Подсветка активных строк в таблице', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('активные потоки подсвечиваются зелёным при включенном фильтре isActive', async () => {
    const today = new Date();
    const formatDate = (offsetDays) => {
      const d = new Date(today);
      d.setDate(d.getDate() + offsetDays);
      return d.toISOString().split('T')[0];
    };

    fetchReports.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [
          {
            streamName: 'Inactive Stream',
            startDate: '2020-01-01',
            endDate: '2020-12-31',
            teamCardName: 'Team Inactive',
            username: 'User1',
            averageTeamGrade: 4.0,
            averageUserGrade: 4.0,
            meetingsCountFact: 5,
            meetingsCountPlan: 10,
            ntiMarkets: ['Market1'],
            readinessLevel: '5'
          },
          {
            streamName: 'Active Stream',
            startDate: formatDate(-30),
            endDate: formatDate(30),
            teamCardName: 'Team Active',
            username: 'User2',
            averageTeamGrade: 4.5,
            averageUserGrade: 4.5,
            meetingsCountFact: 8,
            meetingsCountPlan: 10,
            ntiMarkets: ['Market2'],
            readinessLevel: '6'
          }
        ]
      }),
    });
    fetchStreams.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: [] }),
    });
    fetchTrackers.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: [] }),
    });

    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(screen.getByText('Team Active')).toBeInTheDocument();
    });

    const rows = screen.getAllByRole('row');
    const activeRow = rows.find(row => within(row).queryByText('Team Active'));
    expect(activeRow).toHaveClass('active-row');
  });
});

test("клик по названию потока вызывает navigate", async () => {
  fetchReports.mockResolvedValueOnce({
    ok: true,
    json: jest.fn().mockResolvedValue({
      content: [
        {
          streamId: "stream-123",
          streamName: "Test Stream",
          startDate: "2024-01-01",
          endDate: "2024-12-31",
          teamCardName: "Team A",
          username: "User1",
          averageTeamGrade: 4,
          averageUserGrade: 4,
          meetingsCountFact: 1,
          meetingsCountPlan: 1,
          ntiMarkets: ["Market1"],
          readinessLevel: "5",
        },
      ],
    }),
  });

  render(
    <Router>
      <ReportPage defaultIsActive={false} />
    </Router>
  );

  await waitFor(() => {
    expect(screen.getByText("Test Stream")).toBeInTheDocument();
  });

  fireEvent.click(screen.getByText("Test Stream"));

  expect(mockNavigate).toHaveBeenCalledWith("/report/stream-123");
});

describe('Поиск по трекерам в фильтре', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('отображается поле поиска в фильтре трекеров', async () => {
    fetchReports.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [
          { streamName: 'Stream 1', startDate: '2024-01-01', endDate: '2024-12-31', teamCardName: 'Team 1', username: 'ivanov', averageTeamGrade: 4, averageUserGrade: 4, meetingsCountFact: 1, meetingsCountPlan: 1, ntiMarkets: ['Market1'], readinessLevel: '5' },
          { streamName: 'Stream 1', startDate: '2024-01-01', endDate: '2024-12-31', teamCardName: 'Team 2', username: 'petrov', averageTeamGrade: 4, averageUserGrade: 4, meetingsCountFact: 1, meetingsCountPlan: 1, ntiMarkets: ['Market1'], readinessLevel: '5' },
        ]
      }),
    });
    fetchStreams.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: [] }),
    });
    fetchTrackers.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [
          { username: 'ivanov', fullName: 'Иванов Иван' },
          { username: 'petrov', fullName: 'Петров Петр' },
        ]
      }),
    });

    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(screen.getByTestId("trackers-btn")).toBeInTheDocument();
    });

    const trackerFilterButton = screen.getByTestId("trackers-btn");
    fireEvent.click(trackerFilterButton);

    await waitFor(() => {
      const input = screen.getByPlaceholderText('Поиск по имени или логину...');
      expect(input).toBeInTheDocument();
    });
  });

test('фильтрует трекеров по имени', async () => {
    fetchReports.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [
          { streamName: 'Stream 1', startDate: '2024-01-01', endDate: '2024-12-31', teamCardName: 'Team 1', username: 'ivanov', averageTeamGrade: 4, averageUserGrade: 4, meetingsCountFact: 1, meetingsCountPlan: 1, ntiMarkets: ['Market1'], readinessLevel: '5' },
          { streamName: 'Stream 1', startDate: '2024-01-01', endDate: '2024-12-31', teamCardName: 'Team 2', username: 'petrov', averageTeamGrade: 4, averageUserGrade: 4, meetingsCountFact: 1, meetingsCountPlan: 1, ntiMarkets: ['Market1'], readinessLevel: '5' },
        ]
      }),
    });
    fetchStreams.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: [] }),
    });
    fetchTrackers.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [
          { username: 'ivanov', fullName: 'Иванов Иван' },
          { username: 'petrov', fullName: 'Петров Петр' },
          { username: 'sidorov', fullName: 'Сидоров Сидор' },
        ]
      }),
    });

    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(screen.getByTestId("trackers-btn")).toBeInTheDocument();
    });

    const trackerFilterButton = screen.getByTestId("trackers-btn");
    fireEvent.click(trackerFilterButton);

    await waitFor(() => {
      const input = screen.getByPlaceholderText('Поиск по имени или логину...');
      fireEvent.change(input, { target: { value: 'Иванов' } });
    });

    await waitFor(() => {
      const dropdown = screen.getByTestId('trackers-dropdown-menu');
      const items = within(dropdown).getAllByRole('button');
      expect(items).toHaveLength(2);
      expect(items[1]).toHaveTextContent('Иванов Иван (ivanov)');
    });
  });

  test('фильтрует трекеров по логину', async () => {
    fetchReports.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [
          { streamName: 'Stream 1', startDate: '2024-01-01', endDate: '2024-12-31', teamCardName: 'Team 1', username: 'ivanov', averageTeamGrade: 4, averageUserGrade: 4, meetingsCountFact: 1, meetingsCountPlan: 1, ntiMarkets: ['Market1'], readinessLevel: '5' },
          { streamName: 'Stream 1', startDate: '2024-01-01', endDate: '2024-12-31', teamCardName: 'Team 2', username: 'petrov', averageTeamGrade: 4, averageUserGrade: 4, meetingsCountFact: 1, meetingsCountPlan: 1, ntiMarkets: ['Market1'], readinessLevel: '5' },
        ]
      }),
    });
    fetchStreams.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: [] }),
    });
    fetchTrackers.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [
          { username: 'ivanov', fullName: 'Иванов Иван' },
          { username: 'petrov', fullName: 'Петров Петр' },
        ]
      }),
    });

    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(screen.getByTestId("trackers-btn")).toBeInTheDocument();
    });

    const trackerFilterButton = screen.getByTestId("trackers-btn");
    fireEvent.click(trackerFilterButton);

    await waitFor(() => {
      const input = screen.getByPlaceholderText('Поиск по имени или логину...');
      fireEvent.change(input, { target: { value: 'petrov' } });
    });

    await waitFor(() => {
      const dropdown = screen.getByTestId('trackers-dropdown-menu');
      const items = within(dropdown).getAllByRole('button');
      expect(items).toHaveLength(2);
      expect(items[1]).toHaveTextContent('Петров Петр (petrov)');
    });
  });

  test('при выборе трекера поле поиска очищается', async () => {
    fetchReports.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [
          { streamName: 'Stream 1', startDate: '2024-01-01', endDate: '2024-12-31', teamCardName: 'Team 1', username: 'ivanov', averageTeamGrade: 4, averageUserGrade: 4, meetingsCountFact: 1, meetingsCountPlan: 1, ntiMarkets: ['Market1'], readinessLevel: '5' },
          { streamName: 'Stream 1', startDate: '2024-01-01', endDate: '2024-12-31', teamCardName: 'Team 2', username: 'petrov', averageTeamGrade: 4, averageUserGrade: 4, meetingsCountFact: 1, meetingsCountPlan: 1, ntiMarkets: ['Market1'], readinessLevel: '5' },
        ]
      }),
    });
    fetchStreams.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ content: [] }),
    });
    fetchTrackers.mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({
        content: [
          { username: 'ivanov', fullName: 'Иванов Иван' },
          { username: 'petrov', fullName: 'Петров Петр' },
        ]
      }),
    });

    render(
      <Router>
        <ReportPage defaultIsActive={false} />
      </Router>
    );

    await waitFor(() => {
      expect(screen.getByTestId("trackers-btn")).toBeInTheDocument();
    });

    const trackerFilterButton = screen.getByTestId("trackers-btn");
    fireEvent.click(trackerFilterButton);

    await waitFor(() => {
      const input = screen.getByPlaceholderText('Поиск по имени или логину...');
      fireEvent.change(input, { target: { value: 'Иванов' } });
    });

    await waitFor(() => {
      const dropdown = screen.getByTestId('trackers-dropdown-menu');
      const items = within(dropdown).getAllByRole('button');
      fireEvent.click(items[1]);
    });

    await waitFor(() => {
      expect(screen.queryByTestId('trackers-dropdown-menu')).not.toBeInTheDocument();
    });

    fireEvent.click(trackerFilterButton);
    await waitFor(() => {
      const input = screen.getByPlaceholderText('Поиск по имени или логину...');
      expect(input.value).toBe('');
    });
  });
});