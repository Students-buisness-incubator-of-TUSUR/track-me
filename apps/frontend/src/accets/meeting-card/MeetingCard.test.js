import React from 'react';
import { render, screen, fireEvent, act, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import MeetingCard from './MeetingCreate.js';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

const mockedNavigate = jest.fn();

jest.mock('react-router-dom', () => {
  const original = jest.requireActual('react-router-dom');
  return {
    ...original,
    useNavigate: () => mockedNavigate,
    useParams: () => ({ meetingId: 'new' }),
    useLocation: () => ({
      search: '?teamId=42&username=testUser',
      state: {},
    }),
  };
});

beforeEach(() => {
  mockedNavigate.mockClear();
  jest.clearAllMocks();
  global.fetch = jest.fn(() =>
    Promise.resolve({
      ok: true,
      json: () => Promise.resolve({ content: [], totalPages: 1 }),
    })
  );
});

describe('MeetingCard Status Dropdown', () => {
  test('toggles dropdown with Enter key on status-selected', async () => {
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/meeting/new?teamId=42&username=testUser']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      );
    });

    const toggle = screen.getByRole('button', { name: /Выбрать статус команды/i });
    expect(screen.queryByText('Есть проблемы')).not.toBeInTheDocument();

    fireEvent.keyDown(toggle, { key: 'Enter' });
    expect(await screen.findByText('Есть проблемы')).toBeInTheDocument();

    fireEvent.keyDown(toggle, { key: 'Enter' });
    await waitFor(() => {
      expect(screen.queryByText('Есть проблемы')).not.toBeInTheDocument();
    });
  });

  test('toggles dropdown with Space key on status-selected', async () => {
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/meeting/new?teamId=42&username=testUser']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      );
    });

    const toggle = screen.getByRole('button', { name: /Выбрать статус команды/i });
    expect(screen.queryByText('Есть проблемы')).not.toBeInTheDocument();

    fireEvent.keyDown(toggle, { key: ' ' });
    expect(await screen.findByText('Есть проблемы')).toBeInTheDocument();

    fireEvent.keyDown(toggle, { key: ' ' });
    await waitFor(() => {
      expect(screen.queryByText('Есть проблемы')).not.toBeInTheDocument();
    });
  });

  test('selects OK status with Enter key', async () => {
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/meeting/new?teamId=42&username=testUser']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      );
    });

    const toggle = screen.getByRole('button', { name: /Выбрать статус команды/i });
    fireEvent.keyDown(toggle, { key: 'Enter' });

    const okOption = await screen.findByRole('button', { name: /Всё ок/i });
    fireEvent.keyDown(okOption, { key: 'Enter' });

    await waitFor(() => {
      expect(screen.getByText('Всё ок')).toBeInTheDocument();
      expect(screen.queryByText('Есть проблемы')).not.toBeInTheDocument();
    });
  });

  test('selects OK status with Space key', async () => {
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/meeting/new?teamId=42&username=testUser']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      );
    });

    const toggle = screen.getByRole('button', { name: /Выбрать статус команды/i });
    fireEvent.keyDown(toggle, { key: 'Enter' });

    const okOption = await screen.findByRole('button', { name: /Всё ок/i });
    fireEvent.keyDown(okOption, { key: ' ' });

    await waitFor(() => {
      expect(screen.getByText('Всё ок')).toBeInTheDocument();
      expect(screen.queryByText('Есть проблемы')).not.toBeInTheDocument();
    });
  });

  test('selects WITH_ISSUES status with Enter key', async () => {
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/meeting/new?teamId=42&username=testUser']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      );
    });

    const toggle = screen.getByRole('button', { name: /Выбрать статус команды/i });
    fireEvent.keyDown(toggle, { key: 'Enter' });

    const issuesOption = await screen.findByRole('button', { name: /Есть проблемы/i });
    fireEvent.keyDown(issuesOption, { key: 'Enter' });

    await waitFor(() => {
      expect(screen.getByText('Есть проблемы')).toBeInTheDocument();
      expect(screen.queryByText('Всё ок')).not.toBeInTheDocument();
    });
  });

  test('selects WITH_ISSUES status with Space key', async () => {
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/meeting/new?teamId=42&username=testUser']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      );
    });

    const toggle = screen.getByRole('button', { name: /Выбрать статус команды/i });
    fireEvent.keyDown(toggle, { key: 'Enter' });

    const issuesOption = await screen.findByRole('button', { name: /Есть проблемы/i });
    fireEvent.keyDown(issuesOption, { key: ' ' });

    await waitFor(() => {
      expect(screen.getByText('Есть проблемы')).toBeInTheDocument();
      expect(screen.queryByText('Всё ок')).not.toBeInTheDocument();
    });
  });

  test('selects MANY_ISSUES status with Enter key', async () => {
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/meeting/new?teamId=42&username=testUser']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      );
    });

    const toggle = screen.getByRole('button', { name: /Выбрать статус команды/i });
    fireEvent.keyDown(toggle, { key: 'Enter' });

    const majorIssuesOption = await screen.findByRole('button', { name: /Есть большие проблемы/i });
    fireEvent.keyDown(majorIssuesOption, { key: 'Enter' });

    await waitFor(() => {
      expect(screen.getByText('Есть большие проблемы')).toBeInTheDocument();
      expect(screen.queryByText('Всё ок')).not.toBeInTheDocument();
    });
  });

  test('selects MANY_ISSUES status with Space key', async () => {
    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/meeting/new?teamId=42&username=testUser']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      );
    });

    const toggle = screen.getByRole('button', { name: /Выбрать статус команды/i });
    fireEvent.keyDown(toggle, { key: 'Enter' });

    const majorIssuesOption = await screen.findByRole('button', { name: /Есть большие проблемы/i });
    fireEvent.keyDown(majorIssuesOption, { key: ' ' });

    await waitFor(() => {
      expect(screen.getByText('Есть большие проблемы')).toBeInTheDocument();
      expect(screen.queryByText('Всё ок')).not.toBeInTheDocument();
    });
  });
});