import { fetchReports, fetchTrackers, fetchStreams, fetchTeams, fetchUserInfo, fetchUserPhoto, updateUserInfo, updateUserPhoto } from './requests';

// Mock global fetch
global.fetch = jest.fn();


describe('fetchReports', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should use correct URL for different page/size', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchReports({ page: 2, size: 20 });

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('page=2&size=20'),
      expect.any(Object)
    );
  });
});


describe('fetchTrackers', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should use correct URL for different page/size', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchTrackers({ page: 2, size: 20 });

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('page=2&size=20&sort=username,asc'),
      expect.any(Object)
    );
  });
  it('should use correct URL for different page/size and sort', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchTrackers({ page: 2, size: 20, sort: ["username,desc"] });

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('page=2&size=20&sort=username,desc'),
      expect.any(Object)
    );
  });
});


describe('fetchStreams', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should use correct URL for different page/size', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchStreams({ page: 2, size: 20 });

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('page=2&size=20&sort=name,asc'),
      expect.any(Object)
    );
  });
  it('should use correct URL for different page/size and sort', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchStreams({ page: 2, size: 20, sort: ["name,desc"] });

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('page=2&size=20&sort=name,desc'),
      expect.any(Object)
    );
  });
});


describe('fetchTeams', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should use correct URL for different page/size (non-admin)', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchTeams({ page: 2, size: 20, admin: false });

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('v1/team-cards?page=2&size=20'),
      expect.any(Object)
    );
  });

  it('should use admin URL when admin=true', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchTeams({ page: 1, size: 10, admin: true });

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('v1/admin/team-cards?page=1&size=10'),
      expect.any(Object)
    );
  });
});

describe('fetchUserInfo', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should use current user endpoint when username is null', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchUserInfo({});

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('account/info'),
      expect.any(Object)
    );
  });

  it('should use specific user endpoint when username provided', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchUserInfo({ username: 'testuser' });

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('users/testuser/info'),
      expect.any(Object)
    );
  });
});

describe('fetchUserPhoto', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should use current user photo endpoint when username is null', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchUserPhoto({});

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('account/photo'),
      expect.any(Object)
    );
  });

  it('should use specific user photo endpoint when username provided', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchUserPhoto({ username: 'testuser' });

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('users/testuser/photo'),
      expect.any(Object)
    );
  });
});

describe('updateUserInfo', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should send POST with user data to update endpoint', async () => {
    fetch.mockResolvedValue({ ok: true });
    const newUserData = { name: 'New Name', email: 'new@example.com' };

    await updateUserInfo({ newUserData });

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('account/update'),
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify(newUserData)
      })
    );
  });
});

describe('updateUserPhoto', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should send FormData with photo file to update endpoint', async () => {
    fetch.mockResolvedValue({ ok: true });
    const mockFile = new File(['content'], 'photo.jpg', { type: 'image/jpeg' });

    await updateUserPhoto({ newUserPhotoFile: mockFile });

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('account/photo'),
      expect.objectContaining({
        method: 'POST',
        body: expect.any(FormData)
      })
    );
  });
});

