import { fetchHello } from './helloClient';

describe('fetchHello', () => {
  const originalFetch = global.fetch;

  afterEach(() => {
    global.fetch = originalFetch;
    jest.clearAllMocks();
  });

  it('returns the hello payload when the request succeeds', async () => {
    const fetchMock = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ message: 'Hello from Spring Boot' }),
    });
    global.fetch = fetchMock as typeof fetch;

    await expect(fetchHello()).resolves.toEqual({ message: 'Hello from Spring Boot' });
    expect(fetchMock).toHaveBeenCalledWith('/api/hello');
  });

  it('throws when the response is not ok', async () => {
    const fetchMock = jest.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => ({ message: 'ignored' }),
    });
    global.fetch = fetchMock as typeof fetch;

    await expect(fetchHello()).rejects.toThrow('Request failed: 500');
    expect(fetchMock).toHaveBeenCalledWith('/api/hello');
  });
});
