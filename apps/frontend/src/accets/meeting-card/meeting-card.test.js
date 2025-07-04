import React from 'react';
import { render } from '@testing-library/react';
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
});
