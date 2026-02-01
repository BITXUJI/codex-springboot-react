import { useEffect, useState } from 'react';
import { ApiError, fetchHello } from './api/helloClient';

// Displays a message fetched from the Spring Boot API.
export default function App() {
  const [message, setMessage] = useState('Loading...');
  const [error, setError] = useState('');

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
