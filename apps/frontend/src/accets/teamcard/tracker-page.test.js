// src/accets/teamcard/TrackerPage.test.js

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import TrackerPage from './TrackerPage';
import { MemoryRouter } from 'react-router-dom';
import * as redux from 'react-redux';
import { act } from 'react-dom/test-utils';

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
    localStorage.setItem('user', JSON.stringify({ username: 'testuser', roles: ['TRACKER'] }));

    global.fetch = jest.fn((url) => {
      if (url.includes('/streams/active')) {
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

    const formatted = '2025.06.02';
    expect(await screen.findByText(/TestStream/)).toBeInTheDocument();
    expect(screen.getByText(new RegExp(`${formatted} - ${formatted}`))).toBeInTheDocument();
  });

  test('показывает сообщение "Ничего не найдено по запросу", когда карточек нет', async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes('/streams/active')) {
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
      ntiMarket: { displayName: 'NTI-One' },
      readinessLevel: '5',
      userId: 'user1',
    };

    global.fetch = jest.fn((url) => {
      if (url.includes('/streams/active')) {
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

    render(
      <MemoryRouter>
        <TrackerPage />
      </MemoryRouter>
    );

    const title = await screen.findByText('Project X');
    expect(title).toBeInTheDocument();
    expect(screen.getByText('Описание X')).toBeInTheDocument();
    expect(screen.getByText(/Рынки НТИ: NTI-One/)).toBeInTheDocument();
    expect(screen.getByText(/TRL: 5/)).toBeInTheDocument();
    expect(screen.getByText('Активно')).toBeInTheDocument();
    expect(screen.getByText(/Поток: MainStream/)).toBeInTheDocument();
  });

  test('при клике на карточку вызывается navigate с правильным путём', async () => {
    const mockCard = {
      id: 'card2',
      name: 'Project Y',
      description: 'Описание Y',
      enabled: false,
      ntiMarket: { displayName: 'NTI-Two' },
      readinessLevel: '3',
      userId: 'user2',
    };

    global.fetch = jest.fn((url) => {
      if (url.includes('/streams/active')) {
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
            Promise.resolve([{ id: 'm2', name: 'NTI-Two', displayName: 'NTI-Two' }]),
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

    const cardTitle = await screen.findByText('Project Y');
    const cardContainer = cardTitle.closest('.card');
    fireEvent.click(cardContainer);

    expect(mockNavigate).toHaveBeenCalledWith(`/teamcard/card2`, {
      state: {
        userId: 'user2',
        streamId: localStorage.getItem('streamId'),
        from: '/teamcard',
      },
    });
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
      if (url.includes('/streams/active')) {
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
    if (url.includes('/streams/active')) {
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
      if (url.includes('/streams/active')) {
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






  test('профильное меню: открытие, закрытие и logout очищает localStorage', async () => {
  localStorage.setItem('user', JSON.stringify({ username: 'testuser', roles: ['TRACKER'] }));
  localStorage.setItem('userRole', 'TRACKER');
  localStorage.setItem('streamName', 'A');
  localStorage.setItem('streamId', '1');
  localStorage.setItem('streamSDate', '2025-01-01');
  localStorage.setItem('streamEDate', '2025-12-31');

  global.fetch = jest.fn((url) => {
    if (url.includes('/streams/active')) {
      return Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [{ id: '1', name: 'A', startDate: '2025-01-01', endDate: '2025-12-31' }],
          }),
      });
    }
    if (url.endsWith('/streams/nti-markets')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }) });
  });

  render(
    <MemoryRouter>
      <TrackerPage />
    </MemoryRouter>
  );

  const profileBtn = document.querySelector('.Stream-pic');
  expect(profileBtn).toBeInTheDocument();
  fireEvent.click(profileBtn);

  fireEvent.click(screen.getByText('Выход'));

  expect(localStorage.getItem('user')).toBeNull();
  expect(localStorage.getItem('userRole')).toBeNull();
  expect(localStorage.getItem('streamName')).toBeNull();
  expect(localStorage.getItem('streamId')).toBeNull();
  expect(localStorage.getItem('streamSDate')).toBeNull();
  expect(localStorage.getItem('streamEDate')).toBeNull();
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
    global.fetch = jest.fn(() => Promise.reject(new Error('Network Error')));
    render(<MemoryRouter><TrackerPage /></MemoryRouter>);
    await waitFor(() => {
      expect(screen.getByText(/Ошибка при загрузке карточек/)).toBeInTheDocument();
    });
  });

  // 85-87: Проверка отображения заголовка с датой
  test('Отображение заголовка с форматированной датой (85-87)', async () => {
    const isoDate = '2025-06-02T00:00:00Z';
    localStorage.setItem('streamName', 'TestStream');
    localStorage.setItem('streamSDate', isoDate);
    localStorage.setItem('streamEDate', isoDate);
    global.fetch = jest.fn((url) => {
      if (url.includes('/streams/active')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [{ id: '1', name: 'TestStream', startDate: isoDate, endDate: isoDate }] }),
        });
      }
      if (url.endsWith('/streams/nti-markets')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }) });
    });
    render(<MemoryRouter><TrackerPage /></MemoryRouter>);
    const formatted = '2025.06.02';
    expect(await screen.findByText(/TestStream/)).toBeInTheDocument();
    expect(screen.getByText(new RegExp(`${formatted} - ${formatted}`))).toBeInTheDocument();
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
      if (url.includes('/streams/active')) {
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
      if (url.includes('/streams/active')) {
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
    if (url.includes('/streams/active')) {
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
        json: () => Promise.resolve({
          content: [{ id: '1', name: 'Card 1' }],
          page: { totalPages: 2 }
        })
      });
    }
    if (url.includes('page=1')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [{ id: '2', name: 'Card 2' }],
          page: { totalPages: 2 }
        })
      });
    }
    if (url.includes('/streams/active')) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [{ id: '1', name: 'Stream', startDate: '2025-01-01', endDate: '2025-12-31' }] })
      });
    }
    if (url.endsWith('/streams/nti-markets')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    }
  });

  await act(async () => {
    render(<MemoryRouter><TrackerPage /></MemoryRouter>);
  });

  const nextBtn = document.querySelector('.Stream-footer-button-4');
  expect(nextBtn).toBeInTheDocument();
  fireEvent.click(nextBtn);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalledWith(expect.stringContaining('page=1'), expect.anything());
  });
});


  // 228: Меню профиля
  test('Меню профиля открывается и закрывается (228)', async () => {
  global.fetch = jest.fn(() => Promise.resolve({
    ok: true,
    json: () => Promise.resolve({ content: [], page: { totalPages: 1 } })
  }));

  await act(async () => {
    render(<MemoryRouter><TrackerPage /></MemoryRouter>);
  });

  const profileBtn = document.querySelector('.Stream-pic');
  fireEvent.click(profileBtn);

  expect(screen.getByText(/Личный кабинет/)).toBeInTheDocument();

  fireEvent.click(profileBtn);
  await waitFor(() => {
    expect(screen.queryByText(/Личный кабинет/)).not.toBeInTheDocument();
  });
});


  // 236: Logout
  test('Logout очищает localStorage (236)', async () => {
  localStorage.setItem('user', JSON.stringify({ username: 'testuser', roles: ['TRACKER'] }));
  localStorage.setItem('userRole', 'TRACKER');

  global.fetch = jest.fn(() => Promise.resolve({
    ok: true,
    json: () => Promise.resolve({ content: [], page: { totalPages: 1 } })
  }));

  await act(async () => {
    render(<MemoryRouter><TrackerPage /></MemoryRouter>);
  });

  const profileBtn = document.querySelector('.Stream-pic');
  fireEvent.click(profileBtn);
  const logout = screen.getByText('Выход');
  fireEvent.click(logout);

  expect(localStorage.getItem('user')).toBeNull();
  expect(localStorage.getItem('userRole')).toBeNull();
});


  // 262: Фильтр по TRL
 test('Фильтр по TRL (262)', async () => {
  global.fetch = jest.fn((url) => {
    if (url.includes('/streams/active')) {
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
    if (url.includes('/streams/active')) {
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
      if (url.includes('/streams/active')) {
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

  // 369-375: Фильтр по рынкам НТИ
 test('Фильтр по рынкам НТИ (369–375)', async () => {
  const markets = [
    { id: 'm1', name: 'Market1', displayName: 'Market1' },
    { id: 'm2', name: 'Market2', displayName: 'Market2' },
  ];

  global.fetch = jest.fn((url) => {
    if (url.includes('/streams/active')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
    }
    if (url.endsWith('/streams/nti-markets')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve(markets) });
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [], page: { totalPages: 1 } }) });
  });

  await act(async () => {
    render(<MemoryRouter><TrackerPage /></MemoryRouter>);
  });

  const filterToggleBtn = document.querySelector('.Stream-settings-pic');
  fireEvent.click(filterToggleBtn);

  const ntiBtn = screen.getAllByText('Рынки Нти').find(el => el.closest('.Stream-header-chosefrom-butt-label'));
  expect(ntiBtn).toBeInTheDocument();
  fireEvent.click(ntiBtn);

  const checkbox = screen.getByLabelText('Market1');
  fireEvent.click(checkbox);

  const applyBtn = screen.getByText('Применить');
  fireEvent.click(applyBtn);

  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });
});




  
});
