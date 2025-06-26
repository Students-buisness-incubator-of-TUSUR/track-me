import { renderHook, act, waitFor } from '@testing-library/react';
import { useTrackerList } from './useTrackerList';

beforeEach(() => {
  jest.clearAllMocks();
  global.fetch = jest.fn();
});

describe('useTrackerList', () => {
  const endpoint = "/mock-endpoint";

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
    expect(result.current.totalPages).toBe(1);
    expect(result.current.totalElements).toBe(0);
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
        Promise.resolve({ ok: true, text: () => Promise.resolve('OK') })
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

  it('deleteUser удаляет пользователя', async () => {
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
      )
      .mockImplementationOnce(() =>
        Promise.resolve({ ok: true, text: () => Promise.resolve('OK') })
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

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.trackers.length).toBe(2));

    await act(async () => {
      await result.current.deleteUser('user1');
    });

    await waitFor(() => expect(result.current.trackers).toEqual([{ username: 'user2' }]));
    expect(result.current.totalElements).toBe(1);
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
});