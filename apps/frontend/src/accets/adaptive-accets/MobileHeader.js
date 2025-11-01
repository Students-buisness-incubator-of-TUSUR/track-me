import React, { useState, useEffect } from 'react';
import './MobileHeader.css';
import { useNavigate } from 'react-router-dom';

const MobileHeader = ({ onNavigate }) => {
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const [userRole, setUserRole] = useState(null);
    const navigate = useNavigate();

    // Получаем роль пользователя при загрузке
    useEffect(() => {
        const savedUser = localStorage.getItem('user');
        if (savedUser) {
            try {
                const userData = JSON.parse(savedUser);
                setUserRole(userData.roles[0]);
            } catch (e) {
                console.error('Ошибка при чтении user из localStorage:', e);
            }
        }
    }, []);

    const handleMenuClick = () => {
        setIsMenuOpen(!isMenuOpen);
    };

    const handleMenuItemClick = (destination) => {
        setIsMenuOpen(false);
        if (onNavigate) onNavigate(destination);
        navigate(destination);
    };

    // 🔹 Функция для перехода на главную по роли
    const goToHomeByRole = () => {
        const targetPath =
            userRole === 'ADMIN' || userRole === 'SUPER_ADMIN'
                ? '/streams'
                : '/team-cards';
        navigate(targetPath);
    };

    return (
        <div className="mobile-header">
            <div
    className="header-left"
    onClick={goToHomeByRole}
    onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && goToHomeByRole()}
    role="button"
    tabIndex={0}
    style={{ cursor: 'pointer' }}
>
    <div className="Stream-header-logo" />
    <div className="mobile-header-text">Track Me</div>
</div>


            <div className="mobile-menu-container">
                <button className="menu-button" onClick={handleMenuClick}>
                    <div className={`hamburger ${isMenuOpen ? 'open' : ''}`}>
                        <span></span>
                        <span></span>
                        <span></span>
                    </div>
                </button>

                {isMenuOpen && (
                    <div className="mobile-menu">
                        <button
                            className="menu-item"
                            onClick={() => handleMenuItemClick('/profile')}
                        >
                            Личный кабинет
                        </button>
                        <button
                            className="menu-item"
                            onClick={goToHomeByRole}
                        >
                            Главная страница
                        </button>
                        <button
                            className="menu-item logout"
                            onClick={() => handleMenuItemClick('/logout')}
                        >
                            Выйти
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
};

export default MobileHeader;
