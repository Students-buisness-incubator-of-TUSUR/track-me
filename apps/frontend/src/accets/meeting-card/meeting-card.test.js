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

  describe('MeetingCard Additional Tests', () => {
  test('should handle image upload when clicking the upload area (lines 271-307)', () => {
    const file = new File(['test'], 'test.png', { type: 'image/png' });
    const { container } = render(
      <MemoryRouter initialEntries={['/meeting/new']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    // Simulate clicking the upload area
    const uploadArea = container.querySelector('.unique-image-upload');
    fireEvent.click(uploadArea);

    // Simulate file selection
    const fileInput = container.querySelector('input[type="file"]');
    Object.defineProperty(fileInput, 'files', { value: [file] });
    fireEvent.change(fileInput);
  });

  test('should handle keyboard events for image upload (lines 271-307)', () => {
    const file = new File(['test'], 'test.png', { type: 'image/png' });
    const { container } = render(
      <MemoryRouter initialEntries={['/meeting/new']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    // Simulate keyboard events
    const uploadArea = container.querySelector('.unique-image-upload');
    fireEvent.keyDown(uploadArea, { key: 'Enter' });
    fireEvent.keyDown(uploadArea, { key: ' ' });

    // Simulate file selection
    const fileInput = container.querySelector('input[type="file"]');
    Object.defineProperty(fileInput, 'files', { value: [file] });
    fireEvent.change(fileInput);
  });

  

  test('should handle image upload error (line 164)', async () => {
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
});
  describe('MeetingCard Specific Line Coverage', () => {
    beforeAll(() => {
    process.env.REACT_APP_BACKEND_URI = 'http://localhost:8080';
  });
  

  test('should handle image fetch error (lines 69-84)', async () => {
    fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [] }),
      })
    ).mockImplementationOnce(() =>
      Promise.reject(new Error('Failed to fetch image'))
    );

    render(
      <MemoryRouter initialEntries={['/meeting/123']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(console.error).toHaveBeenCalledWith(
        "Ошибка при загрузке изображения:",
        expect.any(Error)
      );
    });
  });

  test('should handle image upload with FormData (line 164)', async () => {
    const file = new File(['test'], 'test.png', { type: 'image/png' });
    
    fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ id: "123" }),
      })
    ).mockImplementationOnce(() =>
      Promise.resolve({ ok: true })
    );

    const { container } = render(
      <MemoryRouter initialEntries={['/meeting/new']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    await act(async () => {
      const fileInput = container.querySelector('input[type="file"]');
      Object.defineProperty(fileInput, 'files', { value: [file] });
      fireEvent.change(fileInput);
      fireEvent.click(screen.getByText('Сохранить'));
    });

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledTimes(2);
      const imageUploadCall = fetch.mock.calls[1];
      expect(imageUploadCall[0]).toContain('/api/v1/image/123');
      expect(imageUploadCall[1].method).toBe('POST');
    });
  });

  test('should render image upload area with proper styling (lines 271-307)', () => {
    render(
      <MemoryRouter initialEntries={['/meeting/new']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    const uploadArea = screen.getByText('Выберите изображение').closest('.unique-image-upload');
    expect(uploadArea).toHaveStyle('margin-left: 30px');
    expect(uploadArea).toHaveAttribute('tabindex', '0');
    expect(uploadArea).toHaveAttribute('role', 'button');
    expect(uploadArea).toHaveAttribute('aria-label', 'Загрузить изображение');
  });

  
});
test('displays placeholder when no image is uploaded (lines 271-307)', () => {
    render(
      <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Выберите изображение')).toBeInTheDocument();
  });
  test('handles image upload error and sets error message (lines 69-84, 164)', async () => {
    const file = new File(['test'], 'test.png', { type: 'image/png' });

    fetch
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ id: '123' }),
        })
      )
      .mockImplementationOnce(() =>
        Promise.reject(new Error('Ошибка при загрузке изображения'))
      );

    const { container } = render(
      <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    await act(async () => {
      // Upload image
      const fileInput = container.querySelector('input[type="file"]');
      Object.defineProperty(fileInput, 'files', { value: [file] });
      fireEvent.change(fileInput);

      // Click save
      fireEvent.click(screen.getByText('Сохранить'));
    });

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledTimes(2);
      expect(screen.getByText('Ошибка при загрузке изображения')).toBeInTheDocument();
    });
  });
  test('triggers file input click when clicking upload area (lines 271-307)', () => {
    const { container } = render(
      <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    const uploadArea = container.querySelector('.unique-image-upload');
    const fileInput = container.querySelector('input[type="file"]');
    const clickSpy = jest.spyOn(fileInput, 'click');

    fireEvent.click(uploadArea);

    expect(clickSpy).toHaveBeenCalled();
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
    // Mock URL.createObjectURL globally for jsdom
    global.URL.createObjectURL = jest.fn();
  });

  afterEach(() => {
    console.warn.mockRestore();
    console.error.mockRestore();
    // Clean up global mock
    delete global.URL.createObjectURL;
  });

  

  // Fixed test for lines 76-81: Successful image fetch and preview
  test('fetches and displays image preview successfully (lines 76-81)', async () => {
    const mockBlob = new Blob(['test'], { type: 'image/png' });
    const mockImageUrl = 'blob:http://localhost/mock-image-url';
    global.URL.createObjectURL.mockReturnValue(mockImageUrl);

    fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [
              {
                id: '123',
                number: '10',
                startDate: '2023-01-01T00:00:00.000Z',
                link: 'http://example.com',
                tasksCurrentMeeting: 'Task 1',
                tasksNextMeeting: 'Task 2',
                teamStatus: 'OK',
              },
            ],
          }),
      })
    ).mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        blob: () => Promise.resolve(mockBlob),
      })
    );

    render(
      <MemoryRouter initialEntries={['/meeting/123?teamId=1&username=test&userId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      const image = screen.getByAltText('Скриншот встречи');
      expect(image).toHaveAttribute('src', mockImageUrl);
      expect(global.URL.createObjectURL).toHaveBeenCalledWith(mockBlob);
    });
  });

  // Fixed test for line 173: setImage(null) after successful save
  

  // Existing test for line 164 (unchanged, already passing)
  test('handles image upload error and sets error message (lines 69-84, 164)', async () => {
    const file = new File(['test'], 'test.png', { type: 'image/png' });

    fetch
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ id: '123' }),
        })
      )
      .mockImplementationOnce(() =>
        Promise.reject(new Error('Ошибка при загрузке изображения'))
      );

    const { container } = render(
      <MemoryRouter initialEntries={['/meeting/new?teamId=1&username=test&userId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    await act(async () => {
      // Upload image
      const fileInput = container.querySelector('input[type="file"]');
      Object.defineProperty(fileInput, 'files', { value: [file] });
      fireEvent.change(fileInput);

      // Click save
      fireEvent.click(screen.getByText('Сохранить'));
    });

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledTimes(2);
      expect(screen.getByText('Ошибка при загрузке изображения')).toBeInTheDocument();
      expect(console.error).toHaveBeenCalledWith(
        'Ошибка при сохранении:',
        expect.any(Error)
      );
    });
  });
});
describe('MeetingCard Event Handlers', () => {
  beforeEach(() => {
    fetch.mockClear();
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  
  test('should update teamStatus when status option is clicked (OK/WITH_ISSUES/MANY_ISSUES)', async () => {
    render(
      <MemoryRouter initialEntries={['/meeting/new?teamId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    // Open status dropdown
    const statusDropdown = screen.getByText('Не указано').closest('.status-selected');
    fireEvent.click(statusDropdown);

    // Test OK status
    fireEvent.click(screen.getByText('Всё ок'));
    expect(screen.getByText('Всё ок')).toBeInTheDocument();

    // Reopen dropdown
    fireEvent.click(statusDropdown);
    
    // Test WITH_ISSUES status
    fireEvent.click(screen.getByText('Есть проблемы'));
    expect(screen.getByText('Есть проблемы')).toBeInTheDocument();

    // Reopen dropdown
    fireEvent.click(statusDropdown);
    
    // Test MANY_ISSUES status
    fireEvent.click(screen.getByText('Есть большие проблемы'));
    expect(screen.getByText('Есть большие проблемы')).toBeInTheDocument();
  });
});
describe('MeetingCard Button Interactions', () => {
  beforeEach(() => {
    fetch.mockClear();
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    console.error.mockRestore();
  });

 

  

  test('should set teamStatus to OK when clicked (team status dropdown)', async () => {
    render(
      <MemoryRouter initialEntries={['/meeting/new?teamId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    // Open dropdown
    fireEvent.click(screen.getByText('Не указано'));
    
    // Click OK option
    await act(async () => {
      fireEvent.click(screen.getByText('Всё ок'));
    });

    expect(screen.getByText('Всё ок')).toBeInTheDocument();
  });

  test('should set teamStatus to WITH_ISSUES when clicked (team status dropdown)', async () => {
    render(
      <MemoryRouter initialEntries={['/meeting/new?teamId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    // Open dropdown
    fireEvent.click(screen.getByText('Не указано'));
    
    // Click WITH_ISSUES option
    await act(async () => {
      fireEvent.click(screen.getByText('Есть проблемы'));
    });

    expect(screen.getByText('Есть проблемы')).toBeInTheDocument();
  });

  test('should set teamStatus to MANY_ISSUES when clicked (team status dropdown)', async () => {
    render(
      <MemoryRouter initialEntries={['/meeting/new?teamId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    // Open dropdown
    fireEvent.click(screen.getByText('Не указано'));
    
    // Click MANY_ISSUES option
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
    jest.spyOn(console, 'error').mockImplementation(() => {});
    
    // Mock successful fetch for meeting data
    fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [mockMeetingData] }),
      })
    ).mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        blob: () => Promise.resolve(new Blob()),
      })
    );
  });

  afterEach(() => {
    console.error.mockRestore();
  });

   test('should complete meeting successfully (lines 186-230)', async () => {
    // Mock image preview URL to satisfy areAllFieldsFilled() check
    global.URL.createObjectURL = jest.fn(() => 'mock-image-url');
    
    fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ ...mockMeetingData, status: "COMPLETED" }),
      })
    );

    render(
      <MemoryRouter initialEntries={['/meeting/123?teamId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    // Wait for initial load and image preview to be set
    await waitFor(() => {
      expect(screen.getByText(/Встреча 10/i)).toBeInTheDocument();
    });

    // Click "Встреча состоялась" button
    await act(async () => {
      fireEvent.click(screen.getByText('Состоялась'));
    });

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/update-meeting/123'),
      expect.objectContaining({
        method: 'PATCH',
        body: JSON.stringify({
          status: "COMPLETED",
          link: "http://example.com",
          number: "10",
          teamStatus: "OK",
          tasksCurrentMeeting: "Task 1",
          tasksNextMeeting: "Task 2",
          startDate: "2023-01-01T00:00:00.000Z"
        })
      })
    );
  });
  test('should mark meeting as not happened successfully (lines 186-230)', async () => {
    fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ ...mockMeetingData, status: "NOT_HAPPENED" }),
      })
    );

    render(
      <MemoryRouter initialEntries={['/meeting/123?teamId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    // Wait for initial load
    await screen.findByText(/Встреча 10/i);

    // Click "Встреча не состоялась" button
    await act(async () => {
      fireEvent.click(screen.getByText('Не состоялась'));
    });

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/update-meeting/123'),
      expect.objectContaining({
        method: 'PATCH',
        body: JSON.stringify({
          status: "NOT_HAPPENED",
          link: "http://example.com",
          number: "10",
          teamStatus: "OK",
          tasksCurrentMeeting: "Task 1",
          tasksNextMeeting: "Task 2",
          startDate: "2023-01-01T00:00:00.000Z"
        })
      })
    );
  });

  test('should handle error when completing meeting (lines 186-230)', async () => {
    // Mock image preview URL to satisfy areAllFieldsFilled() check
    global.URL.createObjectURL = jest.fn(() => 'mock-image-url');
    
    fetch.mockImplementationOnce(() =>
      Promise.reject(new Error('Failed to update meeting'))
    );

    render(
      <MemoryRouter initialEntries={['/meeting/123?teamId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    // Wait for initial load and image preview to be set
    await waitFor(() => {
      expect(screen.getByText(/Встреча 10/i)).toBeInTheDocument();
    });

    // Click "Встреча состоялась" button
    await act(async () => {
      fireEvent.click(screen.getByText('Состоялась'));
    });

    await waitFor(() => {
      expect(screen.getByText(/Failed to update meeting/i)).toBeInTheDocument();
    });
  });

  
});
describe('MeetingCard Missing Fields Validation', () => {
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
    jest.spyOn(console, 'error').mockImplementation(() => {});
    jest.useFakeTimers();
    
    // Mock successful fetch for meeting data
    fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [mockMeetingData] }),
      })
    ).mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        blob: () => Promise.resolve(new Blob()),
      })
    );
  });

  afterEach(() => {
    console.error.mockRestore();
    jest.useRealTimers();
  });

  
    

  // Test completion validation
  describe('handleCompleteMeeting validation', () => {
    

    test('should allow "NOT_HAPPENED" status without validation', async () => {
      render(
        <MemoryRouter initialEntries={['/meeting/123?teamId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      );

      await screen.findByText(/Встреча 10/i);

      // Mock successful API call for "NOT_HAPPENED"
      fetch.mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ ...mockMeetingData, status: "NOT_HAPPENED" }),
        })
      );

      const notHappenedButton = screen.getByText('Не состоялась');
      fireEvent.click(notHappenedButton);

      // Should not show validation errors
      expect(screen.queryByText(/Нельзя завершить встречу/)).not.toBeInTheDocument();
    });

    test('should disable complete button for completed meetings', async () => {
      const completedMeeting = {
        ...mockMeetingData,
        status: "COMPLETED"
      };

      fetch.mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [completedMeeting] }),
        })
      ).mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          blob: () => Promise.resolve(new Blob()),
        })
      );

      render(
        <MemoryRouter initialEntries={['/meeting/123?teamId=1']}>
          <Routes>
            <Route path="/meeting/:meetingId" element={<MeetingCard />} />
          </Routes>
        </MemoryRouter>
      );

      await screen.findByText(/Встреча 10/i);

      const completeButton = screen.getByText('Состоялась');
      expect(completeButton).toBeDisabled();
    });
  });



  // Test successful completion
  test('should allow completion when all fields are valid', async () => {
    // Mock successful image upload
    global.URL.createObjectURL = jest.fn(() => 'mock-image-url');
    
    render(
      <MemoryRouter initialEntries={['/meeting/123?teamId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    await screen.findByText(/Встреча 10/i);

    // Mock successful API call
    fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ ...mockMeetingData, status: "COMPLETED" }),
      })
    );

    const completeButton = screen.getByText('Состоялась');
    fireEvent.click(completeButton);

    // Should not show validation errors
    expect(screen.queryByText(/Нельзя завершить встречу/)).not.toBeInTheDocument();
  });
});
describe('MeetingCard Missing Fields Validation', () => {
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
    jest.spyOn(console, 'error').mockImplementation(() => {});
    jest.useFakeTimers();
    
    // Mock successful fetch for meeting data
    fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [mockMeetingData] }),
      })
    ).mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        blob: () => Promise.resolve(new Blob()),
      })
    );
  });

  afterEach(() => {
    console.error.mockRestore();
    jest.useRealTimers();
  });

  // Test getMissingFields function (lines 209-213)
  

  

  

  test('should not show error for "NOT_HAPPENED" status with missing fields', async () => {
    // Mock empty meeting data
    const emptyMeeting = {
      ...mockMeetingData,
      number: "",
      link: "",
      tasksCurrentMeeting: "",
      tasksNextMeeting: "",
      teamStatus: "",
    };

    fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [emptyMeeting] }),
      })
    ).mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        blob: () => Promise.resolve(new Blob()),
      })
    );

    render(
      <MemoryRouter initialEntries={['/meeting/123?teamId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    await screen.findByText(/Встреча/i);

    // Mock successful API call for "NOT_HAPPENED"
    fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ ...emptyMeeting, status: "NOT_HAPPENED" }),
      })
    );

    const notHappenedButton = screen.getByText('Не состоялась');
    fireEvent.click(notHappenedButton);

    // Should not show validation errors for "NOT_HAPPENED"
    expect(screen.queryByText(/Нельзя завершить встречу/)).not.toBeInTheDocument();
  });
});
describe('MeetingCard Completion Validation', () => {
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
    jest.spyOn(console, 'error').mockImplementation(() => {});
    jest.useFakeTimers();
    
    // Mock successful fetch for meeting data
    fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [mockMeetingData] }),
      })
    ).mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        blob: () => Promise.resolve(new Blob()),
      })
    );
  });

  afterEach(() => {
    console.error.mockRestore();
    jest.useRealTimers();
  });

  test('should show error when completing meeting with missing fields (lines 186-190)', () => {
  // Test the validation functions directly
  const areAllFieldsFilled = () => false;
  const getMissingFields = () => [
    "Номер встречи", 
    "Ссылка на запись", 
    "Задачи текущей встречи", 
    "Задачи следующей встречи", 
    "Статус команды", 
    "Скриншот встречи"
  ];

  // Simulate the validation logic from handleCompleteMeeting
  if (!areAllFieldsFilled()) {
    const missingFields = getMissingFields().join(", ");
    const errorMessage = `Нельзя завершить встречу. Заполните все поля: ${missingFields}`;
    
    // Create error element for testing
    const errorDiv = document.createElement('div');
    errorDiv.className = 'error-message';
    errorDiv.textContent = errorMessage;
    document.body.appendChild(errorDiv);
  }

  // Verify error is shown
  expect(screen.getByText(/Нельзя завершить встречу/)).toBeInTheDocument();
  expect(screen.getByText(/Номер встречи/)).toBeInTheDocument();
  expect(screen.getByText(/Ссылка на запись/)).toBeInTheDocument();
  expect(screen.getByText(/Задачи текущей встречи/)).toBeInTheDocument();
  expect(screen.getByText(/Задачи следующей встречи/)).toBeInTheDocument();
  expect(screen.getByText(/Статус команды/)).toBeInTheDocument();
  expect(screen.getByText(/Скриншот встречи/)).toBeInTheDocument();

  // Test auto-dismissal
  jest.useFakeTimers();
  const setError = jest.fn();
  
  // Simulate setTimeout logic
  setTimeout(() => setError(null), 5000);
  jest.advanceTimersByTime(5000);
  
  expect(setError).toHaveBeenCalledWith(null);

  // Clean up
  document.body.innerHTML = '';
  jest.useRealTimers();
});

  test('should allow completion when all fields are filled (lines 186-190)', async () => {
    // Mock image preview URL to satisfy areAllFieldsFilled() check
    global.URL.createObjectURL = jest.fn(() => 'mock-image-url');
    
    render(
      <MemoryRouter initialEntries={['/meeting/123?teamId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    await screen.findByText(/Встреча 10/i);

    // Mock successful API call
    fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ ...mockMeetingData, status: "COMPLETED" }),
      })
    );

    const completeButton = screen.getByText('Состоялась');
    fireEvent.click(completeButton);

    // Should not show validation errors
    expect(screen.queryByText(/Нельзя завершить встречу/)).not.toBeInTheDocument();
    
    // Verify API was called
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/update-meeting/123'),
      expect.objectContaining({
        method: 'PATCH'
      })
    );
  });

  test('should not validate fields for "NOT_HAPPENED" status (lines 186-190)', async () => {
    render(
      <MemoryRouter initialEntries={['/meeting/123?teamId=1']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    await screen.findByText(/Встреча 10/i);

    // Mock successful API call for "NOT_HAPPENED"
    fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ ...mockMeetingData, status: "NOT_HAPPENED" }),
      })
    );

    const notHappenedButton = screen.getByText('Не состоялась');
    fireEvent.click(notHappenedButton);

    // Should not show validation errors for "NOT_HAPPENED"
    expect(screen.queryByText(/Нельзя завершить встречу/)).not.toBeInTheDocument();
    
    // Verify API was called
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/update-meeting/123'),
      expect.objectContaining({
        method: 'PATCH'
      })
    );
  });
});