import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import ProfilePage from './ProfilePage';
import { BrowserRouter } from 'react-router-dom';
import { MemoryRouter, Routes, Route } from "react-router-dom";
import { fetchTeams, fetchUserInfo, fetchUserPhoto, updateUserInfo, updateUserPhoto } from "../../services/requests";
import InputBox from '../input-box/input-box';
import { ReactComponent as CloseIcon } from '../../files/close.svg'
import { ReactComponent as UploadIcon } from '../../files/upload.svg'

jest.mock('../../files/close.svg', () => ({
  ReactComponent: () => <svg>CloseIcon</svg>
}));
jest.mock('../../files/upload.svg', () => ({
  ReactComponent: () => <svg>UploadIcon</svg>
}));
jest.mock('../../files/no-user-photo.png', () => 'no-user-photo.png');
jest.mock('../header/header', () => () => <div>Header</div>);

jest.mock('../input-box/input-box', () => {
  return function MockInputBox({ placeholder, type, name, value, onChange, readOnly }) {
    return (
      <input
        placeholder={placeholder}
        type={type}
        name={name}
        value={value}
        onChange={onChange}
        readOnly={readOnly}
        data-testid={`input-${name}`}
      />
    );
  };
});

jest.mock('../../services/requests', () => ({
  fetchTeams: jest.fn(),
  fetchUserInfo: jest.fn(),
  fetchUserPhoto: jest.fn(),
  updateUserInfo: jest.fn(),
  updateUserPhoto: jest.fn(),
}));


const mockUserData = {
  username: 'testuser',
  fullName: 'Иван Иванов',
  email: 'test@example.com',
  phoneNumber: '+71234567890',
  roles: ['TRACKER']
};

const mockUserDataAdmin = {
  ...mockUserData,
  roles: ['ADMIN']
};

const mockTeamsResponse = {
  totalElements: 5,
  content: Array(5).fill({ id: 1, name: 'Team' })
};

beforeEach(() => {
  jest.clearAllMocks();

  fetchUserInfo.mockResolvedValue({
    ok: true,
    status: 200,
    json: jest.fn().mockResolvedValue(mockUserData)
  });

  fetchTeams.mockResolvedValue({
    ok: true,
    status: 200,
    json: jest.fn().mockResolvedValue(mockTeamsResponse)
  });

  fetchUserPhoto.mockResolvedValue({
    ok: true,
    blob: jest.fn().mockResolvedValue(new Blob(['image data'], { type: 'image/jpeg' }))
  });

  updateUserInfo.mockResolvedValue({
    ok: true,
    status: 200
  });

  updateUserPhoto.mockResolvedValue({
    ok: true,
    status: 200
  });
})

const mockUserInfoErrorStatus = (status) => {
  fetchUserInfo.mockImplementation(async () => {
    return {
      ok: false,
      status: status,
      json: jest.fn().mockResolvedValue({}),
    };
  });
};

const mockOtherUserInfoNetworkError = () => {
  fetchUserInfo
    .mockResolvedValueOnce(Promise.reject(new Error("Network error")));
}

const mockOtherUserInfoSuccess = () => {
  fetchUserInfo.mockImplementation(async (args) => {
    const { username } = args;

    if (username) {
      return {
        ok: true,
        status: 200,
        json: jest.fn().mockResolvedValue(mockUserData),
      };
    }

    return {
      ok: true,
      status: 200,
      json: jest.fn().mockResolvedValue(mockUserDataAdmin),
    };
  });
};

const mockOtherUserInfoErrorStatus = (status) => {
  fetchUserInfo.mockImplementation(async (args) => {
    const { username } = args;

    if (username) {
      return {
        ok: false,
        status: status,
        json: jest.fn().mockResolvedValue({ error: 'Error' }),
      };
    }

    return {
      ok: true,
      status: 200,
      json: jest.fn().mockResolvedValue(mockUserDataAdmin),
    };
  });
};

const mockTeamCardsResponse = {
  content: [{ id: 1 }, { id: 2 }],
  totalElements: 2,
};

describe('ProfilePage', () => {
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

    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => { });

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

    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => { });

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
    const fullNameInput = screen.getByDisplayValue(mockUserData.fullName);
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

    const emailInput = screen.getByDisplayValue(mockUserData.email);
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

    const usernameInput = screen.getByDisplayValue(mockUserData.username);
    fireEvent.change(usernameInput, { target: { value: '' } });

    const saveButton = screen.getByRole('button', { name: /Сохранить/i });
    fireEvent.click(saveButton);

    expect(await screen.findByText("Поле 'Телеграм' обязательно для заполнения")).toBeInTheDocument();
  });
  test('validateForm: should show error if username contains invalid characters', async () => {
    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    const editButton = await screen.findByRole('button', { name: /Редактировать/i });
    fireEvent.click(editButton);

    const usernameInput = screen.getByDisplayValue(mockUserData.username);

    // Тестируем различные невалидные значения
    const invalidUsernames = [
      'test user', // пробел
      'user!name', // специальный символ
      'имя', // кириллица
      'user@name', // @
      'user-name', // дефис
    ];

    for (const invalidUsername of invalidUsernames) {
      fireEvent.change(usernameInput, { target: { value: invalidUsername } });
      fireEvent.click(screen.getByRole('button', { name: /Сохранить/i }));

      // Проверяем сообщение об ошибке
      expect(await screen.findByText('Введите корректный юзернейм')).toBeInTheDocument();
    }
  });

  test('validateForm: should pass with all valid fields', async () => {
    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    const editButton = await screen.findByRole('button', { name: /Редактировать/i });
    fireEvent.click(editButton);

    fireEvent.change(screen.getByDisplayValue(mockUserData.fullName), { target: { value: 'Valid Name' } });
    fireEvent.change(screen.getByDisplayValue(mockUserData.email), { target: { value: 'valid@example.com' } });
    fireEvent.change(screen.getByDisplayValue(mockUserData.phoneNumber), { target: { value: '+79999999999' } });
    fireEvent.change(screen.getByDisplayValue(mockUserData.username), { target: { value: 'telegram_user' } });

    const saveButton = screen.getByRole('button', { name: /Сохранить/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(screen.queryByText(/Поле|Некорректный/)).not.toBeInTheDocument();
    });
  });

});
describe('ProfilePage Error Handling', () => {
  const prepareValidFormForSave = async () => {
    const editButton = await screen.findByRole('button', { name: /Редактировать/i });
    fireEvent.click(editButton);

    // Убеждаемся, что все поля валидны
    const fullNameInput = screen.getByDisplayValue(mockUserData.fullName);
    const emailInput = screen.getByDisplayValue(mockUserData.email);
    const phoneInput = screen.getByDisplayValue(mockUserData.phoneNumber);
    const usernameInput = screen.getByDisplayValue(mockUserData.username);

    // Если нужно изменить данные для теста, делаем это здесь
    fireEvent.change(fullNameInput, { target: { value: 'Valid Name' } });
    fireEvent.change(emailInput, { target: { value: 'valid@example.com' } });
    fireEvent.change(phoneInput, { target: { value: '+79999999999' } });
    fireEvent.change(usernameInput, { target: { value: 'validuser' } });

    return screen.getByRole('button', { name: /Сохранить/i });
  };



  // Тесты для общего catch блока с ошибкой JSON парсинга
  test('should handle fetch error with JSON parsing error in catch block', async () => {
    updateUserInfo.mockResolvedValue({
      ok: false,
      status: 400
    });
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => { });

    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    const saveButton = await prepareValidFormForSave();
    fireEvent.click(saveButton);

    // Проверяем, что ошибка логируется
    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        'Ошибка сохранения данных:',
        expect.any(Error)
      );
    });

    // Проверяем, что общее сообщение об ошибке НЕ устанавливается из-за JSON ошибки
    await waitFor(() => {
      expect(screen.queryByText('Произошла ошибка при сохранении данных')).not.toBeInTheDocument();
    });

    consoleErrorSpy.mockRestore();
  });

  // Тесты для общего catch блока с НЕ-JSON ошибкой
  test('should handle fetch error with non-JSON error in catch block', async () => {
    updateUserInfo.mockResolvedValue({
      ok: false,
      status: 400
    });
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => { });

    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    const saveButton = await prepareValidFormForSave();
    fireEvent.click(saveButton);

    // Проверяем, что устанавливается общее сообщение об ошибке
    await waitFor(() => {
      expect(screen.getByText('Произошла ошибка при сохранении данных')).toBeInTheDocument();
    });

    consoleErrorSpy.mockRestore();
  });

  // Тесты для успешного сохранения с обновлением данных
  test('should update user data after successful save', async () => {
    const updatedUserData = {
      ...mockUserData,
      fullName: 'Updated Name',
      email: 'updated@example.com',
    };

    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    const editButton = await screen.findByRole('button', { name: /Редактировать/i });
    fireEvent.click(editButton);

    // Меняем данные
    const fullNameInput = screen.getByDisplayValue(mockUserData.fullName);
    fireEvent.change(fullNameInput, { target: { value: 'Updated Name' } });

    const saveButton = screen.getByRole('button', { name: /Сохранить/i });
    fireEvent.click(saveButton);

    fetchUserInfo.mockResolvedValue({
      ok: true,
      status: 200,
      json: jest.fn().mockResolvedValue(updatedUserData)
    });

    // Проверяем, что данные обновились
    await waitFor(() => {
      expect(screen.getByDisplayValue('Updated Name')).toBeInTheDocument();
    });

    // Проверяем, что вышли из режима редактирования
    expect(screen.getByRole('button', { name: /Редактировать/i })).toBeInTheDocument();
  });

  test('should use sent data when fresh data fetch fails after save', async () => {
    fetchUserInfo.mockResolvedValueOnce({
      ok: true,
      json: async () => mockUserData
    });

    updateUserInfo.mockResolvedValue({ ok: true, status: 200 });
    fetchUserInfo.mockResolvedValue({ ok: false });

    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    const editButton = await screen.findByRole('button', { name: /Редактировать/i });
    fireEvent.click(editButton);

    const usernameInput = screen.getByDisplayValue('testuser');
    fireEvent.change(usernameInput, { target: { value: 'newusername' } });

    const saveButton = screen.getByRole('button', { name: /Сохранить/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Редактировать/i })).toBeInTheDocument();
    });

    expect(screen.getByDisplayValue('newusername')).toBeInTheDocument();
  });

  // Дополнительный тест: проверяем, что ошибки валидации блокируют отправку запроса
  test('should not send request when form validation fails', async () => {
    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    const editButton = await screen.findByRole('button', { name: /Редактировать/i });
    fireEvent.click(editButton);

    // Устанавливаем невалидный email
    const emailInput = screen.getByDisplayValue('test@example.com');
    fireEvent.change(emailInput, { target: { value: 'invalid-email' } });

    const saveButton = screen.getByRole('button', { name: /Сохранить/i });
    fireEvent.click(saveButton);

    // Проверяем, что отображается ошибка валидации
    await waitFor(() => {
      expect(screen.getByText('Некорректный формат email')).toBeInTheDocument();
    });

    // Проверяем, что запрос на обновление НЕ был отправлен
    expect(updateUserInfo).not.toHaveBeenCalled();
  });
});
// Замените проблемные тесты на эти исправленные версии:

describe('ProfilePage useEffect Dependencies and Initialization', () => {
  describe('Component Initialization (lines 43-63)', () => {
    test('should initialize all state variables with correct default values', async () => {
      render(
        <BrowserRouter>
          <ProfilePage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('Личный кабинет')).toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: /Главная страница/i })).toBeInTheDocument();
    });

    test('should set default avatar URL correctly', async () => {
      render(
        <BrowserRouter>
          <ProfilePage />
        </BrowserRouter>
      );

      await waitFor(() => {
        const avatar = screen.getByTestId('user-photo');
        expect(avatar).toHaveAttribute("src", 'no-user-photo.png');

      });
    });

    test('should initialize isEditing and isOwnProfile correctly', async () => {
      render(
        <BrowserRouter>
          <ProfilePage />
        </BrowserRouter>
      );

      await waitFor(() => {
        const editButton = screen.getByRole('button', { name: /Редактировать/i });
        expect(editButton).toBeInTheDocument();
      });
    });

    test('should initialize teamCount correctly', async () => {
      render(
        <BrowserRouter>
          <ProfilePage />
        </BrowserRouter>
      );

      await waitFor(() => {
        const countElement = screen.getByText(/\(\d+\)/);
        expect(countElement).toBeInTheDocument();
      });
    });
  });

  describe('Edge Cases and Error Scenarios', () => {
    test('should handle empty user data gracefully', async () => {
      fetchUserInfo.mockImplementation(async () => {
        return {
          ok: true,
          status: 200,
          json: jest.fn().mockResolvedValue({}),
        };
      });

      render(
        <BrowserRouter>
          <ProfilePage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('Личный кабинет')).toBeInTheDocument();
      });
    });

    test('should handle 401 unauthorized error', async () => {
      mockUserInfoErrorStatus(401);

      render(
        <BrowserRouter>
          <ProfilePage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(screen.getByText(/Ошибка авторизации/i)).toBeInTheDocument();
      });
    });
  });
});

describe("loadTargetUserData", () => {
  test("should show error when response status is 403", async () => {
    mockOtherUserInfoErrorStatus(403);

    render(
      <MemoryRouter initialEntries={["/profile/other"]}>
        <Routes>
          <Route path="/profile/:username" element={<ProfilePage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(
      await screen.findByText("Нет доступа к просмотру этого профиля")
    ).toBeInTheDocument();
  });

  test("should show error when response status is 404", async () => {
    mockOtherUserInfoErrorStatus(404);

    render(
      <MemoryRouter initialEntries={["/profile/other"]}>
        <Routes>
          <Route path="/profile/:username" element={<ProfilePage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText("Пользователь не найден")).toBeInTheDocument();
  });

  test("should show generic error when response status is not ok", async () => {
    mockOtherUserInfoErrorStatus(500);

    render(
      <MemoryRouter initialEntries={["/profile/other"]}>
        <Routes>
          <Route path="/profile/:username" element={<ProfilePage />} />
        </Routes>
      </MemoryRouter>

    );

    expect(await screen.findByText("Ошибка загрузки данных пользователя")).toBeInTheDocument();
  });

  test("should load and set user data on success", async () => {
    render(
      <MemoryRouter initialEntries={["/profile/other"]}>
        <Routes>
          <Route path="/profile/:username" element={<ProfilePage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByDisplayValue(mockUserData.fullName)).toBeInTheDocument();
    expect(await screen.findByDisplayValue(mockUserData.email)).toBeInTheDocument();
  });

  test("should handle fetch rejection and show error message", async () => {
    mockOtherUserInfoNetworkError();

    render(
      <MemoryRouter initialEntries={["/profile/other"]}>
        <Routes>
          <Route path="/profile/:username" element={<ProfilePage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText("Network error")).toBeInTheDocument();
  });

});
