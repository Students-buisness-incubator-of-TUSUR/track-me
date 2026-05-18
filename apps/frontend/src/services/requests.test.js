import { fetchReports, fetchTrackers, fetchStreams, fetchTeams, fetchUserInfo, fetchUserPhoto, updateUserInfo, updateUserPhoto, fetchMeetingReportExcel, fetchMeetingReport, fetchUserTeams, fetchReportExcel } from './requests';

// Mock global fetch
global.fetch = jest.fn();

describe('fetchMeetingReport', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should construct correct URL with page, size and array of sort', async () => {
    fetch.mockResolvedValue({ ok: true });
    const params = {
      streamId: 'uuid-123',
      filters: [{ fieldName: 'team', type: 'EQ', value: 'Alpha' }],
      page: 0,
      size: 10,
      sort: ['name,asc', 'date,desc']
    };

    await fetchMeetingReport(params);

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('api/v1/meetings/reports?streamId=uuid-123&page=0&size=10&sort=name,asc&sort=date,desc'),
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ filters: params.filters })
      })
    );
  });

  it('should handle single sort string and undefined page/size', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchMeetingReport({ streamId: '1', sort: 'name,asc' });

    const callUrl = fetch.mock.calls[0][0];
    expect(callUrl).toContain('sort=name,asc');
    expect(callUrl).not.toContain('page=');
    expect(callUrl).not.toContain('size=');
    
    expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({ filters: [] });
  });
});

describe('fetchMeetingReportExcel', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should set correct headers and handle sort array', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchMeetingReportExcel({ 
      streamId: '456', 
      sort: ['team,asc', 'status,desc'],
      filters: null 
    });

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('reports/excel?streamId=456&sort=team,asc&sort=status,desc'),
      expect.objectContaining({
        headers: expect.objectContaining({
          'Accept': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        })
      })
    );
  });

  it('should handle single sort string', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchMeetingReportExcel({ streamId: '456', sort: 'single,asc' });

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('sort=single,asc'),
      expect.any(Object)
    );
  });
});


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

describe('fetchUserTeams', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should call correct endpoint with username', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchUserTeams('testuser');

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('admin/team-cards/by-user?username=testuser'),
      expect.objectContaining({
        method: 'GET',
        headers: expect.objectContaining({
          'Accept': 'application/json'
        })
      })
    );
  });
});

describe('fetchReportExcel', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should call correct endpoint for excel report', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchReportExcel({ filters: [{ fieldName: 'status', type: 'EQ', value: 'OK' }] });

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('team-cards/reports/excel'),
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          'Accept': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        })
      })
    );
  });
});
