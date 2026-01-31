import type { HelloResponse } from './generated';

// Calls the backend hello endpoint defined in OpenAPI.
export async function fetchHello(): Promise<HelloResponse> {
  const response = await fetch('/api/hello');
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }
  return (await response.json()) as HelloResponse;
}
