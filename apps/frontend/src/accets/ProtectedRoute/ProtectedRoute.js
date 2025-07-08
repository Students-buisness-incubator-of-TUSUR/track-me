import { Navigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { useEffect, useState, useMemo } from 'react';
import { setUser, clearUser } from '../../store/userSlice';
import LoginService from '../../services/login-service';

const ProtectedRoute = ({ children }) => {
    const user = useSelector(state => state.user.user);
    const dispatch = useDispatch();
    const { getUserInfo } = useMemo(() => LoginService(), []);
    const [isCheckingAuth, setIsCheckingAuth] = useState(true);

    const checkAuth = async () => {
        try {
            const userInfo = await getUserInfo();
            if (userInfo) {
                dispatch(setUser(userInfo));
            } else {
                dispatch(clearUser());
            }
        } catch (error) {
            console.error("Auth check failed:", error);
            dispatch(clearUser());
        } finally {
            setIsCheckingAuth(false);
        }
    };

    useEffect(() => {
        checkAuth();

        // Периодическая проверка каждые 30 секунд
        const interval = setInterval(() => {
            checkAuth();
        }, 30000);

        // Очистка интервала при размонтировании
        return () => clearInterval(interval);
    }, [getUserInfo, dispatch]);

    if (isCheckingAuth) {
        return <div>Loading...</div>;
    }

    if (!user) {
        return <Navigate to="/?sessionExpired=true" replace />;
    }

    return children;
};

export default ProtectedRoute;