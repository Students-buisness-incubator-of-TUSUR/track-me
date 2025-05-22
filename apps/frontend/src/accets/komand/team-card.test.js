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
  return {
    ...original,
    useNavigate: () => mockedNavigate,
    useParams: () => ({ id: '42' }),
    useLocation: () => ({
      state: {},
      get search() { return searchValue; },
      set search(val) { searchValue = val; }
    }),
    __setSearch: (val) => { searchValue = val; }
  };
});

jest.mock('react-redux', () => ({
  ...jest.requireActual('react-redux'),
  useSelector: jest.fn(),
}));

beforeEach(() => {
  mockedNavigate.mockClear();
  jest.clearAllMocks();

  // redux-пользователь
  redux.useSelector.mockImplementation(() => ({
    user: { username: 'reduxUser', roles: ['ADMIN'] }
  }));
  Storage.prototype.getItem = jest.fn(() => JSON.stringify({
    username: 'reduxUser', roles: ['ADMIN']
  }));

  // универсальный stub fetch
  global.fetch = jest.fn((url, opts = {}) => {
    // 1) Обработка PATCH (handleSave)
    if (opts.method === 'PATCH') {
      const body = JSON.parse(opts.body);
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          id: 42,
          name: body.name,
          description: body.description,
          ntiMarket: { id: body.ntiMarketId, displayName: body.ntiMarketId === 20 ? 'NewMarket' : 'OldMarket' },
          readinessLevel: body.readinessLevel,
          stream: { id: 1 },
          username: 'reduxUser'
        })
      });
    }

    // 2) Загрузка списка карточек (ADMIN)
    if (url.includes('/api/v1/admin/team-cards?page')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [
            {
              id: 42,
              name: 'OldName',
              description: 'OldDesc',
              ntiMarket: { id: 10, displayName: 'OldMarket' },
              readinessLevel: '0-2',
              stream: { id: 1 },
              username: 'reduxUser'
            }
          ],
          totalPages: 1
        })
      });
    }

    // 3) NTI-рынки
    if (url.includes('/api/v1/streams/nti-markets')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve([
          { id: 10, displayName: 'OldMarket' },
          { id: 20, displayName: 'NewMarket' },
        ])
      });
    }

    // 4) Потоки
    if (url.match(/\/api\/v1\/streams\?page=0&size=\d+/)) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [{ id: 1, name: 'TestStream' }] })
      });
    }

    // 5) Трекеры
    if (url.includes('/api/v1/users/trackers')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [] })
      });
    }

    // 6) Количество карточек
    if (url.includes('/api/v1/team-card/count')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve(7)
      });
    }

    // 7) Встречи
    if (url.includes('/api/v1/meetings')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [
            { id: 100, startDate: '2025-01-05T00:00:00Z', number: 2 }
          ],
          totalPages: 1
        })
      });
    }

    // 8) По умолчанию
    return Promise.resolve({
      ok: true,
      json: () => Promise.resolve({ content: [], totalPages: 1 })
    });
  });
});

describe('TeamCard basic interactions', () => {
  test('renders main buttons and toggles Edit/Save button', async () => {
    require('react-router-dom').__setSearch('');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });
    const editBtn = screen.getByRole('button', { name: /Редактировать/i });
    expect(editBtn).toBeInTheDocument();
    fireEvent.click(editBtn);
    expect(screen.getByRole('button', { name: /Сохранить/i })).toBeInTheDocument();
  });

  test('clicking "Запланировать" navigates with username', async () => {
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

  test('tracker name input is read-only', async () => {
    require('react-router-dom').__setSearch('');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });
    const trackerInput = screen.getByPlaceholderText(/ФИО трекера/i);
    expect(trackerInput).toHaveAttribute('readOnly');
  });
});

describe('Meetings list and navigation', () => {
  test('loads meetings, shows date and title, navigates on click', async () => {
    require('react-router-dom').__setSearch('');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });
    const meeting = await screen.findByText(/Встреча 2/i);
    expect(meeting).toBeInTheDocument();
    fireEvent.click(meeting);
    expect(mockedNavigate).toHaveBeenCalledWith('/meeting/100?teamId=42&username=reduxUser');
  });
});

describe('Save and Deactivate flows', () => {
  test('handleSave calls PATCH и обновляет UI', async () => {
    require('react-router-dom').__setSearch('?edit=true');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42?edit=true']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });

    // вводим новые значения
    fireEvent.change(screen.getByPlaceholderText(/Карточка команды/i), { target: { value: 'NewName' } });
    fireEvent.click(screen.getByText('OldMarket'));          // открыть NTI
    await screen.findByText('NewMarket');                    // ждём появления
    fireEvent.click(screen.getByText('NewMarket'));
    fireEvent.click(screen.getByText('0-2'));                // открыть TRL
    await screen.findByText('3-5');
    fireEvent.click(screen.getByText('3-5'));
    fireEvent.change(screen.getByPlaceholderText(/Описание карточки/i), { target: { value: 'NewDesc' } });

    // сохраняем
    fireEvent.click(screen.getByRole('button', { name: /Сохранить/i }));
    // дожидаемся выхода из режима редактирования
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Редактировать/i })).toBeInTheDocument();
    });
    // теперь инпуты readonly должны содержать новые значения
    expect(screen.getByPlaceholderText(/Карточка команды/i)).toHaveValue('NewName');
    expect(screen.getByPlaceholderText(/Описание карточки/i)).toHaveValue('NewDesc');
  });

  test('handleDeactivate вызывает confirm, DELETE и navigate', async () => {
    window.confirm = jest.fn(() => true);
    require('react-router-dom').__setSearch('');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes><Route path="/team-card/:id" element={<TeamCard />} /></Routes>
        </MemoryRouter>
      );
    });
    // переключить в edit, чтобы появилась кнопка деактивации
    fireEvent.click(screen.getByRole('button', { name: /Редактировать/i }));
    fireEvent.click(screen.getByText(/Деактивировать/i));
    await waitFor(() => {
      expect(mockedNavigate).toHaveBeenCalledWith('/team-cards');
    });
  });
});
