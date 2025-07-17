import { useDispatch } from 'react-redux';
import { clearUser } from '../store/userSlice';
import LoginService from './login-service';

// Mock dependencies
jest.mock('react-redux', () => ({
  useDispatch: jest.fn(),
}));
jest.mock('../store/userSlice', () => ({
  clearUser: jest.fn(),
}));
jest.mock('axios', () => ({
  create: jest.fn(() => ({
    interceptors: {
      response: {
        use: jest.fn(),
      },
    },
  })),
}));

describe('LoginService axios interceptor', () => {
  let mockDispatch;
  let axiosInstance;

  beforeEach(() => {
    // Clear mocks
    jest.clearAllMocks();
    // Mock useDispatch
    mockDispatch = jest.fn();
    useDispatch.mockReturnValue(mockDispatch);
    // Mock window.location.href
    jest.spyOn(window, 'location', 'get').mockReturnValue({ href: '' });
    // Mock axios.create to return a consistent instance
    axiosInstance = {
      interceptors: {
        response: {
          use: jest.fn(),
        },
      },
    };
    require('axios').create.mockReturnValue(axiosInstance);
  });

  afterEach(() => {
    // Restore spies
    jest.restoreAllMocks();
  });

  test('passes through successful responses', () => {
    // Instantiate LoginService to trigger interceptor setup
    LoginService();
    const [successCallback] = axiosInstance.interceptors.response.use.mock.calls[0];
    const response = { data: { id: 1, name: 'Test User' } };
    expect(successCallback(response)).toEqual(response);
  });

  test('dispatches clearUser and redirects on 401 error', async () => {
    // Instantiate LoginService to trigger interceptor setup
    LoginService();
    const [, errorCallback] = axiosInstance.interceptors.response.use.mock.calls[0];
    const error = { response: { status: 401 } };

    try {
      await errorCallback(error);
    } catch (e) {
      expect(e).toEqual(error);
    }

    expect(mockDispatch).toHaveBeenCalledWith(clearUser());
    expect(window.location.href).toBe('/');
  });

  test('dispatches clearUser and redirects on 302 error', async () => {
    // Instantiate LoginService to trigger interceptor setup
    LoginService();
    const [, errorCallback] = axiosInstance.interceptors.response.use.mock.calls[0];
    const error = { response: { status: 302 } };

    try {
      await errorCallback(error);
    } catch (e) {
      expect(e).toEqual(error);
    }

    expect(mockDispatch).toHaveBeenCalledWith(clearUser());
    expect(window.location.href).toBe('/');
  });

  test('rejects other errors without dispatching or redirecting', async () => {
    // Instantiate LoginService to trigger interceptor setup
    LoginService();
    const [, errorCallback] = axiosInstance.interceptors.response.use.mock.calls[0];
    const error = { response: { status: 500 } };

    try {
      await errorCallback(error);
    } catch (e) {
      expect(e).toEqual(error);
    }

    expect(mockDispatch).not.toHaveBeenCalled();
    expect(window.location.href).toBe('');
  });
});