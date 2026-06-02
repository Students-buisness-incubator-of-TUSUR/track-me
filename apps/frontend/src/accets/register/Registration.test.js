import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import axios from 'axios';
import Registration from './Registration';

// Мокаем axios для перехвата сетевых запросов
jest.mock('axios');

// Мокаем react-router-dom, так как компонент импортирует useLocation
jest.mock('react-router-dom', () => ({
    useLocation: () => ({ search: '' })
}));

describe('Registration Component Tests', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        // Перехватываем стандартный alert, чтобы тесты не падали
        jest.spyOn(window, 'alert').mockImplementation(() => {});
        
        // Сбрасываем window.location.search перед каждым тестом
        delete window.location;
        window.location = { search: '' };
        
        // Мокаем URL.createObjectURL для загрузки изображений
        window.URL.createObjectURL = jest.fn().mockReturnValue('mock-image-url');
    });

    test('автоматически заполняет email и парсит его из URL параметров', () => {
        window.location.search = '?email=test%2Buser%40gmail.com&name=Игорь%20Е';
        
        render(<Registration />);
        
        const emailInput = screen.getByLabelText(/E-mail/i);
        expect(emailInput.value).toBe('test+user@gmail.com');
    });

    test('показывает ошибку (alert), если пароли не совпадают', () => {
        render(<Registration />);
        
        // Заполняем обязательные поля
        fireEvent.change(screen.getByLabelText(/ФИО/i), { target: { value: 'Иванов Иван' } });
        fireEvent.change(screen.getByLabelText(/E-mail/i), { target: { value: 'ivan@test.com' } });
        fireEvent.change(screen.getByLabelText(/Телефон/i), { target: { value: '89991112233' } });
        fireEvent.change(screen.getByLabelText(/Пароль/i), { target: { value: 'password123' } });
        fireEvent.change(screen.getByLabelText(/Повторите пароль/i), { target: { value: 'password321' } }); // Разные пароли
        
        const submitButton = screen.getByRole('button', { name: /Зарегистрироваться/i });
        fireEvent.click(submitButton);
        
        expect(window.alert).toHaveBeenCalledWith('Пароли не совпадают!');
        expect(axios.post).not.toHaveBeenCalled();
    });

    test('успешно отправляет форму регистрации и загружает аватарку', async () => {
        // Настраиваем успешный ответ от бэкенда для регистрации
        axios.post.mockResolvedValueOnce({ data: { success: true } });
        // Настраиваем успешный ответ для загрузки фото
        axios.post.mockResolvedValueOnce({ data: { photoUrl: 'uploaded-path' } });

        render(<Registration />);
        
        // Заполняем все поля формы
        fireEvent.change(screen.getByLabelText(/ФИО/i), { target: { value: 'Еремин Игорь Владимирович' } });
        fireEvent.change(screen.getByLabelText(/E-mail/i), { target: { value: 'igor@test.com' } });
        fireEvent.change(screen.getByLabelText(/Телефон/i), { target: { value: '89992223344' } });
        fireEvent.change(screen.getByLabelText(/Имя пользователя Telegram/i), { target: { value: 'igor_eremin' } });
        fireEvent.change(screen.getByLabelText(/Пароль/i), { target: { value: 'securePass1' } });
        fireEvent.change(screen.getByLabelText(/Повторите пароль/i), { target: { value: 'securePass1' } });
        
        const roleSelect = screen.getByRole('combobox');
        fireEvent.change(roleSelect, { target: { value: 'TRACKER' } });

        // Симулируем выбор файла аватарки
        const file = new File(['dummy content'], 'avatar.png', { type: 'image/png' });
        const fileInput = screen.getByPlaceholderText('', { selector: 'input[type="file"]' }); 
        // Так как инпут скрыт (display: none), ищем его по контейнеру или типу
        const hiddenFileInput = document.getElementById('photo-upload');
        fireEvent.change(hiddenFileInput, { target: { files: [file] } });

        // Отправляем форму
        const submitButton = screen.getByRole('button', { name: /Зарегистрироваться/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            // Проверяем первый запрос (регистрация пользователя)
            expect(axios.post).getMockImplementation();
            expect(axios.post).toHaveBeenCalledWith(
                expect.stringContaining('/api/v1/auth/sing-up'),
                expect.objectContaining({
                    fullName: 'Еремин Игорь Владимирович',
                    firstName: 'Еремин', // сплит по пробелу в коде берет первый элемент как lastName, второй как firstName
                    lastName: 'Еремин',
                    email: 'igor@test.com',
                    role: 'TRACKER'
                }),
                expect.any(Object)
            );
        });

        await waitFor(() => {
            // Проверяем второй запрос (отправка аватарки)
            expect(axios.post).toHaveBeenCalledWith(
                expect.stringContaining('/api/v1/users/igor_eremin/photo'),
                expect.any(FormData),
                expect.any(Object)
            );
        });
    });

    test('обрабатывает ошибку сервера при регистрации', async () => {
        const errorMessage = 'Пользователь с таким Email уже существует';
        axios.post.mockRejectedValueOnce({
            response: {
                data: { message: errorMessage }
            }
        });

        render(<Registration />);
        
        fireEvent.change(screen.getByLabelText(/ФИО/i), { target: { value: 'Иван Иванов' } });
        fireEvent.change(screen.getByLabelText(/E-mail/i), { target: { value: 'ivan@test.com' } });
        fireEvent.change(screen.getByLabelText(/Телефон/i), { target: { value: '89991112233' } });
        fireEvent.change(screen.getByLabelText(/Пароль/i), { target: { value: '123' } });
        fireEvent.change(screen.getByLabelText(/Повторите пароль/i), { target: { value: '123' } });
        
        const submitButton = screen.getByRole('button', { name: /Зарегистрироваться/i });
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(window.alert).toHaveBeenCalledWith(`Ошибка при регистрации: ${errorMessage}`);
        });
    });
});