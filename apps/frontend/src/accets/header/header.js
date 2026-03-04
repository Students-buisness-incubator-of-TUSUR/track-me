import { useState } from "react";
import "./header.css"
import { Link } from 'react-router-dom';
import { adminRoleName, backendURL, superadminRoleName } from "../../services/constants";
import PropTypes from "prop-types";

export default function Header({ userRole = "" }) {
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
    const handleLogout = async () => {
        localStorage.removeItem("user");
        localStorage.removeItem("userRole");
        localStorage.removeItem("streamName");
        localStorage.removeItem("streamId");
        localStorage.removeItem("streamSDate");
        localStorage.removeItem("streamEDate");
        localStorage.removeItem("csrfToken");
        localStorage.removeItem("csrfHeaderName");

    };
    const logoutHost = backendURL + '/logout';
    const openFeedback = () => {
        window.dispatchEvent(new Event('open-feedback'));
    };
    const toggleProfileMenu = () => {
        setIsProfileMenuOpen(!isProfileMenuOpen);
    };
    const defaultPage = (function() {
        switch (userRole) {
            default:
                return "/streams";
        }
    })();
    return (
        <header className="header_container">
            <Link className="header_left-side" to={defaultPage}>
                <img src="/images/logo.svg" alt="logo" />
                <span>TrackMe</span>
            </Link>
            <div className="header_right-side">
                <div className="header_nav-wrapper">
                    {userRole === superadminRoleName &&
                        <Link data-testid="superadmin-link" to="/list-admins">
                            <button className="header_nav-btn">Администраторы</button>
                        </Link>
                    }
                    {(userRole === superadminRoleName || userRole === adminRoleName) &&
                        <Link data-testid="admin-link" to="/list-trackers">
                            <button className="header_nav-btn">Трекеры</button>
                        </Link>
                    }
                    <Link data-testid="everyone-link" to="/all-team-cards"><button className="header_nav-btn">Все команды</button></Link>
                    <Link to="/report"><button className="header_nav-btn">Отчётность</button></Link>
                </div>
                <button data-testid="personal-acc-btn" className="header_account-btn" onClick={toggleProfileMenu}>
                    <img src="/images/personal-acc.svg" alt="Account" className="header_account-icon" />
                </button>
                {isProfileMenuOpen && (
                    <div data-testid="profile-dropdown" className="ProfileDropdown">
                        <Link to="/profile" className="ProfileDropdown-item">
                            Личный кабинет
                        </Link>
                        <Link data-testid="profile-logout-btn" onClick={handleLogout} to={logoutHost} className="ProfileDropdown-item logout">
                            Выход
                        </Link>
                    </div>
                )}
            </div>
            <div className="header_mobile-container">
                <button data-testid="hamburger-btn" className="header_menu-button" onClick={() => setIsMenuOpen(!isMenuOpen)}>
                    <div className="header_hamburger">
                        <span></span>
                        <span></span>
                        <span></span>
                    </div>
                </button>
                {isMenuOpen && (
                    <div data-testid="hamburger-dropdown" className="header_mobile-menu">
                        {userRole === superadminRoleName &&
                            <Link to="/list-admins">
                                <button className="header_menu-item">Администраторы</button>
                            </Link>
                        }
                        {(userRole === superadminRoleName || userRole === adminRoleName) &&
                            <Link to="/list-trackers">
                                <button className="header_menu-item">Трекеры</button>
                            </Link>
                        }
                        <Link to="/all-team-cards"><button className="header_menu-item">Все команды</button></Link>
                        <Link to="/report"> <button className="header_menu-item separator">Отчётность</button></Link>
                        <Link to="/profile"><button className="header_menu-item">Личный кабинет</button></Link>
                        <button className="header_menu-item" onClick={() => { setIsMenuOpen(!isMenuOpen); openFeedback() }}>Обратная связь</button>
                        <Link onClick={handleLogout} to={logoutHost}><button className="header_menu-item logout">Выйти</button></Link>
                    </div>
                )}
            </div>
        </header>
    );
}

Header.propTypes = {
  userRole: PropTypes.string,
};
