import type { components } from './generated';

type ErrorResponse = components['schemas']['ErrorResponse'];
type HelloResponse = components['schemas']['HelloResponse'];

// Calls the backend hello endpoint defined in OpenAPI.
const DEFAULT_TIMEOUT_MS = 8000;
const TRACE_ID_HEADER = 'X-Request-Id';

export class ApiError extends Error {
  code?: string;
  details?: ErrorResponse['details'];
  status?: number;
  traceId?: string;
  isTimeout = false;

  constructor(message: string, options: Partial<ApiError> = {}) {
    super(message);
    this.name = 'ApiError';
    Object.assign(this, options);
  }
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError';
}

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
