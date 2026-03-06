import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import '@testing-library/jest-dom';
import Header from './header';
import { adminRoleName, superadminRoleName, trackerRoleName } from '../../services/constants';

// Mock constants
jest.mock('../../services/constants', () => ({
    adminRoleName: 'ADMIN',
    superadminRoleName: 'SUPER_ADMIN',
    trackerRoleName: 'TRACKER',
    backendURL: 'http://localhost:3000',
}));

// Mock window.dispatchEvent
global.dispatchEvent = jest.fn();

describe('Header', () => {
    const renderWithRouter = ({ userRole = '' } = {}) => {
        return render(
            <BrowserRouter>
                <Header userRole={userRole} />
            </BrowserRouter>
        );
    };

    it('renders logo and TrackMe text', () => {
        renderWithRouter();
        expect(screen.getByAltText('logo')).toBeInTheDocument();
        expect(screen.getByText('TrackMe')).toBeInTheDocument();
    });

    it('shows "Все команды" link for all users', () => {
        renderWithRouter();
        expect(screen.getByTestId('everyone-link')).toBeInTheDocument();
        expect(screen.getByText('Все команды')).toBeInTheDocument();
    });

    it('shows tracker links', () => {
        renderWithRouter({ userRole: trackerRoleName });
        expect(screen.queryByTestId('superadmin-link')).not.toBeInTheDocument();
        expect(screen.queryByTestId('admin-link')).not.toBeInTheDocument();
    });

    it('shows admin links', () => {
        renderWithRouter({ userRole: adminRoleName });
        expect(screen.queryByTestId('superadmin-link')).not.toBeInTheDocument();
        expect(screen.getByTestId('admin-link')).toBeInTheDocument();
    });

    it('shows superadmin links', () => {
        renderWithRouter({ userRole: superadminRoleName });
        expect(screen.getByTestId('superadmin-link')).toBeInTheDocument();
        expect(screen.getByTestId('admin-link')).toBeInTheDocument();
    });

    it('toggles profile menu on account button click', () => {
        renderWithRouter();
        const accountBtn = screen.getByTestId('personal-acc-btn');

        expect(screen.queryByTestId('profile-dropdown')).not.toBeInTheDocument();

        fireEvent.click(accountBtn);
        expect(screen.getByTestId('profile-dropdown')).toBeInTheDocument();

        fireEvent.click(accountBtn);
        expect(screen.queryByTestId('profile-dropdown')).not.toBeInTheDocument();
    });

    it('clears localStorage on logout', () => {
        // Setup localStorage
        localStorage.setItem('user', 'test');
        localStorage.setItem('userRole', 'test');

        renderWithRouter();
        fireEvent.click(screen.getByTestId('personal-acc-btn'));
        fireEvent.click(screen.getByTestId('profile-logout-btn'));

        expect(localStorage.getItem('user')).toBeNull();
        expect(localStorage.getItem('userRole')).toBeNull();
    });

    it('toggles mobile hamburger menu', () => {
        renderWithRouter();
        const hamburgerBtn = screen.getByTestId('hamburger-btn');

        expect(screen.queryByTestId('hamburger-dropdown')).not.toBeInTheDocument();

        fireEvent.click(hamburgerBtn);
        expect(screen.getByTestId('hamburger-dropdown')).toBeInTheDocument();

        fireEvent.click(hamburgerBtn);
        expect(screen.queryByTestId('hamburger-dropdown')).not.toBeInTheDocument();
    });

    it('dispatches open-feedback event on feedback click', () => {
        renderWithRouter();
        fireEvent.click(screen.getByTestId('hamburger-btn')); // Open menu first
        const feedbackBtn = screen.getAllByText('Обратная связь')[0];

        fireEvent.click(feedbackBtn);

        expect(global.dispatchEvent).toHaveBeenCalledWith(expect.any(Event));
    });
});


