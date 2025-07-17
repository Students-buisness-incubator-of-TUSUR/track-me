import { render, screen, waitFor } from '@testing-library/react';
import { act } from 'react-dom/test-utils'; // For act wrapping
import { useDispatch, useSelector } from 'react-redux';
import { Navigate } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import LoginService from '../../services/login-service';
import { setUser, clearUser } from '../../store/userSlice';

// Import jest-dom for toBeInTheDocument
import '@testing-library/jest-dom';

// Mock dependencies
jest.mock('react-redux', () => ({
  useDispatch: jest.fn(),
  useSelector: jest.fn(),
}));
jest.mock('react-router-dom', () => ({
  Navigate: jest.fn(() => null), // Mock Navigate as a component returning null
}));
jest.mock('../../services/login-service', () => {
  return jest.fn(() => ({
    getUserInfo: jest.fn(),
  }));
});
jest.mock('../../store/userSlice', () => ({
  setUser: jest.fn(),
  clearUser: jest.fn(),
}));

describe('ProtectedRoute', () => {
  let mockDispatch;
  let mockGetUserInfo;

  beforeEach(() => {
    // Clear mocks before each test
    jest.clearAllMocks();
    mockDispatch = jest.fn();
    useDispatch.mockReturnValue(mockDispatch);
    // Set up mockGetUserInfo
    mockGetUserInfo = jest.fn();
    LoginService.mockImplementation(() => ({
      getUserInfo: mockGetUserInfo,
    }));
    useSelector.mockImplementation((selector) =>
      selector({ user: { user: null } })
    );
  });

  test('renders loading state while checking auth', () => {
    render(<ProtectedRoute>Content</ProtectedRoute>);
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  test('renders children when user is authenticated', async () => {
    // Mock authenticated user
    useSelector.mockImplementation((selector) =>
      selector({ user: { user: { id: 1, name: 'Test User' } } })
    );
    // Mock successful getUserInfo
    mockGetUserInfo.mockResolvedValue({ id: 1, name: 'Test User' });

    await act(async () => {
      render(<ProtectedRoute>Content</ProtectedRoute>);
    });

    // Wait for auth check to complete
    await waitFor(() => {
      expect(screen.getByText('Content')).toBeInTheDocument();
    });
    expect(Navigate).not.toHaveBeenCalled();
  });

  

  test('dispatches setUser on successful auth check', async () => {
    const userInfo = { id: 1, name: 'Test User' };
    mockGetUserInfo.mockResolvedValue(userInfo);

    await act(async () => {
      render(<ProtectedRoute>Content</ProtectedRoute>);
    });

    // Wait for auth check to complete
    await waitFor(() => {
      expect(mockDispatch).toHaveBeenCalledWith(setUser(userInfo));
    });
    expect(mockGetUserInfo).toHaveBeenCalledTimes(1);
  });

  

  

  test('calls checkAuth only once on mount', async () => {
    mockGetUserInfo.mockResolvedValue({ id: 1, name: 'Test User' });

    await act(async () => {
      render(<ProtectedRoute>Content</ProtectedRoute>);
    });

    // Wait for auth check to complete
    await waitFor(() => {
      expect(mockGetUserInfo).toHaveBeenCalledTimes(1);
    });
  });

  test('handles dependencies correctly in useEffect', async () => {
    const userInfo = { id: 1, name: 'Test User' };
    mockGetUserInfo.mockResolvedValue(userInfo);

    const { rerender } = await act(async () =>
      render(<ProtectedRoute>Content</ProtectedRoute>)
    );

    await waitFor(() => {
      expect(mockGetUserInfo).toHaveBeenCalledTimes(1);
    });

    // Rerender with same dependencies
    await act(async () => rerender(<ProtectedRoute>Content</ProtectedRoute>));

    await waitFor(() => {
      expect(mockGetUserInfo).toHaveBeenCalledTimes(1); // Should not call again
    });
  });
});