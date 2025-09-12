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

  it('должен обрабатывать неверный формат данных', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ invalid: "data" }), // Invalid format
      })
    );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() =>
      expect(result.current.error).toMatch(/Неверный формат данных/)
    );
    expect(result.current.trackers).toEqual([]);
    expect(result.current.totalPages).toBe(1);
    expect(result.current.totalElements).toBe(0);
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
      { username: 'user2', enabled: true }, // активный пользователь первый
      { username: 'user1', enabled: false }, // неактивный пользователь второй
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

    // Test handleNextPage (lines 108-109)
    act(() => {
      result.current.handleNextPage();
    });
    expect(result.current.page).toBe(1);

    // Test handlePrevPage (lines 122-123)
    act(() => {
      result.current.handlePrevPage();
    });
    expect(result.current.page).toBe(0);

    // Test handleFirstPage (line 90)
    act(() => {
      result.current.setPage(2); // Set to a different page
      result.current.handleFirstPage();
    });
    expect(result.current.page).toBe(0);

    // Test handleLastPage (line 91)
    act(() => {
      result.current.handleLastPage();
    });
    expect(result.current.page).toBe(2);
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
});