import { Navigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { useEffect, useState } from 'react';
import { setUser, clearUser } from '../../store/userSlice';
import LoginService from '../../services/login-service';

const ProtectedRoute = ({ children }) => {
    const user = useSelector(state => state.user.user);
    const dispatch = useDispatch();
    const { getUserInfo } = LoginService();
    const [isCheckingAuth, setIsCheckingAuth] = useState(true);

    useEffect(() => {
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

        checkAuth();
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