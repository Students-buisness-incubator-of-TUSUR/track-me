import "./header.css"
export default function Header() {
    return (
        <header className="header_container">
            <div className="header_left-side">
                <img src="/images/logo.svg" alt="logo"/>
                <span>TrackMe</span>
            </div>
            <div className="header_right-side">
                <div className="header_nav-wrapper">
                    <button className="header_nav-btn">Администраторы</button>
                    <button className="header_nav-btn">Трекеры</button>
                    <button className="header_nav-btn">Все команды</button>
                    <button className="header_nav-btn">Отчётность</button>
                </div>
                <button className="header_account-btn">
                    <img src="/images/personal-acc.svg" alt="Account" className="header_account-icon" />
                </button>
            </div>
        </header>
    );
}