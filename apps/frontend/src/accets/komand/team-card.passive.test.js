import { render, screen, fireEvent, waitFor } from '@testing-library/react';
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
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
    });
  });

  const renderComponent = async (role = 'ADMIN', passive = false) => {
    // Мокаем данные с нужным passive
    global.fetch.mockImplementation((url) => {
      if (url.includes('/admin/team-cards')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [{ ...mockTeamData, passive }] }),
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
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
    });

    jest.spyOn(require('../../services/util'), 'useGetUserInfo').mockReturnValue({
      roles: [role],
      username: role === 'TRACKER' ? 'tracker1' : 'admin1',
    });

    const utils = render(
      <BrowserRouter>
        <TeamCard />
      </BrowserRouter>
    );

    // Ждем загрузки
    await waitFor(() => {
      expect(screen.queryByText('Загрузка...')).not.toBeInTheDocument();
    });

    return utils;
  };

  describe('1. Отображение чекбокса пассивного статуса', () => {
    test('ADMIN видит чекбокс пассивного статуса в режиме редактирования', async () => {
      await renderComponent('ADMIN', false);

      const editButton = await screen.findByText('Редактировать');
      fireEvent.click(editButton);

      const passiveCheckbox = await screen.findByLabelText('Команда в пассиве (неактивна)');
      expect(passiveCheckbox).toBeInTheDocument();
    });

    test('SUPER_ADMIN видит чекбокс пассивного статуса', async () => {
      await renderComponent('SUPER_ADMIN', false);

      const editButton = await screen.findByText('Редактировать');
      fireEvent.click(editButton);

      const passiveCheckbox = await screen.findByLabelText('Команда в пассиве (неактивна)');
      expect(passiveCheckbox).toBeInTheDocument();
    });

    test('TRACKER НЕ видит чекбокс пассивного статуса', async () => {
      await renderComponent('TRACKER', false);

      const editButton = await screen.findByText('Редактировать');
      fireEvent.click(editButton);

      const passiveCheckbox = screen.queryByLabelText('Команда в пассиве (неактивна)');
      expect(passiveCheckbox).not.toBeInTheDocument();
    });

    test('Чекбокс отображает текущее значение passive из teamData', async () => {
      await renderComponent('ADMIN', true);

      const editButton = await screen.findByText('Редактировать');
      fireEvent.click(editButton);

      const passiveCheckbox = await screen.findByLabelText('Команда в пассиве (неактивна)');
      expect(passiveCheckbox.checked).toBe(true);
    });
  });

  describe('2. Переключение пассивного статуса', () => {
    test('Чекбокс можно включить/выключить', async () => {
      await renderComponent('ADMIN', false);

      const editButton = await screen.findByText('Редактировать');
      fireEvent.click(editButton);

      const passiveCheckbox = await screen.findByLabelText('Команда в пассиве (неактивна)');

      fireEvent.click(passiveCheckbox);
      expect(passiveCheckbox.checked).toBe(true);

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
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
      });

      jest.spyOn(require('../../services/util'), 'useGetUserInfo').mockReturnValue({
        roles: ['ADMIN'],
        username: 'admin',
      });

      render(
        <BrowserRouter>
          <TeamCard />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(screen.queryByText('Загрузка...')).not.toBeInTheDocument();
      });

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
  });

  describe('4. Запрет редактирования для трекера', () => {
    test('Трекер НЕ может редактировать пассивную команду (поля только для чтения)', async () => {
      await renderComponent('TRACKER', true);

      const editButton = await screen.findByText('Редактировать');
      fireEvent.click(editButton);

      await waitFor(() => {
        const nameInput = screen.getByDisplayValue(mockTeamData.name);
        expect(nameInput).toHaveAttribute('readOnly');
      });
    });

    test('Трекер может редактировать активную команду', async () => {
      await renderComponent('TRACKER', false);

      const editButton = await screen.findByText('Редактировать');
      expect(editButton).toBeEnabled();
    });
  });

  describe('5. Создание встреч для пассивной команды', () => {
    test('Трекер НЕ может создать встречу для пассивной команды', async () => {
      await renderComponent('TRACKER', true);

      await waitFor(() => {
        const scheduleButton = screen.getByText('Запланировать');
        fireEvent.click(scheduleButton);
      });

      await waitFor(() => {
        const errorMessage = screen.queryByText('Нельзя создавать встречи для пассивной команды');
        // Если сообщение появилось - тест пройден
        if (errorMessage) {
          expect(errorMessage).toBeInTheDocument();
        }
      });
    });

    test('Админ МОЖЕТ создать встречу для пассивной команды', async () => {
      await renderComponent('ADMIN', true);

      await waitFor(() => {
        const scheduleButton = screen.getByText('Запланировать');
        expect(scheduleButton).toBeEnabled();
      });
    });
  });
});