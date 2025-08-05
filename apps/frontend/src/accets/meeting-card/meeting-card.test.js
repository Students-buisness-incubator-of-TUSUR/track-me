import React from 'react';
import '@testing-library/jest-dom';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
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

// Mock CSRF utils
jest.mock('../../utils/csrf-utils', () => ({
  getCsrfConfigForFetch: jest.fn().mockReturnValue({
    'X-CSRF-TOKEN': 'mock-token',
    'X-CSRF-HEADER': 'X-CSRF-TOKEN'
  })
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
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    console.warn.mockRestore();
    console.error.mockRestore();
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

    await act(async () => {
      // Upload image first
      const fileInput = container.querySelector('input[type="file"]');
      Object.defineProperty(fileInput, 'files', {
        value: [file]
      });
      fireEvent.change(fileInput);

      // Click save
      fireEvent.click(screen.getByText('Сохранить'));
    });

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledTimes(2);
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

    await act(async () => {
      // Upload image
      const fileInput = container.querySelector('input[type="file"]');
      Object.defineProperty(fileInput, 'files', {
        value: [file]
      });
      fireEvent.change(fileInput);

      // Click save
      fireEvent.click(screen.getByText('Сохранить'));
    });

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledTimes(2);
    });
  });

  test('changes text fields and updates meeting data', () => {
    render(
      <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    const textarea = screen.getAllByRole('textbox')[0];
    fireEvent.change(textarea, { target: { value: 'Updated Task' } });
    expect(textarea.value).toBe('Updated Task');
  });

  test('handles image upload', () => {
    const file = new File(['test'], 'test.png', { type: 'image/png' });
    const { container } = render(
      <MemoryRouter initialEntries={['/meeting/new']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    const fileInput = container.querySelector('input[type="file"]');
    Object.defineProperty(fileInput, 'files', { value: [file] });
    fireEvent.change(fileInput);
  });

  test('navigates back when close button is clicked', () => {
    const mockNavigate = jest.fn();
    jest.spyOn(require('react-router-dom'), 'useNavigate').mockImplementation(() => mockNavigate);

    render(
      <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    fireEvent.click(screen.getByRole('button', { name: /закрыть/i }));
    expect(mockNavigate).toHaveBeenCalled();
  });

  test('toggles status dropdown and selects status', async () => {
    render(
      <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    await act(async () => {
      fireEvent.click(screen.getByTestId('edit-button'));
    });

    const statusSelected = screen.getByTestId('status-selected');
    await act(async () => {
      fireEvent.click(statusSelected);
    });

    expect(screen.getByText('Всё ок')).toBeInTheDocument();

    await act(async () => {
      fireEvent.click(screen.getByText('Всё ок'));
    });

    expect(statusSelected).toHaveTextContent('Всё ок');
  });

  test('shows error message on save failure', async () => {
    fetch.mockImplementationOnce(() =>
      Promise.resolve({ 
        ok: false, 
        text: () => Promise.resolve('Ошибка при сохранении') 
      })
    );

    render(
      <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    await act(async () => {
      fireEvent.click(screen.getByText('Сохранить'));
    });

    await waitFor(() => {
      expect(screen.getByText(/Ошибка при сохранении/i)).toBeInTheDocument();
    });
  });

  
  test('renders link field in editing mode', async () => {
    render(
      <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    await act(async () => {
      fireEvent.click(screen.getByTestId('edit-button'));
    });

    const linkInputs = screen.getAllByRole('textbox');
    const linkInput = linkInputs.find(input => input.name === 'link');
    
    await act(async () => {
      fireEvent.change(linkInput, { target: { value: 'http://example.com' } });
    });

    expect(linkInput.value).toBe('http://example.com');
  });
});