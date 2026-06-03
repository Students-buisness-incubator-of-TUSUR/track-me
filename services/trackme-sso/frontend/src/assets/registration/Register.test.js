import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import Register from './Register';
import { BrowserRouter } from 'react-router-dom';

// Мокаем API сервис, чтобы не дергать реальный бэкенд
jest.mock('../../services/login-service', () => ({
  register: jest.fn(() => Promise.resolve({ status: 200 }))
}));

describe('Register Component', () => {
  const renderWithRouter = (ui) => render(<BrowserRouter>{ui}</BrowserRouter>);

  test('renders registration form correctly', () => {
    renderWithRouter(<Register />);
    expect(screen.getByText(/Регистрация/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Имя пользователя в Telegram/i)).toBeInTheDocument();
  });

  test('shows error message for short username', () => {
    renderWithRouter(<Register />);
    const usernameInput = screen.getByPlaceholderText(/Имя пользователя в Telegram/i);
    
    // Вводим короткое имя
    fireEvent.change(usernameInput, { target: { value: 'abc' } });
    
    // Проверяем, что появилось сообщение об ошибке (оно выводится через usernameChecks)
    expect(screen.getByText(/имя пользователя должно быть не менее 6 символов/i)).toBeInTheDocument();
  });

  test('button is disabled if password validation fails', () => {
    renderWithRouter(<Register />);
    const passwordInput = screen.getByPlaceholderText(/Пароль/i);
    const registerButton = screen.getByRole('button', { name: /Зарегистрироваться/i });

    // Вводим простой пароль (не проходит по сложности)
    fireEvent.change(passwordInput, { target: { value: '123' } });
    
    expect(registerButton).toBeDisabled();
  });
});