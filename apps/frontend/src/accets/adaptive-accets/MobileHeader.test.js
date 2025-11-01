import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter, useNavigate } from 'react-router-dom';
import '@testing-library/jest-dom'; // ✅ добавили сюда
import MobileHeader from './MobileHeader';

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: jest.fn(),
}));

describe('MobileHeader component', () => {
  let mockNavigate;

  beforeEach(() => {
    mockNavigate = jest.fn();
    useNavigate.mockReturnValue(mockNavigate);
    localStorage.clear();
  });

  // ==== 14–18: useEffect ====
  it('должен загрузить роль пользователя из localStorage', () => {
    const userData = { roles: ['ADMIN'] };
    localStorage.setItem('user', JSON.stringify(userData));

    render(<MobileHeader />, { wrapper: MemoryRouter });

    expect(screen.getByText(/Track Me/i)).toBeInTheDocument();
  });

  it('должен корректно обработать невалидный JSON в localStorage', () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    localStorage.setItem('user', '{invalid json');

    render(<MobileHeader />, { wrapper: MemoryRouter });

    expect(consoleSpy).toHaveBeenCalledWith(
      expect.stringContaining('Ошибка при чтении user из localStorage:'),
      expect.any(SyntaxError)
    );
    consoleSpy.mockRestore();
  });

  // ==== 24: handleMenuClick ====
  it('должен переключать состояние меню при клике на кнопку', () => {
    render(<MobileHeader />, { wrapper: MemoryRouter });

    const menuButton = document.querySelector('.menu-button'); // ✅ исправлено
    fireEvent.click(menuButton);
    expect(screen.getByText(/Личный кабинет/i)).toBeInTheDocument();

    fireEvent.click(menuButton);
    expect(screen.queryByText(/Личный кабинет/i)).not.toBeInTheDocument();
  });

  // ==== 28–30, 36–39: handleMenuItemClick ====
  it('должен вызывать onNavigate и navigate при клике на пункт меню', () => {
    const onNavigateMock = jest.fn();
    render(<MobileHeader onNavigate={onNavigateMock} />, { wrapper: MemoryRouter });

    const menuButton = document.querySelector('.menu-button'); // ✅ исправлено
    fireEvent.click(menuButton);

    const profileButton = screen.getByText(/Личный кабинет/i);
    fireEvent.click(profileButton);

    expect(onNavigateMock).toHaveBeenCalledWith('/profile');
    expect(mockNavigate).toHaveBeenCalledWith('/profile');
  });

  // ==== 47–82: goToHomeByRole ====
  it('должен перенаправлять на /streams если роль ADMIN', () => {
    localStorage.setItem('user', JSON.stringify({ roles: ['ADMIN'] }));
    render(<MobileHeader />, { wrapper: MemoryRouter });

    const header = screen.getByText(/Track Me/i);
    fireEvent.click(header);

    expect(mockNavigate).toHaveBeenCalledWith('/streams');
  });

  it('должен перенаправлять на /team-cards если роль не ADMIN', () => {
    localStorage.setItem('user', JSON.stringify({ roles: ['USER'] }));
    render(<MobileHeader />, { wrapper: MemoryRouter });

    const header = screen.getByText(/Track Me/i);
    fireEvent.click(header);

    expect(mockNavigate).toHaveBeenCalledWith('/team-cards');
  });

  it('должен вызывать goToHomeByRole при нажатии Enter или Space', () => {
    localStorage.setItem('user', JSON.stringify({ roles: ['SUPER_ADMIN'] }));
    render(<MobileHeader />, { wrapper: MemoryRouter });

    const header = screen.getByText(/Track Me/i);
    fireEvent.keyDown(header, { key: 'Enter' });
    fireEvent.keyDown(header, { key: ' ' });

    expect(mockNavigate).toHaveBeenCalledTimes(2);
    expect(mockNavigate).toHaveBeenCalledWith('/streams');
  });
});
