import type { components } from './generated';

/** Error payload returned by the backend. */
type ErrorResponse = components['schemas']['ErrorResponse'];
/** Success payload returned by the backend. */
type HelloResponse = components['schemas']['HelloResponse'];

/** Default request timeout in milliseconds. */
const DEFAULT_TIMEOUT_MS = 8000;
/** Trace header used for request correlation. */
const TRACE_ID_HEADER = 'X-Request-Id';

/** API error raised by the client helpers. */
export class ApiError extends Error {
  /** Error code associated with the failure. */
  code?: string;
  /** Structured error details, when available. */
  details?: ErrorResponse['details'];
  /** HTTP status for the failure. */
  status?: number;
  /** Trace id attached to the response or generated locally. */
  traceId?: string;
  /** Indicates a timeout failure. */
  isTimeout = false;

  /**
   * Creates a typed API error.
   *
   * @param message error message
   * @param options extra error metadata
   */
  constructor(message: string, options: Partial<ApiError> = {}) {
    super(message);
    this.name = 'ApiError';
    Object.assign(this, options);
  }
}

/**
 * Detects abort-related errors from fetch.
 *
 * @param error unknown error value
 * @returns true when the error represents an abort
 */
function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError';
}

/**
 * Validates the error response shape.
 *
 * @param payload parsed JSON payload
 * @returns true when the payload is a valid error response
 */
function isErrorResponse(payload: unknown): payload is ErrorResponse {
  if (!payload || typeof payload !== 'object') {
    return false;
  }
  const candidate = payload as ErrorResponse;
  return (
    typeof candidate.code === 'string' &&
    typeof candidate.message === 'string' &&
    typeof candidate.traceId === 'string' &&
    typeof candidate.timestamp === 'string' &&
    typeof candidate.path === 'string'
  );
}

/**
 * Builds an ApiError from a non-2xx response.
 *
 * @param status HTTP status code
 * @param payload parsed response payload
 * @param traceId trace id from headers
 * @returns ApiError instance
 */
function buildErrorFromResponse(
  status: number,
  payload: unknown,
  traceId: string | null,
): ApiError {
  if (isErrorResponse(payload)) {
    return new ApiError(payload.message, {
      status,
      code: payload.code,
      traceId: payload.traceId || traceId || undefined,
      details: payload.details,
    });
  }
  return new ApiError(`Request failed: ${status}`, {
    status,
    code: 'HTTP_ERROR',
    traceId: traceId || undefined,
  });
}

/**
 * Loads the hello message from the backend.
 *
 * @param options request options
 * @returns hello response payload
 */
export async function fetchHello(
  options: { timeoutMs?: number } = {},
): Promise<HelloResponse> {
  const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetch('/api/hello', { signal: controller.signal });
    if (!response.ok) {
      const traceId = response.headers.get(TRACE_ID_HEADER);
      const contentType = response.headers.get('content-type') ?? '';
      const payload =
        contentType.includes('application/json') ? await response.json() : null;
      throw buildErrorFromResponse(response.status, payload, traceId);
    }
    return (await response.json()) as HelloResponse;
  } catch (error) {
    if (isAbortError(error)) {
      throw new ApiError('Network timeout. Please try again.', {
        code: 'NETWORK_TIMEOUT',
        isTimeout: true,
      });
    }
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError('Network error. Please check your connection.', {
      code: 'NETWORK_ERROR',
    });
  } finally {
    window.clearTimeout(timeoutId);
  }
}
