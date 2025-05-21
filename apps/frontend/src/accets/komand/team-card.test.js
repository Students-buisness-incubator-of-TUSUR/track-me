import React from 'react';
import { render, screen, fireEvent, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import TeamCard from './team-card.js';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import * as redux from 'react-redux';

const mockedNavigate = jest.fn();

jest.mock('react-router-dom', () => {
  const originalModule = jest.requireActual('react-router-dom');
  let searchValue = '';
  return {
    ...originalModule,
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
  redux.useSelector.mockImplementation(() => ({ user: { username: 'reduxUser', roles: ['ADMIN'] } }));
  Storage.prototype.getItem = jest.fn(() => JSON.stringify({ username: 'reduxUser', roles: ['ADMIN'] }));
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
    expect(scheduleBtn).toBeInTheDocument();

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

    expect(trackerInput).toBeInTheDocument();
    expect(trackerInput).toHaveAttribute('readonly');
  });
});
