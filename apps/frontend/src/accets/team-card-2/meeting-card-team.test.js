import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import MeetingCard2 from './meeting-card-team';

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useParams: () => ({ teamId: 'test-team-123' }),
}));

describe('MeetingCard2 - Компонент карточки команды', () => {
  const mockTeamData = {
    id: 'test-team-123',
    name: 'Тестовая команда',
    username: 'tracker_user',
    trackerFullName: 'Иван Трекеров',
    teamCardName: 'Тестовая команда',
    streamName: 'Поток 2024',
  };

  const mockMeetings = {
    content: [
      { id: 'm1', startDate: '2024-03-01T10:00:00Z', status: 'COMPLETED' },
      { id: 'm2', startDate: '2024-03-08T10:00:00Z', status: 'SCHEDULED' },
      { id: 'm3', startDate: '2024-03-15T10:00:00Z', status: 'COMPLETED' },
    ],
  };

  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch = jest.fn();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  const renderComponent = () => {
    return render(
      <MemoryRouter initialEntries={['/team-cards-PR/test-team-123']}>
        <Routes>
          <Route path="/team-cards-PR/:teamId" element={<MeetingCard2 />} />
        </Routes>
      </MemoryRouter>
    );
  };

  test('отображает состояние загрузки', () => {
    global.fetch.mockImplementation(() => new Promise(() => {}));

    renderComponent();

    expect(screen.getByText('Загрузка...')).toBeInTheDocument();
  });

  test('отображает данные команды после загрузки', async () => {
    global.fetch
      .mockResolvedValueOnce({
        ok: true,
        json: async () => mockTeamData,
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => mockMeetings,
      });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByDisplayValue('Иван Трекеров')).toBeInTheDocument();
      expect(screen.getByDisplayValue('Тестовая команда')).toBeInTheDocument();
    });
  });

  test('отображает сообщение об ошибке если команда не найдена', async () => {
    global.fetch
      .mockResolvedValueOnce({
        ok: true,
        json: async () => null,
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ content: [] }),
      });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/Команда не найдена/)).toBeInTheDocument();
    });
  });

  test('отображает список встреч', async () => {
    global.fetch
      .mockResolvedValueOnce({
        ok: true,
        json: async () => mockTeamData,
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => mockMeetings,
      });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('1')).toBeInTheDocument();
      expect(screen.getByText('2')).toBeInTheDocument();
      expect(screen.getByText('3')).toBeInTheDocument();
    });
  });

  test('отмечает завершенные встречи', async () => {
    global.fetch
      .mockResolvedValueOnce({
        ok: true,
        json: async () => mockTeamData,
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => mockMeetings,
      });

    renderComponent();

    await waitFor(() => {
      const completedCircles = document.querySelectorAll('.teamcard2-meeting-circle.completed');
      expect(completedCircles).toHaveLength(2);
    });
  });

  test('обрабатывает отсутствие teamId в URL', async () => {
    jest.spyOn(require('react-router-dom'), 'useParams').mockReturnValue({ teamId: undefined });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/Команда не найдена/)).toBeInTheDocument();
    });
  });

  test('обрабатывает teamId = "undefined"', async () => {
    jest.spyOn(require('react-router-dom'), 'useParams').mockReturnValue({ teamId: 'undefined' });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/Команда не найдена/)).toBeInTheDocument();
    });
  });

  test('форматирует даты встреч корректно', async () => {
    global.fetch
      .mockResolvedValueOnce({
        ok: true,
        json: async () => mockTeamData,
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => mockMeetings,
      });

    renderComponent();

    await waitFor(() => {
      const dates = screen.getAllByText(/\d{2}\.\d{2}/);
      expect(dates.length).toBeGreaterThan(0);
    });
  });
});