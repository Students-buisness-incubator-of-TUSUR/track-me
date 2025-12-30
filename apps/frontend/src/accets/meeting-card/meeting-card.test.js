import React from 'react';
import '@testing-library/jest-dom';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { Provider } from 'react-redux';
import { createStore } from 'redux';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import MeetingCard from './meeting-card';

// Utility to fill all required fields for save
async function fillAllRequiredFields(container) {
  // Number
  const numberInput = container.querySelector('input[name="number"]');
  if (numberInput) fireEvent.change(numberInput, { target: { value: '1', name: 'number' } });
  
  // Date
  const dateInput = container.querySelector('input[type="date"]');
  if (dateInput) fireEvent.change(dateInput, { target: { value: '2025-12-13', name: 'startDate' } });
  
  // Textareas
  const textareas = Array.from(container.querySelectorAll('textarea'));
  if (textareas[0]) fireEvent.change(textareas[0], { target: { value: 'a', name: 'tasksCurrentMeeting' } });
  if (textareas[1]) fireEvent.change(textareas[1], { target: { value: 'b', name: 'tasksNextMeeting' } });
  
  // Status dropdown
  const dropdown = container.querySelector('.status-selected');
  if (dropdown) {
    fireEvent.click(dropdown);
    await act(async () => {
      fireEvent.click(screen.getByText('Всё ок'));
    });
  }
  
  // Image
  const fileInput = container.querySelector('input[type="file"]');
  if (fileInput) {
    const file = new File(['test'], 'test.png', { type: 'image/png' });
    Object.defineProperty(fileInput, 'files', { value: [file] });
    fireEvent.change(fileInput);
  }
}

function getTestStore() {
  return createStore(() => ({ user: { user: { roles: ['ADMIN'] } } }));
}

// Mock fetch globally
global.fetch = jest.fn();
jest.setTimeout(10000);

// Mock CSRF utils
jest.mock('../../utils/csrf-utils', () => ({
  getCsrfConfigForFetch: jest.fn().mockReturnValue({
    'X-CSRF-TOKEN': 'mock-token',
    'X-CSRF-HEADER': 'X-CSRF-TOKEN'
  })
}));

// Mock react-router-dom hooks
const mockNavigate = jest.fn();
const mockUseLocation = jest.fn(() => ({
  search: '?teamId=1&username=test&userId=1',
}));

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useLocation: mockUseLocation,
  useParams: jest.fn(),
}));

describe('MeetingCard Component', () => {
  const mockMeetingData = {
    id: "123",
    number: "10",
    startDate: "2023-01-01T00:00:00.000Z",
    link: "http://example.com",
    tasksCurrentMeeting: "Task 1",
    tasksNextMeeting: "Task 2",
    teamStatus: "OK",
    status: "SCHEDULED"
  };

  beforeEach(() => {
    fetch.mockClear();
    fetch.mockReset();
    mockNavigate.mockClear();
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    jest.spyOn(console, 'error').mockImplementation(() => {});
    
    global.URL.createObjectURL = jest.fn(() => 'mock-image-url');
    
    Object.defineProperty(window, 'location', {
      value: { origin: 'http://localhost' },
      writable: true
    });
    
    process.env.REACT_APP_BACKEND_URI = '';
    
    // Мокаем useParams для новых встреч по умолчанию
    const { useParams } = require('react-router-dom');
    useParams.mockReturnValue({ meetingId: 'new' });
  });

  afterEach(() => {
    console.warn.mockRestore();
    console.error.mockRestore();
    jest.restoreAllMocks();
  });

  test('handles save with image upload', async () => {
    fetch.mockImplementation((url) => {
      if (url.includes('/api/v1/meetings?teamCardId=')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ id: "123" }),
        });
      }
      if (url.includes('/api/v1/image/')) {
        return Promise.resolve({ ok: true });
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    const { container } = render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    await fillAllRequiredFields(container);
    await act(async () => {
      fireEvent.click(screen.getByText('Сохранить'));
    });

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledTimes(2);
    });
  });

  test('handles error during image upload', async () => {
    fetch.mockImplementation((url) => {
      if (url.includes('/api/v1/meetings?teamCardId=')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ id: "123" }),
        });
      }
      if (url.includes('/api/v1/image/')) {
        return Promise.reject(new Error('Image upload failed'));
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    const { container } = render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    await act(async () => {
      await fillAllRequiredFields(container);
      fireEvent.click(screen.getByText('Сохранить'));
    });

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledTimes(2);
    });
  });

  test('changes text fields and updates meeting data', () => {
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    const textareas = screen.getAllByRole('textbox').filter(el => el.tagName === 'TEXTAREA');
    if (textareas[0]) {
      fireEvent.change(textareas[0], { target: { value: 'Updated Task' } });
      expect(textareas[0].value).toBe('Updated Task');
    }
  });

  test('handles image upload', async () => {
    const file = new File(['test'], 'test.png', { type: 'image/png' });
    const { container } = render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    const fileInput = container.querySelector('input[type="file"]');
    Object.defineProperty(fileInput, 'files', { value: [file] });
    fireEvent.change(fileInput);
    
    await waitFor(() => {
      expect(global.URL.createObjectURL).toHaveBeenCalled();
    });
  });

  test('navigates back when close button is clicked', () => {
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    fireEvent.click(screen.getByRole('button', { name: /закрыть/i }));
    expect(mockNavigate).toHaveBeenCalled();
  });

  test('shows error message on save failure', async () => {
    fetch.mockImplementation(() =>
      Promise.resolve({ 
        ok: false, 
        text: () => Promise.resolve('Ошибка при сохранении') 
      })
    );

    const { container } = render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    await fillAllRequiredFields(container);
    await act(async () => {
      fireEvent.click(screen.getByText('Сохранить'));
    });

    await waitFor(() => {
      expect(screen.getByText(/Ошибка при сохранении/i)).toBeInTheDocument();
    });
  });
});

describe('MeetingCard Delete Functionality', () => {
  beforeEach(() => {
    fetch.mockClear();
    fetch.mockReset();
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    jest.spyOn(console, 'error').mockImplementation(() => {});
    global.URL.createObjectURL = jest.fn(() => 'mock-image-url');
    
    // Мокаем useParams для существующей встречи
    const { useParams } = require('react-router-dom');
    useParams.mockReturnValue({ meetingId: '123' });
  });

  afterEach(() => {
    console.warn.mockRestore();
    console.error.mockRestore();
  });

  test('deletes meeting card successfully', async () => {
    fetch.mockImplementation((url) => {
      if (url.includes('/api/v1/meetings') && !url.includes('delete-meeting')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ 
            content: [{ 
              id: '123', 
              number: '10', 
              startDate: '2023-01-01T00:00:00.000Z', 
              link: '', 
              tasksCurrentMeeting: '', 
              tasksNextMeeting: '', 
              teamStatus: '', 
              status: 'SCHEDULED' 
            }] 
          })
        });
      }
      if (url.includes('/api/v1/image/')) {
        return Promise.resolve({ 
          ok: true, 
          blob: () => Promise.resolve(new Blob()) 
        });
      }
      if (url.includes('/api/v1/delete-meeting/')) {
        return Promise.resolve({ ok: true });
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/123?teamId=1&userId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    await waitFor(() => screen.getByText('Редактировать'), { timeout: 3000 });
    
    fireEvent.click(screen.getByText('Редактировать'));
    
    await waitFor(() => screen.getByText('Удалить'));
    fireEvent.click(screen.getByText('Удалить'));
    
    await waitFor(() => screen.getByTestId('delete-confirm-button'));
    fireEvent.click(screen.getByTestId('delete-confirm-button'));

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/delete-meeting/123'),
        expect.objectContaining({ method: 'DELETE' })
      );
    });
  });

  test('shows error on delete failure', async () => {
    fetch.mockImplementation((url) => {
      if (url.includes('/api/v1/meetings') && !url.includes('delete-meeting')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ 
            content: [{ 
              id: '123', 
              number: '10', 
              startDate: '2023-01-01T00:00:00.000Z', 
              link: '', 
              tasksCurrentMeeting: '', 
              tasksNextMeeting: '', 
              teamStatus: '', 
              status: 'SCHEDULED' 
            }] 
          })
        });
      }
      if (url.includes('/api/v1/image/')) {
        return Promise.resolve({ 
          ok: true, 
          blob: () => Promise.resolve(new Blob()) 
        });
      }
      if (url.includes('/api/v1/delete-meeting/')) {
        return Promise.resolve({ 
          ok: false, 
          text: () => Promise.resolve('Ошибка удаления') 
        });
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/123?teamId=1&userId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    await waitFor(() => screen.getByText('Редактировать'), { timeout: 3000 });
    
    fireEvent.click(screen.getByText('Редактировать'));
    
    await waitFor(() => screen.getByText('Удалить'));
    fireEvent.click(screen.getByText('Удалить'));
    
    await waitFor(() => screen.getByTestId('delete-confirm-button'));
    fireEvent.click(screen.getByTestId('delete-confirm-button'));

    await waitFor(() => {
      expect(screen.getByText(/Ошибка удаления/i)).toBeInTheDocument();
    }, { timeout: 5000 });
  });

  test('can cancel delete modal', async () => {
    fetch.mockImplementation((url) => {
      if (url.includes('/api/v1/meetings') && !url.includes('delete-meeting')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ 
            content: [{ 
              id: '123', 
              number: '10', 
              startDate: '2023-01-01T00:00:00.000Z', 
              link: '', 
              tasksCurrentMeeting: '', 
              tasksNextMeeting: '', 
              teamStatus: '', 
              status: 'SCHEDULED' 
            }] 
          })
        });
      }
      if (url.includes('/api/v1/image/')) {
        return Promise.resolve({ 
          ok: true, 
          blob: () => Promise.resolve(new Blob()) 
        });
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/123?teamId=1&userId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    await waitFor(() => screen.getByText('Редактировать'), { timeout: 3000 });
    
    fireEvent.click(screen.getByText('Редактировать'));
    
    await waitFor(() => screen.getByText('Удалить'));
    fireEvent.click(screen.getByText('Удалить'));
    
    await waitFor(() => screen.getByText('Отмена'));
    fireEvent.click(screen.getByText('Отмена'));

    expect(screen.queryByTestId('delete-modal-title')).not.toBeInTheDocument();
  });
});

describe('MeetingCard Additional Tests', () => {
  beforeEach(() => {
    fetch.mockClear();
    const { useParams } = require('react-router-dom');
    useParams.mockReturnValue({ meetingId: 'new' });
  });

  test('should handle image upload when clicking the upload area (lines 271-307)', () => {
    const file = new File(['test'], 'test.png', { type: 'image/png' });
    const { container } = render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    const uploadArea = container.querySelector('.unique-image-upload');
    const fileInput = container.querySelector('input[type="file"]');
    const clickSpy = jest.spyOn(fileInput, 'click');
    
    fireEvent.click(uploadArea);
    expect(clickSpy).toHaveBeenCalled();
    
    clickSpy.mockRestore();
  });

  test('should handle keyboard events for image upload (lines 271-307)', () => {
    const { container } = render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    const uploadArea = container.querySelector('.unique-image-upload');
    const fileInput = container.querySelector('input[type="file"]');
    const clickSpy = jest.spyOn(fileInput, 'click');

    fireEvent.keyDown(uploadArea, { key: 'Enter' });
    expect(clickSpy).toHaveBeenCalled();

    clickSpy.mockRestore();
  });

  test('displays placeholder when no image is uploaded (lines 271-307)', () => {
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    expect(screen.getByText('Выберите изображение')).toBeInTheDocument();
  });

  test('triggers file input click when clicking upload area (lines 271-307)', () => {
    const { container } = render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    const uploadArea = container.querySelector('.unique-image-upload');
    const fileInput = container.querySelector('input[type="file"]');
    const clickSpy = jest.spyOn(fileInput, 'click');

    fireEvent.click(uploadArea);
    expect(clickSpy).toHaveBeenCalled();
    
    clickSpy.mockRestore();
  });
});

describe('MeetingCard Specific Line Coverage', () => {
  beforeAll(() => {
    process.env.REACT_APP_BACKEND_URI = 'http://localhost:8080';
  });

  beforeEach(() => {
    fetch.mockClear();
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    jest.spyOn(console, 'error').mockImplementation(() => {});
    global.URL.createObjectURL = jest.fn(() => 'mock-image-url');
    
    // Мокаем useParams для существующей встречи
    const { useParams } = require('react-router-dom');
    useParams.mockReturnValue({ meetingId: '123' });
  });

  afterEach(() => {
    console.warn.mockRestore();
    console.error.mockRestore();
  });

  test('should handle image fetch error (lines 69-84)', async () => {
    fetch.mockImplementation((url) => {
      if (url.includes('/api/v1/meetings')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [] }),
        });
      }
      if (url.includes('/api/v1/image/')) {
        return Promise.reject(new Error('Failed to fetch image'));
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/123']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    await waitFor(() => {
      expect(console.error).toHaveBeenCalledWith(
        "Ошибка при загрузке изображения:",
        expect.any(Error)
      );
    });
  });

  test('should handle image upload with FormData (line 164)', async () => {
    // Мокаем useParams для новой встречи в этом тесте
    const { useParams } = require('react-router-dom');
    useParams.mockReturnValue({ meetingId: 'new' });
    
    fetch.mockImplementation((url) => {
      if (url.includes('/api/v1/meetings?teamCardId=') && url.includes('new')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ id: "123" }),
        });
      }
      if (url.includes('/api/v1/image/')) {
        return Promise.resolve({ ok: true });
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    const { container } = render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    await act(async () => {
      await fillAllRequiredFields(container);
      fireEvent.click(screen.getByText('Сохранить'));
    });

    await waitFor(() => {
      const imageUploadCalls = fetch.mock.calls.filter(call => 
        call[0].includes('/api/v1/image/')
      );
      expect(imageUploadCalls.length).toBeGreaterThan(0);
      const imageUploadCall = imageUploadCalls[0];
      expect(imageUploadCall[0]).toContain('/api/v1/image/');
      expect(imageUploadCall[1].method).toBe('POST');
    });
  });

  test('fetches and displays image preview successfully (lines 76-81)', async () => {
    const mockBlob = new Blob(['test'], { type: 'image/png' });
    global.URL.createObjectURL.mockReturnValue('blob:http://localhost/mock-image-url');

    fetch.mockImplementation((url) => {
      if (url.includes('/api/v1/meetings') && !url.includes('image')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [{
              id: '123',
              number: '10',
              startDate: '2023-01-01T00:00:00.000Z',
              link: 'http://example.com',
              tasksCurrentMeeting: 'Task 1',
              tasksNextMeeting: 'Task 2',
              teamStatus: 'OK',
            }],
          }),
        });
      }
      if (url.includes('/api/v1/image/')) {
        return Promise.resolve({
          ok: true,
          blob: () => Promise.resolve(mockBlob),
        });
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/123?teamId=1&username=test&userId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    await waitFor(() => {
      const image = screen.getByAltText('Скриншот встречи');
      expect(image).toBeInTheDocument();
    }, { timeout: 3000 });
  });

  test('should render image upload area with proper styling (lines 271-307)', () => {
    const { useParams } = require('react-router-dom');
    useParams.mockReturnValue({ meetingId: 'new' });
    
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    const uploadArea = screen.getByText('Выберите изображение').closest('.unique-image-upload');
    expect(uploadArea).toHaveAttribute('tabindex', '0');
    expect(uploadArea).toHaveAttribute('role', 'button');
    expect(uploadArea).toHaveAttribute('aria-label', 'Загрузить изображение');
  });
});

describe('MeetingCard Event Handlers', () => {
  beforeEach(() => {
    fetch.mockClear();
    jest.useFakeTimers();
    const { useParams } = require('react-router-dom');
    useParams.mockReturnValue({ meetingId: 'new' });
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  test('should update teamStatus when status option is clicked (OK/WITH_ISSUES/MANY_ISSUES)', async () => {
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    const statusDropdown = screen.getByText('Не указано').closest('.status-selected');
    fireEvent.click(statusDropdown);
    fireEvent.click(screen.getByText('Всё ок'));
    expect(screen.getByText('Всё ок')).toBeInTheDocument();

    fireEvent.click(statusDropdown);
    fireEvent.click(screen.getByText('Есть проблемы'));
    expect(screen.getByText('Есть проблемы')).toBeInTheDocument();

    fireEvent.click(statusDropdown);
    fireEvent.click(screen.getByText('Есть большие проблемы'));
    expect(screen.getByText('Есть большие проблемы')).toBeInTheDocument();
  });
});

describe('MeetingCard Button Interactions', () => {
  beforeEach(() => {
    fetch.mockClear();
    jest.spyOn(console, 'error').mockImplementation(() => {});
    global.URL.createObjectURL = jest.fn(() => 'mock-image-url');
    const { useParams } = require('react-router-dom');
    useParams.mockReturnValue({ meetingId: 'new' });
  });

  afterEach(() => {
    console.error.mockRestore();
  });

  test('should set teamStatus to OK when clicked (team status dropdown)', async () => {
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    fireEvent.click(screen.getByText('Не указано'));
    await act(async () => {
      fireEvent.click(screen.getByText('Всё ок'));
    });
    expect(screen.getByText('Всё ок')).toBeInTheDocument();
  });

  test('should set teamStatus to WITH_ISSUES when clicked (team status dropdown)', async () => {
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    fireEvent.click(screen.getByText('Не указано'));
    await act(async () => {
      fireEvent.click(screen.getByText('Есть проблемы'));
    });
    expect(screen.getByText('Есть проблемы')).toBeInTheDocument();
  });

  test('should set teamStatus to MANY_ISSUES when clicked (team status dropdown)', async () => {
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    fireEvent.click(screen.getByText('Не указано'));
    await act(async () => {
      fireEvent.click(screen.getByText('Есть большие проблемы'));
    });
    expect(screen.getByText('Есть большие проблемы')).toBeInTheDocument();
  });
});

describe('MeetingCard Completion and Editing', () => {
  const mockMeetingData = {
    id: "123",
    number: "10",
    startDate: "2023-01-01T00:00:00.000Z",
    link: "http://example.com",
    tasksCurrentMeeting: "Task 1",
    tasksNextMeeting: "Task 2",
    teamStatus: "OK",
    status: "SCHEDULED"
  };

  beforeEach(() => {
    fetch.mockClear();
    fetch.mockReset();
    jest.spyOn(console, 'error').mockImplementation(() => {});
    global.URL.createObjectURL = jest.fn(() => 'mock-image-url');
    
    // Мокаем useParams для существующей встречи
    const { useParams } = require('react-router-dom');
    useParams.mockReturnValue({ meetingId: '123' });
  });

  afterEach(() => {
    console.error.mockRestore();
  });

  test('should complete meeting successfully (lines 186-230)', async () => {
    fetch.mockImplementation((url) => {
      if (url.includes('/api/v1/meetings') && !url.includes('update-meeting')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [mockMeetingData] }),
        });
      }
      if (url.includes('/api/v1/image/') && !url.includes('update-meeting')) {
        return Promise.resolve({
          ok: true,
          blob: () => Promise.resolve(new Blob()),
        });
      }
      if (url.includes('/api/v1/update-meeting/')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ ...mockMeetingData, status: "COMPLETED" }),
        });
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/123?teamId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    await waitFor(() => {
      expect(screen.getByText(/Встреча 10/i)).toBeInTheDocument();
    }, { timeout: 3000 });

    await act(async () => {
      const completeButton = screen.getByText('Состоялась');
      fireEvent.click(completeButton);
    });

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/update-meeting/123'),
        expect.objectContaining({
          method: 'PATCH'
        })
      );
    });
  });

  test('should handle error when completing meeting (lines 186-230)', async () => {
    fetch.mockImplementation((url) => {
      if (url.includes('/api/v1/meetings') && !url.includes('update-meeting')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [mockMeetingData] }),
        });
      }
      if (url.includes('/api/v1/image/') && !url.includes('update-meeting')) {
        return Promise.resolve({
          ok: true,
          blob: () => Promise.resolve(new Blob()),
        });
      }
      if (url.includes('/api/v1/update-meeting/')) {
        return Promise.reject(new Error('Failed to update meeting'));
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/123?teamId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    await waitFor(() => {
      expect(screen.getByText(/Встреча 10/i)).toBeInTheDocument();
    }, { timeout: 3000 });

    await act(async () => {
      const completeButton = screen.getByText('Состоялась');
      fireEvent.click(completeButton);
    });

    await waitFor(() => {
      expect(screen.getByText(/Failed to update meeting/i)).toBeInTheDocument();
    }, { timeout: 5000 });
  });
});

describe('Textarea Auto-resize Functionality', () => {
  beforeEach(() => {
    fetch.mockClear();
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    jest.spyOn(console, 'error').mockImplementation(() => {});
    const { useParams } = require('react-router-dom');
    useParams.mockReturnValue({ meetingId: 'new' });
  });

  afterEach(() => {
    console.warn.mockRestore();
    console.error.mockRestore();
  });

  test('should auto-resize textarea on focus (lines 45-48)', () => {
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    const textareas = screen.getAllByRole('textbox').filter(el => el.tagName === 'TEXTAREA');
    
    textareas.forEach(textarea => {
      Object.defineProperty(textarea, 'style', {
        value: { height: '' },
        writable: true
      });

      Object.defineProperty(textarea, 'scrollHeight', {
        value: 100,
        configurable: true
      });

      fireEvent.focus(textarea);
      expect(textarea.style.height).toBe('100px');
    });
  });

  test('should auto-resize textarea on change (lines 35-38)', () => {
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    const textareas = screen.getAllByRole('textbox').filter(el => el.tagName === 'TEXTAREA');
    const textarea = textareas[0];

    Object.defineProperty(textarea, 'style', {
      value: { height: '' },
      writable: true
    });

    Object.defineProperty(textarea, 'scrollHeight', {
      value: 80,
      configurable: true
    });

    fireEvent.change(textarea, { target: { value: 'New task value', name: 'tasksCurrentMeeting' } });
    expect(textarea.style.height).toBe('80px');
  });
});

describe('BigBlueButton Integration (lines 633-687)', () => {
  beforeEach(() => {
    jest.spyOn(window, 'open').mockImplementation(() => ({
      focus: () => {},
      close: () => {}
    }));
    
    global.alert = jest.fn();

    Object.defineProperty(window, 'innerWidth', { writable: true, value: 1920 });
    Object.defineProperty(window, 'innerHeight', { writable: true, value: 1080 });
    Object.defineProperty(window, 'screenX', { writable: true, value: 0 });
    Object.defineProperty(window, 'screenY', { writable: true, value: 0 });
    
    const { useParams } = require('react-router-dom');
    useParams.mockReturnValue({ meetingId: 'new' });
  });

  afterEach(() => {
    window.open.mockRestore();
    if (global.alert.mockRestore) {
      global.alert.mockRestore();
    }
  });

  test('should update bbbLink on input change', () => {
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    const input = screen.getByPlaceholderText('https://demo.bigbluebutton.org/rooms/...');
    fireEvent.change(input, { target: { value: 'https://bbb.example.com/rooms/test' } });

    expect(input.value).toBe('https://bbb.example.com/rooms/test');
  });

  test('should show alert when URL is empty', () => {
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    const button = screen.getByText('Подключиться');
    fireEvent.click(button);

    expect(global.alert).toHaveBeenCalledWith('Пожалуйста, введите ссылку на встречу');
    expect(window.open).not.toHaveBeenCalled();
  });

  test('should open window with correct features', () => {
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    const input = screen.getByPlaceholderText('https://demo.bigbluebutton.org/rooms/...');
    fireEvent.change(input, { target: { value: 'https://bbb.example.com/rooms/test' } });

    const button = screen.getByText('Подключиться');
    fireEvent.click(button);

    expect(window.open).toHaveBeenCalledWith(
      'https://bbb.example.com/rooms/test',
      'bbb_meeting_window',
      expect.stringContaining('width=1100')
    );
  });
});

describe("MeetingCard tooltip hover minimal", () => {
  beforeEach(() => {
    fetch.mockClear();
    const { useParams } = require('react-router-dom');
    useParams.mockReturnValue({ meetingId: 'new' });
  });

  test("вызывает onMouseEnter/onMouseLeave для обеих кнопок", () => {
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    const completeButton = screen.getByText("Состоялась");
    const notHappenedButton = screen.getByText("Не состоялась");

    fireEvent.mouseEnter(completeButton);
    fireEvent.mouseLeave(completeButton);
    fireEvent.mouseEnter(notHappenedButton);
    fireEvent.mouseLeave(notHappenedButton);
  });
});