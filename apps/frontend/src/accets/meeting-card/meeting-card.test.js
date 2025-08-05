import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import MeetingCard from './meeting-card';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

// Mock fetch globally
global.fetch = jest.fn();

// Mock react-router-dom hooks
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => jest.fn(),
  useLocation: () => ({
    search: '?teamId=1&username=test&userId=1',
  }),
}));

describe('MeetingCard Component', () => {
  const mockMeetingData = {
    id: "123",
    number: "10",
    startDate: "2023-01-01T00:00:00.000Z",
    link: "http://example.com",
    tasksCurrentMeeting: "Task 1",
    tasksNextMeeting: "Task 2",
    teamStatus: "OK"
  };

  beforeEach(() => {
    fetch.mockClear();
  });

  

  

  

  test('handles save with image upload', async () => {
    const file = new File(['test'], 'test.png', { type: 'image/png' });
    
    fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ id: "123" }),
      })
    ).mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
      })
    );

    const { container } = render(
      <MemoryRouter initialEntries={['/meeting/new']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    // Upload image first
    const fileInput = container.querySelector('input[type="file"]');
    Object.defineProperty(fileInput, 'files', {
      value: [file]
    });
    fireEvent.change(fileInput);

    // Click save
    fireEvent.click(screen.getByText('Сохранить'));

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledTimes(2); // One for meeting save, one for image upload
    });
  });

  test('handles error during image upload', async () => {
    const file = new File(['test'], 'test.png', { type: 'image/png' });
    
    fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ id: "123" }),
      })
    ).mockImplementationOnce(() =>
      Promise.reject(new Error('Image upload failed'))
    );

    const { container } = render(
      <MemoryRouter initialEntries={['/meeting/new']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    // Upload image
    const fileInput = container.querySelector('input[type="file"]');
    Object.defineProperty(fileInput, 'files', {
      value: [file]
    });
    fireEvent.change(fileInput);

    // Click save
    fireEvent.click(screen.getByText('Сохранить'));

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledTimes(2);
    });
  });
});