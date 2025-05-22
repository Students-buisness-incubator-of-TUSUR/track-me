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
          ntiMarket: {
            id: body.ntiMarketId,
            displayName: body.ntiMarketId === 20 ? 'NewMarket' : 'OldMarket'
          },
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
            ntiMarket: { id: 10, displayName: 'OldMarket' },
            readinessLevel: '0-2',
            stream: { id: 1 },
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
            ntiMarket: { id: 10, displayName: 'OldMarket' },
            readinessLevel: '0-2',
            stream: { id: 1 },
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
    fireEvent.click(screen.getByText('OldMarket'));
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
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });
    expect(await screen.findByText('MyStream')).toBeInTheDocument();
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

   



});
