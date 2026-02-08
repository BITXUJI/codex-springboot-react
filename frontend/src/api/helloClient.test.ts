import { ApiError, fetchHello } from './helloClient';

/** Tests for the hello API client. */
describe('fetchHello', () => {
  /** Keeps the original fetch implementation. */
  const originalFetch = global.fetch;

  /** Restores fetch and clears mocks after each test. */
  afterEach(() => {
    global.fetch = originalFetch;
    jest.clearAllMocks();
  });

  /**
   * Successful requests return the payload.
   *
   * <pre>
   * Theme: API success
   * Test view: Successful requests return the payload
   * Test conditions: fetch resolves with ok response
   * Test result: Payload is returned and fetch options include a signal
   * </pre>
   */
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

  /**
   * Error responses surface the API message.
   *
   * <pre>
   * Theme: API errors
   * Test view: Error responses surface the API message
   * Test conditions: fetch resolves with non-ok response and error payload
   * Test result: ApiError message matches the payload
   * </pre>
   */
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

  /**
   * Invalid error payloads fall back to a generic message.
   *
   * <pre>
   * Theme: API errors
   * Test view: Invalid error payloads fall back to a generic message
   * Test conditions: Non-ok response without JSON error shape
   * Test result: Error message includes HTTP status
   * </pre>
   */
  it('falls back to a generic error when payload is not a valid error response', async () => {
    const fetchMock = jest.fn().mockResolvedValue({
      ok: false,
      status: 502,
      headers: {
        get: (name: string) => (name === 'content-type' ? null : null),
      },
      json: async () => ({ message: 'ignored' }),
    });
    global.fetch = fetchMock as typeof fetch;

    await expect(fetchHello()).rejects.toThrow('Request failed: 502');
  });

  /**
   * Invalid JSON payloads still keep HTTP error semantics.
   *
   * <pre>
   * Theme: API errors
   * Test view: Invalid JSON payloads still keep HTTP error semantics
   * Test conditions: Non-ok response advertises JSON but parsing throws
   * Test result: Error message includes HTTP status
   * </pre>
   */
  it('falls back to HTTP error when error payload JSON cannot be parsed', async () => {
    const fetchMock = jest.fn().mockResolvedValue({
      ok: false,
      status: 502,
      headers: {
        get: (name: string) =>
          name === 'content-type' ? 'application/json' : null,
      },
      json: async () => {
        throw new SyntaxError('Unexpected token');
      },
    });
    global.fetch = fetchMock as typeof fetch;

    await expect(fetchHello()).rejects.toThrow('Request failed: 502');
  });

  /**
   * Header trace id is used when payload is blank.
   *
   * <pre>
   * Theme: API errors
   * Test view: Header trace id is used when payload is blank
   * Test conditions: Error payload traceId is empty and header is present
   * Test result: ApiError uses header trace id
   * </pre>
   */
  it('uses the header trace id when payload trace id is blank', async () => {
    expect.assertions(2);
    const fetchMock = jest.fn().mockResolvedValue({
      ok: false,
      status: 400,
      headers: {
        get: (name: string) => {
          if (name === 'content-type') {
            return 'application/json';
          }
          if (name === 'X-Request-Id') {
            return 'trace-header';
          }
          return null;
        },
      },
      json: async () => ({
        code: 'BAD_REQUEST',
        message: 'Bad request',
        traceId: '',
        timestamp: '2026-02-01T12:34:56Z',
        path: '/api/hello',
      }),
    });
    global.fetch = fetchMock as typeof fetch;

    try {
      await fetchHello();
    } catch (error) {
      expect(error).toBeInstanceOf(ApiError);
      expect((error as ApiError).traceId).toBe('trace-header');
    }
  });

  /**
   * Trace id remains undefined when absent.
   *
   * <pre>
   * Theme: API errors
   * Test view: Trace id remains undefined when absent
   * Test conditions: Error payload traceId is empty and header missing
   * Test result: ApiError trace id is undefined
   * </pre>
   */
  it('leaves trace id undefined when payload and header are empty', async () => {
    expect.assertions(2);
    const fetchMock = jest.fn().mockResolvedValue({
      ok: false,
      status: 400,
      headers: {
        get: (name: string) => (name === 'content-type' ? 'application/json' : null),
      },
      json: async () => ({
        code: 'BAD_REQUEST',
        message: 'Bad request',
        traceId: '',
        timestamp: '2026-02-01T12:34:56Z',
        path: '/api/hello',
      }),
    });
    global.fetch = fetchMock as typeof fetch;

    try {
      await fetchHello();
    } catch (error) {
      expect(error).toBeInstanceOf(ApiError);
      expect((error as ApiError).traceId).toBeUndefined();
    }
  });

  /**
   * Unexpected failures are reported as network errors.
   *
   * <pre>
   * Theme: API errors
   * Test view: Unexpected failures are reported as network errors
   * Test conditions: fetch rejects with non-ApiError
   * Test result: Network error message is returned
   * </pre>
   */
  it('wraps unexpected errors as network errors', async () => {
    const fetchMock = jest.fn().mockRejectedValue(new TypeError('network down'));
    global.fetch = fetchMock as typeof fetch;

    await expect(fetchHello()).rejects.toThrow(
      'Network error. Please check your connection.',
    );
  });

  /**
   * Requests time out when they exceed the limit.
   *
   * <pre>
   * Theme: API timeout
   * Test view: Requests time out when they exceed the limit
   * Test conditions: fetch never resolves and timeout elapses
   * Test result: ApiError signals timeout
   * </pre>
   */
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
