import { useState } from "react";
import "./header.css"
import { Link, useNavigate } from 'react-router-dom';

export default function Header({ userRole = "" }) {
    const navigate = useNavigate();
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
    const logoutHost = (process.env.REACT_APP_BACKEND_URI || 'http://localhost:8080') + '/logout';
    const openFeedback = () => {
        window.dispatchEvent(new Event('open-feedback'));
    };
    const toggleProfileMenu = () => {
        setIsProfileMenuOpen(!isProfileMenuOpen);
    };
    return (
        <>
            <header className="header_container">
                <div className="header_left-side" onClick={() => navigate("/streams")}>
                    <img src="/images/logo.svg" alt="logo" />
                    <span>TrackMe</span>
                </div>
                <div className="header_right-side">
                    <div className="header_nav-wrapper">
                        {userRole === "SUPER_ADMIN" &&
                            <Link to="/list-admins">
                                <button className="header_nav-btn">Администраторы</button>
                            </Link>
                        }
                        {(userRole === "SUPER_ADMIN" || userRole === "ADMIN") && 
                            <Link to="/list-trackers">
                                <button className="header_nav-btn">Трекеры</button>
                            </Link>
                        }
                        <Link to="/all-team-cards"><button className="header_nav-btn">Все команды</button></Link>
                        <Link to="/report"><button className="header_nav-btn">Отчётность</button></Link>
                    </div>
                    <button className="header_account-btn" onClick={toggleProfileMenu}>
                        <img src="/images/personal-acc.svg" alt="Account" className="header_account-icon" />
                    </button>
                    {isProfileMenuOpen && (
                        <div className="ProfileDropdown">
                            <Link to="/profile" className="ProfileDropdown-item">
                                Личный кабинет
                            </Link>
                            <Link onClick={handleLogout} to={logoutHost} className="ProfileDropdown-item logout">
                                Выход
                            </Link>
                        </div>
                    )}
                </div>
                <div className="header_mobile-container">
                    <button className="header_menu-button" onClick={() => setIsMenuOpen(!isMenuOpen)}>
                        <div className="header_hamburger">
                            <span></span>
                            <span></span>
                            <span></span>
                        </div>
                    </button>
                    {isMenuOpen && (
                        <div className="header_mobile-menu">
                            {userRole === "SUPER_ADMIN" &&
                                <Link to="/list-admins">
                                    <button className="header_menu-item">Администраторы</button>
                                </Link>}
                            <Link to="/list-trackers"><button className="header_menu-item">Трекеры</button></Link>
                            <Link to="/all-team-cards"><button className="header_menu-item">Все команды</button></Link>
                            <Link to="/report"> <button className="header_menu-item separator">Отчётность</button></Link>
                            <Link to="/profile"><button className="header_menu-item">Личный кабинет</button></Link>
                            <button className="header_menu-item" onClick={() => { setIsMenuOpen(!isMenuOpen); openFeedback() }}>Обратная связь</button>
                            <Link onClick={handleLogout} to={logoutHost}><button className="header_menu-item logout">Выйти</button></Link>
                        </div>
                    )}
                </div>
            </header>
        </>
    );
}