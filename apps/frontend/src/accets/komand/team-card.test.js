// src/accets/komand/team-card.test.js
import React from 'react';
import { render, screen, fireEvent, act, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import TeamCard from './team-card.js';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import * as redux from 'react-redux';

const mockedNavigate = jest.fn();

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

  // redux-пользователь по умолчанию — ADMIN
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
        streams: [{ // Изменено с stream на streams
          id: 1,
          name: 'MyStream',
          startDate: '2025-03-01T00:00:00Z',
          endDate: '2025-03-10T00:00:00Z'
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
        streams: [{ // Изменено с stream на streams
          id: 1,
          name: 'MyStream',
          startDate: '2025-03-01T00:00:00Z',
          endDate: '2025-03-10T00:00:00Z'
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
            endDate: '2025-03-10T00:00:00Z'
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

  test('Запланировать → navigate', async () => {
    require('react-router-dom').__setSearch('?edit=true');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });
    fireEvent.click(screen.getByRole('button', { name: /Запланировать/i }));
    expect(mockedNavigate).toHaveBeenCalledWith('/meeting-create/42?username=reduxUser');
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

  expect(screen.getByText('5 команд')).toBeInTheDocument();
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

  test('Если в localStorage нет user — берёт из reduxUser и сохраняет его туда', async () => {
    jest.spyOn(Storage.prototype, 'getItem').mockReturnValueOnce(null);
    const spySet = jest.spyOn(Storage.prototype, 'setItem');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes>
            <Route path="/team-card/:id" element={<TeamCard />} />
          </Routes>
        </MemoryRouter>
      );
    });
    expect(spySet).toHaveBeenCalledWith(
      'user',
      JSON.stringify({ user: { username: 'reduxUser', roles: ['ADMIN'] } })
    );
    spySet.mockRestore();
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

  test('Stream selection via Enter key', async () => {
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

    // Open stream dropdown
    fireEvent.click(screen.getByText('Stream1'));
    const streamOption = await screen.findByText('Stream2');

    // Simulate Enter key on stream label
    fireEvent.keyDown(streamOption, { key: 'Enter' });

    // Verify dropdown closed and Stream2 is selected
    await waitFor(() => {
      expect(screen.queryAllByText('Stream2').length).toBe(1); // Only toggle remains
      expect(screen.getByText('Stream2')).toBeInTheDocument();
    });
  });

  test('Stream selection via Space key', async () => {
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

    // Open stream dropdown
    fireEvent.click(screen.getByText('Stream1'));
    const streamOption = await screen.findByText('Stream2');

    // Simulate Space key on stream label
    fireEvent.keyDown(streamOption, { key: ' ' });

    // Verify dropdown closed and Stream2 is selected
    await waitFor(() => {
      expect(screen.queryAllByText('Stream2').length).toBe(1); // Only toggle remains
      expect(screen.getByText('Stream2')).toBeInTheDocument();
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