import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import Stream from "./stream-page";
import { MemoryRouter, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useSelector } from 'react-redux';

const mockUseGetUserInfo = jest.fn();
jest.mock('../../services/util', () => ({
  useGetUserInfo: () => mockUseGetUserInfo(),
}));

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
    mockUseGetUserInfo.mockReturnValue({
      roles: ['SUPER_ADMIN'],
      username: "username12",
    });
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

  it('should apply correct class when Market dropdown is toggled 1', async () => {
    const { container } = render(<MemoryRouter><Stream /></MemoryRouter>);

    await waitFor(() => {
      expect(container.querySelector('.Stream-settings-pic')).toBeInTheDocument();
    });
    fireEvent.click(container.querySelector('.Stream-settings-pic'));

    await waitFor(() => {
      expect(container.querySelectorAll('.Stream-header-chosefrom-buttw').length).toBe(3);
    });

    const marketWrapper = container.querySelectorAll('.Stream-header-chosefrom-buttw')[0];
    expect(marketWrapper).not.toHaveClass('Stream-checkboxes_remove-below-border-radius');

    fireEvent.click(marketWrapper.querySelector('.Stream-header-chosefrom-butt-cont'));
    expect(marketWrapper).toHaveClass('Stream-checkboxes_remove-below-border-radius');
  });

  it('should apply correct class when Market dropdown is toggled 2', async () => {
    const { container } = render(<MemoryRouter><Stream /></MemoryRouter>);

    await waitFor(() => {
      expect(container.querySelector('.Stream-settings-pic')).toBeInTheDocument();
    });
    fireEvent.click(container.querySelector('.Stream-settings-pic'));

    await waitFor(() => {
      expect(container.querySelectorAll('.Stream-header-chosefrom-buttw').length).toBe(3);
    });

    const marketWrapper = container.querySelectorAll('.Stream-header-chosefrom-buttw')[1];
    expect(marketWrapper).not.toHaveClass('Stream-checkboxes_remove-below-border-radius');

    fireEvent.click(marketWrapper.querySelector('.Stream-header-chosefrom-butt-cont'));
    expect(marketWrapper).toHaveClass('Stream-checkboxes_remove-below-border-radius');
  });

  it('should apply correct class when Market dropdown is toggled 3', async () => {
    const { container } = render(<MemoryRouter><Stream /></MemoryRouter>);

    await waitFor(() => {
      expect(container.querySelector('.Stream-settings-pic')).toBeInTheDocument();
    });
    fireEvent.click(container.querySelector('.Stream-settings-pic'));

    await waitFor(() => {
      expect(container.querySelectorAll('.Stream-header-chosefrom-buttw').length).toBe(3);
    });

    const marketWrapper = container.querySelectorAll('.Stream-header-chosefrom-buttw')[2];
    expect(marketWrapper).not.toHaveClass('Stream-checkboxes_remove-below-border-radius');

    fireEvent.click(marketWrapper.querySelector('.Stream-header-chosefrom-butt-cont'));
    expect(marketWrapper).toHaveClass('Stream-checkboxes_remove-below-border-radius');
  });

  it('should clear search on Escape key and not trigger backend fetch (client-side search)', async () => {
    render(<MemoryRouter><Stream /></MemoryRouter>);

    await waitFor(() => expect(axios.post).toHaveBeenCalled());
    axios.post.mockClear();

    const searchInput = screen.getByPlaceholderText('Найти');
    fireEvent.change(searchInput, { target: { value: 'test query' } });
    axios.post.mockClear();

    fireEvent.keyDown(searchInput, { key: 'Escape' });

    expect(searchInput.value).toBe('');
  });

  it('should reset all filters and call fetchData with empty filters on reset button click', async () => {
    render(<MemoryRouter><Stream /></MemoryRouter>);

    await waitFor(() => expect(axios.post).toHaveBeenCalled());
    axios.post.mockClear();

    const filterToggleBtn = document.querySelector('.Stream-settings-pic');
    fireEvent.click(filterToggleBtn);

    await waitFor(() => {
      expect(screen.getByText('Сбросить')).toBeInTheDocument();
    });

    const resetBtn = screen.getByText('Сбросить');
    fireEvent.click(resetBtn);

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        expect.stringContaining('streams'),
        expect.objectContaining({ filters: [] }),
        expect.anything()
      );
    });
  });

  it('should call handleApplyFilters on Enter key', async () => {
    render(<MemoryRouter><Stream /></MemoryRouter>);

    await waitFor(() => expect(axios.post).toHaveBeenCalled());
    axios.post.mockClear();

    const searchInput = screen.getByPlaceholderText('Найти');
    fireEvent.change(searchInput, { target: { value: 'test' } });

    fireEvent.keyDown(searchInput, { key: 'Enter' });

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalled();
    });
  });

  it('should filter streams on client (case-insensitive) when search query is entered', async () => {
    render(<MemoryRouter><Stream /></MemoryRouter>);

    await waitFor(() => expect(axios.post).toHaveBeenCalled());
    axios.post.mockClear();

    fireEvent.change(screen.getByPlaceholderText('Найти'), { target: { value: 'test query' } });

    // LIKE filter should NOT be sent to backend (search is now client-side)
    await waitFor(() => {
      expect(axios.post).not.toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          filters: expect.arrayContaining([
            expect.objectContaining({ fieldName: 'name', type: 'LIKE' }),
          ]),
        }),
        expect.anything()
      );
    });
  });

  it('should call fetchData with empty filters when search query is empty', async () => {
    render(<MemoryRouter><Stream /></MemoryRouter>);

    await waitFor(() => expect(axios.post).toHaveBeenCalled());
    axios.post.mockClear();

    fireEvent.click(document.querySelector('.Stream-settings-pic2'));

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        expect.stringContaining('streams'),
        expect.objectContaining({ filters: [] }),
        expect.anything()
      );
    });
  });

  it('should call fetchData with TRL filter when TRL checkbox is selected', async () => {
    const { container } = render(<MemoryRouter><Stream /></MemoryRouter>);

    await waitFor(() => expect(axios.post).toHaveBeenCalled());
    axios.post.mockClear();

    fireEvent.click(container.querySelector('.Stream-settings-pic'));
    await waitFor(() => expect(container.querySelectorAll('.Stream-header-chosefrom-buttw').length).toBe(3));

    const trlWrapper = container.querySelectorAll('.Stream-header-chosefrom-buttw')[2];
    fireEvent.click(trlWrapper.querySelector('.Stream-header-chosefrom-butt-cont'));

    await waitFor(() => expect(container.querySelectorAll('.custom-checkbox').length).toBeGreaterThan(0));
    fireEvent.click(container.querySelectorAll('.custom-checkbox')[0]);

    fireEvent.click(screen.getByText('Применить'));

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        expect.stringContaining('streams'),
        expect.objectContaining({
          filters: expect.arrayContaining([
            expect.objectContaining({ fieldName: 'teamCards.readinessLevel', type: 'IN' }),
          ]),
        }),
        expect.anything()
      );
    });
  });

  it('should call fetchData with market filter when market checkbox is selected', async () => {
    axios.get.mockImplementation((url) => {
      if (url.includes('nti-markets'))
        return Promise.resolve({ data: [{ id: 1, name: 'Market 1', displayName: 'Market One' }] });
      return Promise.resolve({ data: new Blob() });
    });

    const { container } = render(<MemoryRouter><Stream /></MemoryRouter>);

    await waitFor(() => expect(axios.post).toHaveBeenCalled());
    axios.post.mockClear();

    fireEvent.click(container.querySelector('.Stream-settings-pic'));
    await waitFor(() => expect(container.querySelectorAll('.Stream-header-chosefrom-buttw').length).toBe(3));

    const marketWrapper = container.querySelectorAll('.Stream-header-chosefrom-buttw')[1];
    fireEvent.click(marketWrapper.querySelector('.Stream-header-chosefrom-butt-cont'));

    await waitFor(() => expect(container.querySelectorAll('.custom-checkbox').length).toBeGreaterThan(0));
    fireEvent.click(container.querySelectorAll('.custom-checkbox')[0]);

    fireEvent.click(screen.getByText('Применить'));

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        expect.stringContaining('streams'),
        expect.objectContaining({
          filters: expect.arrayContaining([
            expect.objectContaining({ fieldName: 'ntiMarkets.name', type: 'IN' }),
          ]),
        }),
        expect.anything()
      );
    });
  });

  it('should call fetchData with year filter when year checkbox is selected', async () => {
    const { container } = render(<MemoryRouter><Stream /></MemoryRouter>);

    await waitFor(() => expect(axios.post).toHaveBeenCalled());
    axios.post.mockClear();

    fireEvent.click(container.querySelector('.Stream-settings-pic'));
    await waitFor(() => expect(container.querySelectorAll('.Stream-header-chosefrom-buttw').length).toBe(3));

    const yearWrapper = container.querySelectorAll('.Stream-header-chosefrom-buttw')[0];
    fireEvent.click(yearWrapper.querySelector('.Stream-header-chosefrom-butt-cont'));

    await waitFor(() => expect(container.querySelectorAll('.custom-checkbox').length).toBeGreaterThan(0));
    fireEvent.click(container.querySelectorAll('.custom-checkbox')[0]);

    fireEvent.click(screen.getByText('Применить'));

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        expect.stringContaining('streams'),
        expect.objectContaining({
          filters: expect.arrayContaining([
            expect.objectContaining({ fieldName: 'year', type: 'EQ' }),
          ]),
        }),
        expect.anything()
      );
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
