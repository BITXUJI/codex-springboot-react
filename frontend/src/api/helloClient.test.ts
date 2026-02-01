import { ApiError, fetchHello } from './helloClient';

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
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/hello',
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    );
  });

  it('throws when the response is not ok', async () => {
    const fetchMock = jest.fn().mockResolvedValue({
      ok: false,
      status: 500,
      headers: {
        get: (name: string) => (name === 'content-type' ? 'application/json' : null),
      },
      json: async () => ({
        code: 'INTERNAL_ERROR',
        message: 'Unexpected error',
        traceId: 'trace-id',
        timestamp: '2026-02-01T12:34:56Z',
        path: '/api/hello',
      }),
    });
    global.fetch = fetchMock as typeof fetch;

    await expect(fetchHello()).rejects.toThrow('Unexpected error');
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/hello',
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    );
  });

  it('times out when the request exceeds the limit', async () => {
    jest.useFakeTimers();
    const fetchMock = jest.fn().mockImplementation((_url, init) => {
      return new Promise((_, reject) => {
        if (init?.signal) {
          init.signal.addEventListener('abort', () => {
            reject(new DOMException('Aborted', 'AbortError'));
          });
        }
      });
    });
    global.fetch = fetchMock as typeof fetch;

    const request = fetchHello({ timeoutMs: 10 });
    jest.advanceTimersByTime(20);

    await expect(request).rejects.toBeInstanceOf(ApiError);
    await expect(request).rejects.toThrow('Network timeout. Please try again.');

    jest.useRealTimers();
  });
});
