import { renderHook, act, waitFor } from '@testing-library/react';
import { useTrackerList } from './useTrackerList';

beforeEach(() => {
  global.fetch = jest.fn(() =>
    Promise.resolve({
      ok: true,
      json: () => Promise.resolve({ content: [] }),
    })
  );
});

describe('useTrackerList', () => {
  const endpoint = "/mock-endpoint";

  it('должен инициализироваться с начальными значениями', async () => {
    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.trackers).toEqual([]));
    expect(result.current.error).toBe(null);
  });

  it('должен загружать пользователей успешно', async () => {
    fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [{ username: "testUser" }] }),
      })
    );
    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.trackers).toEqual([{ username: "testUser" }]));
  });

  it('должен обрабатывать ошибку 401', async () => {
    fetch.mockImplementationOnce(() =>
      Promise.resolve({ ok: false, status: 401 })
    );
    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.error).toMatch(/авторизац/i));
  });

  it('confirmUser обновляет enabled', async () => {
    fetch
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [{ username: "testUser", enabled: false }],
          }),
        })
      )
      .mockImplementationOnce(() =>
        Promise.resolve({ ok: true, text: () => Promise.resolve("OK") })
      );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.trackers).toEqual([{ username: "testUser", enabled: false }]));

    await act(async () => {
      await result.current.confirmUser("testUser");
    });

    expect(result.current.trackers[0].enabled).toBe(true);
  });

  it('deleteUser удаляет пользователя', async () => {
    fetch
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            content: [
              { username: "user1" },
              { username: "user2" },
            ],
          }),
        })
      )
      .mockImplementationOnce(() =>
        Promise.resolve({ ok: true, text: () => Promise.resolve("OK") })
      );

    const { result } = renderHook(() => useTrackerList(endpoint));
    await waitFor(() => expect(result.current.trackers.length).toBe(2));

    await act(async () => {
      await result.current.deleteUser("user1");
    });

    expect(result.current.trackers).toEqual([{ username: "user2" }]);
  });

  it('setSearchQuery обновляет строку поиска', () => {
    const { result } = renderHook(() => useTrackerList(endpoint));

    act(() => {
      result.current.setSearchQuery("новый");
    });

    expect(result.current.searchQuery).toBe("новый");
  });
});
