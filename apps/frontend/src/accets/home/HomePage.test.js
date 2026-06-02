import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import HomePage from './HomePage';

// Глобальный мок для react-router-dom, чтобы управлять параметрами строки поиска
const mockSearch = jest.fn().mockReturnValue('');
jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useLocation: () => ({
        search: mockSearch()
    })
}));

describe('HomePage Component Coverage Tests', () => {
    beforeEach(() => {
        mockSearch.mockReturnValue('');
        // Мокаем window.location для проверки редиректов
        delete window.location;
        window.location = { 
            href: '', 
            origin: 'http://localhost:3000' 
        };
    });

    test('успешный рендеринг главной страницы без предупреждения о сессии', () => {
        render(<HomePage />);
        
        expect(screen.getByText('Добро пожаловать в TrackMe')).toBeInTheDocument();
        // Сообщение об истекшей сессии не должно отображаться
        expect(screen.queryByText(/Ваша сессия истекла/i)).not.toBeInTheDocument();
    });

    test('отображение предупреждения, если сессия истекла', () => {
        // Симулируем урл вида: ?sessionExpired=true
        mockSearch.mockReturnValue('?sessionExpired=true');
        
        render(<HomePage />);
        
        expect(screen.getByText(/Ваша сессия истекла. Пожалуйста, авторизируйтесь заново./i)).toBeInTheDocument();
    });

    test('редирект на эндпоинт авторизации Google при клике', () => {
        render(<HomePage />);
        
        const googleBtn = screen.getByRole('button', { name: /google/i });
        fireEvent.click(googleBtn);
        
        // Проверяем, куда улетает пользователь
        expect(window.location.href).toContain('/oauth2/authorization/google');
        expect(window.location.href).toContain('redirect_uri=' + encodeURIComponent('http://localhost:3000/register'));
    });

    test('редирект на эндпоинт авторизации Yandex при клике', () => {
        render(<HomePage />);
        
        const yandexBtn = screen.getByRole('button', { name: /yandex/i });
        fireEvent.click(yandexBtn);
        
        expect(window.location.href).toContain('/oauth2/authorization/yandex');
        expect(window.location.href).toContain('redirect_uri=' + encodeURIComponent('http://localhost:3000/register'));
    });

    test('редирект на эндпоинт авторизации основного SSO при клике', () => {
        render(<HomePage />);
        
        const ssoBtn = screen.getByRole('button', { name: /Войти через SSO/i });
        fireEvent.click(ssoBtn);
        
        expect(window.location.href).toContain('/oauth2/authorization/track-me-client');
        expect(window.location.href).toContain('redirect_uri=' + encodeURIComponent('http://localhost:3000/after-login'));
    });
});