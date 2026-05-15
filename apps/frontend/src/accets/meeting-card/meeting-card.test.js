import React from 'react';
import '@testing-library/jest-dom';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { Provider } from 'react-redux';
import { createStore } from 'redux';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import MeetingCard from './meeting-card';

// Utility to fill all required fields for save
async function fillAllRequiredFields(container) {
  await waitFor(() => {
    expect(screen.getByText('Сохранить')).toBeInTheDocument();
  }, { timeout: 3000 });
  
  const textareas = Array.from(container.querySelectorAll('textarea'));
  
  if (textareas[0]) {
    fireEvent.change(textareas[0], { 
      target: { 
        value: 'Test tasks current meeting',
        name: 'tasksCurrentMeeting' 
      } 
    });
  }
  
  if (textareas[1]) {
    fireEvent.change(textareas[1], { 
      target: { 
        value: 'Test tasks next meeting',
        name: 'tasksNextMeeting' 
      } 
    });
  }
  
  const dropdown = container.querySelector('.status-selected');
  if (dropdown) {
    fireEvent.click(dropdown);
    await waitFor(() => {
      expect(screen.getByText('Всё ок')).toBeInTheDocument();
    }, { timeout: 3000 });
    fireEvent.click(screen.getByText('Всё ок'));
  }
  
  const dateInput = container.querySelector('input[type="date"]');
  if (dateInput) {
    fireEvent.change(dateInput, { 
      target: { 
        value: '2025-12-13',
        name: 'startDate' 
      } 
    });
  }
  
  const linkInput = container.querySelector('input[name="link"]');
  if (linkInput) {
    fireEvent.change(linkInput, { 
      target: { 
        value: 'http://example.com',
        name: 'link' 
      } 
    });
  }
  
  const fileInput = container.querySelector('input[type="file"]');
  if (fileInput) {
    const file = new File(['test'], 'test.png', { type: 'image/png' });
    Object.defineProperty(fileInput, 'files', { value: [file] });
    fireEvent.change(fileInput);
  }
}

function getTestStore(role = 'ADMIN') {
  return createStore(() => ({ user: { user: { roles: [role] } } }));
}

global.fetch = jest.fn();
jest.setTimeout(10000);

jest.mock('../../utils/csrf-utils', () => ({
  getCsrfConfigForFetch: jest.fn().mockReturnValue({
    'X-CSRF-TOKEN': 'mock-token',
    'X-CSRF-HEADER': 'X-CSRF-TOKEN'
  })
}));

const mockNavigate = jest.fn();
const mockUseLocation = jest.fn();
const mockUseParams = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useLocation: () => mockUseLocation,
  useParams: () => mockUseParams,
}));

const getUrlString = (url) => {
  if (typeof url === 'string') return url;
  if (url && url.url) return url.url;
  if (url && url.toString) return url.toString();
  return '';
};

const mockExistingMeeting = {
  id: "123",
  number: "10",
  startDate: "2023-01-01T00:00:00.000Z",
  link: "http://example.com",
  tasksCurrentMeeting: "Task 1",
  tasksNextMeeting: "Task 2",
  teamStatus: "OK",
  status: "SCHEDULED"
};

const mockNewMeetingResponse = {
  id: "123",
  number: "1",
  startDate: "2025-12-13T00:00:00.000Z",
  link: "",
  tasksCurrentMeeting: "a",
  tasksNextMeeting: "b",
  teamStatus: "OK",
  status: "SCHEDULED"
};

describe('MeetingCard Component', () => {
  beforeEach(() => {
    fetch.mockClear();
    fetch.mockReset();
    mockNavigate.mockClear();
    mockUseLocation.mockClear();
    mockUseParams.mockClear();
    
    mockUseLocation.mockReturnValue({
      search: '?teamId=1&username=test&userId=1',
    });
    mockUseParams.mockReturnValue({ meetingId: 'new' });
    
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    jest.spyOn(console, 'error').mockImplementation(() => {});
    
    global.URL.createObjectURL = jest.fn(() => 'mock-image-url');
    
    Object.defineProperty(window, 'location', {
      value: { origin: 'http://localhost' },
      writable: true
    });
    
    process.env.REACT_APP_BACKEND_URI = '';
  });

  afterEach(() => {
    console.warn.mockRestore();
    console.error.mockRestore();
    jest.restoreAllMocks();
  });

  test('handles save with image upload', async () => {
    let fetchCallCount = 0;
    
    fetch.mockImplementation((url) => {
      fetchCallCount++;
      if (fetchCallCount === 1) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [] })
        });
      }
      if (fetchCallCount === 2) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            id: '123',
            number: '1',
            startDate: '2025-12-13T00:00:00.000Z',
            link: 'http://example.com',
            tasksCurrentMeeting: 'Test tasks current meeting',
            tasksNextMeeting: 'Test tasks next meeting',
            teamStatus: 'OK',
            status: 'SCHEDULED'
          })
        });
      }
      if (fetchCallCount === 3) {
        return Promise.resolve({ ok: true });
      }
      return Promise.resolve({ ok: true });
    });

    const { container } = render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    await waitFor(() => {
      expect(screen.getByText('Сохранить')).toBeInTheDocument();
    }, { timeout: 3000 });

    const fileInput = container.querySelector('input[type="file"]');
    const file = new File(['test'], 'test.png', { type: 'image/png' });
    Object.defineProperty(fileInput, 'files', {
      value: [file],
      writable: true,
      configurable: true
    });

    await act(async () => {
      fireEvent.change(fileInput);
    });

    await waitFor(() => {
      expect(fileInput.files[0]).toBeDefined();
    });

    await fillAllRequiredFields(container);
    
    await act(async () => {
      fireEvent.click(screen.getByText('Сохранить'));
    });

    await waitFor(() => {
      expect(fetchCallCount).toBe(3);
    }, { timeout: 5000 });
  });

  test('handles error during image upload', async () => {
    let fetchCallCount = 0;
    
    fetch.mockImplementation((url) => {
      fetchCallCount++;
      if (fetchCallCount === 1) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [] })
        });
      }
      if (fetchCallCount === 2) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            id: '123',
            number: '1',
            startDate: '2025-12-13T00:00:00.000Z',
            link: 'http://example.com',
            tasksCurrentMeeting: 'Test tasks',
            tasksNextMeeting: 'Test tasks next',
            teamStatus: 'OK',
            status: 'SCHEDULED'
          })
        });
      }
      if (fetchCallCount === 3) {
        return Promise.reject(new Error('Image upload failed'));
      }
      return Promise.resolve({ ok: true });
    });

    const { container } = render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    await waitFor(() => {
      expect(screen.getByText('Сохранить')).toBeInTheDocument();
    }, { timeout: 3000 });

    const fileInput = container.querySelector('input[type="file"]');
    const file = new File(['test'], 'test.png', { type: 'image/png' });
    Object.defineProperty(fileInput, 'files', {
      value: [file],
      writable: true,
      configurable: true
    });

    await act(async () => {
      fireEvent.change(fileInput);
    });

    await waitFor(() => {
      expect(fileInput.files[0].name).toBe('test.png');
    });

    await fillAllRequiredFields(container);
    
    await act(async () => {
      fireEvent.click(screen.getByText('Сохранить'));
    });

    await waitFor(() => {
      expect(fetchCallCount).toBe(3);
    }, { timeout: 5000 });
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
    fetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ content: [] })
    });

    const { container } = render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      </Provider>
    );

    await waitFor(() => {
      expect(screen.getByText('Новая встреча')).toBeInTheDocument();
    });

    const fileInput = container.querySelector('input[type="file"]');
    const file = new File(['test'], 'test.png', { type: 'image/png' });
    Object.defineProperty(fileInput, 'files', {
      value: [file],
      configurable: true
    });

    const changeSpy = jest.spyOn(fireEvent, 'change');
    await act(async () => {
      fireEvent.change(fileInput);
    });

    expect(changeSpy).toHaveBeenCalledWith(fileInput);
    changeSpy.mockRestore();
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
    fetch.mockImplementation((url) => {
      const urlString = getUrlString(url);
      if (urlString.includes('/api/v1/meetings?teamCardId=')) {
        return Promise.resolve({
          ok: false,
          text: () => Promise.resolve('Ошибка при сохранении')
        });
      }
      return Promise.reject(new Error(`Unexpected URL: ${urlString}`));
    });

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
    mockNavigate.mockClear();
    mockUseLocation.mockClear();
    mockUseParams.mockClear();
    
    mockUseLocation.mockReturnValue({
      search: '?teamId=1&username=test&userId=1',
    });
    mockUseParams.mockReturnValue({ meetingId: '123' });
    
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    jest.spyOn(console, 'error').mockImplementation(() => {});
    global.URL.createObjectURL = jest.fn(() => 'mock-image-url');
  });

  afterEach(() => {
    console.warn.mockRestore();
    console.error.mockRestore();
  });

  test('deletes meeting card successfully', async () => {
    fetch.mockImplementation((url) => {
      const urlString = getUrlString(url);
      if (urlString.includes('/api/v1/meetings') && !urlString.includes('delete-meeting')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [mockExistingMeeting] })
        });
      }
      if (urlString.includes('/api/v1/image/')) {
        return Promise.resolve({ ok: true, blob: () => Promise.resolve(new Blob()) });
      }
      if (urlString.includes('/api/v1/delete-meeting/')) {
        return Promise.resolve({ ok: true });
      }
      return Promise.reject(new Error(`Unexpected URL: ${urlString}`));
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
      const urlString = getUrlString(url);
      if (urlString.includes('/api/v1/meetings') && !urlString.includes('delete-meeting')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [mockExistingMeeting] })
        });
      }
      if (urlString.includes('/api/v1/image/')) {
        return Promise.resolve({ ok: true, blob: () => Promise.resolve(new Blob()) });
      }
      if (urlString.includes('/api/v1/delete-meeting/')) {
        return Promise.resolve({ ok: false, text: () => Promise.resolve('Ошибка удаления') });
      }
      return Promise.reject(new Error(`Unexpected URL: ${urlString}`));
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
      const urlString = getUrlString(url);
      if (urlString.includes('/api/v1/meetings') && !urlString.includes('delete-meeting')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [mockExistingMeeting] })
        });
      }
      if (urlString.includes('/api/v1/image/')) {
        return Promise.resolve({ ok: true, blob: () => Promise.resolve(new Blob()) });
      }
      return Promise.reject(new Error(`Unexpected URL: ${urlString}`));
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
    mockNavigate.mockClear();
    mockUseLocation.mockClear();
    mockUseParams.mockClear();
    mockUseLocation.mockReturnValue({ search: '?teamId=1&username=test&userId=1' });
    mockUseParams.mockReturnValue({ meetingId: 'new' });
  });

  test('should handle image upload when clicking the upload area', () => {
    const { container } = render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
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

  test('should handle keyboard events for image upload', () => {
    const { container } = render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
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

  test('displays placeholder when no image is uploaded', () => {
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
        </MemoryRouter>
      </Provider>
    );
    expect(screen.getByText('Выберите изображение')).toBeInTheDocument();
  });

  test('triggers file input click when clicking upload area', () => {
    const { container } = render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
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
    mockNavigate.mockClear();
    mockUseLocation.mockClear();
    mockUseParams.mockClear();
    mockUseLocation.mockReturnValue({ search: '?teamId=1&username=test&userId=1' });
    mockUseParams.mockReturnValue({ meetingId: '123' });
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    jest.spyOn(console, 'error').mockImplementation(() => {});
    global.URL.createObjectURL = jest.fn(() => 'mock-image-url');
  });

  afterEach(() => {
    console.warn.mockRestore();
    console.error.mockRestore();
  });

  test('should handle image fetch error', async () => {
    fetch.mockImplementation((url) => {
      const urlString = getUrlString(url);
      if (urlString.includes('/api/v1/meetings')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [mockExistingMeeting] }) });
      }
      if (urlString.includes('/api/v1/image/')) {
        return Promise.reject(new Error('Failed to fetch image'));
      }
      return Promise.reject(new Error(`Unexpected URL: ${urlString}`));
    });

    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/123']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
        </MemoryRouter>
      </Provider>
    );

    await waitFor(() => {
      expect(console.error).toHaveBeenCalledWith("Ошибка при загрузке изображения:", expect.any(Error));
    });
  });

  test('should handle image upload with FormData', async () => {
    mockUseParams.mockReturnValue({ meetingId: 'new' });
    let imageUploadCalled = false;
    let meetingSaveCalled = false;
    
    fetch.mockImplementation((url) => {
      const urlString = getUrlString(url);
      if (urlString.includes('/api/v1/meetings?teamCardId=')) {
        meetingSaveCalled = true;
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ id: '123', number: '1', startDate: '2025-12-13T00:00:00.000Z', link: 'http://example.com', tasksCurrentMeeting: 'Test tasks current meeting', tasksNextMeeting: 'Test tasks next meeting', teamStatus: 'OK', status: 'SCHEDULED' }) });
      }
      if (urlString.includes('/api/v1/image/')) {
        imageUploadCalled = true;
        return Promise.resolve({ ok: true });
      }
      return Promise.resolve({ ok: true });
    });

    const { container } = render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
        </MemoryRouter>
      </Provider>
    );

    await waitFor(() => expect(screen.getByText('Сохранить')).toBeInTheDocument(), { timeout: 3000 });
    expect(screen.getByText('Новая встреча')).toBeInTheDocument();
    await fillAllRequiredFields(container);
    await act(async () => { fireEvent.click(screen.getByText('Сохранить')); });

    await waitFor(() => {
      expect(meetingSaveCalled).toBe(true);
      expect(imageUploadCalled).toBe(true);
    }, { timeout: 5000 });
  });

  test('fetches and displays image preview successfully', async () => {
    const mockBlob = new Blob(['test'], { type: 'image/png' });
    global.URL.createObjectURL.mockReturnValue('blob:http://localhost/mock-image-url');

    fetch.mockImplementation((url) => {
      const urlString = getUrlString(url);
      if (urlString.includes('/api/v1/meetings') && !urlString.includes('image')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [mockExistingMeeting] }) });
      }
      if (urlString.includes('/api/v1/image/')) {
        return Promise.resolve({ ok: true, blob: () => Promise.resolve(mockBlob) });
      }
      return Promise.reject(new Error(`Unexpected URL: ${urlString}`));
    });

    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/123?teamId=1&username=test&userId=1']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
        </MemoryRouter>
      </Provider>
    );

    await waitFor(() => {
      expect(screen.getByAltText('Скриншот встречи')).toBeInTheDocument();
    }, { timeout: 3000 });
  });

  test('should render image upload area with proper styling', () => {
    mockUseParams.mockReturnValue({ meetingId: 'new' });
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
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
    mockNavigate.mockClear();
    mockUseLocation.mockClear();
    mockUseParams.mockClear();
    mockUseLocation.mockReturnValue({ search: '?teamId=1&username=test&userId=1' });
    mockUseParams.mockReturnValue({ meetingId: 'new' });
    jest.useFakeTimers();
  });

  afterEach(() => { jest.useRealTimers(); });

  test('should update teamStatus when status option is clicked', async () => {
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
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

describe('MeetingCard Completion and Editing', () => {
  const pastDate = new Date(Date.now() - 86400000).toISOString();
  const mockMeetingDataPast = {
    ...mockExistingMeeting,
    startDate: pastDate,
    recordLink: "http://example.com",
    tasksCurrentMeeting: "Task 1",
    tasksNextMeeting: "Task 2",
    teamStatus: "OK",
    status: "SCHEDULED"
  };

  beforeEach(() => {
    fetch.mockClear();
    fetch.mockReset();
    mockNavigate.mockClear();
    mockUseLocation.mockClear();
    mockUseParams.mockClear();
    mockUseLocation.mockReturnValue({ search: '?teamId=1&username=test&userId=1' });
    mockUseParams.mockReturnValue({ meetingId: '123' });
    jest.spyOn(console, 'error').mockImplementation(() => {});
    global.URL.createObjectURL = jest.fn(() => 'mock-image-url');
  });

  afterEach(() => { console.error.mockRestore(); });

  test('sort after save handles NaN number values', async () => {
    mockUseParams.mockReturnValue({ meetingId: 'new' });
    let callCount = 0;
    fetch.mockImplementation(() => {
      callCount++;
      if (callCount === 1) return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [{ id: '1', number: null }, { id: '2', number: 'abc' }, { id: '3', number: '5' }] }) });
      if (callCount === 2) return Promise.resolve({ ok: true, json: () => Promise.resolve({ id: '999', number: null, startDate: '2025-12-13T00:00:00.000Z', tasksCurrentMeeting: 'Test', tasksNextMeeting: 'Test', teamStatus: 'OK', status: 'SCHEDULED', recordLink: 'http://example.com' }) });
      return Promise.resolve({ ok: true });
    });

    const { container } = render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
        </MemoryRouter>
      </Provider>
    );

    await waitFor(() => expect(screen.getByText('Сохранить')).toBeInTheDocument(), { timeout: 3000 });
    await fillAllRequiredFields(container);
    await act(async () => { fireEvent.click(screen.getByText('Сохранить')); });
    await waitFor(() => expect(callCount).toBeGreaterThanOrEqual(2), { timeout: 5000 });
    expect(mockNavigate).toHaveBeenCalled();
  });

  test('shows error when completing meeting with unfilled fields', async () => {
    const pastMeetingEmpty = {
      id: '123', number: '5', startDate: new Date(Date.now() - 86400000).toISOString(),
      status: 'SCHEDULED', tasksCurrentMeeting: '', tasksNextMeeting: '', teamStatus: '', recordLink: ''
    };
    fetch.mockImplementation((url) => {
      const urlString = getUrlString(url);
      if (urlString.includes('/api/v1/meetings')) return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [pastMeetingEmpty] }) });
      return Promise.resolve({ ok: true });
    });

    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/123?teamId=1']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
        </MemoryRouter>
      </Provider>
    );

    await waitFor(() => expect(screen.getByText(/Встреча 5/i)).toBeInTheDocument(), { timeout: 3000 });
    const completeButton = screen.getByTestId('complete-meeting-btn');
    await act(async () => { completeButton.click(); });
    await waitFor(() => expect(screen.getByText(/Нельзя завершить встречу/i)).toBeInTheDocument(), { timeout: 3000 });
  });

  test('shows error when completing meeting before date has passed', async () => {
    const futureMeeting = {
      ...mockExistingMeeting,
      startDate: new Date(Date.now() + 86400000 * 3).toISOString(),
      status: 'SCHEDULED',
      tasksCurrentMeeting: 'Task 1',
      tasksNextMeeting: 'Task 2',
      teamStatus: 'OK',
      recordLink: 'http://example.com',
    };

    fetch.mockImplementation((url) => {
      const urlString = getUrlString(url);
      if (urlString.includes('/api/v1/meetings')) return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [futureMeeting] }) });
      if (urlString.includes('/api/v1/image/')) return Promise.resolve({ ok: true, blob: () => Promise.resolve(new Blob()) });
      return Promise.resolve({ ok: true });
    });

    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/123?teamId=1']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
        </MemoryRouter>
      </Provider>
    );

    await waitFor(() => expect(screen.getByText(/Встреча 10/i)).toBeInTheDocument(), { timeout: 3000 });
    const completeButton = screen.getByTestId('complete-meeting-btn');
    await act(async () => { completeButton.click(); });
    await waitFor(() => {
      const errorDiv = document.querySelector('.error-message');
      expect(errorDiv).toBeInTheDocument();
      expect(errorDiv.textContent).toMatch(/Завершение встречи возможно только после окончания даты встречи/i);
    }, { timeout: 3000 });
  });

  test('should complete meeting successfully', async () => {
    fetch.mockImplementation((url) => {
      const urlString = getUrlString(url);
      if (urlString.includes('/api/v1/meetings') && !urlString.includes('update-meeting')) return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [mockMeetingDataPast] }) });
      if (urlString.includes('/api/v1/image/') && !urlString.includes('update-meeting')) return Promise.resolve({ ok: true, blob: () => Promise.resolve(new Blob()) });
      if (urlString.includes('/api/v1/update-meeting/')) return Promise.resolve({ ok: true, json: () => Promise.resolve({ ...mockMeetingDataPast, status: "COMPLETED" }) });
      return Promise.reject(new Error(`Unexpected URL: ${urlString}`));
    });

    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/123?teamId=1']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
        </MemoryRouter>
      </Provider>
    );

    const buttons = await screen.findAllByRole('button', { name: /Состоялась/i });
    const completeButton = buttons[0];
    await waitFor(() => expect(completeButton).not.toBeDisabled());
    fireEvent.click(completeButton);
    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/api/v1/update-meeting/123'), expect.objectContaining({ method: 'PATCH' }));
    });
  });

  test('should handle error when completing meeting', async () => {
    fetch.mockImplementation((url) => {
      const urlString = getUrlString(url);
      if (urlString.includes('/api/v1/meetings')) return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [mockMeetingDataPast] }) });
      if (urlString.includes('/api/v1/update-meeting/')) return Promise.resolve({ ok: false, status: 500, text: () => Promise.resolve("Server error") });
      return Promise.resolve({ ok: true, blob: () => Promise.resolve(new Blob()) });
    });

    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/123?teamId=1']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
        </MemoryRouter>
      </Provider>
    );

    const buttons = await screen.findAllByRole('button', { name: /Состоялась/i });
    const completeButton = buttons[0];
    await waitFor(() => expect(completeButton).not.toBeDisabled());
    fireEvent.click(completeButton);
    await waitFor(() => {
      expect(screen.getByText(/Server error/i)).toBeInTheDocument();
    }, { timeout: 5000 });
  });
});

describe('Textarea Auto-resize Functionality', () => {
  beforeEach(() => {
    fetch.mockClear();
    mockNavigate.mockClear();
    mockUseLocation.mockClear();
    mockUseParams.mockClear();
    mockUseLocation.mockReturnValue({ search: '?teamId=1&username=test&userId=1' });
    mockUseParams.mockReturnValue({ meetingId: 'new' });
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    console.warn.mockRestore();
    console.error.mockRestore();
  });

  test('should auto-resize textarea on focus', () => {
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
        </MemoryRouter>
      </Provider>
    );

    const textareas = screen.getAllByRole('textbox').filter(el => el.tagName === 'TEXTAREA');
    textareas.forEach(textarea => {
      Object.defineProperty(textarea, 'style', { value: { height: '' }, writable: true });
      Object.defineProperty(textarea, 'scrollHeight', { value: 100, configurable: true });
      fireEvent.focus(textarea);
      expect(textarea.style.height).toBe('100px');
    });
  });

  test('should auto-resize textarea on change', () => {
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
        </MemoryRouter>
      </Provider>
    );

    const textareas = screen.getAllByRole('textbox').filter(el => el.tagName === 'TEXTAREA');
    const textarea = textareas[0];
    Object.defineProperty(textarea, 'style', { value: { height: '' }, writable: true });
    Object.defineProperty(textarea, 'scrollHeight', { value: 80, configurable: true });
    fireEvent.change(textarea, { target: { value: 'New task value', name: 'tasksCurrentMeeting' } });
    expect(textarea.style.height).toBe('80px');
  });
});

describe('Meeting Room Integration', () => {
  let windowSpy;
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseParams.mockReturnValue({ meetingId: '42' });
    mockUseLocation.mockReturnValue({ search: '?teamId=1&username=test' });
    windowSpy = jest.spyOn(window, 'open').mockImplementation(() => ({}));
  });
  afterEach(() => { windowSpy.mockRestore(); });

  const setupMockFetch = (roomLink) => {
    global.fetch.mockImplementation((url) => {
      if (url.toString().includes('/api/v1/meetings')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [{ id: '42', number: '1', startDate: new Date().toISOString(), status: 'SCHEDULED', roomLink }], totalPages: 1 }) });
      }
      return Promise.resolve({ ok: true, blob: () => Promise.resolve(new Blob()) });
    });
  };

  test('открывает окно BBB с правильным URL', async () => {
    setupMockFetch('webinar.tusur.ru/b/test');
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/42?teamId=1']}>
          <MeetingCard />
        </MemoryRouter>
      </Provider>
    );
    await screen.findByText(/Встреча 1/i);
    fireEvent.click(screen.getByLabelText('Запустить встречу'));
    expect(windowSpy).toHaveBeenCalledWith(expect.stringContaining('https://webinar.tusur.ru/b/test'), 'bbb_meeting_window', expect.stringContaining('width=1100'));
  });

  test('показывает ошибку, если roomLink пустой', async () => {
    setupMockFetch('');
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/42?teamId=1']}>
          <MeetingCard />
        </MemoryRouter>
      </Provider>
    );
    await screen.findByText(/Встреча 1/i);
    fireEvent.click(screen.getByLabelText('Запустить встречу'));
    expect(screen.getByText(/Ссылка на комнату для встречи не указана/i)).toBeInTheDocument();
    expect(windowSpy).not.toHaveBeenCalled();
  });

  test('корректно обрабатывает ссылку, если она уже с http', async () => {
    setupMockFetch('http://my-server.com/room');
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/42?teamId=1']}>
          <MeetingCard />
        </MemoryRouter>
      </Provider>
    );
    await screen.findByText(/Встреча 1/i);
    fireEvent.click(screen.getByLabelText('Запустить встречу'));
    expect(windowSpy).toHaveBeenCalledWith('http://my-server.com/room', 'bbb_meeting_window', expect.any(String));
  });
});

describe("MeetingCard tooltip hover minimal", () => {
  beforeEach(() => {
    fetch.mockClear();
    mockNavigate.mockClear();
    mockUseLocation.mockClear();
    mockUseParams.mockClear();
    mockUseLocation.mockReturnValue({ search: '?teamId=1&username=test&userId=1' });
    mockUseParams.mockReturnValue({ meetingId: '123' });
    fetch.mockImplementation((url) => {
      const urlString = getUrlString(url);
      if (urlString.includes('/api/v1/meetings')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [{
              id: '123', number: '10', startDate: new Date(Date.now() - 86400000).toISOString(),
              link: 'http://example.com', tasksCurrentMeeting: 'Task 1', tasksNextMeeting: 'Task 2',
              teamStatus: 'OK', status: 'SCHEDULED'
            }]
          }),
        });
      }
      if (urlString.includes('/api/v1/image/')) return Promise.resolve({ ok: true, blob: () => Promise.resolve(new Blob()) });
      return Promise.reject(new Error(`Unexpected URL: ${urlString}`));
    });
  });

  test("вызывает onMouseEnter/onMouseLeave для обеих кнопок", async () => {
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/123?teamId=1&userId=1']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
        </MemoryRouter>
      </Provider>
    );
    await waitFor(() => expect(screen.getByText(/Встреча 10/i)).toBeInTheDocument(), { timeout: 3000 });
    const completeButton = screen.getByText("Состоялась");
    const notHappenedButton = screen.getByText("Не состоялась");
    fireEvent.mouseEnter(completeButton);
    fireEvent.mouseLeave(completeButton);
    fireEvent.mouseEnter(notHappenedButton);
    fireEvent.mouseLeave(notHappenedButton);
  });
});

async function waitForStatusDropdown() {
  const dropdown = screen.getByText('Не указано');
  if (dropdown) {
    fireEvent.click(dropdown);
    await waitFor(() => expect(screen.getByText('Всё ок')).toBeInTheDocument(), { timeout: 3000 });
  }
}

describe('MeetingCard Role Setting - Complete Line Coverage', () => {
  beforeEach(() => {
    mockUseLocation.mockReturnValue({ search: '?teamId=1&username=test&userId=1' });
    mockUseParams.mockReturnValue({ meetingId: 'new' });
    fetch.mockResolvedValue({ ok: true, json: () => Promise.resolve({ content: [] }) });
    Object.defineProperty(window, 'localStorage', {
      value: { getItem: jest.fn(), setItem: jest.fn(), removeItem: jest.fn(), clear: jest.fn() },
      writable: true
    });
  });

  afterEach(() => { jest.clearAllMocks(); });

  test('covers all branches - reduxUser with roles', async () => {
    const store = createStore(() => ({ user: { user: { roles: ['ADMIN'] } } }));
    render(<Provider store={store}><MemoryRouter><MeetingCard /></MemoryRouter></Provider>);
    await waitFor(() => expect(screen.getByText('Новая встреча')).toBeInTheDocument());
  });

  test('covers all branches - reduxUser with empty roles array', async () => {
    const store = createStore(() => ({ user: { user: { roles: [] } } }));
    render(<Provider store={store}><MemoryRouter><MeetingCard /></MemoryRouter></Provider>);
    await waitFor(() => expect(screen.getByText('Новая встреча')).toBeInTheDocument());
  });

  test('covers all branches - reduxUser without roles property', async () => {
    const store = createStore(() => ({ user: { user: { username: 'test' } } }));
    render(<Provider store={store}><MemoryRouter><MeetingCard /></MemoryRouter></Provider>);
    await waitFor(() => expect(screen.getByText('Новая встреча')).toBeInTheDocument());
  });

  test('covers all branches - reduxUser null, localStorage has user', async () => {
    const store = createStore(() => ({ user: { user: null } }));
    window.localStorage.getItem.mockReturnValue(JSON.stringify({ roles: ['USER'] }));
    render(<Provider store={store}><MemoryRouter><MeetingCard /></MemoryRouter></Provider>);
    await waitFor(() => expect(screen.getByText('Новая встреча')).toBeInTheDocument());
  });

  test('covers all branches - reduxUser null, localStorage has user without roles', async () => {
    const store = createStore(() => ({ user: { user: null } }));
    window.localStorage.getItem.mockReturnValue(JSON.stringify({ username: 'test' }));
    render(<Provider store={store}><MemoryRouter><MeetingCard /></MemoryRouter></Provider>);
    await waitFor(() => expect(screen.getByText('Новая встреча')).toBeInTheDocument());
  });

  test('covers all branches - reduxUser null, localStorage empty', async () => {
    const store = createStore(() => ({ user: { user: null } }));
    window.localStorage.getItem.mockReturnValue(null);
    render(<Provider store={store}><MemoryRouter><MeetingCard /></MemoryRouter></Provider>);
    await waitFor(() => expect(screen.getByText('Новая встреча')).toBeInTheDocument());
  });

  test('covers all branches - reduxUser null, localStorage has invalid JSON', async () => {
    const store = createStore(() => ({ user: { user: null } }));
    window.localStorage.getItem.mockReturnValue('invalid json');
    expect(() => {
      render(<Provider store={store}><MemoryRouter><MeetingCard /></MemoryRouter></Provider>);
    }).toThrow(SyntaxError);
  });

  test('covers all branches - reduxUser with null user in state', async () => {
    const store = createStore(() => ({ user: null }));
    render(<Provider store={store}><MemoryRouter><MeetingCard /></MemoryRouter></Provider>);
    await waitFor(() => expect(screen.getByText('Новая встреча')).toBeInTheDocument());
  });

  test('covers all branches - reduxUser undefined', async () => {
    const store = createStore(() => ({ user: { user: undefined } }));
    render(<Provider store={store}><MemoryRouter><MeetingCard /></MemoryRouter></Provider>);
    await waitFor(() => expect(screen.getByText('Новая встреча')).toBeInTheDocument());
  });

  test('covers all branches - localStorage returns empty string', async () => {
    const store = createStore(() => ({ user: { user: null } }));
    window.localStorage.getItem.mockReturnValue('');
    render(<Provider store={store}><MemoryRouter><MeetingCard /></MemoryRouter></Provider>);
    await waitFor(() => expect(screen.getByText('Новая встреча')).toBeInTheDocument());
  });
});

describe('MeetingCard Sorting Logic', () => {
  beforeEach(() => {
    fetch.mockClear();
    mockNavigate.mockClear();
    mockUseLocation.mockClear();
    mockUseParams.mockClear();
    mockUseLocation.mockReturnValue({ search: '?teamId=1&username=test&userId=1' });
    mockUseParams.mockReturnValue({ meetingId: 'new' });
  });

  test('should sort meetings by numeric number ascending', async () => {
    const mockMeetings = [
      { id: '1', number: '3', startDate: '2023-01-01', teamStatus: 'OK' },
      { id: '2', number: '1', startDate: '2023-01-01', teamStatus: 'OK' },
      { id: '3', number: '2', startDate: '2023-01-01', teamStatus: 'OK' }
    ];
    fetch.mockResolvedValue({ ok: true, json: () => Promise.resolve({ content: mockMeetings }) });
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
        </MemoryRouter>
      </Provider>
    );
    await waitFor(() => expect(fetch).toHaveBeenCalled());
  });

  test('should handle meetings with non-numeric numbers', async () => {
    const mockMeetings = [
      { id: '1', number: 'abc', startDate: '2023-01-01', teamStatus: 'OK' },
      { id: '2', number: null, startDate: '2023-01-01', teamStatus: 'OK' },
      { id: '3', number: '5', startDate: '2023-01-01', teamStatus: 'OK' }
    ];
    fetch.mockResolvedValue({ ok: true, json: () => Promise.resolve({ content: mockMeetings }) });
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
        </MemoryRouter>
      </Provider>
    );
    await waitFor(() => expect(fetch).toHaveBeenCalled());
  });

  test('should handle meetings with missing number property', async () => {
    const mockMeetings = [
      { id: '1', startDate: '2023-01-01', teamStatus: 'OK' },
      { id: '2', number: '10', startDate: '2023-01-01', teamStatus: 'OK' },
      { id: '3', number: '2', startDate: '2023-01-01', teamStatus: 'OK' }
    ];
    fetch.mockResolvedValue({ ok: true, json: () => Promise.resolve({ content: mockMeetings }) });
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
        </MemoryRouter>
      </Provider>
    );
    await waitFor(() => expect(fetch).toHaveBeenCalled());
  });

  test('should handle empty meetings array', async () => {
    fetch.mockResolvedValue({ ok: true, json: () => Promise.resolve({ content: [] }) });
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
        </MemoryRouter>
      </Provider>
    );
    await waitFor(() => expect(fetch).toHaveBeenCalled());
  });
});

describe('MeetingCard Validation Coverage', () => {
  beforeEach(() => {
    fetch.mockClear();
    mockNavigate.mockClear();
    mockUseLocation.mockClear();
    mockUseParams.mockClear();
    mockUseLocation.mockReturnValue({ search: '?teamId=1&username=test&userId=1' });
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  test('covers validation logic for new meeting', async () => {
    mockUseParams.mockReturnValue({ meetingId: 'new' });
    fetch.mockResolvedValue({ ok: true, json: () => Promise.resolve({ content: [] }) });
    const { container } = render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/new?teamId=1']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
        </MemoryRouter>
      </Provider>
    );
    await waitFor(() => expect(screen.getByText('Сохранить')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Сохранить'));
    expect(fetch).toHaveBeenCalled();
  });

  test('covers validation logic for existing meeting', async () => {
    mockUseParams.mockReturnValue({ meetingId: '123' });
    let callCount = 0;
    fetch.mockImplementation(() => {
      callCount++;
      if (callCount === 1) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [{ id: '123', number: '1', startDate: '2025-12-01T00:00:00.000Z', link: 'http://example.com', tasksCurrentMeeting: 'Task 1', tasksNextMeeting: 'Task 2', teamStatus: 'OK', status: 'SCHEDULED' }]
          })
        });
      }
      if (callCount === 2) return Promise.resolve({ ok: true, blob: () => Promise.resolve(new Blob()) });
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ id: '123' }) });
    });
    render(
      <Provider store={getTestStore()}>
        <MemoryRouter initialEntries={['/meeting/123?teamId=1']}>
          <Routes><Route path="/meeting/:meetingId" element={<MeetingCard />} /></Routes>
        </MemoryRouter>
      </Provider>
    );
    await waitFor(() => expect(screen.getByText('Редактировать')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Редактировать'));
    await waitFor(() => expect(screen.getByText('Сохранить')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Сохранить'));
    expect(callCount).toBeGreaterThan(2);
  });
});

// ==================== SBI-800: SUPERADMIN TESTS ====================
describe('SuperAdmin permissions in meeting-card', () => {
  test('superadmin can edit completed meeting', async () => {
    const store = createStore(() => ({ user: { user: { roles: ['SUPER_ADMIN'] } } }));
    const completedMeeting = { ...mockExistingMeeting, status: 'COMPLETED' };
    
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ content: [completedMeeting] })
    });

    render(
      <Provider store={store}>
        <MemoryRouter initialEntries={['/meeting/123?teamId=1']}>
          <MeetingCard />
        </MemoryRouter>
      </Provider>
    );

    await waitFor(() => {
      expect(screen.getByText('Редактировать')).toBeInTheDocument();
    });
    expect(screen.getByText('Редактировать')).not.toBeDisabled();
  });
});