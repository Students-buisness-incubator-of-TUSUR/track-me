import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import Register from './Register';
import LoginAPI from '../../services/login-service';

// Имитируем работу API службы авторизации
jest.mock('../../services/login-service', () => ({
    register: jest.fn()
}));

describe('Register Component Coverage Tests', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        // Мокаем window.location.href
        delete window.location;
        window.location = { href: '', search: '' };
    });

    test('успешный рендеринг формы и заполнение полей', () => {
        render(<Register />);
        
        expect(screen.getByText('Регистрация')).toBeInTheDocument();
        
        // Проверяем, что кнопка заблокирована при пустой форме
        const submitBtn = screen.getByRole('button', { name: /Зарегистрироваться/i });
        expect(submitBtn).toBeDisabled();
    });

    test('чтение email и имени из параметров адресной строки (useEffect)', () => {
        window.location.search = '?email=test@tusur.ru&name=Иван+Петров';
        
        render(<Register />);
        
        expect(screen.getByPlaceholderText('Email')).value = 'test@tusur.ru';
        expect(screen.getByPlaceholderText('ФИО')).value = 'Иван Петров';
    });

    test('валидация некорректного пароля и вывод ошибок', async () => {
        render(<Register />);
        
        const passwordInput = screen.getByPlaceholderText('Пароль');
        
        // Вводим слишком простой пароль
        fireEvent.change(passwordInput, { target: { value: '123' } });
        
        // Должен отобразиться текст с требованиями к паролю
        expect(screen.getByText(/длина пароля должна быть не менее 6 символов/i)).toBeInTheDocument();
    });

    test('полный цикл регистрации: заполнение формы, открытие модалки, согласие и успешный ответ API', async () => {
        // Настраиваем успешный ответ от заглушки API
        LoginAPI.register.mockResolvedValue({ status: 200 });

        render(<Register />);

        // Заполняем валидные данные
        fireEvent.change(screen.getByPlaceholderText(/Имя пользователя в Telegram/i), { target: { value: 'ivan_tg' } });
        fireEvent.change(screen.getByPlaceholderText('Пароль'), { target: { value: 'ValidPassword1!' } });
        fireEvent.change(screen.getByPlaceholderText('ФИО'), { target: { value: 'Иванов Иван' } });
        fireEvent.change(screen.getByPlaceholderText('Email'), { target: { value: 'ivan@tusur.ru' } });
        fireEvent.change(screen.getByPlaceholderText('Номер телефона'), { target: { value: '89991112233' } });

        const submitBtn = screen.getByRole('button', { name: /Зарегистрироваться/i });
        expect(submitBtn).not.toBeDisabled();
        
        // Нажимаем "Зарегистрироваться" -> открывается модалка оферты
        fireEvent.click(submitBtn);
        expect(screen.getByText('Пользовательское соглашение')).toBeInTheDocument();

        // Кнопка "Принимаю" должна быть заблокирована, пока не нажат чекбокс
        const acceptBtn = screen.getByRole('button', { name: /Принимаю/i });
        expect(acceptBtn).toBeDisabled();

        // Отмечаем чекбокс согласия с обработкой ПДн ТУСУР
        const checkbox = screen.getByRole('checkbox');
        fireEvent.click(checkbox);
        expect(acceptBtn).not.toBeDisabled();

        // Отправляем форму из модалки
        fireEvent.click(acceptBtn);

        // Проверяем, что API был вызван с правильными объектами данных
        await waitFor(() => {
            expect(LoginAPI.register).toHaveBeenCalledWith({
                username: 'ivan_tg',
                password: 'ValidPassword1!',
                fullName: 'Иванов Иван',
                email: 'ivan@tusur.ru',
                phoneNumber: '89991112233',
                role: 'TRACKER'
            });
            expect(window.location.href).toContain('/client/registration-success');
        });
    });
});