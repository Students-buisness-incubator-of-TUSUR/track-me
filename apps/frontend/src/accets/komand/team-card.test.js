// src/accets/komand/team-card.test.js
import React from 'react';
import { render, screen, fireEvent, act } from '@testing-library/react';
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
  redux.useSelector.mockImplementation(() => ({
    user: { username: 'reduxUser', roles: ['ADMIN'] }
  }));
  Storage.prototype.getItem = jest.fn(() => JSON.stringify({
    username: 'reduxUser', roles: ['ADMIN']
  }));

  // stub fetch для всех useEffect
  global.fetch = jest.fn((url) => {
    // 1) Загрузка списка карточек (ADMIN)
    if (url.includes('/api/v1/admin/team-cards?page')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [
            {
              id: 42,
              ntiMarket: { id: 10, displayName: 'Market10' },
              readinessLevel: '3-5',
              description: 'desc',
              stream: { id: 1 },
              username: 'reduxUser'
            }
          ],
          totalPages: 1
        })
      });
    }
    // 2) Загрузка NTI-рынков
    if (url.includes('/api/v1/streams/nti-markets')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve([
          { id: 10, displayName: 'Market10' },
          { id: 20, displayName: 'Market20' },
        ])
      });
    }
    // 3) Загрузка потоков для dropdown
    if (url.match(/\/api\/v1\/streams\?page=0&size=\d+/)) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [{ id: 1, name: 'TestStream' }] })
      });
    }
    // 4) Загрузка ФИО трекера
    if (url.includes('/api/v1/users/') && url.endsWith('/info')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ fullName: 'Test User' })
      });
    }
    // 5) Загрузка трекеров
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
        json: () => Promise.resolve(0)
      });
    }
    // 7) Встречи
    if (url.includes('/api/v1/meetings')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [], totalPages: 1 })
      });
    }
    // default
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
          <Routes>
            <Route path="/team-card/:id" element={<TeamCard />} />
          </Routes>
        </MemoryRouter>
      );
    });

    const editBtn = screen.getByRole('button', { name: /Редактировать/i });
    expect(editBtn).toBeInTheDocument();
    expect(editBtn).toBeEnabled();

    fireEvent.click(editBtn);

    expect(screen.getByRole('button', { name: /Сохранить/i })).toBeInTheDocument();
  });

  test('clicking "Запланировать" navigates with username from redux/localStorage', async () => {
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

    const scheduleBtn = screen.getByRole('button', { name: /Запланировать/i });
    fireEvent.click(scheduleBtn);
    expect(mockedNavigate).toHaveBeenCalledWith('/meeting-create/42?username=reduxUser');
  });

  test('tracker name input is rendered and readonly', async () => {
    require('react-router-dom').__setSearch('');
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/team-card/42']}>
          <Routes>
            <Route path="/team-card/:id" element={<TeamCard />} />
          </Routes>
        </MemoryRouter>
      );
    });

    const trackerInput = screen.getByPlaceholderText(/ФИО трекера/i);
    expect(trackerInput).toHaveAttribute('readOnly');
  });
});

describe('NTI and TRL dropdown interactions', () => {
  test('NTI dropdown selection updates displayed value', async () => {
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

    // initial из teamData
    const ntToggle = await screen.findByText('Market10');
    expect(ntToggle).toBeInTheDocument();

    fireEvent.click(ntToggle);
    const opt20 = await screen.findByText('Market20');
    fireEvent.click(opt20);
    expect(screen.getByText('Market20')).toBeInTheDocument();
  });

  test('TRL dropdown selection updates displayed value', async () => {
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

    const trlToggle = await screen.findByText('3-5');
    expect(trlToggle).toBeInTheDocument();

    fireEvent.click(trlToggle);
    const opt68 = await screen.findByText('6-8');
    fireEvent.click(opt68);
    expect(screen.getByText('6-8')).toBeInTheDocument();
  });
});
