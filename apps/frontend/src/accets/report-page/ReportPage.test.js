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
    const trackerIcons = document.querySelectorAll('.dropdown-icon-img');
    expect(trackerIcons[0]).toHaveAttribute('alt', 'Открыто');
    
    // Открываем фильтр и проверяем изменение иконки
    const trackerFilterButton = screen.getByTestId("trackers-btn");
    fireEvent.click(trackerFilterButton);
    
    const updatedIcons = document.querySelectorAll('.dropdown-icon-img');
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
      expect(dropdownMenu.children).toHaveLength(1);
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
      json: jest.fn().mockResolvedValue({ content: [] })
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
      json: jest.fn().mockResolvedValue({ content: [] })
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
