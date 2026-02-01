import { useEffect, useState } from 'react';
import { ApiError, fetchHello } from './api/helloClient';

/** Renders the hello message returned by the API. */
export default function App() {
  /** Hello message displayed in the UI. */
  const [message, setMessage] = useState('Loading...');
  /** Error message displayed when the request fails. */
  const [error, setError] = useState('');

  /** Loads the hello message on mount. */
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
    <main style={{ fontFamily: 'Arial, sans-serif', padding: '2rem' }}>
      <h1>Codex React + Spring Boot</h1>
      {error ? <p role="alert">{error}</p> : <p>{message}</p>}
    </main>
  );
}
