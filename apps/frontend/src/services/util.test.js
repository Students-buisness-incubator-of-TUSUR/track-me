import { useGetUserInfo } from './util';
import { useSelector } from 'react-redux';

const mockLocalStorage = {
  getItem: jest.fn(),
  setItem: jest.fn(),
};
Object.defineProperty(window, 'localStorage', {
  value: mockLocalStorage,
});

jest.mock('react-redux', () => ({
  useSelector: jest.fn(),
}));

const mockUseSelector = useSelector;

describe('useGetUserInfo', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns user.user from Redux state when available', () => {
    const savedUserData = { user: { userRole: ["ADMIN"], username: 'username12' } };
    mockUseSelector.mockReturnValue(savedUserData);

    mockLocalStorage.getItem.mockReturnValue(null);

    const result = useGetUserInfo();

    expect(result).toEqual(savedUserData.user);
    expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
      'user',
      JSON.stringify(savedUserData.user)
    );
    expect(mockUseSelector).toHaveBeenCalledWith(expect.any(Function));
  });

  it('returns saved user from localStorage when no Redux user', () => {
    const savedUserData = { userRole: ["ADMIN"], username: 'username12' };
    mockLocalStorage.getItem.mockReturnValue(JSON.stringify(savedUserData));
    mockUseSelector.mockReturnValue({}); // No user.user

    const result = useGetUserInfo();

    expect(result).toEqual(savedUserData);
    expect(mockLocalStorage.getItem).toHaveBeenCalledWith('user');
  });

  it('returns null when no Redux user and no localStorage user', () => {
    mockUseSelector.mockReturnValue({});
    mockLocalStorage.getItem.mockReturnValue(null);

    const result = useGetUserInfo();

    expect(result).toBeNull();
  });
});

