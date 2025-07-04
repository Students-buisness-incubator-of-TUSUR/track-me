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

    it('должен обрабатывать ошибку API при создании', async () => {
      global.fetch.mockImplementation((url) => {
        if (typeof url === 'string' && url.includes('api/v1/meetings?teamCardId=team123')) {
          return Promise.resolve({
            ok: false,
            text: () => Promise.resolve('Ошибка валидации'),
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
        expect(screen.getByText('Ошибка валидации')).toBeInTheDocument();
      });
    });

    
  });
});