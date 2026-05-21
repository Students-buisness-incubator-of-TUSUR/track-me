import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import TeamCard from '../accets/komand/team-card';

// Мокаем зависимости
jest.mock('../../utils/csrf-utils', () => ({
  getCsrfConfigForFetch: jest.fn(() => ({})),
}));

jest.mock('../../services/util', () => ({
  useGetUserInfo: jest.fn(() => ({
    roles: ['ADMIN'],
    username: 'testadmin',
  })),
}));

jest.mock('../../services/requests', () => ({
  fetchTrackers: jest.fn(() => Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) })),
}));

global.fetch = jest.fn();

describe('TeamCard - Пассивный статус', () => {
  const mockTeamData = {
    id: '123',
    name: 'Тестовая команда',
    description: 'Описание',
    meetingRoomLink: 'https://example.com',
    readinessLevel: '3-5',
    passive: false,
    enabled: true,
    username: 'tracker1',
    ntiMarkets: [{ id: 1, displayName: 'Рынок 1' }],
    streams: [{ id: 1, name: 'Поток 1', startDate: '2024-01-01', endDate: '2024-12-31' }],
  };

  const mockMeetings = [
    { id: '1', number: '1', startDate: '2024-05-01T10:00:00Z', status: 'SCHEDULED' },
  ];

  beforeEach(() => {
    jest.clearAllMocks();

    // Мок для загрузки карточки
    global.fetch.mockImplementation((url) => {
      if (url.includes('/admin/team-cards')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [mockTeamData] }),
        });
      }
      if (url.includes('/meetings')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: mockMeetings, totalPages: 1 }),
        });
      }
      if (url.includes('/streams/nti-markets')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([{ id: 1, displayName: 'Рынок 1' }]),
        });
      }
      if (url.includes('/streams?page=0&size=1500')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [{ id: 1, name: 'Поток 1', active: true }] }),
        });
      }
      if (url.includes('/team-card/count')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(5),
        });
      }
      return Promise.reject(new Error('Unknown endpoint'));
    });
  });

  const renderComponent = (role = 'ADMIN', passive = false) => {
    jest.spyOn(require('../../services/util'), 'useGetUserInfo').mockReturnValue({
      roles: [role],
      username: role === 'TRACKER' ? 'tracker1' : 'admin1',
    });

    return render(
      <BrowserRouter>
        <TeamCard />
      </BrowserRouter>
    );
  };

  describe('1. Отображение чекбокса пассивного статуса', () => {
    test('ADMIN видит чекбокс пассивного статуса в режиме редактирования', async () => {
      renderComponent('ADMIN');

      // Ждем загрузки и кликаем "Редактировать"
      const editButton = await screen.findByText('Редактировать');
      fireEvent.click(editButton);

      // Проверяем наличие чекбокса
      const passiveCheckbox = await screen.findByLabelText('Команда в пассиве (неактивна)');
      expect(passiveCheckbox).toBeInTheDocument();
    });

    test('SUPER_ADMIN видит чекбокс пассивного статуса', async () => {
      renderComponent('SUPER_ADMIN');

      const editButton = await screen.findByText('Редактировать');
      fireEvent.click(editButton);

      const passiveCheckbox = await screen.findByLabelText('Команда в пассиве (неактивна)');
      expect(passiveCheckbox).toBeInTheDocument();
    });

    test('TRACKER НЕ видит чекбокс пассивного статуса', async () => {
      renderComponent('TRACKER');

      const editButton = await screen.findByText('Редактировать');
      fireEvent.click(editButton);

      const passiveCheckbox = screen.queryByLabelText('Команда в пассиве (неактивна)');
      expect(passiveCheckbox).not.toBeInTheDocument();
    });

    test('Чекбокс отображает текущее значение passive из teamData', async () => {
      // Мокаем данные с passive: true
      global.fetch.mockImplementation((url) => {
        if (url.includes('/admin/team-cards')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ content: [{ ...mockTeamData, passive: true }] }),
          });
        }
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
      });

      renderComponent('ADMIN');

      const editButton = await screen.findByText('Редактировать');
      fireEvent.click(editButton);

      const passiveCheckbox = await screen.findByLabelText('Команда в пассиве (неактивна)');
      expect(passiveCheckbox.checked).toBe(true);
    });
  });

  describe('2. Переключение пассивного статуса', () => {
    test('Чекбокс можно включить/выключить', async () => {
      renderComponent('ADMIN');

      const editButton = await screen.findByText('Редактировать');
      fireEvent.click(editButton);

      const passiveCheckbox = await screen.findByLabelText('Команда в пассиве (неактивна)');

      // Включаем
      fireEvent.click(passiveCheckbox);
      expect(passiveCheckbox.checked).toBe(true);

      // Выключаем
      fireEvent.click(passiveCheckbox);
      expect(passiveCheckbox.checked).toBe(false);
    });
  });

  describe('3. Сохранение пассивного статуса', () => {
    test('При сохранении отправляется поле passive на сервер', async () => {
      let patchRequest = null;

      global.fetch.mockImplementation((url, options) => {
        if (url.includes('/admin/team-card') && options?.method === 'PATCH') {
          patchRequest = options;
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ ...mockTeamData, passive: true }),
          });
        }
        if (url.includes('/admin/team-cards')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ content: [mockTeamData] }),
          });
        }
        if (url.includes('/meetings')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ content: mockMeetings }),
          });
        }
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
      });

      renderComponent('ADMIN');

      const editButton = await screen.findByText('Редактировать');
      fireEvent.click(editButton);

      const passiveCheckbox = await screen.findByLabelText('Команда в пассиве (неактивна)');
      fireEvent.click(passiveCheckbox);

      const saveButton = await screen.findByText('Сохранить');
      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(patchRequest).not.toBeNull();
        const body = JSON.parse(patchRequest.body);
        expect(body).toHaveProperty('passive', true);
      });
    });

    test('При выключенном пассивном статусе отправляется passive: false', async () => {
      let patchRequest = null;

      global.fetch.mockImplementation((url, options) => {
        if (url.includes('/admin/team-card') && options?.method === 'PATCH') {
          patchRequest = options;
          return Promise.resolve({
            ok: true,
           json: () => Promise.resolve({ ...mockTeamData, passive: false }),
          });
        }
        if (url.includes('/admin/team-cards')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ content: [{ ...mockTeamData, passive: true }] }),
          });
        }
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
      });

      renderComponent('ADMIN');

      const editButton = await screen.findByText('Редактировать');
      fireEvent.click(editButton);

      // Чекбокс включен по умолчанию (из мока)
      const passiveCheckbox = await screen.findByLabelText('Команда в пассиве (неактивна)');
      expect(passiveCheckbox.checked).toBe(true);

      // Выключаем
      fireEvent.click(passiveCheckbox);

      const saveButton = await screen.findByText('Сохранить');
      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(patchRequest).not.toBeNull();
        const body = JSON.parse(patchRequest.body);
        expect(body).toHaveProperty('passive', false);
      });
    });
  });

  describe('4. Запрет редактирования для трекера', () => {
    test('Трекер может редактировать активную команду', async () => {
      global.fetch.mockImplementation((url) => {
        if (url.includes('/team-cards')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ content: [{ ...mockTeamData, passive: false }] }),
          });
        }
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
      });

      renderComponent('TRACKER');

      const editButton = await screen.findByText('Редактировать');
      expect(editButton).toBeEnabled();
    });

    test('Трекер НЕ может редактировать пассивную команду', async () => {
      global.fetch.mockImplementation((url) => {
        if (url.includes('/team-cards')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ content: [{ ...mockTeamData, passive: true }] }),
          });
        }
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
      });

      renderComponent('TRACKER');

      const editButton = await screen.findByText('Редактировать');
      fireEvent.click(editButton);

      // Поля должны быть только для чтения
      await waitFor(() => {
        const nameInput = screen.getByDisplayValue(mockTeamData.name);
        expect(nameInput).toHaveAttribute('readOnly');
      });
    });

    test('Трекеру недоступно поле названия команды при редактировании пассивной команды', async () => {
      global.fetch.mockImplementation((url) => {
        if (url.includes('/team-cards')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ content: [{ ...mockTeamData, passive: true }] }),
          });
        }
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
      });

      renderComponent('TRACKER');

      const editButton = await screen.findByText('Редактировать');
      fireEvent.click(editButton);

      await waitFor(() => {
        const nameInput = screen.getByDisplayValue(mockTeamData.name);
        expect(nameInput).toHaveAttribute('readOnly');
      });
    });
  });

  describe('5. Создание встреч для пассивной команды', () => {
    test('Трекер НЕ может создать встречу для пассивной команды', async () => {
      global.fetch.mockImplementation((url) => {
        if (url.includes('/team-cards')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ content: [{ ...mockTeamData, passive: true }] }),
          });
        }
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
      });

      renderComponent('TRACKER');

      await waitFor(() => {
        const scheduleButton = screen.getByText('Запланировать');
        fireEvent.click(scheduleButton);
      });

      await waitFor(() => {
        const errorMessage = screen.getByText('Нельзя создавать встречи для пассивной команды');
        expect(errorMessage).toBeInTheDocument();
      });
    });

    test('Админ МОЖЕТ создать встречу для пассивной команды', async () => {
      global.fetch.mockImplementation((url) => {
        if (url.includes('/admin/team-cards')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ content: [{ ...mockTeamData, passive: true }] }),
          });
        }
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
      });

      renderComponent('ADMIN');

      await waitFor(() => {
        const scheduleButton = screen.getByText('Запланировать');
        expect(scheduleButton).toBeEnabled();
      });
    });
  });

  describe('6. Состояние canEdit', () => {
    test('canEdit = true для админа с активной командой', async () => {
      renderComponent('ADMIN');

      const editButton = await screen.findByText('Редактировать');
      fireEvent.click(editButton);

      await waitFor(() => {
        const nameInput = screen.getByDisplayValue(mockTeamData.name);
        expect(nameInput).not.toHaveAttribute('readOnly');
      });
    });

    test('canEdit = true для админа с пассивной командой', async () => {
      global.fetch.mockImplementation((url) => {
        if (url.includes('/admin/team-cards')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ content: [{ ...mockTeamData, passive: true }] }),
          });
        }
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
      });

      renderComponent('ADMIN');

      const editButton = await screen.findByText('Редактировать');
      fireEvent.click(editButton);

      await waitFor(() => {
        const nameInput = screen.getByDisplayValue(mockTeamData.name);
        expect(nameInput).not.toHaveAttribute('readOnly');
      });
    });
  });

  describe('7. Интеграционные тесты', () => {
    test('Полный цикл: включение пассивного статуса -> сохранение -> отключение', async () => {
      let savedPassiveValue = null;

      global.fetch.mockImplementation((url, options) => {
        if (url.includes('/admin/team-cards')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ content: [{ ...mockTeamData, passive: false }] }),
          });
        }
        if (url.includes('/admin/team-card') && options?.method === 'PATCH') {
          savedPassiveValue = JSON.parse(options.body).passive;
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ ...mockTeamData, passive: savedPassiveValue }),
          });
        }
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
      });

      renderComponent('ADMIN');

      // Режим редактирования
      const editButton = await screen.findByText('Редактировать');
      fireEvent.click(editButton);

      // Включаем пассивный статус
      const passiveCheckbox = await screen.findByLabelText('Команда в пассиве (неактивна)');
      fireEvent.click(passiveCheckbox);
      expect(passiveCheckbox.checked).toBe(true);

      // Сохраняем
      const saveButton = await screen.findByText('Сохранить');
      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(savedPassiveValue).toBe(true);
      });
    });
  });
});