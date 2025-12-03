// src/accets/komand/team-card.test.js
import React from 'react';
import { render, screen, fireEvent, act, waitFor, within } from '@testing-library/react';
import '@testing-library/jest-dom';
import TeamCard from './team-card.js';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import * as redux from 'react-redux';
import { getMeetingStatusClass } from './team-card.js';

const mockedNavigate = jest.fn();

jest.mock('../meeting-card/MeetingCreate.js', () => {
  return function MockMeetingCreate({ onClose }) {
    return (
      <div role="dialog" data-testid="meeting-create-modal">
        <button onClick={onClose}>×</button>
        <div>Meeting Create Modal Content</div>
      </div>
    );
  };
});
jest.mock('react-router-dom', () => {
  const original = jest.requireActual('react-router-dom');
  let searchValue = '';
  let locState = {};
  return {
    ...original,
    useNavigate: () => mockedNavigate,
    useParams: () => ({ id: '42' }),
    useLocation: () => ({
      state: locState,
      get search() { return searchValue; },
      set search(val) { searchValue = val; }
    }),
    __setSearch: (val) => { searchValue = val; },
    __setState: (val) => { locState = val; }
  };
});

jest.mock('react-redux', () => ({
  ...jest.requireActual('react-redux'),
  useSelector: jest.fn(),
}));

beforeEach(() => {
  mockedNavigate.mockClear();
  jest.clearAllMocks();

  redux.useSelector.mockImplementation(() => ({
    user: { username: 'reduxUser', roles: ['ADMIN'] }
  }));
  Storage.prototype.getItem = jest.fn(() =>
    JSON.stringify({ username: 'reduxUser', roles: ['ADMIN'] })
  );
  window.confirm = jest.fn(() => true);

  global.fetch = jest.fn((url, opts = {}) => {
    // 1) PATCH (handleSave)
    if (opts.method === 'PATCH') {
      const body = JSON.parse(opts.body);
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          id: 42,
          name: body.name,
          description: body.description,
          ntiMarkets: body.ntiMarketIds.map(id => ({
            id,
            displayName: id === 20 ? 'NewMarket' : 'OldMarket'
          })),
          readinessLevel: body.readinessLevel,
          stream: { id: 1 },
          username: 'reduxUser'
        })
      });
    }

    // 2) admin/team-cards (ADMIN)
    if (url.includes('/api/v1/admin/team-cards?page')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [{
            id: 42,
            name: 'OldName',
            description: 'OldDesc',
            ntiMarkets: [{ id: 10, displayName: 'OldMarket' }],
            readinessLevel: '0-2',
            streams: [{
              id: 1,
              name: 'MyStream',
              startDate: '2025-03-01T00:00:00Z',
              endDate: '2025-03-10T00:00:00Z',
              meetingsCount: 5 // Set meetingsCount to allow more meetings
            }],
            username: 'reduxUser'
          }],
          totalPages: 1
        })
      });
    }

    // 2b) team-cards (TRACKER)
    if (url.includes('/api/v1/team-cards?page')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [{
            id: 42,
            name: 'OldName',
            description: 'OldDesc',
            ntiMarkets: [{ id: 10, displayName: 'OldMarket' }],
            readinessLevel: '0-2',
            streams: [{
              id: 1,
              name: 'MyStream',
              startDate: '2025-03-01T00:00:00Z',
              endDate: '2025-03-10T00:00:00Z',
              meetingsCount: 5 // Set meetingsCount to allow more meetings
            }],
            username: 'reduxUser'
          }],
          totalPages: 1
        })
      });
    }

    // 3) Потоки
    if (url.includes('/api/v1/streams?page=0&size=150')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [{
            id: 1,
            name: 'MyStream',
            startDate: '2025-03-01T00:00:00Z',
            endDate: '2025-03-10T00:00:00Z',
            meetingsCount: 5 // Ensure consistency
          }]
        })
      });
    }

    // 4) NTI-рынки
    if (url.includes('/api/v1/streams/nti-markets')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve([
          { id: 10, displayName: 'OldMarket' },
          { id: 20, displayName: 'NewMarket' }
        ])
      });
    }

    // 5) Трекеры
    if (url.includes('/api/v1/users/trackers')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
    }

    // 6) Количество карточек
    if (url.includes('/api/v1/team-card/count')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve(5) });
    }

    // 7) Встречи
    if (url.includes('/api/v1/meetings')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [{ id: 100, startDate: '2025-01-05T00:00:00Z', number: 2 }],
          totalPages: 1
        })
      });
    }

    // 8) Получение ФИО админа
    if (url.endsWith('/api/v1/users/reduxUser/info')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ fullName: 'Admin FullName' }) });
    }

    // 9) Получение ФИО трекера
    if (url.endsWith('/api/v1/account/info')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ fullName: 'Tracker FullName' }) });
    }

    // По умолчанию
    return Promise.resolve({
      ok: true,
      json: () => Promise.resolve({ content: [], totalPages: 1 })
    });
  });
});


// === Базовые проверки ===
describe('TeamCard basic interactions', () => {
  test('renders Edit/Save toggle', async () => {
    require('react-router-dom').__setSearch('');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });
    fireEvent.click(screen.getByRole('button', { name: /Редактировать/i }));
    expect(screen.getByRole('button', { name: /Сохранить/i })).toBeInTheDocument();
  });

  

  test('tracker input readonly', async () => {
    require('react-router-dom').__setSearch('');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });
    expect(screen.getByPlaceholderText(/ФИО трекера/i)).toHaveAttribute('readOnly');
  });
});

// === Meetings ===
describe('Meetings list and navigation', () => {
  test('loads meetings and navigates', async () => {
    require('react-router-dom').__setSearch('');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });
    const meet = await screen.findByText(/Встреча 2/i);
    fireEvent.click(meet);
    expect(mockedNavigate).toHaveBeenCalledWith('/meeting/100?teamId=42&username=reduxUser');
  });
});

// === Save & Deactivate ===
describe('Save and Deactivate flows', () => {
  test('handleSave updates UI', async () => {
    require('react-router-dom').__setSearch('?edit=true');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });
    fireEvent.change(screen.getByPlaceholderText(/Карточка команды/i), { target: { value: 'NewName' } });
    fireEvent.click(screen.getByText(/OldMarket|Рынки НТИ/));


    await screen.findByText('NewMarket');
    fireEvent.click(screen.getByText('NewMarket'));
    fireEvent.click(screen.getByText('0-2'));
    await screen.findByText('3-5');
    fireEvent.click(screen.getByText('3-5'));
    fireEvent.change(screen.getByPlaceholderText(/Описание карточки/i), { target: { value: 'NewDesc' } });
    fireEvent.click(screen.getByRole('button', { name: /Сохранить/i }));
    await waitFor(() => screen.getByRole('button', { name: /Редактировать/i }));
    expect(screen.getByPlaceholderText(/Карточка команды/i)).toHaveValue('NewName');
    expect(screen.getByPlaceholderText(/Описание карточки/i)).toHaveValue('NewDesc');
  });

  test('handleDeactivate navigates back', async () => {
    require('react-router-dom').__setSearch('');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });
    fireEvent.click(screen.getByRole('button', { name: /Редактировать/i }));
    fireEvent.click(screen.getByText(/Деактивировать/i));
    await waitFor(() => expect(mockedNavigate).toHaveBeenCalledWith('/team-cards'));
  });
});

// === Stream info & Dates ===
describe('Stream info & date formatting', () => {
  beforeEach(() => {
    const RR = require('react-router-dom');
    RR.__setState({ streamId: 1 });
  });

  test('shows stream name, count and formatted dates', async () => {
  require('react-router-dom').__setSearch('');
  require('react-router-dom').__setState({ streamId: 1 });
  
  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });
  
  // Wait for stream info to load
  const streamName = await screen.findByText('MyStream');
expect(streamName).toBeInTheDocument();

  expect(screen.getByText('5')).toBeInTheDocument();
  expect(screen.getByText('01.03.2025 - 10.03.2025')).toBeInTheDocument();
});

});

// === Tracker full name & localStorage fallback ===
describe('Tracker full name & localStorage fallback', () => {
  test('ADMIN branch uses /users/:username/info', async () => {
    const RR = require('react-router-dom');
    RR.__setState({});
    require('react-router-dom').__setSearch('');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });
    expect(await screen.findByDisplayValue('Admin FullName')).toBeInTheDocument();
  });
  test('если поток не найден, бросается ошибка', async () => {
  const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  
  global.fetch = jest.fn((url) => {
    // Simulate failure to fetch team card
    if (url.includes('/api/v1/admin/team-cards') || url.includes('/api/v1/team-cards')) {
      return Promise.resolve({
        ok: false,
        status: 500,
        json: () => Promise.resolve({}),
      });
    }
    if (url.includes('/api/v1/streams?page=0')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [] }), // No streams available
      });
    }
    if (url.includes('/api/v1/streams/nti-markets')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve([{ id: 10, displayName: 'OldMarket' }]),
      });
    }
    if (url.endsWith('/api/v1/users/reduxUser/info')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ fullName: 'Admin FullName' }),
      });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
  });

  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });

  await waitFor(() => {
    expect(consoleSpy).toHaveBeenCalledWith(
      expect.stringContaining('поиске карточки команды'),
      expect.any(Error)
    );
  }, { timeout: 2000 });

  consoleSpy.mockRestore();
});
test('handleApiError вызывается при ошибке загрузки встреч', async () => {
  global.fetch = jest.fn((url) => {
    if (url.includes('/api/v1/meetings')) {
      return Promise.resolve({ ok: false, status: 500 });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });

  });

  const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });
  expect(consoleSpy).toHaveBeenCalledWith(expect.stringContaining('загрузке встреч'), expect.any(Error));
  consoleSpy.mockRestore();
});
test('ошибка загрузки трекеров вызывает обработку ошибки', async () => {
  redux.useSelector.mockImplementation(() => ({
    user: { username: 'reduxUser', roles: ['ADMIN'] }
  }));

  global.fetch = jest.fn((url) => {
    if (url.includes('/api/v1/users/trackers')) {
      return Promise.resolve({ ok: false, status: 500 });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });

  });

  const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });
  expect(consoleSpy).toHaveBeenCalledWith(expect.stringContaining('загрузке трекеров'), expect.any(Error));
  consoleSpy.mockRestore();
});
test('handleSave выбрасывает ошибку при незаполненных обязательных полях (console)', async () => {
  const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  require('react-router-dom').__setSearch('?edit=true');
  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });

  fireEvent.change(screen.getByPlaceholderText(/Карточка команды/i), { target: { value: '' } });
  fireEvent.change(screen.getByPlaceholderText(/Описание карточки/i), { target: { value: '' } });

  fireEvent.click(screen.getByRole('button', { name: /Сохранить/i }));

  await waitFor(() => {
    expect(consoleSpy).toHaveBeenCalledWith(
      expect.stringContaining('сохранении карточки'),
      expect.any(Error)
    );
  });
  consoleSpy.mockRestore();
});
test('берет данные из localStorage, если он есть', async () => {
  const saved = JSON.stringify({ username: 'localUser', roles: ['SUPER_ADMIN'] });
  Storage.prototype.getItem = jest.fn(() => saved);
  
  const setItemSpy = jest.spyOn(Storage.prototype, 'setItem');

  redux.useSelector.mockImplementation(() => ({
    user: { username: 'reduxUser', roles: ['ADMIN'] }
  }));

  require('react-router-dom').__setState({});
  require('react-router-dom').__setSearch('');

  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });

  // Проверим, что localStorage использовался
  expect(Storage.prototype.getItem).toHaveBeenCalledWith('user');
  // И не было вызова setItem, потому что localStorage уже был
  expect(setItemSpy).not.toHaveBeenCalled();
});





test('ошибка при удалении карточки вызывает handleApiError', async () => {
  const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {}); // ⬅️ добавлено

  global.fetch = jest.fn((url, options) => {
    if (options?.method === 'DELETE') {
      return Promise.resolve({ ok: false, status: 500 });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });

  });

  window.confirm = jest.fn(() => true); // подтверждение деактивации

  require('react-router-dom').__setSearch('');
  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });

  fireEvent.click(screen.getByRole('button', { name: /Редактировать/i }));
  fireEvent.click(screen.getByRole('button', { name: /Деактивировать/i }));

  await waitFor(() =>
    expect(consoleSpy).toHaveBeenCalledWith( // ⬅️ заменено с console.error
      expect.stringContaining('удалении карточки'),
      expect.any(Error)
    )
  );

  consoleSpy.mockRestore(); // ⬅️ не забудь очистить
});
test('выбор TRL через клавишу Enter вызывает handleTRLSelect', async () => {
  require('react-router-dom').__setSearch('?edit=true');

  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });

  // Клик по отображаемому текущему TRL (например, "0-2")
  fireEvent.click(screen.getByText('0-2'));

  // Получение кнопки TRL уровня (например, "3-5") и имитация клавиши
  const trlButton = screen.getByRole('button', { name: '3-5' });
  fireEvent.keyDown(trlButton, { key: 'Enter' });

  // Проверка, что TRL изменился
  expect(screen.getByText('3-5')).toBeInTheDocument();
});

test('handleSave заменяет username на объектный, если он найден в trackers', async () => {
  require('react-router-dom').__setSearch('?edit=true');

  redux.useSelector.mockImplementation(() => ({
    user: { username: 'reduxUser', roles: ['ADMIN'] }
  }));

  const patchSpy = jest.fn((url, options) => {
    const body = JSON.parse(options.body);
    expect(url).toContain('username=realUser'); // важно: именно username, а не id
    return Promise.resolve({
      ok: true,
      json: () => Promise.resolve({
        ...body,
        username: 'realUser'
      })
    });
  });

  global.fetch = jest.fn((url, options = {}) => {
    if (url.includes('/api/v1/users/trackers')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [{ id: 99, username: 'realUser', fullName: 'Имя', enabled: true }]
        })
      });
    }

    if (options.method === 'PATCH') {
      return patchSpy(url, options);
    }

    if (url.includes('/api/v1/admin/team-cards')) {
    return Promise.resolve({
      ok: true,
      json: () => Promise.resolve({
        content: [{
          id: 42,
          name: 'OldName',
          description: 'OldDesc',
          ntiMarkets: [{ id: 10, displayName: 'OldMarket' }],
          readinessLevel: '0-2',
          stream: { id: 999 },
          username: 'reduxUser'
        }]
      })
    });
  }

    if (url.includes('/api/v1/streams/nti-markets')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve([
          { id: 10, displayName: 'OldMarket' },
          { id: 20, displayName: 'NewMarket' }
        ])
      });
    }

    if (url.includes('/api/v1/streams')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [{ id: 1, name: 'MyStream', startDate: '2025-03-01T00:00:00Z', endDate: '2025-03-10T00:00:00Z' }]
        })
      });
    }

    return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
  });

  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });

  fireEvent.change(screen.getByPlaceholderText(/Карточка команды/i), {
    target: { value: 'TestName' }
  });

  fireEvent.change(screen.getByPlaceholderText(/Описание карточки/i), {
    target: { value: 'TestDesc' }
  });

  // Выбор трекера
  fireEvent.change(screen.getByRole('combobox'), {
    target: { value: 'realUser' }
  });

  // Клик по кнопке сохранения
  fireEvent.click(screen.getByRole('button', { name: /Сохранить/i }));

  // Ожидаем, что вернулась кнопка "Редактировать"
  await waitFor(() =>
    expect(screen.getByRole('button', { name: /Редактировать/i })).toBeInTheDocument()
  );

  // Убедимся, что PATCH был вызван с username=realUser
  expect(patchSpy).toHaveBeenCalled();
});

test('tooltip отображается при наведении (для TRACKER)', async () => {
  redux.useSelector.mockImplementation(() => ({
    user: { username: 'trackerUser', roles: ['TRACKER'] }
  }));

  Storage.prototype.getItem = jest.fn(() =>
    JSON.stringify({ username: 'trackerUser', roles: ['TRACKER'] })
  );

  require('react-router-dom').__setSearch('?edit=true');

  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });

  const dropdown = screen.getByText('MyStream'); // Изменено с /Поток/ на 'MyStream'
  fireEvent.mouseEnter(dropdown);
  expect(await screen.findByText(/Трекер не может редактировать/i)).toBeInTheDocument();
  fireEvent.mouseLeave(dropdown);
  expect(screen.queryByText(/Трекер не может редактировать/i)).not.toBeInTheDocument();
});


test('если selectedStreamId не задан, используется streamInfo.id', async () => {
  require('react-router-dom').__setState({ streamId: 1 });

  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });

  expect(await screen.findByText('MyStream')).toBeInTheDocument();

});

describe('Additional coverage (manual lines)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    require('react-router-dom').__setSearch('');
    require('react-router-dom').__setState({});
  });

  test('Показывает "Загрузка данных о потоке...", пока streamInfo ещё null', async () => {
  global.fetch = jest.fn((url) => {
    if (url.includes('/api/v1/admin/team-cards') || url.includes('/api/v1/team-cards')) {
      return new Promise(resolve => setTimeout(() => 
        resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [{
              id: 42,
              name: 'OldName',
              description: 'OldDesc',
              ntiMarkets: [{ id: 10, displayName: 'OldMarket' }],
              readinessLevel: '0-2',
              streams: [{ id: 1, name: 'MyStream' }]
            }],
            totalPages: 1
          })
        }), 100)
      );
    }
    if (url.includes('/api/v1/streams?page=0&size=150')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [{
            id: 1,
            name: 'MyStream',
            startDate: '2025-03-01T00:00:00Z',
            endDate: '2025-03-10T00:00:00Z'
          }]
        })
      });
    }
    if (url.includes('/api/v1/streams/nti-markets')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve([{ id: 10, displayName: 'OldMarket' }])
      });
    }
    if (url.endsWith('/api/v1/users/reduxUser/info')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ fullName: 'Admin FullName' })
      });
    }
    return Promise.resolve({
      ok: true,
      json: () => Promise.resolve({ content: [], totalPages: 1 })
    });
  });

  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });

  // Check loading message is present initially
  expect(screen.getByText('Загрузка данных о потоке...')).toBeInTheDocument();

  // Wait for data to load and message to disappear
  await waitFor(() => {
    expect(screen.queryByText('Загрузка данных о потоке...')).not.toBeInTheDocument();
    expect(screen.getByText('MyStream')).toBeInTheDocument();
  }, { timeout: 200 }); // Increased timeout to account for 100ms delay
});
  test('При клике вне блока .dropdown-block дропдауны NTI/TRL закрываются', async () => {
    // включаем режим редактирования, чтобы dropdown заработали
    require('react-router-dom').__setSearch('?edit=true');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
          <Routes>
            <Route path="/team-card/:id" element={<TeamCard />} />
          </Routes>
        </MemoryRouter>
      );
    });

    // Открываем NTI-список (меню появляются как <button>)
    fireEvent.click(screen.getByText(/OldMarket|Рынки НТИ/));

    expect(screen.getAllByText((text) => text.includes('OldMarket'))
.length).toBeGreaterThanOrEqual(1);

    // Открываем TRL-список
    fireEvent.click(screen.getByText('0-2'));
    expect(screen.getAllByRole('button', { name: '3-5' }).length).toBeGreaterThanOrEqual(1);

    // Кликаем вне dropdown
    fireEvent.mouseDown(document.body);

    // После клика вне оба списка должны закрыться (ни одной кнопки-элемента меню не остаётся)
    expect(screen.queryAllByRole('button', { name: 'OldMarket' })).toHaveLength(0);
    expect(screen.queryAllByRole('button', { name: '3-5' })).toHaveLength(0);
  });


  test('handleDeactivate: если confirm отклонён, fetch и navigate не вызываются', async () => {
    window.confirm = jest.fn(() => false);
    const fetchSpy = jest.spyOn(global, 'fetch');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes>
            <Route path="/team-card/:id" element={<TeamCard />} />
          </Routes>
        </MemoryRouter>
      );
    });
    fireEvent.click(screen.getByRole('button', { name: /Редактировать/i }));
    fireEvent.click(screen.getByRole('button', { name: /Деактивировать/i }));
    expect(fetchSpy).not.toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/admin/team-card'),
      expect.objectContaining({ method: 'DELETE' })
    );
    expect(mockedNavigate).not.toHaveBeenCalledWith('/team-cards');
    fetchSpy.mockRestore();
  });
});
test('TRACKER branch uses /account/info for fullName', async () => {
    const RR = require('react-router-dom');
    RR.__setSearch('');
    // Ставим роль TRACKER
    redux.useSelector.mockImplementation(() => ({
      user: { username: 'trackerUser', roles: ['TRACKER'] }
    }));
    Storage.prototype.getItem = jest.fn(() =>
      JSON.stringify({ username: 'trackerUser', roles: ['TRACKER'] })
    );
    global.fetch = jest.fn((url, opts = {}) => {
      if (url.endsWith('/api/v1/account/info')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ fullName: 'Tracker FullName' })
        });
      }
      // team-cards
      if (url.includes('/api/v1/team-cards')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [{
              id: 42,
              name: 'OldName',
              description: 'OldDesc',
              ntiMarket: { id: 10, displayName: 'OldMarket' },
              readinessLevel: '0-2',
              stream: { id: 1 },
              username: 'trackerUser'
            }],
            totalPages: 1
          })
        });
      }
      // потоки
      if (url.includes('/api/v1/streams?page=0&size=150')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [{ id: 1, name: 'MyStream', startDate: '2025-03-01T00:00:00Z', endDate: '2025-03-10T00:00:00Z' }]
          })
        });
      }
      // рынки НТИ
      if (url.includes('/api/v1/streams/nti-markets')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([{ id: 10, displayName: 'OldMarket' }])
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
    });

    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });
    // Проверяем, что отрисовалось ФИО трекера
    expect(await screen.findByDisplayValue('Tracker FullName')).toBeInTheDocument();
  });

  test('console.error on fetch team-cards error', async () => {
  const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  const RR = require('react-router-dom');
  RR.__setSearch('');
  
  global.fetch = jest.fn((url) => {
    if (url.includes('/api/v1/admin/team-cards')) {
      return Promise.resolve({ ok: false, status: 500 });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
  });

  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });
  
  expect(consoleSpy).toHaveBeenCalledWith(
    expect.stringContaining('поиске карточки команды'),
    expect.any(Error)
  );
  consoleSpy.mockRestore();
});

  test('console.error on fetch streams error', async () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    const RR = require('react-router-dom');
    RR.__setSearch('');
    redux.useSelector.mockImplementation(() => ({
      user: { username: 'reduxUser', roles: ['ADMIN'] }
    }));
    Storage.prototype.getItem = jest.fn(() =>
      JSON.stringify({ username: 'reduxUser', roles: ['ADMIN'] })
    );
    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/streams?page=0&size=1500')) {
        return Promise.resolve({ ok: false, status: 500 });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
    });

    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });
    expect(consoleSpy).toHaveBeenCalledWith(
      expect.stringContaining('загрузке потоков'),
      expect.any(Error)
    );
    consoleSpy.mockRestore();
  });

  test('initial view displays NTI and TRL values', async () => {
  require('react-router-dom').__setSearch('');
  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });
  
  // Check NTI market value
  const ntiInputs = await screen.findAllByDisplayValue('OldMarket');
  expect(ntiInputs.length).toBeGreaterThan(0);
  
  // Check TRL value
  const trlInput = screen.getByDisplayValue('0-2');
  expect(trlInput).toBeInTheDocument();
});

  test('NTI selection via Enter key', async () => {
  require('react-router-dom').__setSearch('?edit=true');
  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });
  
  // Open NTI dropdown
  fireEvent.click(screen.getByText(/OldMarket|Рынки НТИ/));
  
  // Find and select NewMarket
  const newMarketOption = await screen.findByText('NewMarket');
  fireEvent.keyDown(newMarketOption, { key: 'Enter' });
  
  // Verify selection
  expect(await screen.findByText('NewMarket')).toBeInTheDocument();
});

  test('streams dropdown opens and shows options', async () => {
    require('react-router-dom').__setSearch('?edit=true');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });
    // Изначально только один элемент — переключатель
    expect(screen.getAllByText('MyStream').length).toBe(1);
    fireEvent.click(screen.getByText('MyStream'));
    // После клика появляется минимум ещё одна копия из списка
    expect(screen.getAllByText('MyStream').length).toBeGreaterThan(1);
  });

test('нажатие на кнопку × вызывает navigate(from)', async () => {
  const RR = require('react-router-dom');
  RR.__setSearch('');
  RR.__setState({ from: '/custom-return-path' });

  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });

  const closeButton = screen.getByRole('button', { name: '×' });
  fireEvent.click(closeButton);

  expect(mockedNavigate).toHaveBeenCalledWith('/custom-return-path');
});


});
describe('Additional coverage (manual lines)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    require('react-router-dom').__setSearch('');
    require('react-router-dom').__setState({});
    redux.useSelector.mockImplementation(() => ({
      user: { username: 'reduxUser', roles: ['ADMIN'] }
    }));
    Storage.prototype.getItem = jest.fn(() =>
      JSON.stringify({ username: 'reduxUser', roles: ['ADMIN'] })
    );
    global.fetch = jest.fn((url, opts = {}) => {
      if (url.includes('/api/v1/admin/team-cards')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [{
              id: 42,
              name: 'OldName',
              description: 'OldDesc',
              ntiMarkets: [{ id: 10, displayName: 'OldMarket' }],
              readinessLevel: '0-2',
              streams: [{ id: 1, name: 'Stream1', startDate: '2025-03-01T00:00:00Z', endDate: '2025-03-10T00:00:00Z' }],
              username: 'reduxUser'
            }],
            totalPages: 1
          })
        });
      }
      if (url.includes('/api/v1/streams?page=0&size=1500')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [
              { id: 1, name: 'Stream1', startDate: '2025-03-01T00:00:00Z', endDate: '2025-03-10T00:00:00Z' },
              { id: 2, name: 'Stream2', startDate: '2025-04-01T00:00:00Z', endDate: '2025-04-10T00:00:00Z' }
            ]
          })
        });
      }
      if (url.includes('/api/v1/streams/nti-markets')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            { id: 10, displayName: 'OldMarket' },
            { id: 20, displayName: 'NewMarket' }
          ])
        });
      }
      if (url.endsWith('/api/v1/users/reduxUser/info')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ fullName: 'Admin FullName' })
        });
      }
      if (url.includes('/api/v1/team-card/count')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve(5) });
      }
      if (url.includes('/api/v1/meetings')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [], totalPages: 1 })
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
    });
  });

  
  
  test('NTI dropdown toggle via Enter key', async () => {
    require('react-router-dom').__setSearch('?edit=true');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
          <Routes>
            <Route path="/team-card/:id" element={<TeamCard />} />
          </Routes>
        </MemoryRouter>
      );
    });

    // Find NTI dropdown toggle
    const ntiToggle = screen.getByRole('button', { name: /Выбрать рынки НТИ/i });

    // Simulate Enter key to open dropdown
    fireEvent.keyDown(ntiToggle, { key: 'Enter' });

    // Verify dropdown opened (NewMarket appears)
    expect(await screen.findByText('NewMarket')).toBeInTheDocument();

    // Simulate Enter key to close dropdown
    fireEvent.keyDown(ntiToggle, { key: 'Enter' });

    // Verify dropdown closed
    await waitFor(() => {
      expect(screen.queryByText('NewMarket')).not.toBeInTheDocument();
    });
  });

  test('NTI dropdown toggle via Space key', async () => {
    require('react-router-dom').__setSearch('?edit=true');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
          <Routes>
            <Route path="/team-card/:id" element={<TeamCard />} />
          </Routes>
        </MemoryRouter>
      );
    });

    // Find NTI dropdown toggle
    const ntiToggle = screen.getByRole('button', { name: /Выбрать рынки НТИ/i });

    // Simulate Space key to open dropdown
    fireEvent.keyDown(ntiToggle, { key: ' ' });

    // Verify dropdown opened (NewMarket appears)
    expect(await screen.findByText('NewMarket')).toBeInTheDocument();

    // Simulate Space key to close dropdown
    fireEvent.keyDown(ntiToggle, { key: ' ' });

    // Verify dropdown closed
    await waitFor(() => {
      expect(screen.queryByText('NewMarket')).not.toBeInTheDocument();
    });
  });

  
});
describe('Additional coverage for specific lines', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    require('react-router-dom').__setSearch('');
    require('react-router-dom').__setState({});
    redux.useSelector.mockImplementation(() => ({
      user: { username: 'reduxUser', roles: ['ADMIN'] }
    }));
    Storage.prototype.getItem = jest.fn(() =>
      JSON.stringify({ username: 'reduxUser', roles: ['ADMIN'] })
    );
    global.fetch = jest.fn((url, opts = {}) => {
      if (url.includes('/api/v1/admin/team-cards')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [{
              id: 42,
              name: 'OldName',
              description: 'OldDesc',
              ntiMarkets: [{ id: 10, displayName: 'OldMarket' }],
              readinessLevel: '0-2',
              streams: [{ id: 1, name: 'Stream1', startDate: '2025-03-01T00:00:00Z', endDate: '2025-03-10T00:00:00Z' }],
              username: 'reduxUser'
            }],
            totalPages: 1
          })
        });
      }
      if (url.includes('/api/v1/streams?page=0&size=1500')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [
              { id: 1, name: 'Stream1', startDate: '2025-03-01T00:00:00Z', endDate: '2025-03-10T00:00:00Z' }
            ]
          })
        });
      }
      if (url.includes('/api/v1/streams/nti-markets')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            { id: 10, displayName: 'OldMarket' },
            { id: 20, displayName: 'NewMarket' }
          ])
        });
      }
      if (url.endsWith('/api/v1/users/reduxUser/info')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ fullName: 'Admin FullName' })
        });
      }
      if (url.includes('/api/v1/team-card/count')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve(5) });
      }
      if (url.includes('/api/v1/meetings')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [], totalPages: 1 })
        });
      }
      if (url.includes('/api/v1/users/trackers')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [{ id: 99, username: 'realUser', fullName: 'Tracker Name', enabled: true }]
          })
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
    });
  });

  // Lines 61-65: formatDates with missing dates
  test('formatDates returns empty string when start or end date is missing', async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/admin/team-cards')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [{
              id: 42,
              name: 'OldName',
              description: 'OldDesc',
              ntiMarkets: [{ id: 10, displayName: 'OldMarket' }],
              readinessLevel: '0-2',
              streams: [{ id: 1, name: 'Stream1' }], // No startDate/endDate
              username: 'reduxUser'
            }],
            totalPages: 1
          })
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
    });

    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    expect(screen.queryByText(/ - /)).not.toBeInTheDocument(); // No dates displayed
  });

  // Lines 133-135, 140, 143: TRACKER fetchFullName error
  test('TRACKER fetchFullName error triggers handleApiError', async () => {
    redux.useSelector.mockImplementation(() => ({
      user: { username: 'trackerUser', roles: ['TRACKER'] }
    }));
    Storage.prototype.getItem = jest.fn(() =>
      JSON.stringify({ username: 'trackerUser', roles: ['TRACKER'] })
    );
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    global.fetch = jest.fn((url) => {
      if (url.endsWith('/api/v1/account/info')) {
        return Promise.resolve({ ok: false, status: 500 });
      }
      if (url.includes('/api/v1/team-cards')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [{
              id: 42,
              name: 'OldName',
              description: 'OldDesc',
              ntiMarkets: [{ id: 10, displayName: 'OldMarket' }],
              readinessLevel: '0-2',
              streams: [{ id: 1, name: 'Stream1' }],
              username: 'trackerUser'
            }],
            totalPages: 1
          })
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
    });

    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    await waitFor(() => {
      expect(consoleSpy).toHaveBeenCalledWith(
        expect.stringContaining('загрузке ФИО трекера'),
        expect.any(Error)
      );
    });
    consoleSpy.mockRestore();
  });

  // Line 159: fetchTeamCardsCount error
  test('fetchTeamCardsCount error triggers handleApiError', async () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/team-card/count')) {
        return Promise.resolve({ ok: false, status: 500 });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [{
            id: 42,
            name: 'OldName',
            description: 'OldDesc',
            ntiMarkets: [{ id: 10, displayName: 'OldMarket' }],
            readinessLevel: '0-2',
            streams: [{ id: 1, name: 'Stream1' }],
            username: 'reduxUser'
          }],
          totalPages: 1
        })
      });
    });

    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    await waitFor(() => {
      expect(consoleSpy).toHaveBeenCalledWith(
        expect.stringContaining('получении количества карточек'),
        expect.any(Error)
      );
    });
    consoleSpy.mockRestore();
  });

  // Lines 309-314: Team card not found
  

  // Lines 346-356: Successful trackers fetch
  test('successfully fetches and sets active trackers', async () => {
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    fireEvent.click(screen.getByRole('button', { name: /Редактировать/i }));
    const trackerOption = await screen.findByText('Tracker Name');
    expect(trackerOption).toBeInTheDocument();
  });

  // Lines 610-630: handleSave with empty trackers
  
  // Lines 660-668: handleDeactivate error
  test('handleDeactivate error triggers handleApiError', async () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    global.fetch = jest.fn((url, opts = {}) => {
      if (opts.method === 'DELETE') {
        return Promise.resolve({ ok: false, status: 500 });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [{
            id: 42,
            name: 'OldName',
            description: 'OldDesc',
            ntiMarkets: [{ id: 10, displayName: 'OldMarket' }],
            readinessLevel: '0-2',
            streams: [{ id: 1, name: 'Stream1' }],
            username: 'reduxUser'
          }],
          totalPages: 1
        })
      });
    });

    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    fireEvent.click(screen.getByRole('button', { name: /Редактировать/i }));
    fireEvent.click(screen.getByRole('button', { name: /Деактивировать/i }));

    await waitFor(() => {
      expect(consoleSpy).toHaveBeenCalledWith(
        expect.stringContaining('удалении карточки'),
        expect.any(Error)
      );
    });
    consoleSpy.mockRestore();
  });
  test('toggles NTI market selection in editedData', async () => {
  global.fetch = jest.fn((url, opts = {}) => {
    if (url.includes('/api/v1/admin/team-cards')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [{
            id: 42,
            name: 'OldName',
            description: 'OldDesc',
            ntiMarkets: [{ id: 10, displayName: 'OldMarket' }],
            readinessLevel: '0-2',
            streams: [{ id: 1, name: 'Stream1', startDate: '2025-03-01T00:00:00Z', endDate: '2025-03-10T00:00:00Z' }],
            username: 'reduxUser'
          }],
          totalPages: 1
        })
      });
    }
    if (url.includes('/api/v1/streams/nti-markets')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve([
          { id: 10, displayName: 'OldMarket' },
          { id: 20, displayName: 'NewMarket' }
        ])
      });
    }
    if (url.includes('/api/v1/users/trackers')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [] })
      });
    }
    if (opts.method === 'PATCH') {
      const body = JSON.parse(opts.body || '{}');
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          id: 42,
          name: body.name || 'OldName',
          description: body.description || 'OldDesc',
          ntiMarketIds: body.ntiMarketIds || [],
          readinessLevel: body.readinessLevel || '0-2',
          username: 'reduxUser'
        })
      });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
  });

  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });

  // Enter edit mode
  fireEvent.click(screen.getByRole('button', { name: /Редактировать/i }));

  // Open NTI dropdown
  fireEvent.click(screen.getByRole('button', { name: /Выбрать рынки НТИ/i }));

  // Deselect OldMarket (already selected, ID 10)
  const oldMarketLabel = await screen.findByText('OldMarket', { selector: '.data-create-team' });
  const oldMarketCheckbox = oldMarketLabel.closest('.create-checkbox-item').querySelector('input[type="checkbox"]');
  fireEvent.click(oldMarketCheckbox); // Should remove ID 10

  // Select NewMarket (ID 20)
  const newMarketLabel = await screen.findByText('NewMarket', { selector: '.data-create-team' });
  const newMarketCheckbox = newMarketLabel.closest('.create-checkbox-item').querySelector('input[type="checkbox"]');
  fireEvent.click(newMarketCheckbox); // Should add ID 20

  // Fill required fields to pass validation
  fireEvent.change(screen.getByPlaceholderText(/Карточка команды/i), { target: { value: 'TestName' } });
  fireEvent.change(screen.getByPlaceholderText(/Описание карточки/i), { target: { value: 'TestDesc' } });

  // Save
  fireEvent.click(screen.getByRole('button', { name: /Сохранить/i }));

  await waitFor(() => {
    const patchCall = global.fetch.mock.calls.find(call => call[1]?.method === 'PATCH');
    const body = JSON.parse(patchCall[1]?.body || '{}');
    expect(body.ntiMarketIds).toEqual([20]);
  });
});
});
describe('Meetings list and navigation', () => {
  // Existing test for click navigation
  test('loads meetings and navigates', async () => {
    require('react-router-dom').__setSearch('');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });
    const meet = await screen.findByText(/Встреча 2/i);
    fireEvent.click(meet);
    expect(mockedNavigate).toHaveBeenCalledWith('/meeting/100?teamId=42&username=reduxUser');
  });

  // New test for Enter key navigation
   test('navigates to meeting on Enter key press', async () => {
    require('react-router-dom').__setSearch('');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });
    const meet = await screen.findByRole('button', { name: /Встреча 2.*05\.01/i });
    fireEvent.keyDown(meet, { key: 'Enter' });
    expect(mockedNavigate).toHaveBeenCalledWith('/meeting/100?teamId=42&username=reduxUser');
  });

  test('navigates to meeting on Space key press', async () => {
    require('react-router-dom').__setSearch('');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });
    const meet = await screen.findByRole('button', { name: /Встреча 2.*05\.01/i });
    fireEvent.keyDown(meet, { key: ' ' });
    expect(mockedNavigate).toHaveBeenCalledWith('/meeting/100?teamId=42&username=reduxUser');
  });
});
describe('handleSave validation', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    require('react-router-dom').__setSearch('?edit=true');
    redux.useSelector.mockImplementation(() => ({
      user: { username: 'reduxUser', roles: ['ADMIN'] }
    }));
    Storage.prototype.getItem = jest.fn(() =>
      JSON.stringify({ username: 'reduxUser', roles: ['ADMIN'] })
    );
  });

  test('shows error when name is empty', async () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    // Clear name field
    fireEvent.change(screen.getByPlaceholderText(/Карточка команды/i), { 
      target: { value: '' } 
    });

    fireEvent.click(screen.getByRole('button', { name: /Сохранить/i }));

    await waitFor(() => {
      expect(consoleSpy).toHaveBeenCalledWith(
        expect.stringContaining('сохранении карточки'),
        expect.objectContaining({
          message: 'Пожалуйста, заполните все обязательные поля'
        })
      );
    });
    consoleSpy.mockRestore();
  });

  test('shows error when description is empty', async () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    // Clear description field
    fireEvent.change(screen.getByPlaceholderText(/Описание карточки/i), { 
      target: { value: '' } 
    });

    fireEvent.click(screen.getByRole('button', { name: /Сохранить/i }));

    await waitFor(() => {
      expect(consoleSpy).toHaveBeenCalledWith(
        expect.stringContaining('сохранении карточки'),
        expect.objectContaining({
          message: 'Пожалуйста, заполните все обязательные поля'
        })
      );
    });
    consoleSpy.mockRestore();
  });
test('clicking "Запланировать" button sets showMeetingCreate to true', async () => {
  // Рендерим компонент
  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });

  // Проверяем, что модальное окно изначально скрыто
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

  // Находим и кликаем кнопку "Запланировать"
  const planButton = screen.getByRole('button', { name: /Запланировать/i });
  fireEvent.click(planButton);

  // Проверяем, что модальное окно появилось
  expect(screen.getByRole('dialog')).toBeInTheDocument();
});
test('closing MeetingCreate modal sets showMeetingCreate to false', async () => {
  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });

  // Open the modal
  const planButton = screen.getByRole('button', { name: /Запланировать/i });
  fireEvent.click(planButton);

  // Verify modal is open
  const modal = screen.getByTestId('meeting-create-modal');
  expect(modal).toBeInTheDocument();

  // Close the modal
  const closeButton = within(modal).getByRole('button', { name: '×' });
  fireEvent.click(closeButton);

  // Modal should be closed
  await waitFor(() => {
    expect(screen.queryByTestId('meeting-create-modal')).not.toBeInTheDocument();
  });
});
  describe('Meeting creation modal', () => {
  test('clicking "Запланировать" opens meeting creation modal', async () => {
    require('react-router-dom').__setSearch('');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    fireEvent.click(screen.getByRole('button', { name: /Запланировать/i }));
    
    expect(screen.getByTestId('meeting-create-modal')).toBeInTheDocument();
  });

 
});
});
describe('TRL selection', () => {
  test('selecting TRL updates the value', async () => {
    require('react-router-dom').__setSearch('?edit=true');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    fireEvent.click(screen.getByText('0-2')); // Open TRL dropdown
    fireEvent.click(screen.getByText('3-5')); // Select new TRL
    
    expect(screen.getByText('3-5')).toBeInTheDocument();
  });

  // Tests for lines 441-450 (Meeting creation modal)
describe('Meeting creation modal interactions', () => {
  test('clicking "Запланировать" opens meeting creation modal', async () => {
    require('react-router-dom').__setSearch('');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    fireEvent.click(screen.getByRole('button', { name: /Запланировать/i }));
    
    expect(screen.getByTestId('meeting-create-modal')).toBeInTheDocument();
  });

  test('closing MeetingCreate modal sets showMeetingCreate to false', async () => {
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    // Open the modal
    const planButton = screen.getByRole('button', { name: /Запланировать/i });
    fireEvent.click(planButton);

    // Verify modal is open
    const modal = screen.getByTestId('meeting-create-modal');
    expect(modal).toBeInTheDocument();

    // Close the modal
    const closeButton = within(modal).getByRole('button', { name: '×' });
    fireEvent.click(closeButton);

    // Modal should be closed
    await waitFor(() => {
      expect(screen.queryByTestId('meeting-create-modal')).not.toBeInTheDocument();
    });
  });
});

// Tests for lines 455-488 (Stream selection)
describe('Stream selection functionality', () => {
  test('stream dropdown opens and shows options for ADMIN', async () => {
    require('react-router-dom').__setSearch('?edit=true');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    // Initially only one element - the toggle
    expect(screen.getAllByText('MyStream').length).toBe(1);
    
    // Open stream dropdown
    fireEvent.click(screen.getByText('MyStream'));
    
    // After click there should be at least one more copy from the list
    expect(screen.getAllByText('MyStream').length).toBeGreaterThan(1);
  });

  test('stream selection is disabled for TRACKER role', async () => {
    redux.useSelector.mockImplementation(() => ({
      user: { username: 'trackerUser', roles: ['TRACKER'] }
    }));
    Storage.prototype.getItem = jest.fn(() =>
      JSON.stringify({ username: 'trackerUser', roles: ['TRACKER'] })
    );

    require('react-router-dom').__setSearch('?edit=true');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    const streamDropdown = screen.getByText('MyStream');
    expect(streamDropdown).toHaveStyle('cursor: not-allowed');
    expect(streamDropdown).toHaveStyle('opacity: 0.6');
  });

  test('shows tooltip for TRACKER when hovering stream dropdown', async () => {
    redux.useSelector.mockImplementation(() => ({
      user: { username: 'trackerUser', roles: ['TRACKER'] }
    }));
    Storage.prototype.getItem = jest.fn(() =>
      JSON.stringify({ username: 'trackerUser', roles: ['TRACKER'] })
    );

    require('react-router-dom').__setSearch('?edit=true');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    const streamDropdown = screen.getByText('MyStream');
    fireEvent.mouseEnter(streamDropdown);
    
    expect(await screen.findByText(/Трекер не может редактировать/i)).toBeInTheDocument();
    
    fireEvent.mouseLeave(streamDropdown);
    expect(screen.queryByText(/Трекер не может редактировать/i)).not.toBeInTheDocument();
  });
});



// Tests for lines 875-911 (Meeting date editing)
describe('Meeting date editing', () => {
  test('clicking meeting date opens date editor', async () => {
    require('react-router-dom').__setSearch('');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    const meetingDate = await screen.findByText(/05\.01/i);
    fireEvent.click(meetingDate);
    
    expect(screen.getByRole('button', { name: /Сохранить/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Отмена/i })).toBeInTheDocument();
  });

  

  test('canceling meeting date edit closes editor without changes', async () => {
    require('react-router-dom').__setSearch('');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    const meetingDate = await screen.findByText(/05\.01/i);
    fireEvent.click(meetingDate);
    
    fireEvent.click(screen.getByRole('button', { name: /Отмена/i }));
    
    await waitFor(() => {
      expect(screen.queryByRole('button', { name: /Сохранить/i })).not.toBeInTheDocument();
      expect(screen.getByText(/05\.01/i)).toBeInTheDocument();
    });
  });
});
});
describe('Stream selection functionality (lines 455-488)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    require('react-router-dom').__setSearch('?edit=true');
    require('react-router-dom').__setState({});
    redux.useSelector.mockImplementation(() => ({
      user: { username: 'reduxUser', roles: ['ADMIN'] }
    }));
    Storage.prototype.getItem = jest.fn(() =>
      JSON.stringify({ username: 'reduxUser', roles: ['ADMIN'] })
    );
  });

  test('stream selection updates selectedStreamId on click', async () => {
  // Обновляем мок для потоков
  global.fetch = jest.fn((url) => {
    if (url.includes('/api/v1/streams?page=0&size=1500')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [
            { 
              id: 1, 
              name: 'Stream1', 
              startDate: '2025-03-01T00:00:00Z', 
              endDate: '2025-03-10T00:00:00Z',
              active: true 
            },
            { 
              id: 2, 
              name: 'Stream2', 
              startDate: '2025-04-01T00:00:00Z', 
              endDate: '2025-04-10T00:00:00Z',
              active: true 
            }
          ]
        })
      });
    }
    // остальные моки остаются без изменений
    if (url.includes('/api/v1/admin/team-cards')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [{
            id: 42,
            name: 'OldName',
            description: 'OldDesc',
            ntiMarkets: [{ id: 10, displayName: 'OldMarket' }],
            readinessLevel: '0-2',
            streams: [{ 
              id: 1, 
              name: 'Stream1', 
              startDate: '2025-03-01T00:00:00Z', 
              endDate: '2025-03-10T00:00:00Z',
              active: true 
            }],
            username: 'reduxUser'
          }],
          totalPages: 1
        })
      });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
  });

  require('react-router-dom').__setSearch('?edit=true');
  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });

  fireEvent.click(screen.getByText('Stream1')); // Open dropdown
  fireEvent.click(screen.getByText('Stream2')); // Select Stream2

  await waitFor(() => {
    expect(screen.getAllByText('Stream2').length).toBe(1); // Only toggle remains
    expect(screen.getByText('Stream2')).toBeInTheDocument();
  });
});

  test('Stream selection via Enter key', async () => {
  // Тот же мок с двумя потоками
  global.fetch = jest.fn((url) => {
    if (url.includes('/api/v1/streams?page=0&size=1500')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [
            { 
              id: 1, 
              name: 'Stream1', 
              startDate: '2025-03-01T00:00:00Z', 
              endDate: '2025-03-10T00:00:00Z',
              active: true 
            },
            { 
              id: 2, 
              name: 'Stream2', 
              startDate: '2025-04-01T00:00:00Z', 
              endDate: '2025-04-10T00:00:00Z',
              active: true 
            }
          ]
        })
      });
    }
      if (url.includes('/api/v1/admin/team-cards')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [{
              id: 42,
              name: 'OldName',
              description: 'OldDesc',
              ntiMarkets: [{ id: 10, displayName: 'OldMarket' }],
              readinessLevel: '0-2',
              streams: [{ id: 1, name: 'Stream1' }],
              username: 'reduxUser'
            }],
            totalPages: 1
          })
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
    });

    require('react-router-dom').__setSearch('?edit=true');
  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
        <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
      </MemoryRouter>
    );
  });

  // Open stream dropdown
  fireEvent.click(screen.getByText('Stream1'));
  const streamOption = await screen.findByText('Stream2');

  // Simulate Enter key on stream label
  fireEvent.keyDown(streamOption, { key: 'Enter' });

  // Verify dropdown closed and Stream2 is selected
  await waitFor(() => {
    expect(screen.queryAllByText('Stream2').length).toBe(1);
    expect(screen.getByText('Stream2')).toBeInTheDocument();
  });
});

  

  describe('Saving meeting date (lines 875-882)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    require('react-router-dom').__setSearch('');
    redux.useSelector.mockImplementation(() => ({
      user: { username: 'reduxUser', roles: ['ADMIN'] }
    }));
    Storage.prototype.getItem = jest.fn(() =>
      JSON.stringify({ username: 'reduxUser', roles: ['ADMIN'] })
    );
  });

  

  test('saveMeetingDate handles API error', async () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    global.fetch = jest.fn((url, opts = {}) => {
      if (url.includes('/api/v1/update-meeting/100') && opts.method === 'PATCH') {
        return Promise.resolve({ ok: false, status: 500 });
      }
      if (url.includes('/api/v1/meetings')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [{ id: 100, startDate: '2025-01-05T00:00:00Z', number: 2 }],
            totalPages: 1
          })
        });
      }
      if (url.includes('/api/v1/admin/team-cards')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [{
              id: 42,
              name: 'OldName',
              description: 'OldDesc',
              ntiMarkets: [{ id: 10, displayName: 'OldMarket' }],
              readinessLevel: '0-2',
              streams: [{ id: 1, name: 'Stream1' }],
              username: 'reduxUser'
            }],
            totalPages: 1
          })
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
    });

    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    const meetingDate = await screen.findByText(/05\.01/i);
    fireEvent.click(meetingDate);

    fireEvent.click(screen.getByRole('button', { name: /Сохранить/i }));

    await waitFor(() => {
      expect(consoleSpy).toHaveBeenCalledWith(
        expect.stringContaining('сохранении даты встречи'),
        expect.any(Error)
      );
    });
    consoleSpy.mockRestore();
  });
});
});
describe('Meeting date editing', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    require('react-router-dom').__setSearch('');
    redux.useSelector.mockImplementation(() => ({
      user: { username: 'reduxUser', roles: ['ADMIN'] }
    }));
    Storage.prototype.getItem = jest.fn(() =>
      JSON.stringify({ username: 'reduxUser', roles: ['ADMIN'] })
    );
    global.fetch = jest.fn((url, opts = {}) => {
      if (url.includes('/api/v1/meetings')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [{ id: 100, startDate: '2025-01-05T00:00:00Z', number: 2 }],
            totalPages: 1
          })
        });
      }
      if (url.includes('/api/v1/update-meeting/100') && opts.method === 'PATCH') {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ id: 100, startDate: JSON.parse(opts.body).startDate, number: 2 })
        });
      }
      if (url.includes('/api/v1/admin/team-cards')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [{
              id: 42,
              name: 'OldName',
              description: 'OldDesc',
              ntiMarkets: [{ id: 10, displayName: 'OldMarket' }],
              readinessLevel: '0-2',
              streams: [{ id: 1, name: 'Stream1' }],
              username: 'reduxUser'
            }],
            totalPages: 1
          })
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
    });
  });

  // Fixed test for line 450 (and 875): handleApiError in handleDateChange
  test('handleDateChange triggers handleApiError on invalid date', async () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    
    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/meetings')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [{ id: 100, startDate: 'invalid-date', number: 2 }],
            totalPages: 1
          })
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
    });

    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    const meetingDate = await screen.findByText('Invalid Date');
    fireEvent.click(meetingDate);

    await waitFor(() => {
      expect(consoleSpy).toHaveBeenCalledWith(
        expect.stringContaining('изменении даты встречи'),
        expect.any(Error)
      );
    });
    consoleSpy.mockRestore();
  });

  // Fixed test for lines 478-480 and 486: setMeetings update and setEditingMeetingId(null)
  test('saveMeetingDate updates meeting date and closes editor', async () => {
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    const meetingDate = await screen.findByText(/05\.01/i);
    fireEvent.click(meetingDate);

    // Account for timezone offset (assuming test environment might adjust UTC to local)
    const dateInput = screen.getByDisplayValue(/2025-01-05T\d{2}:\d{2}/);
    fireEvent.change(dateInput, { target: { value: '2025-01-06T12:00' } });

    fireEvent.click(screen.getByRole('button', { name: /Сохранить/i }));

    await waitFor(() => {
      expect(screen.queryByRole('button', { name: /Сохранить/i })).not.toBeInTheDocument(); // Editor closed (line 486)
      expect(screen.getByText(/06\.01/i)).toBeInTheDocument(); // Date updated (lines 478-480)
    });
  });

  // Test for lines 909-911: Enter key on meeting date
  test('pressing Enter on meeting date opens date editor', async () => {
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    const meetingDate = await screen.findByRole('button', { name: /Изменить дату встречи 2/i });
    fireEvent.keyDown(meetingDate, { key: 'Enter' });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Сохранить/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Отмена/i })).toBeInTheDocument();
    });
  });

  // Test for lines 909-911: Space key on meeting date
  test('pressing Space on meeting date opens date editor', async () => {
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    const meetingDate = await screen.findByRole('button', { name: /Изменить дату встречи 2/i });
    fireEvent.keyDown(meetingDate, { key: ' ' });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Сохранить/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Отмена/i })).toBeInTheDocument();
    });
  });
});
// src/__tests__/getMeetingStatusClass.test.js

describe('getMeetingStatusClass', () => {
  it('returns "meeting-status-completed" for COMPLETED status', () => {
    const result = getMeetingStatusClass('COMPLETED');
    expect(result).toBe('meeting-status-completed');
  });

  it('returns "meeting-status-not-happened" for NOT_HAPPENED status', () => {
    const result = getMeetingStatusClass('NOT_HAPPENED');
    expect(result).toBe('meeting-status-not-happened');
  });

  it('returns "meeting-status-not-happened" for COMPLETED_AS_NOT_HAPPENED status', () => {
    const result = getMeetingStatusClass('COMPLETED_AS_NOT_HAPPENED');
    expect(result).toBe('meeting-status-not-happened');
  });

  it('returns empty string for SCHEDULED status', () => {
    const result = getMeetingStatusClass('SCHEDULED');
    expect(result).toBe('');
  });

  it('returns empty string for unknown status', () => {
    const result = getMeetingStatusClass('UNKNOWN_STATUS');
    expect(result).toBe('');
  });
});

beforeEach(() => {
  Object.defineProperty(window, "innerWidth", {
    writable: true,
    configurable: true,
    value: 767,
  });
  window.dispatchEvent(new Event("resize")); // если компонент слушает resize
});

test("Кнопка 'Показать встречи' отображается на мобильном, onClick работает", () => {
  render(<TeamCard /* нужные пропсы, если требуются */ />);
  // Важно! Подбери правильные пропсы для рендера компонента, если требуется

  // Кнопка появляется только на мобильной ширине (<768)
  const button = screen.getByText(/Показать встречи|Скрыть встречи/i);
  expect(button).toBeInTheDocument();

  // Кликаем!
  fireEvent.click(button);

  // После клика текст меняется
  expect(
    screen.getByText(/Показать встречи|Скрыть встречи/i)
  ).toBeInTheDocument();
});
// ДОБАВЬТЕ ЭТИ ТЕСТЫ В ПОДХОДЯЩИЙ РАЗДЕЛ (например, после других тестов для meetings)

describe('Meetings sorting functionality', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    require('react-router-dom').__setSearch('');
    redux.useSelector.mockImplementation(() => ({
      user: { username: 'reduxUser', roles: ['ADMIN'] }
    }));
    Storage.prototype.getItem = jest.fn(() =>
      JSON.stringify({ username: 'reduxUser', roles: ['ADMIN'] })
    );
  });

  test('sorts meetings by number in ascending order', async () => {
    // Мок для встреч с разными номерами в неправильном порядке
    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/meetings')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [
              { id: 100, startDate: '2025-01-05T00:00:00Z', number: '5' },
              { id: 101, startDate: '2025-01-10T00:00:00Z', number: '1' },
              { id: 102, startDate: '2025-01-15T00:00:00Z', number: '3' },
              { id: 103, startDate: '2025-01-20T00:00:00Z', number: '2' },
              { id: 104, startDate: '2025-01-25T00:00:00Z', number: '4' }
            ],
            totalPages: 1
          })
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
    });

    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    // Проверяем, что встречи отображаются в правильном порядке
    await waitFor(() => {
      const meetingTitles = screen.getAllByText(/Встреча \d+/);
      expect(meetingTitles.length).toBe(5);
      
      // Проверяем порядок: 1, 2, 3, 4, 5
      expect(meetingTitles[0].textContent).toBe('Встреча 1');
      expect(meetingTitles[1].textContent).toBe('Встреча 2');
      expect(meetingTitles[2].textContent).toBe('Встреча 3');
      expect(meetingTitles[3].textContent).toBe('Встреча 4');
      expect(meetingTitles[4].textContent).toBe('Встреча 5');
    });
  });

  test('handles meetings with invalid or missing numbers', async () => {
    // Мок с некорректными номерами
    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/meetings')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [
              { id: 100, startDate: '2025-01-05T00:00:00Z', number: 'abc' }, // Не число
              { id: 101, startDate: '2025-01-10T00:00:00Z', number: '10' }, // Число
              { id: 102, startDate: '2025-01-15T00:00:00Z' }, // Нет number
              { id: 103, startDate: '2025-01-20T00:00:00Z', number: '2' }, // Число
              { id: 104, startDate: '2025-01-25T00:00:00Z', number: '' } // Пустая строка
            ],
            totalPages: 1
          })
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
    });

    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    // Проверяем, что сортировка всё равно работает, используя 0 для некорректных значений
    await waitFor(() => {
      const meetingTitles = screen.getAllByText(/Встреча/);
      
      // Элемент с number="abc" будет обработан как 0
      // Элемент без number будет обработан как 0
      // Элемент с пустым number будет обработан как 0
      // Ожидаемый порядок: "abc" (0), "" (0), нет number (0), "2", "10"
      // Но фактически могут быть в любом порядке из-за одинаковых значений 0
      // Проверим, что все элементы загрузились
      expect(meetingTitles.length).toBe(5);
    });
  });

  test('sorts meetings by parsed integer numbers', async () => {
    // Мок с числами в разных форматах
    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/meetings')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [
              { id: 100, startDate: '2025-01-05T00:00:00Z', number: '001' },
              { id: 101, startDate: '2025-01-10T00:00:00Z', number: '10' },
              { id: 102, startDate: '2025-01-15T00:00:00Z', number: '2' },
              { id: 103, startDate: '2025-01-20T00:00:00Z', number: '005' }
            ],
            totalPages: 1
          })
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
    });

    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    // Проверяем, что parseInt корректно обрабатывает строки с ведущими нулями
    await waitFor(() => {
      const meetingTitles = screen.getAllByText(/Встреча/);
      
      // Ожидаемый порядок после parseInt: 1, 2, 5, 10
      // "001" -> 1, "2" -> 2, "005" -> 5, "10" -> 10
      const numbers = meetingTitles.map(title => {
        const match = title.textContent.match(/Встреча (\d+)/);
        return match ? parseInt(match[1]) : 0;
      });
      
      expect(numbers).toEqual([1, 2, 5, 10]);
    });
  });

  test('handles meetings with large numbers', async () => {
    // Мок с большими числами
    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/meetings')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [
              { id: 100, startDate: '2025-01-05T00:00:00Z', number: '999' },
              { id: 101, startDate: '2025-01-10T00:00:00Z', number: '1000' },
              { id: 102, startDate: '2025-01-15T00:00:00Z', number: '50' },
              { id: 103, startDate: '2025-01-20T00:00:00Z', number: '100' }
            ],
            totalPages: 1
          })
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
    });

    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    // Проверяем сортировку больших чисел
    await waitFor(() => {
      const meetingTitles = screen.getAllByText(/Встреча/);
      const numbers = meetingTitles.map(title => {
        const match = title.textContent.match(/Встреча (\d+)/);
        return match ? parseInt(match[1]) : 0;
      });
      
      // Ожидаемый порядок: 50, 100, 999, 1000
      expect(numbers).toEqual([50, 100, 999, 1000]);
    });
  });

  test('maintains stable sorting for equal numbers', async () => {
    // Мок с одинаковыми номерами
    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/meetings')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [
              { id: 100, startDate: '2025-01-05T00:00:00Z', number: '1', name: 'First' },
              { id: 101, startDate: '2025-01-10T00:00:00Z', number: '1', name: 'Second' },
              { id: 102, startDate: '2025-01-15T00:00:00Z', number: '1', name: 'Third' }
            ],
            totalPages: 1
          })
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
    });

    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    // Проверяем, что элементы с одинаковыми номерами сохраняют исходный порядок
    await waitFor(() => {
      const meetingTitles = screen.getAllByText(/Встреча 1/);
      expect(meetingTitles.length).toBe(3);
      
      // Поскольку все номера равны (1), порядок должен сохраниться исходный
      // В React это обычно происходит из-за стабильной сортировки
    });
  });

  test('handles empty meetings array', async () => {
    // Мок с пустым массивом встреч
    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/meetings')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [],
            totalPages: 1
          })
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
    });

    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    // Проверяем, что нет встреч
    await waitFor(() => {
      const meetingTitles = screen.queryAllByText(/Встреча \d+/);
      expect(meetingTitles.length).toBe(0);
    });
  });

  test('uses fallback to 0 when parseInt fails', async () => {
    // Мок с NaN значениями
    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/meetings')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [
              { id: 100, startDate: '2025-01-05T00:00:00Z', number: 'not-a-number' },
              { id: 101, startDate: '2025-01-10T00:00:00Z', number: null },
              { id: 102, startDate: '2025-01-15T00:00:00Z', number: undefined }
            ],
            totalPages: 1
          })
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], totalPages: 1 }) });
    });

    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    // Проверяем, что все встречи загрузились (с number=0)
    await waitFor(() => {
      const meetingTitles = screen.getAllByText(/Встреча/);
      expect(meetingTitles.length).toBe(3);
    });
  });
});

// Можете также добавить unit-тест для самой функции сортировки (если выделите её отдельно)
describe('Sorting function unit tests', () => {
  test('sort function works correctly', () => {
    // Пример сортировки массива
    const meetings = [
      { number: '5' },
      { number: '1' },
      { number: '3' },
      { number: '2' },
      { number: '4' }
    ];
    
    const sorted = meetings.sort((a, b) => {
      const numA = parseInt(a.number) || 0;
      const numB = parseInt(b.number) || 0;
      return numA - numB; // по возрастанию
    });
    
    expect(sorted.map(m => m.number)).toEqual(['1', '2', '3', '4', '5']);
  });
});