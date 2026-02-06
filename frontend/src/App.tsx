import { useEffect, useState } from 'react';
import { ApiError, fetchHello } from './api/helloClient';
import './App.css';

/**
 * Renders the hello message returned by the API.
 * <pre>
 * Responsibilities:
 * 1) Trigger the hello API call once when the component mounts.
 * 2) Render the success message when the request completes.
 * 3) Render a user-friendly error message when the request fails.
 * </pre>
 */
export default function App() {
  /** Hello message displayed in the UI. */
  const [message, setMessage] = useState('Loading...');
  /** Error message displayed when the request fails. */
  const [error, setError] = useState('');

  /**
   * Loads the hello message on mount.
   * <pre>
   * Algorithm:
   * 1) Call fetchHello() from the API client.
   * 2) Store response.message in local state on success.
   * 3) Normalize ApiError, Error, and unknown failures to display text.
   * </pre>
   */
  useEffect(() => {
    fetchHello()
      .then((data) => setMessage(data.message))
      .catch((err: unknown) => {
        if (err instanceof ApiError) {
          setError(err.message);
          return;
        }
        if (err instanceof Error) {
          setError(err.message);
          return;
        }
        setError('Unexpected error');
      });
  }, []);

  return (
    <main className="app-shell">
      <h1>Codex React + Spring Boot</h1>
      {error ? <p role="alert">{error}</p> : <p>{message}</p>}
    </main>
  );
}
