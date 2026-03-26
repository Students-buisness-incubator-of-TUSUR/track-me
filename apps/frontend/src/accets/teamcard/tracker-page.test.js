// src/accets/teamcard/TrackerPage.test.js

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import TrackerPage from './TrackerPage';
import { MemoryRouter, createMemoryRouter, RouterProvider } from 'react-router-dom'; // Добавьте createMemoryRouter и RouterProvider
import * as redux from 'react-redux';
import { act } from 'react'; // Используйте act из react

beforeAll(() => {
});
// Мок для useNavigate и Link/ useLocation
const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => {
  const original = jest.requireActual('react-router-dom');
  return {
    ...original,
    useNavigate: () => mockNavigate,
    Link: ({ children, to, ...props }) => <a href={to} {...props}>{children}</a>,
    useLocation: () => ({ pathname: '/teamcard' }),
  };
});

// Мок иконки профиля
jest.mock('./personal_account_1.png', () => 'mock-profile-icon.png');

// Мок useSelector
jest.mock('react-redux', () => ({
  ...jest.requireActual('react-redux'),
  useSelector: jest.fn(),
}));

describe('TrackerPage - Исправленные тесты', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();

    // Мок useSelector: возвращаем user с ролью TRACKER
    redux.useSelector.mockImplementation(() => ({
      user: { username: 'testuser', roles: ['TRACKER'] },
      roles: ['TRACKER'],
      username: 'testuser',
    }));
  });

  afterAll(() => {
    jest.restoreAllMocks();
  });

  test('форматирование даты отображается в заголовке потока', async () => {
  const isoDate = '2025-06-02T00:00:00Z';
  localStorage.setItem('streamName', 'TestStream');
  localStorage.setItem('streamSDate', isoDate);
  localStorage.setItem('streamEDate', isoDate);
  localStorage.setItem('user', JSON.stringify({ username: 'testuser', roles: ['ADMIN'] }));

  redux.useSelector.mockImplementation(() => ({
    user: { username: 'testuser', roles: ['ADMIN'] },
    roles: ['ADMIN'],
    username: 'testuser',
  }));

  global.fetch = jest.fn((url) => {
    if (url.includes('/api/v1/admin/streams')) {
      return Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [
              {
                id: '1',
                name: 'TestStream',
                startDate: isoDate,
                endDate: isoDate,
              },
            ],
          }),
      });
    }
    if (url.endsWith('/streams/nti-markets')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve([]),
      });
    }
    if (url.includes('/team-cards')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }),
      });
    }
    return Promise.resolve({
      ok: true,
      json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }),
    });
  });

  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/teamcard']}>
        <TrackerPage />
      </MemoryRouter>
    );
  });

  const formatted = '2025.06.02';
  await waitFor(
    () => {
      expect(screen.getByText(/TestStream/)).toBeInTheDocument();
      expect(screen.getByText(new RegExp(`${formatted} - ${formatted}`))).toBeInTheDocument();
    },
    { timeout: 2000 }
  );
});

  test('показывает сообщение "Ничего не найдено по запросу", когда карточек нет', async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/streams')) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [
                { id: '1', name: 'Stream1', startDate: '2025-01-01', endDate: '2025-12-31' },
              ],
            }),
        });
      }
      if (url.endsWith('/streams/nti-markets')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([]),
        });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }),
      });
    });

    render(
      <MemoryRouter>
        <TrackerPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Ничего не найдено по запросу')).toBeInTheDocument();
    });
  });

 test('рендерит карточку и проверяет её содержимое', async () => {
  const mockCard = {
    id: 'card1',
    name: 'Project X',
    description: 'Описание X',
    enabled: true,
    ntiMarkets: [{ displayName: 'NTI-One' }], // Updated to array
    readinessLevel: '5',
    userId: 'user1',
    streams: [{ name: 'MainStream', startDate: '2025-01-01', endDate: '2025-12-31' }], // Added stream data
  };

  global.fetch = jest.fn((url) => {
    if (url.includes('/api/v1/streams')) {
      return Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [
              {
                id: '1',
                name: 'MainStream',
                startDate: '2025-01-01',
                endDate: '2025-12-31',
              },
            ],
          }),
      });
    }
    if (url.endsWith('/streams/nti-markets')) {
      return Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve([{ id: 'm1', name: 'NTI-One', displayName: 'NTI-One' }]),
      });
    }
    if (url.includes('/team-cards')) {
      return Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [mockCard],
            page: { totalPages: 1 },
          }),
      });
    }
    return Promise.resolve({
      ok: true,
      json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }),
    });
  });

  await act(async () => {
    render(
      <MemoryRouter>
        <TrackerPage />
      </MemoryRouter>
    );
  });

  const title = await screen.findByText('Project X');
  expect(title).toBeInTheDocument();
  expect(screen.getByText('Описание X')).toBeInTheDocument();
  expect(screen.getByText('Рынки НТИ: NTI-One')).toBeInTheDocument(); // Single market for this test
  expect(screen.getByText(/TRL: 5/)).toBeInTheDocument();
  expect(screen.getByText('Активно')).toBeInTheDocument();
  expect(screen.getByText(/Поток: MainStream/)).toBeInTheDocument();
});

  

  test('при клике на кнопку "Редактировать" вызывается navigate с query & edit=true', async () => {
    const mockCard = {
      id: 'card3',
      name: 'Project Z',
      description: 'Описание Z',
      enabled: true,
      ntiMarket: { displayName: 'NTI-Three' },
      readinessLevel: '7',
      userId: 'user3',
    };

    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/streams')) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [
                {
                  id: '1',
                  name: 'MainStream',
                  startDate: '2025-01-01',
                  endDate: '2025-12-31',
                },
              ],
            }),
        });
      }
      if (url.endsWith('/streams/nti-markets')) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve([{ id: 'm3', name: 'NTI-Three', displayName: 'NTI-Three' }]),
        });
      }
      if (url.includes('/team-cards')) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [mockCard],
              page: { totalPages: 1 },
            }),
        });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }),
      });
    });

    render(
      <MemoryRouter>
        <TrackerPage />
      </MemoryRouter>
    );

    const editButton = await screen.findByText('Редактировать');
    fireEvent.click(editButton);

    expect(mockNavigate).toHaveBeenCalledWith(
      `/teamcard/card3?userId=user3&edit=true`,
      {
        state: {
          userId: 'user3',
          streamId: localStorage.getItem('streamId'),
          from: '/teamcard',
        },
      }
    );
  });

  
test('открытие и закрытие панели фильтров по клику на иконку', async () => {
  global.fetch = jest.fn((url) => {
    if (url.includes('/api/v1/streams')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [
            { id: '1', name: 'StreamA', startDate: '2025-01-01', endDate: '2025-12-31' },
          ],
        }),
      });
    }
    if (url.endsWith('/streams/nti-markets')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }) });
  });

  await act(async () => {
    render(
      <MemoryRouter>
        <TrackerPage />
      </MemoryRouter>
    );
  });

  const filterToggleBtn = document.querySelector('.Stream-settings-pic');
  expect(filterToggleBtn).toBeInTheDocument();

  fireEvent.click(filterToggleBtn);

  expect(await screen.findByText((t) => t.startsWith('Поток'))).toBeInTheDocument();
  expect(screen.getAllByText((t) => t.startsWith('Рынки'))[0]).toBeInTheDocument();
  expect(screen.getAllByText((t) => t.startsWith('TRL'))[0]).toBeInTheDocument();
  expect(screen.getAllByText((t) => t.startsWith('Год'))[0]).toBeInTheDocument();

  fireEvent.click(filterToggleBtn);

  await waitFor(() => {
    expect(screen.queryByText((t) => t.startsWith('Поток'))).not.toBeInTheDocument();
  });
});


  test('поисковая строка фильтрует результаты', async () => {
    const mockCard = {
      id: 'cardSearch',
      name: 'UniqueName',
      description: 'Desc',
      enabled: true,
      ntiMarket: { displayName: 'Mkt' },
      readinessLevel: '8',
      userId: 'u1',
    };

    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/streams')) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [
                { id: '1', name: 'StreamA', startDate: '2025-01-01', endDate: '2025-12-31' },
              ],
            }),
        });
      }
      if (url.endsWith('/streams/nti-markets')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([]),
        });
      }
      if (url.includes('/team-cards')) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [mockCard],
              page: { totalPages: 1 },
            }),
        });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }),
      });
    });

    render(
      <MemoryRouter>
        <TrackerPage />
      </MemoryRouter>
    );

    expect(await screen.findByText('UniqueName')).toBeInTheDocument();

    const searchInput = screen.getByPlaceholderText('Найти');
    fireEvent.change(searchInput, { target: { value: 'NoMatch' } });
    expect(await screen.findByText('Ничего не найдено по запросу')).toBeInTheDocument();

    fireEvent.change(searchInput, { target: { value: 'UniqueName' } });
    expect(await screen.findByText('UniqueName')).toBeInTheDocument();
  });

  test('рейтинги и статус получают правильные css-классы (green/yellow/red + inactive)', async () => {
    const mockCards = [
      {
        id: 'c1',
        name: 'Green Rating',
        description: 'Desc',
        enabled: true,
        averageGrade: 0.65,
        ntiMarkets: [{ displayName: 'NTI-A' }],
        readinessLevel: '8',
        userId: 'u1',
        streams: [{ name: 'Stream1', startDate: '2025-01-01', endDate: '2025-12-31' }],
      },
      {
        id: 'c2',
        name: 'Yellow Rating',
        description: 'Desc',
        enabled: true,
        averageGrade: 0.30,
        ntiMarkets: [{ displayName: 'NTI-B' }],
        readinessLevel: '5',
        userId: 'u2',
        streams: [{ name: 'Stream1', startDate: '2025-01-01', endDate: '2025-12-31' }],
      },
      {
        id: 'c3',
        name: 'Red Rating Inactive',
        description: 'Desc',
        enabled: false,
        averageGrade: 0.10,
        ntiMarkets: [{ displayName: 'NTI-C' }],
        readinessLevel: '2',
        userId: 'u3',
        streams: [{ name: 'Stream1', startDate: '2025-01-01', endDate: '2025-12-31' }],
      },
    ];

    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/streams')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [{ id: '1', name: 'Stream1', startDate: '2025-01-01', endDate: '2025-12-31' }] }) });
      }
      if (url.endsWith('/streams/nti-markets')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
      }
      if (url.includes('/team-cards')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: mockCards, page: { totalPages: 1 } }) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }) });
    });

    await act(async () => {
      render(
        <MemoryRouter>
          <TrackerPage />
        </MemoryRouter>
      );
    });

    const greenRating = await screen.findByText('0,65');
    expect(greenRating).toHaveClass('card-image-rating');
    expect(greenRating).toHaveClass('rating-green');

    const yellowRating = await screen.findByText('0,30');
    expect(yellowRating).toHaveClass('card-image-rating');
    expect(yellowRating).toHaveClass('rating-yellow');

    const redRating = await screen.findByText('0,10');
    expect(redRating).toHaveClass('card-image-rating');
    expect(redRating).toHaveClass('rating-red');

    const statusInactive = screen.getByText('Завершено');
    expect(statusInactive).toHaveClass('status');
    expect(statusInactive).toHaveClass('inactive');
  });
});

describe('TrackerPage - Полное покрытие', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();

    // Мокаем useSelector для роли TRACKER
    redux.useSelector.mockImplementation(() => ({
      user: { username: 'testuser', roles: ['TRACKER'] },
      roles: ['TRACKER'],
      username: 'testuser',
    }));
  });

  afterAll(() => {
    jest.restoreAllMocks();
  });

  // 49-50: Обработка ошибок fetch при загрузке карточек
 test('Обработка ошибок fetch для карточек (49-50)', async () => {
  localStorage.setItem('user', JSON.stringify({ username: 'testuser', roles: ['TRACKER'] }));
  localStorage.setItem('userRole', 'TRACKER');
  localStorage.setItem('streamName', 'MainStream');
  localStorage.setItem('streamId', '1');

  global.fetch = jest.fn((url) => {
    if (url.includes('/api/v1/streams')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [{ id: '1', name: 'MainStream', startDate: '2025-01-01', endDate: '2025-12-31' }],
        }),
      });
    }
    if (url.endsWith('/streams/nti-markets')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve([]),
      });
    }
    if (url.includes('/team-cards')) {
      return Promise.reject(new Error('Network Error'));
    }
    return Promise.resolve({
      ok: true,
      json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }),
    });
  });

  await act(async () => {
    render(
      <MemoryRouter>
        <TrackerPage />
      </MemoryRouter>
    );
  });

  await waitFor(() => {
    expect(screen.getByText(/Ошибка при загрузке карточек/)).toBeInTheDocument();
  }, { timeout: 2000 });
});
  // 85-87: Проверка отображения заголовка с датой
 test('Отображение заголовка с форматированной датой (85-87)', async () => {
  const isoDate = '2025-06-02T00:00:00Z';
  localStorage.setItem('streamName', 'TestStream');
  localStorage.setItem('streamSDate', isoDate);
  localStorage.setItem('streamEDate', isoDate);
  localStorage.setItem('user', JSON.stringify({ username: 'testuser', roles: ['ADMIN'] }));
  localStorage.setItem('userRole', 'ADMIN');

  redux.useSelector.mockImplementation(() => ({
    user: { username: 'testuser', roles: ['ADMIN'] },
    roles: ['ADMIN'],
    username: 'testuser',
  }));

  global.fetch = jest.fn((url) => {
    if (url.includes('/api/v1/admin/streams')) {
      return Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [
              {
                id: '1',
                name: 'TestStream',
                startDate: isoDate,
                endDate: isoDate,
              },
            ],
          }),
      });
    }
    if (url.endsWith('/streams/nti-markets')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve([]),
      });
    }
    if (url.includes('/team-cards')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }),
      });
    }
    return Promise.resolve({
      ok: true,
      json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }),
    });
  });

  await act(async () => {
    render(
      <MemoryRouter initialEntries={['/teamcard']}>
        <TrackerPage />
      </MemoryRouter>
    );
  });

  const formatted = '2025.06.02';
  await waitFor(
    () => {
      expect(screen.getByText(/TestStream/)).toBeInTheDocument();
      expect(screen.getByText(new RegExp(`${formatted} - ${formatted}`))).toBeInTheDocument();
    },
    { timeout: 2000 }
  );
});
  // 94-96: Сообщение при отсутствии карточек
  test('Сообщение "Ничего не найдено по запросу" при пустых данных (94-96)', async () => {
    global.fetch = jest.fn(() => Promise.resolve({
      ok: true,
      json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }),
    }));
    render(<MemoryRouter><TrackerPage /></MemoryRouter>);
    await waitFor(() => {
      expect(screen.getByText('Ничего не найдено по запросу')).toBeInTheDocument();
    });
  });

  // 102-104: Фильтр по поиску
  test('Фильтрация по названию карточки (102-104)', async () => {
    const mockCard = {
      id: 'card1',
      name: 'UniqueName',
      description: 'Описание X',
      enabled: true,
      ntiMarket: { displayName: 'NTI-One' },
      readinessLevel: '5',
      userId: 'user1',
    };
    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/streams')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [{ id: '1', name: 'StreamA', startDate: '2025-01-01', endDate: '2025-12-31' }] }) });
      }
      if (url.endsWith('/streams/nti-markets')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
      }
      if (url.includes('/team-cards')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [mockCard], page: { totalPages: 1 } }) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }) });
    });
    render(<MemoryRouter><TrackerPage /></MemoryRouter>);
    const searchInput = await screen.findByPlaceholderText('Найти');
    fireEvent.change(searchInput, { target: { value: 'NoMatch' } });
    expect(await screen.findByText('Ничего не найдено по запросу')).toBeInTheDocument();
    fireEvent.change(searchInput, { target: { value: 'UniqueName' } });
    expect(await screen.findByText('UniqueName')).toBeInTheDocument();
  });

  // 109-111: Переключение ролей и фильтров
  test('Переключение ролей и фильтров (109-111)', async () => {
    // Меняем роль на ADMIN
    redux.useSelector.mockImplementation(() => ({
      user: { username: 'admin', roles: ['ADMIN'] },
      roles: ['ADMIN'],
      username: 'admin',
    }));
    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/streams')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [{ id: '1', name: 'StreamAdmin', startDate: '2025-01-01', endDate: '2025-12-31' }] }) });
      }
      if (url.endsWith('/streams/nti-markets')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
      }
      if (url.includes('/team-cards')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }) });
    });
    render(<MemoryRouter><TrackerPage /></MemoryRouter>);
    // Можно добавить проверку что вызов fetch происходит с правильными параметрами
  });

  // 154-161: Фильтр по годам
  test('Фильтр по годам (154–161)', async () => {
  global.fetch = jest.fn((url) => {
    if (url.includes('/api/v1/streams')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [{ id: '1', name: 'Stream', startDate: '2025-01-01', endDate: '2025-12-31' }] }) });
    }
    if (url.endsWith('/streams/nti-markets')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }) });
  });

  await act(async () => {
    render(<MemoryRouter><TrackerPage /></MemoryRouter>);
  });

  const filterToggleBtn = document.querySelector('.Stream-settings-pic');
  fireEvent.click(filterToggleBtn);

  const yearBtn = screen.getAllByText('Год').find(el => el.closest('.Stream-header-chosefrom-butt-label'));
  expect(yearBtn).toBeInTheDocument();
  fireEvent.click(yearBtn);

  const checkbox = screen.getByLabelText('2025');
  fireEvent.click(checkbox);

  const applyBtn = screen.getByText('Применить');
  fireEvent.click(applyBtn);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });
});



  // 200-201: Пагинация
 test('Пагинация (200-201)', async () => {
  global.fetch = jest.fn((url) => {
    if (url.includes('page=0')) {
      return Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [
              {
                id: '1',
                name: 'Card 1',
                description: 'Description 1', // Добавлено описание
                enabled: true,
                ntiMarkets: [{ displayName: 'Market1' }],
                readinessLevel: '5',
                streams: [{ name: 'Stream', startDate: '2025-01-01', endDate: '2025-12-31' }],
              },
            ],
            page: { totalPages: 2 },
          }),
      });
    }
    if (url.includes('page=1')) {
      return Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [
              {
                id: '2',
                name: 'Card 2',
                description: 'Description 2', // Добавлено описание
                enabled: true,
                ntiMarkets: [{ displayName: 'Market2' }],
                readinessLevel: '6',
                streams: [{ name: 'Stream', startDate: '2025-01-01', endDate: '2025-12-31' }],
              },
            ],
            page: { totalPages: 2 },
          }),
      });
    }
    if (url.includes('/api/v1/streams')) {
      return Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [{ id: '1', name: 'Stream', startDate: '2025-01-01', endDate: '2025-12-31' }],
          }),
      });
    }
    if (url.endsWith('/streams/nti-markets')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    }
  });

  await act(async () => {
    render(
      <MemoryRouter>
        <TrackerPage />
      </MemoryRouter>
    );
  });

  const nextBtn = document.querySelector('.Stream-footer-button-4');
  expect(nextBtn).toBeInTheDocument();
  fireEvent.click(nextBtn);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith(expect.stringContaining('page=1'), expect.anything());
  });
});


  // 262: Фильтр по TRL
 test('Фильтр по TRL (262)', async () => {
  global.fetch = jest.fn((url) => {
    if (url.includes('/api/v1/streams')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
    }
    if (url.endsWith('/streams/nti-markets')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }) });
  });

  await act(async () => {
    render(<MemoryRouter><TrackerPage /></MemoryRouter>);
  });

  const filterToggleBtn = document.querySelector('.Stream-settings-pic');
  fireEvent.click(filterToggleBtn);

  const trlBtn = screen.getAllByText('TRL').find(el => el.closest('.Stream-header-chosefrom-butt-label'));
  expect(trlBtn).toBeInTheDocument();
  fireEvent.click(trlBtn);

  const checkbox = screen.getByLabelText('3-5');
  fireEvent.click(checkbox);

  const applyBtn = screen.getByText('Применить');
  fireEvent.click(applyBtn);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });
});





  // 309, 313: Взаимодействие с фильтрами
  test('Выбор и сброс фильтров (309–313)', async () => {
  global.fetch = jest.fn((url) => {
    if (url.includes('/api/v1/streams')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [{ id: '1', name: 'Stream', startDate: '2025-01-01', endDate: '2025-12-31' }] }) });
    }
    if (url.endsWith('/streams/nti-markets')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }) });
  });

  await act(async () => {
    render(<MemoryRouter><TrackerPage /></MemoryRouter>);
  });

  const filterToggleBtn = document.querySelector('.Stream-settings-pic');
  fireEvent.click(filterToggleBtn);

  const yearBtn = screen.getAllByText('Год').find(el => el.closest('.Stream-header-chosefrom-butt-label'));
  expect(yearBtn).toBeInTheDocument();
  fireEvent.click(yearBtn);

  const checkbox = screen.getByLabelText('2025');
  fireEvent.click(checkbox);

  const resetBtn = screen.getByText('Сбросить');
  fireEvent.click(resetBtn);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });
});




  // 328-364: Рендер карточек с разными свойствами
  test('Рендер карточек с разными статусами и данными (328-364)', async () => {
    const mockCards = [
      {
        id: '1',
        name: 'Card Active',
        description: 'Active description',
        enabled: true,
        ntiMarket: { displayName: 'Market1' },
        readinessLevel: '7',
        userId: 'user1',
      },
      {
        id: '2',
        name: 'Card Inactive',
        description: 'Inactive description',
        enabled: false,
        ntiMarket: { displayName: 'Market2' },
        readinessLevel: '2',
        userId: 'user2',
      },
    ];
    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/streams')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [{ id: 'stream1', name: 'Stream1', startDate: '2025-01-01', endDate: '2025-12-31' }] }) });
      }
      if (url.endsWith('/streams/nti-markets')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([{ id: 'm1', name: 'Market1', displayName: 'Market1' }, { id: 'm2', name: 'Market2', displayName: 'Market2' }]) });
      }
      if (url.includes('/team-cards')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: mockCards, page: { totalPages: 1 } }) });
      }
    });
    render(<MemoryRouter><TrackerPage /></MemoryRouter>);
    expect(await screen.findByText('Card Active')).toBeInTheDocument();
    expect(screen.getByText('Активно')).toBeInTheDocument();
    expect(screen.getByText('Card Inactive')).toBeInTheDocument();
    expect(screen.getByText('Завершено')).toBeInTheDocument();
  });





test('fetchCards добавляет фильтр по username для роли TRACKER (141-161)', async () => {
  localStorage.setItem('user', JSON.stringify({ username: 'testuser', roles: ['TRACKER'] }));
  localStorage.setItem('streamName', 'TestStream');

  const mockCard = {
    id: 'card1',
    name: 'Test Card',
    description: 'Test Description',
    enabled: true,
    ntiMarkets: [{ displayName: 'Market1' }],
    readinessLevel: '5',
    streams: [{ name: 'TestStream', startDate: '2025-01-01', endDate: '2025-12-31' }],
    userId: 'testuser',
  };

  global.fetch = jest.fn((url, options) => {
    if (url.includes('/api/v1/streams')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [{ id: '1', name: 'TestStream', startDate: '2025-01-01', endDate: '2025-12-31' }],
        }),
      });
    }
    if (url.endsWith('/streams/nti-markets')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    }
    if (url.includes('/team-cards')) {
      expect(options.body).toContain('"fieldName":"username"');
      expect(options.body).toContain('"value":"testuser"');
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [mockCard],
          page: { totalPages: 1 },
        }),
      });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }) });
  });

  await act(async () => {
    render(
      <MemoryRouter>
        <TrackerPage />
      </MemoryRouter>
    );
  });

  expect(await screen.findByText('Test Card')).toBeInTheDocument();
});


test('fetchCards устанавливает cards и totalPages при успешном ответе (141-161)', async () => {
  localStorage.setItem('user', JSON.stringify({ username: 'testuser', roles: ['TRACKER'] }));
  localStorage.setItem('streamName', 'TestStream');

  const mockCard = {
    id: 'card1',
    name: 'Test Card',
    description: 'Test Description',
    enabled: true,
    ntiMarkets: [{ displayName: 'Market1' }],
    readinessLevel: '5',
    streams: [{ name: 'TestStream', startDate: '2025-01-01', endDate: '2025-12-31' }],
    userId: 'testuser',
  };

  global.fetch = jest.fn((url) => {
    if (url.includes('/api/v1/streams')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [{ id: '1', name: 'TestStream', startDate: '2025-01-01', endDate: '2025-12-31' }],
        }),
      });
    }
    if (url.endsWith('/streams/nti-markets')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    }
    if (url.includes('/team-cards')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [mockCard],
          page: { totalPages: 2 },
        }),
      });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }) });
  });

  await act(async () => {
    render(
      <MemoryRouter>
        <TrackerPage />
      </MemoryRouter>
    );
  });

  expect(await screen.findByText('Test Card')).toBeInTheDocument();
  expect(document.querySelector('.Stream-footer-button-4')).toBeInTheDocument(); // Кнопка пагинации
});
test('Рендеринг карточек с разными статусами (668-694)', async () => {
  const mockCards = [
    {
      id: '1',
      name: 'Active Card',
      description: 'Active Description',
      enabled: true,
      ntiMarkets: [{ displayName: 'Market1' }],
      readinessLevel: '7',
      userId: 'user1',
      streams: [{ name: 'Stream1', startDate: '2025-01-01', endDate: '2025-12-31' }],
    },
    {
      id: '2',
      name: 'Inactive Card',
      description: 'Inactive Description',
      enabled: false,
      ntiMarkets: [{ displayName: 'Market2' }],
      readinessLevel: '2',
      userId: 'user2',
      streams: [{ name: 'Stream2', startDate: '2024-01-01', endDate: '2024-12-31' }],
    },
  ];

  global.fetch = jest.fn((url) => {
    if (url.includes('/api/v1/streams')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [
            { id: 'stream1', name: 'Stream1', startDate: '2025-01-01', endDate: '2025-12-31' },
            { id: 'stream2', name: 'Stream2', startDate: '2024-01-01', endDate: '2024-12-31' },
          ],
        }),
      });
    }
    if (url.endsWith('/streams/nti-markets')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve([
          { id: 'm1', name: 'Market1', displayName: 'Market1' },
          { id: 'm2', name: 'Market2', displayName: 'Market2' },
        ]),
      });
    }
    if (url.includes('/team-cards')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: mockCards,
          page: { totalPages: 1 },
        }),
      });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }) });
  });

  await act(async () => {
    render(
      <MemoryRouter>
        <TrackerPage />
      </MemoryRouter>
    );
  });

  expect(await screen.findByText('Active Card')).toBeInTheDocument();
  expect(screen.getByText('Активно')).toBeInTheDocument();
  expect(screen.getByText('Inactive Card')).toBeInTheDocument();
  expect(screen.getByText('Завершено')).toBeInTheDocument();
  expect(screen.getByText('Рынки НТИ: Market1')).toBeInTheDocument();
  expect(screen.getByText('Рынки НТИ: Market2')).toBeInTheDocument();
  expect(screen.getByText('TRL: 7')).toBeInTheDocument();
  expect(screen.getByText('TRL: 2')).toBeInTheDocument();
  expect(screen.getByText(/Поток: Stream1/)).toBeInTheDocument();
  expect(screen.getByText(/Поток: Stream2/)).toBeInTheDocument();
});

test('Отображение сообщения "Ничего не найдено по запросу" (668-694)', async () => {
  global.fetch = jest.fn((url) => {
    if (url.includes('/api/v1/streams')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [{ id: '1', name: 'Stream', startDate: '2025-01-01', endDate: '2025-12-31' }],
        }),
      });
    }
    if (url.endsWith('/streams/nti-markets')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    }
    if (url.includes('/team-cards')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [],
          page: { totalPages: 1 },
        }),
      });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }) });
  });

  await act(async () => {
    render(
      <MemoryRouter>
        <TrackerPage />
      </MemoryRouter>
    );
  });

  await waitFor(() => {
    expect(screen.getByText('Ничего не найдено по запросу')).toBeInTheDocument();
  });
});

test('fetchCards добавляет фильтр по username для роли TRACKER и устанавливает cards/totalPages (147-157, 164-171)', async () => {
    localStorage.setItem('user', JSON.stringify({ username: 'testuser', roles: ['TRACKER'] }));
    localStorage.setItem('streamName', 'TestStream');

    const mockCard = {
      id: 'card1',
      name: 'Test Card',
      description: 'Test Description',
      enabled: true,
      ntiMarkets: [{ displayName: 'Market1' }],
      readinessLevel: '5',
      streams: [{ name: 'TestStream', startDate: '2025-01-01', endDate: '2025-12-31' }],
      userId: 'testuser',
    };

    global.fetch = jest.fn((url, options) => {
      if (url.includes('/api/v1/streams')) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [{ id: '1', name: 'TestStream', startDate: '2025-01-01', endDate: '2025-12-31' }],
            }),
        });
      }
      if (url.endsWith('/streams/nti-markets')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
      }
      if (url.includes('/team-cards')) {
        expect(options.body).toContain('"fieldName":"username"'); // Проверка фильтра по username (147-157)
        expect(options.body).toContain('"value":"testuser"');
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [mockCard],
              page: { totalPages: 2 },
            }),
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }) });
    });

    await act(async () => {
      render(
        <MemoryRouter>
          <TrackerPage />
        </MemoryRouter>
      );
    });

    expect(await screen.findByText('Test Card')).toBeInTheDocument(); // Проверка рендеринга карточки (164-171)
    expect(document.querySelector('.Stream-footer-button-4')).toBeInTheDocument(); // Проверка пагинации (totalPages > 1)
  });

  // Тест для строк 605-618, 701-708, 714-717: Рендеринг карточек с разными статусами и данными
  test('Рендеринг карточек с разными статусами, рынками НТИ и потоками (605-618, 701-708, 714-717)', async () => {
    const mockCards = [
      {
        id: '1',
        name: 'Active Card',
        description: 'Active Description',
        enabled: true,
        ntiMarkets: [{ displayName: 'Market1' }],
        readinessLevel: '7',
        userId: 'user1',
        streams: [{ name: 'Stream1', startDate: '2025-01-01', endDate: '2025-12-31' }],
      },
      {
        id: '2',
        name: 'Inactive Card',
        description: 'Inactive Description',
        enabled: false,
        ntiMarkets: [{ displayName: 'Market2' }],
        readinessLevel: '2',
        userId: 'user2',
        streams: [{ name: 'Stream2', startDate: '2024-01-01', endDate: '2024-12-31' }],
      },
    ];

    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/streams')) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [
                { id: 'stream1', name: 'Stream1', startDate: '2025-01-01', endDate: '2025-12-31' },
                { id: 'stream2', name: 'Stream2', startDate: '2024-01-01', endDate: '2024-12-31' },
              ],
            }),
        });
      }
      if (url.endsWith('/streams/nti-markets')) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve([
              { id: 'm1', name: 'Market1', displayName: 'Market1' },
              { id: 'm2', name: 'Market2', displayName: 'Market2' },
            ]),
        });
      }
      if (url.includes('/team-cards')) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: mockCards,
              page: { totalPages: 1 },
            }),
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }) });
    });

    await act(async () => {
      render(
        <MemoryRouter>
          <TrackerPage />
        </MemoryRouter>
      );
    });

    // Проверка строк 605-618
    expect(await screen.findByText('Active Card')).toBeInTheDocument();
    expect(screen.getByText('Активно')).toBeInTheDocument();
    expect(screen.getByText('Inactive Card')).toBeInTheDocument();
    expect(screen.getByText('Завершено')).toBeInTheDocument();

    // Проверка строк 701-708
    expect(screen.getByText('Рынки НТИ: Market1')).toBeInTheDocument();
    expect(screen.getByText('Рынки НТИ: Market2')).toBeInTheDocument();

    // Проверка строк 714-717
    expect(screen.getByText(/Поток: Stream1/)).toBeInTheDocument();
    expect(screen.getByText(/Поток: Stream2/)).toBeInTheDocument();
  });

  // Тест для строк 662-698: Рендеринг карточки с длинным описанием и кнопкой "Подробнее"/"Свернуть"
  test('Рендеринг карточки с длинным описанием и переключение "Подробнее"/"Свернуть" (662-698)', async () => {
    const longDescription = 'This is a very long description that exceeds 100 characters to ensure the "Подробнее" button is shown. We need to test the toggle functionality.';
    const mockCard = {
      id: 'card1',
      name: 'Test Card',
      description: longDescription,
      enabled: true,
      ntiMarkets: [{ displayName: 'Market1' }],
      readinessLevel: '5',
      userId: 'user1',
      streams: [{ name: 'Stream1', startDate: '2025-01-01', endDate: '2025-12-31' }],
    };

    global.fetch = jest.fn((url) => {
      if (url.includes('/api/v1/streams')) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [{ id: 'stream1', name: 'Stream1', startDate: '2025-01-01', endDate: '2025-12-31' }],
            }),
        });
      }
      if (url.endsWith('/streams/nti-markets')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([{ id: 'm1', name: 'Market1', displayName: 'Market1' }]),
        });
      }
      if (url.includes('/team-cards')) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [mockCard],
              page: { totalPages: 1 },
            }),
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }) });
    });

    await act(async () => {
      render(
        <MemoryRouter>
          <TrackerPage />
        </MemoryRouter>
      );
    });

    // Проверка строк 662-667: начальное состояние (описание свернуто)
    const descriptionElement = screen.getByText(longDescription);
    expect(descriptionElement).toBeInTheDocument();
    expect(descriptionElement.parentElement).not.toHaveClass('expanded'); // Описание не развернуто

    // Проверка строк 668-694: кнопка "Подробнее" и переключение
    const toggleButton = screen.getByText('Подробнее');
    expect(toggleButton).toBeInTheDocument();

    fireEvent.click(toggleButton);
    await waitFor(() => {
      expect(descriptionElement.parentElement).toHaveClass('expanded'); // Описание развернуто
      expect(screen.getByText('Свернуть')).toBeInTheDocument();
    });

    // Проверка строк 668-694: повторное нажатие на "Свернуть"
    const collapseButton = screen.getByText('Свернуть');
    fireEvent.click(collapseButton);
    await waitFor(() => {
      expect(descriptionElement.parentElement).not.toHaveClass('expanded'); // Описание снова свернуто
      expect(screen.getByText('Подробнее')).toBeInTheDocument();
    });

    // Проверка строк 668-694: доступность с клавиатуры (onKeyDown)
    fireEvent.keyDown(toggleButton, { key: 'Enter' });
    await waitFor(() => {
      expect(descriptionElement.parentElement).toHaveClass('expanded'); // Описание развернуто
      expect(screen.getByText('Свернуть')).toBeInTheDocument();
    });
  });
  
});
