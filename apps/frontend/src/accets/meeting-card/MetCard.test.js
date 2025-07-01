import React from 'react';
import { render, screen, act, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import MeetingCard from './meeting-card.js';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

const mockedNavigate = jest.fn();

// Mock URL methods
global.URL.createObjectURL = jest.fn(() => 'mock-image-url');
global.URL.revokeObjectURL = jest.fn();

// Mock environment variable
process.env.REACT_APP_BACKEND_URI = 'http://localhost:8080';

jest.mock('react-router-dom', () => {
  const original = jest.requireActual('react-router-dom');
  return {
    ...original,
    useNavigate: () => mockedNavigate,
    useParams: () => ({ meetingId: '123' }),
    useLocation: () => ({
      search: '?teamId=42&username=testUser&userId=1',
      state: {},
    }),
  };
});

describe('MeetingCard Image Display', () => {
  const mockMeetingData = {
    content: [{
      id: '123',
      number: '1',
      startDate: '2023-01-01T00:00:00Z',
      link: 'https://example.com',
      tasksCurrentMeeting: 'Task 1',
      tasksNextMeeting: 'Task 2',
      status: 'OK',
    }],
    totalPages: 1,
  };

  beforeEach(() => {
    mockedNavigate.mockClear();
    jest.clearAllMocks();
    global.URL.createObjectURL.mockClear();
    global.URL.revokeObjectURL.mockClear();
  });

  

  test('does not try to load image for new meeting', async () => {
    global.fetch = jest.fn();

    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/meeting/new']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      );
    });

    expect(fetch).not.toHaveBeenCalled();
  });

  test('handles image loading error gracefully', async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/meetings?teamCardId=42')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockMeetingData),
        });
      }
      return Promise.reject(new Error('Image load failed'));
    });

    await act(async () => {
      render(
        <MemoryRouter initialEntries={['/meeting/123']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      );
    });

    const image = await screen.findByAltText('Скриншот встречи');
    expect(image).toBeInTheDocument();
    expect(image).not.toHaveAttribute('src');
  });
});