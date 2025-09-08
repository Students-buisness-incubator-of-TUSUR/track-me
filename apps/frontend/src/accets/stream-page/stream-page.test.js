import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import Stream from "./stream-page";
import { MemoryRouter, useNavigate } from 'react-router-dom';
import axios from 'axios';

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

  it('should toggle profile menu', async () => {
    render(
      <MemoryRouter>
        <Stream />
      </MemoryRouter>
    );

    await waitFor(() => screen.getByText("TrackMe"));

    const profileButton = document.querySelector(".Stream-pic");
    fireEvent.click(profileButton);
    expect(screen.getByText('Личный кабинет')).toBeInTheDocument();

    fireEvent.click(profileButton);
    await waitFor(() => {
      expect(screen.queryByText('Личный кабинет')).not.toBeInTheDocument();
    });
  });

  it('should clear localStorage on logout', async () => {
    render(
      <MemoryRouter>
        <Stream />
      </MemoryRouter>
    );

    await waitFor(() => screen.getByText("TrackMe"));

    const profileButton = document.querySelector(".Stream-pic");
    fireEvent.click(profileButton);

    const logoutLink = screen.getByText('Выход');
    fireEvent.click(logoutLink);

    expect(localStorage.removeItem).toHaveBeenCalledWith("user");
    expect(localStorage.removeItem).toHaveBeenCalledWith("userRole");
    expect(localStorage.removeItem).toHaveBeenCalledWith("streamName");
    expect(localStorage.removeItem).toHaveBeenCalledWith("streamId");
    expect(localStorage.removeItem).toHaveBeenCalledWith("streamSDate");
    expect(localStorage.removeItem).toHaveBeenCalledWith("streamEDate");
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

  // Тесты для логотипа и заголовка
  test('logo navigates to /streams on click', async () => {
    render(
      <MemoryRouter>
        <Stream />
      </MemoryRouter>
    );

    await waitFor(() => {
      const logo = document.querySelector('.Stream-header-logo');
      expect(logo).toBeInTheDocument();
      
      fireEvent.click(logo);
      expect(mockNavigate).toHaveBeenCalledWith('/streams');
    });
  });

  test('logo navigates to /streams on Enter key press', async () => {
    render(
      <MemoryRouter>
        <Stream />
      </MemoryRouter>
    );

    await waitFor(() => {
      const logo = document.querySelector('.Stream-header-logo');
      expect(logo).toBeInTheDocument();
      
      fireEvent.keyDown(logo, { key: 'Enter' });
      expect(mockNavigate).toHaveBeenCalledWith('/streams');
    });
  });

  test('logo navigates to /streams on Space key press', async () => {
    render(
      <MemoryRouter>
        <Stream />
      </MemoryRouter>
    );

    await waitFor(() => {
      const logo = document.querySelector('.Stream-header-logo');
      expect(logo).toBeInTheDocument();
      
      fireEvent.keyDown(logo, { key: ' ' });
      expect(mockNavigate).toHaveBeenCalledWith('/streams');
    });
  });

  test('logo ignores other key presses', async () => {
    render(
      <MemoryRouter>
        <Stream />
      </MemoryRouter>
    );

    await waitFor(() => {
      const logo = document.querySelector('.Stream-header-logo');
      expect(logo).toBeInTheDocument();
      
      fireEvent.keyDown(logo, { key: 'Escape' });
      expect(mockNavigate).not.toHaveBeenCalled();
    });
  });

  test('title navigates to /streams on click', async () => {
    render(
      <MemoryRouter>
        <Stream />
      </MemoryRouter>
    );

    await waitFor(() => {
      const title = screen.getByText('TrackMe');
      expect(title).toBeInTheDocument();
      
      fireEvent.click(title);
      expect(mockNavigate).toHaveBeenCalledWith('/streams');
    });
  });

  test('title navigates to /streams on Enter key press', async () => {
    render(
      <MemoryRouter>
        <Stream />
      </MemoryRouter>
    );

    await waitFor(() => {
      const title = screen.getByText('TrackMe');
      expect(title).toBeInTheDocument();
      
      fireEvent.keyDown(title, { key: 'Enter' });
      expect(mockNavigate).toHaveBeenCalledWith('/streams');
    });
  });

  test('title navigates to /streams on Space key press', async () => {
    render(
      <MemoryRouter>
        <Stream />
      </MemoryRouter>
    );

    await waitFor(() => {
      const title = screen.getByText('TrackMe');
      expect(title).toBeInTheDocument();
      
      fireEvent.keyDown(title, { key: ' ' });
      expect(mockNavigate).toHaveBeenCalledWith('/streams');
    });
  });

  test('title ignores other key presses', async () => {
    render(
      <MemoryRouter>
        <Stream />
      </MemoryRouter>
    );

    await waitFor(() => {
      const title = screen.getByText('TrackMe');
      expect(title).toBeInTheDocument();
      
      fireEvent.keyDown(title, { key: 'Tab' });
      expect(mockNavigate).not.toHaveBeenCalled();
    });
  });

  test('logo has correct accessibility attributes', async () => {
    render(
      <MemoryRouter>
        <Stream />
      </MemoryRouter>
    );

    await waitFor(() => {
      const logo = document.querySelector('.Stream-header-logo');
      expect(logo).toHaveAttribute('tabIndex', '0');
      expect(logo).toHaveAttribute('role', 'button');
      expect(logo).toHaveAttribute('aria-label', 'Вернуться на главную страницу');
      expect(logo).toHaveStyle('cursor: pointer');
    });
  });

  test('title has correct accessibility attributes', async () => {
    render(
      <MemoryRouter>
        <Stream />
      </MemoryRouter>
    );

    await waitFor(() => {
      const title = screen.getByText('TrackMe');
      expect(title).toHaveAttribute('tabIndex', '0');
      expect(title).toHaveAttribute('role', 'button');
      expect(title).toHaveAttribute('aria-label', 'Вернуться на главную страницу');
      expect(title).toHaveStyle('cursor: pointer');
    });
  });

  test('logo and title are rendered in the header', async () => {
    render(
      <MemoryRouter>
        <Stream />
      </MemoryRouter>
    );

    await waitFor(() => {
      const logo = document.querySelector('.Stream-header-logo');
      const title = screen.getByText('TrackMe');
      
      expect(logo).toBeInTheDocument();
      expect(title).toBeInTheDocument();
      expect(logo.closest('.Stream-header-cont')).toContainElement(title);
    });
  });
});