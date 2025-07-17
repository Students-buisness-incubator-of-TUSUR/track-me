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
  roles: ['TRACKER'],
};

const mockTeamCardsResponse = {
  content: [{ id: 1 }, { id: 2 }],
  totalElements: 2,
};

describe('ProfilePage', () => {
  beforeEach(() => {
    fetch.mockImplementation((url) => {
      if (url.includes('/account/info')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockUserData),
        });
      }
      if (url.includes('/team-cards')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockTeamCardsResponse),
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
  
  // Existing test: Tooltip visibility for team count
  test('should show tooltip with team count when hovering over count element', async () => {
    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    const teamCardsButton = await screen.findByRole('button', { name: /Карточки команд/i });
    expect(teamCardsButton).toBeInTheDocument();

    const countElement = await screen.findByText('(2)');
    expect(countElement).toBeInTheDocument();

    fireEvent.mouseEnter(countElement);
    const tooltip = await screen.findByText('Количество моих команд');
    expect(tooltip).toBeInTheDocument();
    expect(tooltip).toHaveClass('profile-tooltip');

    fireEvent.mouseLeave(countElement);
    await waitFor(() => {
      expect(screen.queryByText('Количество моих команд')).not.toBeInTheDocument();
    });
  });

  // Existing test: Tooltip position update
  test('should update tooltip position on mouse move', async () => {
    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    await screen.findByText('Карточки команд');
    const countElement = await screen.findByText('(2)');

    const mockRect = {
      left: 100,
      top: 200,
      width: 50,
      height: 20,
      right: 150,
      bottom: 220,
      x: 100,
      y: 200,
    };
    countElement.getBoundingClientRect = jest.fn(() => mockRect);

    fireEvent.mouseMove(countElement);
    fireEvent.mouseEnter(countElement);
    const tooltip = await screen.findByText('Количество моих команд');
    expect(tooltip).toBeInTheDocument();
  });

  // Existing test: Non-tracker users
  test('should not show tooltip for non-tracker users', async () => {
    const nonTrackerUser = {
      ...mockUserData,
      roles: ['ADMIN'],
    };

    fetch.mockImplementation((url) => {
      if (url.includes('/account/info')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(nonTrackerUser),
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

  // Existing test: Team cards fetch error
  test('should handle team cards fetch error and show zero count', async () => {
    fetch.mockImplementation((url) => {
      if (url.includes('/account/info')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockUserData),
        });
      }
      if (url.includes('/team-cards')) {
        return Promise.reject(new Error('Ошибка при загрузке карточек команд'));
      }
      if (url.includes('/account/photo')) {
        return Promise.reject(new Error('Photo not found'));
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });

    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});

    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    await screen.findByText('Карточки команд');
    const countElement = await screen.findByText('(0)');
    expect(countElement).toBeInTheDocument();

    expect(consoleErrorSpy).toHaveBeenCalledWith(
      'Ошибка при загрузке карточек:',
      expect.any(Error),
    );

    consoleErrorSpy.mockRestore();
  });

  // New test: Verify team cards fetch request details (lines 163-179)
  test('should make correct team cards fetch request for TRACKER role', async () => {
    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    await screen.findByText('Карточки команд');

    // Verify the fetch call for team cards
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/team-cards?page=0&size=1000'),
      expect.objectContaining({
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          filters: [
            {
              fieldName: 'username',
              type: 'EQ',
              value: mockUserData.username,
            },
            {
              fieldName: 'enabled',
              type: 'EQ',
              value: true,
            },
          ],
        }),
      }),
    );

    // Verify team count is set correctly
    const countElement = await screen.findByText('(2)');
    expect(countElement).toBeInTheDocument();
  });

  // New test: Handle team cards response with no totalElements (lines 163-179)
  test('should set team count based on content length when totalElements is absent', async () => {
    fetch.mockImplementation((url) => {
      if (url.includes('/account/info')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockUserData),
        });
      }
      if (url.includes('/team-cards')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [{ id: 1 }, { id: 2 }, { id: 3 }] }),
        });
      }
      if (url.includes('/account/photo')) {
        return Promise.reject(new Error('Photo not found'));
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });

    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    const countElement = await screen.findByText('(3)');
    expect(countElement).toBeInTheDocument();
  });

  // New test: Skip team cards fetch for non-TRACKER role (lines 163-179)
  test('should not fetch team cards for non-TRACKER role', async () => {
    const nonTrackerUser = {
      ...mockUserData,
      roles: ['ADMIN'],
    };

    fetch.mockImplementation((url) => {
      if (url.includes('/account/info')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(nonTrackerUser),
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
    expect(fetch).not.toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/team-cards'),
      expect.any(Object),
    );
    expect(screen.queryByText(/\(\d+\)/)).not.toBeInTheDocument();
  });

  // Existing test (modified): Handle team cards fetch error with non-ok response (lines 163-179)
  test('should throw error when team cards request fails with non-ok response', async () => {
    fetch.mockImplementation((url) => {
      if (url.includes('/account/info')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockUserData),
        });
      }
      if (url.includes('/team-cards')) {
        return Promise.resolve({
          ok: false,
          status: 500,
        });
      }
      if (url.includes('/account/photo')) {
        return Promise.reject(new Error('Photo not found'));
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });

    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});

    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        'Ошибка при загрузке карточек:',
        expect.any(Error),
      );
      expect(screen.getByText('(0)')).toBeInTheDocument();
    });

    consoleErrorSpy.mockRestore();
  });
  test('validateForm: should show error if fullName is empty', async () => {
  render(
    <BrowserRouter>
      <ProfilePage />
    </BrowserRouter>
  );

  const editButton = await screen.findByRole('button', { name: /Редактировать/i });
  fireEvent.click(editButton);

  // Находим input по значению по умолчанию
  const fullNameInput = screen.getByDisplayValue('Test User');
  fireEvent.change(fullNameInput, { target: { value: '' } });

  const saveButton = screen.getByRole('button', { name: /Сохранить/i });
  fireEvent.click(saveButton);

  expect(await screen.findByText("Поле 'ФИО' обязательно для заполнения")).toBeInTheDocument();
});
test('validateForm: should show error if email is invalid', async () => {
  render(
    <BrowserRouter>
      <ProfilePage />
    </BrowserRouter>
  );

  const editButton = await screen.findByRole('button', { name: /Редактировать/i });
  fireEvent.click(editButton);

  const emailInput = screen.getByDisplayValue('test@example.com');
  fireEvent.change(emailInput, { target: { value: 'invalid-email' } });

  const saveButton = screen.getByRole('button', { name: /Сохранить/i });
  fireEvent.click(saveButton);

  expect(await screen.findByText("Некорректный формат email")).toBeInTheDocument();
});
test('validateForm: should show error if phoneNumber is invalid', async () => {
  render(
    <BrowserRouter>
      <ProfilePage />
    </BrowserRouter>
  );

  const editButton = await screen.findByRole('button', { name: /Редактировать/i });
  fireEvent.click(editButton);

  const phoneInput = screen.getByDisplayValue('+79123456789');
  fireEvent.change(phoneInput, { target: { value: '+799' } });

  const saveButton = screen.getByRole('button', { name: /Сохранить/i });
  fireEvent.click(saveButton);

  expect(await screen.findByText("Некорректный формат телефона")).toBeInTheDocument();
});
test('validateForm: should show error if username is empty', async () => {
  render(
    <BrowserRouter>
      <ProfilePage />
    </BrowserRouter>
  );

  const editButton = await screen.findByRole('button', { name: /Редактировать/i });
  fireEvent.click(editButton);

  const usernameInput = screen.getByDisplayValue('testuser');
  fireEvent.change(usernameInput, { target: { value: '' } });

  const saveButton = screen.getByRole('button', { name: /Сохранить/i });
  fireEvent.click(saveButton);

  expect(await screen.findByText("Поле 'Телеграм' обязательно для заполнения")).toBeInTheDocument();
});
test('validateForm: should pass with all valid fields', async () => {
  render(
    <BrowserRouter>
      <ProfilePage />
    </BrowserRouter>
  );

  const editButton = await screen.findByRole('button', { name: /Редактировать/i });
  fireEvent.click(editButton);

  fireEvent.change(screen.getByDisplayValue('Test User'), { target: { value: 'Valid Name' } });
  fireEvent.change(screen.getByDisplayValue('test@example.com'), { target: { value: 'valid@example.com' } });
  fireEvent.change(screen.getByDisplayValue('+79123456789'), { target: { value: '+79999999999' } });
  fireEvent.change(screen.getByDisplayValue('testuser'), { target: { value: 'telegram_user' } });

  const saveButton = screen.getByRole('button', { name: /Сохранить/i });
  fireEvent.click(saveButton);

  await waitFor(() => {
    expect(screen.queryByText(/Поле|Некорректный/)).not.toBeInTheDocument();
  });
});

});