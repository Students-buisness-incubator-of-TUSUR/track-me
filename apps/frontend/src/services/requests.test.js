// fetchReports.test.js
import { fetchReports } from './requests'; // adjust path as needed

// Mock global fetch
global.fetch = jest.fn();

describe('fetchReports', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should make request to correct URL with page and size params', async () => {
    fetch.mockResolvedValue({ ok: true });

    await fetchReports(1, 10);

    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch).toHaveBeenCalledWith(
      'https://api.trackme.test.startup-poligon.com/backend/api/v1/team-cards/reports?page=1&size=10',
      expect.any(Object)
    );
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

