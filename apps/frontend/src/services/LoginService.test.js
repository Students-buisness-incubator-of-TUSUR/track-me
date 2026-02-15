import { useDispatch } from 'react-redux';
import { clearUser } from '../store/userSlice';
import LoginService from './login-service';
import api from './api';

// Mock dependencies
jest.mock('react-redux', () => ({
  useDispatch: jest.fn(),
}));
jest.mock('../store/userSlice', () => ({
  setUser: jest.fn((data) => ({ type: 'user/setUser', payload: data })),
  clearUser: jest.fn(() => ({ type: 'user/clearUser' })),
}));
jest.mock('./api', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    post: jest.fn(),
    interceptors: {
      response: { use: jest.fn() },
    },
  },
}));

describe('LoginService', () => {
  let mockDispatch;

  beforeEach(() => {
    jest.clearAllMocks();
    mockDispatch = jest.fn();
    useDispatch.mockReturnValue(mockDispatch);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('calls api.post with correct arguments in register', async () => {
    api.post.mockResolvedValue({ status: 200 });

    const { register } = LoginService();
    const userData = { username: 'test', password: '123' };
    await register(userData);

    expect(api.post).toHaveBeenCalledWith(
      '/register',
      userData,
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
        }),
      })
    );
  });

  test('calls api.post for logout', async () => {
    api.post.mockResolvedValue({ status: 200 });

    const { logout } = LoginService();
    await logout();

    expect(api.post).toHaveBeenCalledWith(
      '/logout',
      {},
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/x-www-form-urlencoded',
        }),
      })
    );
  });

  test('fetches user info and dispatches setUser', async () => {
    const userData = { id: 1, name: 'Test User', roles: ['ADMIN'] };
    api.get.mockResolvedValue({ status: 200, data: userData });

    const { getUserInfo } = LoginService();
    const result = await getUserInfo();

    expect(api.get).toHaveBeenCalledWith('/sso/api/v1/account/info');
    expect(result).toEqual(userData);
  });

  test('throws error when register fails', async () => {
    api.post.mockRejectedValue(new Error('Network error'));

    const { register } = LoginService();
    await expect(register({ username: 'test' })).rejects.toThrow('Network error');
  });

  test('throws error when getUserInfo fails', async () => {
    api.get.mockRejectedValue(new Error('Unauthorized'));

    const { getUserInfo } = LoginService();
    await expect(getUserInfo()).rejects.toThrow('Unauthorized');
  });
});
