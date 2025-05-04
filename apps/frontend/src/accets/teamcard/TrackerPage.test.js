import '@testing-library/jest-dom';
import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import TrackerPage from './TrackerPage';
import { MemoryRouter } from 'react-router-dom';

const mockStore = configureStore([]);
const store = mockStore({
  user: {
    user: {
      roles: ['TRACKER'],
      username: 'testuser',
    },
  },
});

describe('TrackerPage', () => {
  beforeEach(() => {
    render(
      <Provider store={store}>
        <MemoryRouter>
          <TrackerPage />
        </MemoryRouter>
      </Provider>
    );
  });

  test('рендерит заголовок Track-me', () => {
    const heading = screen.getByText(/track-me/i);
    expect(heading).toBeInTheDocument();
  });

  test('рендерит кнопку выхода', () => {
    const logoutButton = screen.getByText(/выход/i);
    expect(logoutButton).toBeInTheDocument();
  });

  test('кнопка выхода кликабельна', () => {
    const logoutButton = screen.getByText(/выход/i);
    fireEvent.click(logoutButton);
    // Можно добавить проверку перехода или вызова, если используешь useNavigate / window.location
  });

  test('рендерится кнопка создания карточки', () => {
    const createCardButton = screen.getByText(/\+ создать карточку/i);
    expect(createCardButton).toBeInTheDocument();
  });

  test('рендерится сообщение "Ничего не найдено"', () => {
    const message = screen.getByText(/ничего не найдено/i);
    expect(message).toBeInTheDocument();
  });
});
