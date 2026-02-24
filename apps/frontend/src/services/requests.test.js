import { fetchReports, fetchStreams, fetchTrackers } from './requests';

// Mock global fetch
global.fetch = jest.fn();

describe('fetchReports', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should use correct URL for different page/size', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchReports(2, 20);

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

    await fetchTrackers(2, 20);

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('page=2&size=20&sort=username,asc'),
      expect.any(Object)
    );
  });
  it('should use correct URL for different page/size and sort', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchTrackers(2, 20, ["username,desc"]);

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

    await fetchStreams(2, 20);

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('page=2&size=20&sort=name,asc'),
      expect.any(Object)
    );
  });
  it('should use correct URL for different page/size and sort', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchStreams(2, 20, ["name,desc"]);

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('page=2&size=20&sort=name,desc'),
      expect.any(Object)
    );
  });
});

