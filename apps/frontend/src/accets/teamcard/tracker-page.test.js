import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import TrackerPage from './TrackerPage';
import { MemoryRouter } from 'react-router-dom';
import * as redux from 'react-redux';

jest.mock('./personal_account_1.png', () => 'mock-profile-icon.png');

// Мок useSelector для пользователя
jest.mock('react-redux', () => ({
  ...jest.requireActual('react-redux'),
  useSelector: jest.fn(),
}));

describe('TrackerPage - профильное меню', () => {
  beforeEach(() => {
    redux.useSelector.mockImplementation(() => ({
      username: 'testuser',
      roles: ['TRACKER'],
    }));

    Storage.prototype.removeItem = jest.fn();
    Storage.prototype.setItem = jest.fn();
    jest.clearAllMocks();
  });

  const renderComponent = () => {
    render(
      <MemoryRouter>
        <TrackerPage />
      </MemoryRouter>
    );
  };

  test('кнопка профиля отображается и открывает меню', async () => {
    renderComponent();

    const profileButton = document.querySelector('.Stream-pic');
    expect(profileButton).toBeInTheDocument();

    fireEvent.click(profileButton);
    expect(screen.getByText('Личный кабинет')).toBeInTheDocument();
    expect(screen.getByText('Выход')).toBeInTheDocument();
  });

  test('повторный клик по кнопке профиля скрывает меню', async () => {
    renderComponent();

    const profileButton = document.querySelector('.Stream-pic');
    fireEvent.click(profileButton);
    expect(screen.getByText('Личный кабинет')).toBeInTheDocument();

    fireEvent.click(profileButton);
    await waitFor(() => {
      expect(screen.queryByText('Личный кабинет')).not.toBeInTheDocument();
    });
  });

  test('при клике на "Выход" очищается localStorage', async () => {
    renderComponent();

    const profileButton = document.querySelector('.Stream-pic');
    fireEvent.click(profileButton);

    const logoutLink = screen.getByText('Выход');
    fireEvent.click(logoutLink);

    expect(localStorage.removeItem).toHaveBeenCalledWith('user');
    expect(localStorage.removeItem).toHaveBeenCalledWith('userRole');
    expect(localStorage.removeItem).toHaveBeenCalledWith('streamName');
    expect(localStorage.removeItem).toHaveBeenCalledWith('streamId');
    expect(localStorage.removeItem).toHaveBeenCalledWith('streamSDate');
    expect(localStorage.removeItem).toHaveBeenCalledWith('streamEDate');
  });
});
