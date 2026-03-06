import { useSelector } from "react-redux";

export function useGetUserInfo() {
  const user = useSelector((state) => state.user);
  const savedUser = localStorage.getItem('user');
  if (user?.user) {
    localStorage.setItem('user', JSON.stringify(user.user));
    return user.user;
  } else if (savedUser) {
    const userData = JSON.parse(savedUser);
    return userData;
  }

  return null;
}
