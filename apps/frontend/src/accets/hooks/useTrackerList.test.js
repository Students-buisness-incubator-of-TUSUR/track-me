import { renderHook, act, waitFor } from '@testing-library/react';
import { useTrackerList } from './useTrackerList';

// Мокаем fetchUserTeams
jest.mock('../../services/requests', () => ({
  fetchUserTeams: jest.fn(),
}));

// Мокаем валидацию
jest.mock('../../utils/validation', () => ({
  isValidUsername: jest.fn(() => true),
}));

const { fetchUserTeams } = require('../../services/requests');
const { isValidUsername } = require('../../utils/validation');

beforeEach(() => {
  jest.clearAllMocks();
  global.fetch = jest.fn();
  fetchUserTeams.mockReset();
  isValidUsername.mockReturnValue(true);
});

describe('useTrackerList', () => {
  const endpoint = '/api/v1/users/search';

  it('должен инициализироваться с начальными значениями', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [],
            page: { totalPages: 1, totalElements: 0 },
          }),
      })
    );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.trackers).toEqual([]));
    expect(result.current.error).toBeNull();
    expect(result.current.totalPages).toBe(1);
    expect(result.current.totalElements).toBe(0);
    expect(result.current.showLockedOnly).toBe(false);
    expect(result.current.showDeleteConfirm).toBe(false);
    expect(result.current.showTeamsWarning).toBe(false);
    expect(result.current.attachedTeams).toEqual([]);
    expect(result.current.userToDelete).toBeNull();
  });

  it('должен загружать пользователей успешно', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [{ username: 'testUser', enabled: false }],
            page: { totalPages: 1, totalElements: 1 },
          }),
      })
    );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() =>
      expect(result.current.trackers).toEqual([{ username: 'testUser', enabled: false }])
    );
    expect(result.current.totalPages).toBe(1);
    expect(result.current.totalElements).toBe(1);
  });

  it('должен обрабатывать ошибку 401', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({ ok: false, status: 401 })
    );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() =>
      expect(result.current.error).toMatch(/Ошибка авторизации/)
    );
    expect(result.current.trackers).toEqual([]);
  });

  it('должен обрабатывать неверный формат данных', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ invalid: "data" }),
      })
    );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() =>
      expect(result.current.error).toMatch(/Неверный формат данных/)
    );
  });

  it('должен обрабатывать массив без пагинации', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve([
            { username: 'user1', enabled: false },
            { username: 'user2', enabled: true },
          ]),
      })
    );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() =>
      expect(result.current.trackers).toEqual([
        { username: 'user2', enabled: true },
        { username: 'user1', enabled: false },
      ])
    );
    expect(result.current.totalPages).toBe(1);
    expect(result.current.totalElements).toBe(2);
  });

  it('confirmUser обновляет enabled', async () => {
    global.fetch
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [{ username: 'testUser', enabled: false }],
              page: { totalPages: 1, totalElements: 1 },
            }),
        })
      )
      .mockImplementationOnce(() =>
        Promise.resolve({ ok: true })
      )
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [{ username: 'testUser', enabled: true }],
              page: { totalPages: 1, totalElements: 1 },
            }),
        })
      );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() =>
      expect(result.current.trackers).toEqual([{ username: 'testUser', enabled: false }])
    );

    await act(async () => {
      await result.current.confirmUser('testUser');
    });

    await waitFor(() =>
      expect(result.current.trackers).toEqual([{ username: 'testUser', enabled: true }])
    );
  });

  it('toggleUserLock блокирует пользователя (showLockedOnly=false)', async () => {
    global.fetch
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [{ username: 'user1', enabled: true }],
              page: { totalPages: 1, totalElements: 1 },
            }),
        })
      )
      .mockImplementationOnce(() =>
        Promise.resolve({ ok: true })
      )
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [{ username: 'user1', enabled: false }],
              page: { totalPages: 1, totalElements: 1 },
            }),
        })
      );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.trackers.length).toBe(1));

    await act(async () => {
      await result.current.toggleUserLock('user1');
    });

    await waitFor(() =>
      expect(result.current.trackers[0].enabled).toBe(false)
    );
  });

  it('toggleUserLock разблокирует пользователя (showLockedOnly=true)', async () => {
    global.fetch
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [{ username: 'user1', enabled: true }],
              page: { totalPages: 1, totalElements: 1 },
            }),
        })
      )
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [{ username: 'user1', enabled: false }],
              page: { totalPages: 1, totalElements: 1 },
            }),
        })
      )
      .mockImplementationOnce(() =>
        Promise.resolve({ ok: true })
      )
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [{ username: 'user1', enabled: true }],
              page: { totalPages: 1, totalElements: 1 },
            }),
        })
      );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.trackers.length).toBe(1));

    await act(async () => {
      result.current.toggleShowLocked();
    });

    await waitFor(() => expect(result.current.showLockedOnly).toBe(true));

    await act(async () => {
      await result.current.toggleUserLock('user1');
    });

    await waitFor(() =>
      expect(result.current.trackers[0].enabled).toBe(true)
    );
  });

  it('handleDeleteClick открывает модалку с командами', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [{ username: 'user1', enabled: true }],
            page: { totalPages: 1, totalElements: 1 },
          }),
      })
    );

    fetchUserTeams.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve([{ id: '1', name: 'Team 1' }, { id: '2', name: 'Team 2' }]),
      })
    );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.trackers.length).toBe(1));

    await act(async () => {
      await result.current.handleDeleteClick('user1');
    });

    await waitFor(() => {
      expect(result.current.showTeamsWarning).toBe(true);
      expect(result.current.attachedTeams).toEqual([
        { id: '1', name: 'Team 1' },
        { id: '2', name: 'Team 2' },
      ]);
      expect(result.current.userToDelete).toBe('user1');
    });
  });

  it('handleDeleteClick открывает подтверждение если команд нет', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [{ username: 'user1', enabled: true }],
            page: { totalPages: 1, totalElements: 1 },
          }),
      })
    );

    fetchUserTeams.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve([]),
      })
    );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.trackers.length).toBe(1));

    await act(async () => {
      await result.current.handleDeleteClick('user1');
    });

    await waitFor(() => {
      expect(result.current.showDeleteConfirm).toBe(true);
      expect(result.current.attachedTeams).toEqual([]);
      expect(result.current.userToDelete).toBe('user1');
    });
  });

  it('handleDeleteClick обрабатывает ошибку загрузки команд', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [{ username: 'user1', enabled: true }],
            page: { totalPages: 1, totalElements: 1 },
          }),
      })
    );

    fetchUserTeams.mockImplementationOnce(() =>
      Promise.resolve({
        ok: false,
      })
    );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.trackers.length).toBe(1));

    await act(async () => {
      await result.current.handleDeleteClick('user1');
    });

    await waitFor(() => {
      expect(result.current.showDeleteConfirm).toBe(true);
      expect(result.current.userToDelete).toBe('user1');
    });
  });

  it('confirmDeleteUser удаляет пользователя', async () => {
    global.fetch
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [{ username: 'user1' }, { username: 'user2' }],
              page: { totalPages: 1, totalElements: 2 },
            }),
        })
      );

    fetchUserTeams.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve([{ id: '1', name: 'Team 1' }]),
      })
    );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.trackers.length).toBe(2));

    await act(async () => {
      await result.current.handleDeleteClick('user1');
    });

    await waitFor(() => {
      expect(result.current.userToDelete).toBe('user1');
    });

    act(() => {
      result.current.closeTeamsWarning();
    });

    expect(result.current.showDeleteConfirm).toBe(true);

    global.fetch
      .mockImplementationOnce(() =>
        Promise.resolve({ ok: true })
      )
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [{ username: 'user2' }],
              page: { totalPages: 1, totalElements: 1 },
            }),
        })
      );

    await act(async () => {
      await result.current.confirmDeleteUser();
    });

    await waitFor(() => {
      expect(result.current.trackers).toEqual([{ username: 'user2' }]);
      expect(result.current.showDeleteConfirm).toBe(false);
      expect(result.current.userToDelete).toBeNull();
    });
  });

  it('confirmDeleteUser не делает ничего если userToDelete null', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [{ username: 'user1' }],
            page: { totalPages: 1, totalElements: 1 },
          }),
      })
    );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.trackers.length).toBe(1));

    await act(async () => {
      await result.current.confirmDeleteUser();
    });

    expect(result.current.trackers.length).toBe(1);
  });

  it('closeTeamsWarning закрывает предупреждение и открывает подтверждение', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [{ username: 'user1', enabled: true }],
            page: { totalPages: 1, totalElements: 1 },
          }),
      })
    );

    fetchUserTeams.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve([{ id: '1', name: 'Team 1' }]),
      })
    );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.trackers).toHaveLength(1));

    await act(async () => {
      await result.current.handleDeleteClick('user1');
    });

    await waitFor(() => {
      expect(result.current.showTeamsWarning).toBe(true);
    });

    act(() => {
      result.current.closeTeamsWarning();
    });

    expect(result.current.showTeamsWarning).toBe(false);
    expect(result.current.showDeleteConfirm).toBe(true);
  });

  it('cancelTeamsWarning закрывает предупреждение и сбрасывает состояние', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [{ username: 'user1', enabled: true }],
            page: { totalPages: 1, totalElements: 1 },
          }),
      })
    );

    fetchUserTeams.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve([{ id: '1', name: 'Team 1' }]),
      })
    );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.trackers).toHaveLength(1));

    await act(async () => {
      await result.current.handleDeleteClick('user1');
    });

    await waitFor(() => {
      expect(result.current.showTeamsWarning).toBe(true);
      expect(result.current.userToDelete).toBe('user1');
    });

    act(() => {
      result.current.cancelTeamsWarning();
    });

    expect(result.current.showTeamsWarning).toBe(false);
    expect(result.current.userToDelete).toBeNull();
    expect(result.current.attachedTeams).toEqual([]);
  });

  it('setSearchQuery обновляет строку поиска', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [],
            page: { totalPages: 1, totalElements: 0 },
          }),
      })
    );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.trackers).toEqual([]));

    act(() => {
      result.current.setSearchQuery('новый');
    });

    expect(result.current.searchQuery).toBe('новый');
  });

  it('должен обрабатывать навигацию по страницам', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [{ username: 'user1' }],
            page: { totalPages: 3, totalElements: 45 },
          }),
      })
    );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.totalPages).toBe(3));

    act(() => {
      result.current.handleNextPage();
    });
    expect(result.current.page).toBe(1);

    act(() => {
      result.current.handlePrevPage();
    });
    expect(result.current.page).toBe(0);

    act(() => {
      result.current.setPage(2);
      result.current.handleFirstPage();
    });
    expect(result.current.page).toBe(0);

    act(() => {
      result.current.handleLastPage();
    });
    expect(result.current.page).toBe(2);

    act(() => {
      result.current.setPage(0);
    });
    expect(result.current.page).toBe(0);
    
    act(() => {
      result.current.handlePageJump(1);
    });
    expect(result.current.page).toBe(1);
  });

  it('должен устанавливать hoveredTracker и hoveredButton', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [{ username: 'user1' }],
            page: { totalPages: 1, totalElements: 1 },
          }),
      })
    );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.trackers.length).toBe(1));

    act(() => {
      result.current.setHoveredTracker('user1');
    });
    expect(result.current.hoveredTracker).toBe('user1');

    act(() => {
      result.current.setHoveredButton('confirm');
    });
    expect(result.current.hoveredButton).toBe('confirm');
  });

  it('toggleShowLocked должен переключать фильтр и сбрасывать страницу', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [{ username: 'user1', enabled: true }],
            page: { totalPages: 3, totalElements: 45 },
          }),
      })
    );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.trackers.length).toBe(1));

    act(() => {
      result.current.setPage(2);
    });

    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [{ username: 'lockedUser', enabled: false }],
            page: { totalPages: 1, totalElements: 1 },
          }),
      })
    );

    act(() => {
      result.current.toggleShowLocked();
    });

    expect(result.current.showLockedOnly).toBe(true);
    expect(result.current.page).toBe(0);
  });

  it('showLockedOnly переключается через toggleShowLocked', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [],
            page: { totalPages: 1, totalElements: 0 },
          }),
      })
    );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.trackers).toEqual([]));

    act(() => {
      result.current.toggleShowLocked();
    });
    expect(result.current.showLockedOnly).toBe(true);

    act(() => {
      result.current.toggleShowLocked();
    });
    expect(result.current.showLockedOnly).toBe(false);
  });

  it('fetchTrackers вызывается при изменении showLockedOnly', async () => {
    global.fetch
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [{ username: 'active', enabled: true }],
              page: { totalPages: 1, totalElements: 1 },
            }),
        })
      )
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [{ username: 'locked', enabled: false }],
              page: { totalPages: 1, totalElements: 1 },
            }),
        })
      );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() =>
      expect(result.current.trackers).toEqual([{ username: 'active', enabled: true }])
    );

    await act(async () => {
      result.current.toggleShowLocked();
    });

    await waitFor(() =>
      expect(result.current.trackers).toEqual([{ username: 'locked', enabled: false }])
    );
    expect(result.current.showLockedOnly).toBe(true);
  });

  it('fetchTrackers обрабатывает ошибку сети', async () => {
    global.fetch.mockImplementationOnce(() => Promise.reject(new Error('Network failure')));

    const { result } = renderHook(() => useTrackerList(endpoint));
      
    await waitFor(() => {
      expect(result.current.error).toBe('Network failure');
      expect(result.current.trackers).toEqual([]);
    });
  });
  
  it('выбрасывает ошибку при невалидном endpoint', async () => {
    const { result } = renderHook(() => useTrackerList('/invalid-endpoint'));
    
    await waitFor(() => {
      expect(result.current.error).toBe('Invalid endpoint: /invalid-endpoint');
    });
  });
});