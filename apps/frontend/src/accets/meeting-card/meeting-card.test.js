process.env.REACT_APP_BACKEND_URI = 'http://localhost:8080';

import React from 'react';
import { render, waitFor } from '@testing-library/react';
import MeetingCard from './meeting-card';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

// Подменим useState, чтобы задать статус "Не указано"
jest.mock('react', () => {
  const actualReact = jest.requireActual('react');
  return {
    ...actualReact,
    useState: (initial) => {
      if (typeof initial === 'object' && initial !== null && 'status' in initial) {
        return [
          { ...initial, status: "Не указано" }, // подменяем статус
          jest.fn(),
        ];
      }
      return [initial, jest.fn()];
    },
  };
});
jest.mock('./meeting-card', () => {
  const originalModule = jest.requireActual('./meeting-card');
  process.env.REACT_APP_BACKEND_URI = 'http://localhost:8080';
  return originalModule;
});

// Мокаем react-router-dom
jest.mock('react-router-dom', () => {
  const original = jest.requireActual('react-router-dom');
  return {
    ...original,
    useNavigate: () => jest.fn(),
    useLocation: () => ({
      search: '?teamId=1&username=test&userId=1',
    }),
  };
});

describe('MeetingCard компонент', () => {
  test('отображает "Не указано", когда статус пустой', () => {
    const { container } = render(
      <MemoryRouter initialEntries={['/meeting/new']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    const statusDiv = container.querySelector('.status-selected');
    expect(statusDiv).not.toBeNull();
    expect(statusDiv.textContent).toMatch(/Не указано/);
  });
  test('устанавливает статус "Не указано", если его нет в данных встречи', async () => {
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          content: [{ id: "123", number: "10", startDate: "2023-01-01" }] // без status
        }),
      })
    );

    const { container } = render(
      <MemoryRouter initialEntries={['/meeting/123']}>
        <Routes>
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      const status = container.querySelector('.unique-status');
      expect(status).not.toBeNull();
      expect(status.textContent).toBe(""); // вне режима редактирования "Не указано" не выводится
    });
  });
});
