import React from 'react';
import { render, screen, fireEvent, act, waitFor } from '@testing-library/react';
import { useNavigate, useLocation } from 'react-router-dom';
import MeetingCreate from './MeetingCreate';
import '@testing-library/jest-dom';

// Мокируем react-router-dom
jest.mock('react-router-dom', () => ({
  useNavigate: jest.fn(),
  useLocation: jest.fn(),
}));

// Мокируем глобальный fetch
global.fetch = jest.fn();

describe('Компонент MeetingCreate', () => {
  const mockOnClose = jest.fn();
  const mockNavigate = jest.fn();
  const teamId = 'team123';

  beforeEach(() => {
    jest.clearAllMocks();
    
    // Мокируем useLocation и useNavigate
    useLocation.mockReturnValue({
      search: '?userId=testUser123',
    });
    useNavigate.mockReturnValue(mockNavigate);

    // Устанавливаем переменную окружения, которую ожидает компонент
    process.env.REACT_APP_BACKEND_URI = 'http://localhost:8080';

    // Мокируем успешный запрос встреч по умолчанию
    global.fetch.mockImplementation((url) => {
      if (typeof url === 'string' && url.includes('api/v1/meetings')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [] }),
        });
      }
      return Promise.reject(new Error('Неожиданный URL'));
    });
  });

  afterEach(() => {
    delete process.env.REACT_APP_BACKEND_URI;
  });

  describe('Инициализация и загрузка встреч (строки 23-45)', () => {
    
    it('должен устанавливать номер встречи 1 если нет других встреч', async () => {
      global.fetch.mockImplementation((url) => {
        if (typeof url === 'string' && url.includes('api/v1/meetings')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ content: [] }),
          });
        }
        return Promise.reject(new Error('Неожиданный URL'));
      });

      render(<MeetingCreate onClose={mockOnClose} teamId={teamId} />);

      await waitFor(() => {
        expect(screen.getByText(/Запланировать встречу #1/)).toBeInTheDocument();
      });
    });

    it('должен обрабатывать ошибку загрузки встреч', async () => {
      global.fetch.mockImplementation(() => 
        Promise.reject(new Error('Ошибка загрузки'))
      );

      render(<MeetingCreate onClose={mockOnClose} teamId={teamId} />);

      await waitFor(() => {
        expect(screen.getByText('Не удалось загрузить список встреч')).toBeInTheDocument();
      });
    });
  });

  describe('Обработка клика вне попапа (строки 70-71)', () => {
    it('должен вызывать onClose при клике вне попапа', async () => {
      render(
        <div>
          <div data-testid="outside-element">Снаружи</div>
          <MeetingCreate onClose={mockOnClose} teamId={teamId} />
        </div>
      );

      fireEvent.mouseDown(screen.getByTestId('outside-element'));
      expect(mockOnClose).toHaveBeenCalled();
    });

    it('не должен вызывать onClose при клике внутри попапа', async () => {
      render(<MeetingCreate onClose={mockOnClose} teamId={teamId} />);

      const createButton = await screen.findByText('Создать');
      fireEvent.mouseDown(createButton);
      expect(mockOnClose).not.toHaveBeenCalled();
    });
  });

  

  describe('Создание встречи', () => {
    it('должен успешно создавать встречу', async () => {
      const mockResponse = { id: 'meeting123' };
      
      global.fetch.mockImplementation((url) => {
        if (typeof url === 'string' && url.includes('api/v1/meetings?teamCardId=team123')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockResponse),
          });
        }
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [] }),
        });
      });

      render(<MeetingCreate onClose={mockOnClose} teamId={teamId} />);

      const createButton = await screen.findByText('Создать');
      fireEvent.click(createButton);

      await waitFor(() => {
        expect(mockOnClose).toHaveBeenCalled();
        expect(mockNavigate).toHaveBeenCalledWith(
          '/meeting/meeting123?teamId=team123&username=testUser123'
        );
      });
    });

    it('должен обрабатывать ошибку API при создании встречи с сообщением', async () => {
    global.fetch.mockImplementation((url) => {
      if (typeof url === 'string' && url.includes('api/v1/meetings?teamCardId=team123')) {
        return Promise.resolve({
          ok: false,
          json: () => Promise.resolve({ message: 'Не удалось создать встречу' }),
        });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [] }),
      });
    });

    render(<MeetingCreate onClose={mockOnClose} teamId={teamId} />);

    const createButton = await screen.findByText('Создать');
    fireEvent.click(createButton);

    await waitFor(() => {
      expect(screen.getByText('Не удалось создать встречу')).toBeInTheDocument();
      expect(mockOnClose).not.toHaveBeenCalled();
      expect(mockNavigate).not.toHaveBeenCalled();
    });
  });

  
  

    
  });
  describe('Error handling for meeting creation (lines 98-110)', () => {
    it('should handle server error when creating meeting', async () => {
      global.fetch.mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [] }),
        })
      ).mockImplementationOnce(() =>
        Promise.resolve({
          ok: false,
          json: () => Promise.resolve({ message: 'Creation failed' }),
        })
      );

      render(<MeetingCreate onClose={mockOnClose} teamId={teamId} />);

      const createButton = await screen.findByText('Создать');
      fireEvent.click(createButton);

      await waitFor(() => {
        expect(screen.getByText('Creation failed')).toBeInTheDocument();
      });
    });

    it('should show default error when no message from server on creation', async () => {
      global.fetch.mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [] }),
        })
      ).mockImplementationOnce(() =>
        Promise.resolve({
          ok: false,
          json: () => Promise.resolve({}),
        })
      );

      render(<MeetingCreate onClose={mockOnClose} teamId={teamId} />);

      const createButton = await screen.findByText('Создать');
      fireEvent.click(createButton);

      await waitFor(() => {
        expect(screen.getByText('Ошибка при создании встречи')).toBeInTheDocument();
      });
    });
  });
  describe('Валидация данных встречи', () => {
  

  it('должен показывать ошибку если дата встречи в прошлом', async () => {
    render(<MeetingCreate onClose={mockOnClose} teamId={teamId} />);

    // Simulate a past date
    const pastDate = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString().slice(0, 16);
    const dateInput = screen.getByDisplayValue(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/);
    fireEvent.change(dateInput, { target: { value: pastDate, name: 'startDate' } });

    const createButton = await screen.findByText('Создать');
    fireEvent.click(createButton);

    await waitFor(() => {
      expect(screen.getByText('Дата встречи должна быть в будущем')).toBeInTheDocument();
    });
  });
});
it('должен очищать ошибку при изменении поля ввода', async () => {
  global.fetch.mockImplementation((url) => {
    if (typeof url === 'string' && url.includes('api/v1/meetings?teamCardId=team123')) {
      return Promise.resolve({
        ok: false,
        json: () => Promise.resolve({ message: 'Не удалось создать встречу' }),
      });
    }
    return Promise.resolve({
      ok: true,
      json: () => Promise.resolve({ content: [] }),
    });
  });

  render(<MeetingCreate onClose={mockOnClose} teamId={teamId} />);

  // Trigger an error by attempting to create a meeting
  const createButton = await screen.findByText('Создать');
  fireEvent.click(createButton);

  await waitFor(() => {
    expect(screen.getByText('Не удалось создать встречу')).toBeInTheDocument();
  });

  // Change the date input
  const dateInput = screen.getByDisplayValue(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/);
  fireEvent.change(dateInput, {
    target: { value: new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString().slice(0, 16), name: 'startDate' },
  });

  await waitFor(() => {
    expect(screen.queryByText('Не удалось создать встречу')).not.toBeInTheDocument();
  });
});

});