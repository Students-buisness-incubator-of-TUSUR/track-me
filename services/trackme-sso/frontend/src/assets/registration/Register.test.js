import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import Register from './Register';

// Мокаем API сервис, чтобы не дергать реальный бэкенд
jest.mock('../../services/login-service', () => ({
  register: jest.fn(() => Promise.resolve({ status: 200 }))
}));

describe('Register Component', () => {
  const renderWithRouter = (ui) => render(ui);

  test('renders registration form correctly', () => {
    renderWithRouter(<Register />);
    expect(screen.getByText(/Регистрация/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Имя пользователя в Telegram/i)).toBeInTheDocument();
  });

  test('shows error message for short username', () => {
    renderWithRouter(<Register />);
    const usernameInput = screen.getByLabelText(/Имя пользователя в Telegram/i);
    
    // Вводим короткое имя
    fireEvent.change(usernameInput, { target: { value: 'abc' } });
    
    // Проверяем, что появилось сообщение об ошибке (оно выводится через usernameChecks)
    expect(screen.getByText(/имя пользователя должно быть не менее 6 символов/i)).toBeInTheDocument();
  });

  test('rejects submission if password validation fails', () => {
    renderWithRouter(<Register />);
    const passwordInput = screen.getByLabelText(/Пароль/i);
    const registerButton = screen.getByRole('button', { name: /Зарегистрироваться/i });

    // Вводим простой пароль (не проходит по сложности)
    fireEvent.change(passwordInput, { target: { value: '123' } });
    
    fireEvent.change(screen.getByLabelText(/Имя пользователя в Telegram/i), { target: { value: 'valid_user' } });
    fireEvent.submit(registerButton.closest('form'));
    expect(screen.getByText(/Пароль не удовлетворяет требованиям/i)).toBeInTheDocument();
  });
});
test('preserves literal plus and percent characters from OAuth registration parameters', () => {
  const originalUrl = window.location.pathname + window.location.search;
  window.history.replaceState({}, '', '/client/registration?email=user%2Btag%40example.org&name=Test%20%2B%20100%25');
  try {
    render(<Register />);
    expect(screen.getByDisplayValue('user+tag@example.org')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Test + 100%')).toBeInTheDocument();
  } finally {
    window.history.replaceState({}, '', originalUrl);
  }
});
