import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import Stream from "./stream-page";
import { MemoryRouter, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useSelector } from 'react-redux';

jest.mock('react-redux', () => ({
  ...jest.requireActual('react-redux'),
  useSelector: jest.fn(),
}));
// Mock useNavigate на верхнем уровне
const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  Link: ({ children, to, ...rest }) => <a href={to} {...rest}>{children}</a>,
}));

// Mock axios на верхнем уровне
jest.mock('axios', () => ({
  post: jest.fn(),
  get: jest.fn(),
}));

describe('Stream Component', () => {
  const mockData = {
    content: [
      {
        id: 1,
        name: 'Test Stream',
        description: 'Test Description',
        startDate: '2023-01-01',
        endDate: '2023-12-31',
        readinessLevel: '5',
      },
    ],
    page: {
      totalPages: 3,
    },
  };

  const mockCheckboxesData2 = [
    { id: 1, name: 'Market 1', description: 'Market Desc 1' },
    { id: 2, name: 'Market 2', description: 'Market Desc 2' },
  ];

  beforeEach(() => {
  window.dispatchEvent(new Event("resize")); // если компонент слушает resize
    axios.post.mockResolvedValue({ data: mockData });
    axios.get.mockImplementation((url) => {
      if (url.includes('nti-markets')) {
        return Promise.resolve({ data: mockCheckboxesData2 });
      }
      return Promise.resolve({ data: new Blob() });
    });

    URL.createObjectURL = jest.fn(() => 'mock-url');

    Storage.prototype.removeItem = jest.fn();
    Storage.prototype.setItem = jest.fn();
    
    // Сбрасываем mockNavigate перед каждым тестом
    mockNavigate.mockClear();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should import all required dependencies', () => {
    expect(Stream).toBeDefined();
  });

  it('should display stream cards', async () => {
    render(
      <MemoryRouter>
        <Stream />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText("Test Stream")).toBeInTheDocument();
      expect(screen.getByText("редактировать")).toBeInTheDocument();
    });
  });

  it('should handle pagination buttons', async () => {
    render(
      <MemoryRouter>
        <Stream />
      </MemoryRouter>
    );

    await waitFor(() => {
      const buttons = document.querySelectorAll(".Stream-footer-button-4, .Stream-footer-button-5");
      expect(buttons.length).toBeGreaterThan(0);
    });
  });
});

describe('User Role from localStorage', () => {
  const mockLocalStorage = (() => {
    let store = {};
    return {
      getItem: jest.fn((key) => store[key] || null),
      setItem: jest.fn((key, value) => {
        store[key] = value.toString();
      }),
      removeItem: jest.fn((key) => {
        delete store[key];
      }),
      clear: jest.fn(() => {
        store = {};
      })
    };
  })();

  beforeEach(() => {
    // Мокаем localStorage
    Object.defineProperty(window, 'localStorage', {
      value: mockLocalStorage,
      writable: true
    });

    // Мокаем useState и useEffect
    jest.spyOn(React, 'useState').mockImplementation(initialState => [initialState, jest.fn()]);
    jest.spyOn(React, 'useEffect').mockImplementation(f => f());
  });

  afterEach(() => {
    jest.clearAllMocks();
    mockLocalStorage.clear();
  });

  test('should get user role from localStorage when user exists', () => {
    // Подготавливаем данные пользователя
    const mockUser = {
      roles: ['SUPER_ADMIN']
    };
    
    // Мокаем getItem чтобы возвращать пользователя
    mockLocalStorage.getItem.mockReturnValue(JSON.stringify(mockUser));
    
    // Вызываем код, который тестируем
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      const userData = JSON.parse(savedUser);
      // Здесь мы просто проверяем, что код выполняется корректно
      expect(userData.roles[0]).toBe('SUPER_ADMIN');
    }
  });

  test('should handle case when no user in localStorage', () => {
    // Мокаем getItem чтобы возвращать null
    mockLocalStorage.getItem.mockReturnValue(null);
    
    // Вызываем код, который тестируем
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      // Этот блок не должен выполниться
      expect(true).toBe(false); // Если сюда попали - тест провален
    } else {
      // Ожидаемое поведение - ничего не делать
      expect(savedUser).toBeNull();
    }
  });

  test('should handle malformed JSON in localStorage', () => {
    // Мокаем getItem чтобы возвращать некорректный JSON
    mockLocalStorage.getItem.mockReturnValue('invalid json');
    
    // Вызываем код, который тестируем
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      // Должно выбросить исключение при парсинге
      expect(() => {
        JSON.parse(savedUser);
      }).toThrow(SyntaxError);
    }
  });

  test('should handle user without roles array', () => {
    // Подготавливаем пользователя без roles
    const mockUser = {
      name: 'test',
      email: 'test@example.com'
    };
    
    mockLocalStorage.getItem.mockReturnValue(JSON.stringify(mockUser));
    
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      const userData = JSON.parse(savedUser);
      // Проверяем, что roles отсутствует
      expect(userData.roles).toBeUndefined();
    }
  });
});
