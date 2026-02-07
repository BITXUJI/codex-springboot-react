import type { components } from './generated';

/** Error payload returned by the backend. */
type ErrorResponse = components['schemas']['ErrorResponse'];
/** Success payload returned by the backend. */
type HelloResponse = components['schemas']['HelloResponse'];

/** Default request timeout in milliseconds. */
const DEFAULT_TIMEOUT_MS = 8000;
/** Trace header used for request correlation. */
const TRACE_ID_HEADER = 'X-Request-Id';

/**
 * API error raised by the client helpers.
 * <pre>
 * Responsibilities:
 * 1) Carry a user-facing error message.
 * 2) Attach transport metadata such as status, code, and trace id.
 * 3) Distinguish timeout failures from other network failures.
 * </pre>
 */
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
   * <pre>
   * Algorithm:
   * 1) Initialize the base Error with the provided message.
   * 2) Set a stable error name for runtime checks.
   * 3) Merge optional metadata fields into the instance.
   * </pre>
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
 * <pre>
 * Algorithm:
 * 1) Check whether the value is a DOMException.
 * 2) Verify that the exception name is "AbortError".
 * 3) Return true only when both conditions are satisfied.
 * </pre>
 *
 * @param error unknown error value
 * @returns true when the error represents an abort
 */
function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError';
}

/**
 * Validates the error response shape.
 * <pre>
 * Algorithm:
 * 1) Reject null and non-object payloads.
 * 2) Cast to ErrorResponse candidate for field checks.
 * 3) Verify required fields and their primitive types.
 * </pre>
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
 * <pre>
 * Algorithm:
 * 1) Reuse backend error payload when it matches ErrorResponse.
 * 2) Fallback to a generic HTTP_ERROR when payload shape is unknown.
 * 3) Preserve trace id from payload first, then from response headers.
 * </pre>
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
 * <pre>
 * Algorithm:
 * 1) Create an AbortController with timeout-based cancellation.
 * 2) Call /api/hello and parse JSON on success.
 * 3) Convert non-2xx responses into ApiError with trace metadata.
 * 4) Map abort and unknown failures to typed network errors.
 * 5) Always clear the timeout in the finally block.
 * </pre>
 *
 * @param options request options
 * @returns hello response payload
 */
export async function fetchHello(
  options: { timeoutMs?: number } = {},
): Promise<HelloResponse> {
  const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;
  const controller = new AbortController();
  const timeoutId = globalThis.setTimeout(() => controller.abort(), timeoutMs);

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
    globalThis.clearTimeout(timeoutId);
  }
}
