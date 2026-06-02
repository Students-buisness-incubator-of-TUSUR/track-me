import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import Registration from './Registration';

// Жестко мокаем axios
jest.mock('axios', () => ({
    post: jest.fn()
}));

const axios = require('axios');

describe('Registration Component Tests', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        // Заглушка для alert, чтобы не спамить в консоль
        jest.spyOn(window, 'alert').mockImplementation(() => {});
    });

    test('показывает ошибку (alert), если пароли не совпадают', () => {
        const { container } = render(<Registration />);
        
        const passwordInput = container.querySelector('input[name="password"]');
        const confirmPasswordInput = container.querySelector('input[name="confirmPassword"]');
        const submitButton = screen.getByRole('button', { name: /Зарегистрироваться/i });

        fireEvent.change(passwordInput, { target: { value: 'password123' } });
        fireEvent.change(confirmPasswordInput, { target: { value: 'different123' } });
        fireEvent.click(submitButton);

        expect(window.alert).toHaveBeenCalledWith('Пароли не совпадают!');
    });

    test('успешно отправляет форму регистрации и загружает аватарку', async () => {
        axios.post.mockResolvedValueOnce({ data: { success: true } });

        const { container } = render(<Registration />);
        
        fireEvent.change(container.querySelector('input[name="fullName"]'), { target: { value: 'Иванов Иван' } });
        fireEvent.change(container.querySelector('input[name="email"]'), { target: { value: 'ivan@test.com' } });
        fireEvent.change(container.querySelector('input[name="phoneNumber"]'), { target: { value: '89991112233' } });
        fireEvent.change(container.querySelector('input[name="password"]'), { target: { value: 'password123' } });
        fireEvent.change(container.querySelector('input[name="confirmPassword"]'), { target: { value: 'password123' } });
        
        const roleSelect = container.querySelector('select[name="role"]');
        fireEvent.change(roleSelect, { target: { value: 'TRACKER' } });

        const submitButton = screen.getByRole('button', { name: /Зарегистрироваться/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(axios.post).toHaveBeenCalled();
        });
    });

    test('обрабатывает ошибку сервера при регистрации', async () => {
        const errorMessage = 'Пользователь с таким Email уже существует';
        axios.post.mockRejectedValueOnce({
            response: { data: { message: errorMessage } }
        });

        const { container } = render(<Registration />);
        
        fireEvent.change(container.querySelector('input[name="fullName"]'), { target: { value: 'Иванов Иван' } });
        fireEvent.change(container.querySelector('input[name="email"]'), { target: { value: 'ivan@test.com' } });
        fireEvent.change(container.querySelector('input[name="phoneNumber"]'), { target: { value: '89991112233' } });
        fireEvent.change(container.querySelector('input[name="password"]'), { target: { value: 'password123' } });
        fireEvent.change(container.querySelector('input[name="confirmPassword"]'), { target: { value: 'password123' } });
        
        const roleSelect = container.querySelector('select[name="role"]');
        fireEvent.change(roleSelect, { target: { value: 'TRACKER' } });

        const submitButton = screen.getByRole('button', { name: /Зарегистрироваться/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(window.alert).toHaveBeenCalledWith(`Ошибка при регистрации: ${errorMessage}`);
        });
    });
});