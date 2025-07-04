import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import ProfilePage from './ProfilePage';
import { BrowserRouter } from 'react-router-dom';

global.fetch = jest.fn();

const mockUserData = {
  username: 'testuser',
  fullName: 'Test User',
  email: 'test@example.com',
  phoneNumber: '+79123456789',
  roles: ['TRACKER']
};

const mockTeamCardsResponse = {
  content: [{ id: 1 }, { id: 2 }],
  totalElements: 2
};

describe('ProfilePage Tooltip', () => {
  beforeEach(() => {
    fetch.mockImplementation((url) => {
      if (url.includes('/account/info')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockUserData)
        });
      }
      if (url.includes('/team-cards')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockTeamCardsResponse)
        });
      }
      if (url.includes('/account/photo')) {
        return Promise.reject(new Error('Photo not found'));
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  test('should show tooltip with team count when hovering over count element', async () => {
    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    // Ждем пока загрузятся данные и появится кнопка с количеством команд
    const teamCardsButton = await screen.findByRole('button', { name: /Карточки команд/i });
    expect(teamCardsButton).toBeInTheDocument();

    // Находим элемент с количеством команд
    const countElement = await screen.findByText('(2)');
    expect(countElement).toBeInTheDocument();

    // Симулируем наведение мыши на элемент
    fireEvent.mouseEnter(countElement);

    // Проверяем, что подсказка появилась
    const tooltip = await screen.findByText('Количество моих команд');
    expect(tooltip).toBeInTheDocument();

    // Проверяем что подсказка имеет класс для стилей
    expect(tooltip).toHaveClass('profile-tooltip');

    // Симулируем уход мыши с элемента
    fireEvent.mouseLeave(countElement);

    // Проверяем, что подсказка исчезла
    await waitFor(() => {
      expect(screen.queryByText('Количество моих команд')).not.toBeInTheDocument();
    });
  });

  test('should update tooltip position on mouse move', async () => {
    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    // Ждем загрузки данных
    await screen.findByText('Карточки команд');

    // Находим элемент с количеством команд
    const countElement = await screen.findByText('(2)');

    // Мокаем getBoundingClientRect
    const mockRect = {
      left: 100,
      top: 200,
      width: 50,
      height: 20,
      right: 150,
      bottom: 220,
      x: 100,
      y: 200
    };
    countElement.getBoundingClientRect = jest.fn(() => mockRect);

    // Симулируем движение мыши
    fireEvent.mouseMove(countElement);

    // Проверяем, что подсказка появилась
    fireEvent.mouseEnter(countElement);
    const tooltip = await screen.findByText('Количество моих команд');
    expect(tooltip).toBeInTheDocument();
  });

  test('should not show tooltip for non-tracker users', async () => {
    const nonTrackerUser = {
      ...mockUserData,
      roles: ['ADMIN']
    };

    fetch.mockImplementation((url) => {
      if (url.includes('/account/info')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(nonTrackerUser)
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });

    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    await screen.findByText('Карточки команд');
    expect(screen.queryByText(/\(\d+\)/)).not.toBeInTheDocument();
    expect(screen.queryByText('Количество моих команд')).not.toBeInTheDocument();
  });

  test('should handle team cards fetch error', async () => {
    fetch.mockImplementation((url) => {
      if (url.includes('/account/info')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockUserData)
        });
      }
      if (url.includes('/team-cards')) {
        return Promise.reject(new Error('Failed to fetch'));
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });

    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    await screen.findByText('Карточки команд');
    const countElement = await screen.findByText('(0)');
    expect(countElement).toBeInTheDocument();
  });
  test('should handle team cards fetch error and show zero count', async () => {
  // Mock successful user data fetch
  fetch.mockImplementation((url) => {
    if (url.includes('/account/info')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve(mockUserData)
      });
    }
    if (url.includes('/team-cards')) {
      // Simulate a failed response
      return Promise.reject(new Error("Ошибка при загрузке карточек команд"));
    }
    if (url.includes('/account/photo')) {
      return Promise.reject(new Error('Photo not found'));
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
  });

  // Spy on console.error to verify the error is logged
  const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});

  render(
    <BrowserRouter>
      <ProfilePage />
    </BrowserRouter>
  );

  // Wait for the component to render
  await screen.findByText('Карточки команд');

  // Verify that the error was logged
  expect(consoleErrorSpy).toHaveBeenCalledWith(
    "Ошибка при загрузке карточек:",
    expect.any(Error)
  );

  // Verify that the team count shows 0 when there's an error
  const countElement = await screen.findByText('(0)');
  expect(countElement).toBeInTheDocument();

  // Clean up the spy
  consoleErrorSpy.mockRestore();
});
test('should throw error when team cards request fails', async () => {
  // 1. Мокаем успешный запрос данных пользователя
  fetch.mockImplementationOnce(() => 
    Promise.resolve({
      ok: true,
      json: () => Promise.resolve({
        ...mockUserData,
        roles: ["TRACKER"] // Важно для выполнения условия
      })
    })
  );

  // 2. Мокаем запрос фото (необязательно, но для полноты)
  fetch.mockImplementationOnce(() => 
    Promise.reject(new Error("Photo not found"))
  );

  // 3. Мокаем НЕуспешный запрос карточек команд
  fetch.mockImplementationOnce(() => 
    Promise.resolve({
      ok: false, // Именно это вызовет throw new Error
      status: 500
    })
  );

  // 4. Спи на console.error чтобы проверить лог
  const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});

  render(
    <BrowserRouter>
      <ProfilePage />
    </BrowserRouter>
  );

  // 5. Ждем пока компонент обработает ошибку
  await waitFor(() => {
    // Проверяем что ошибка была залогирована
    expect(consoleErrorSpy).toHaveBeenCalledWith(
      "Ошибка при загрузке карточек:",
      expect.any(Error)
    );
    
    // Проверяем что счетчик команд = 0 (обработка ошибки)
    expect(screen.getByText("(0)")).toBeInTheDocument();
  });

  // 6. Восстанавливаем console.error
  consoleErrorSpy.mockRestore();
});
});