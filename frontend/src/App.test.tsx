import { render, screen } from '@testing-library/react';
import App from './App';
import * as api from './api/helloClient';

// Verifies the UI renders the hello message.
jest.spyOn(api, 'fetchHello').mockResolvedValue({ message: 'Hello from Spring Boot' });

it('renders the hello message', async () => {
  render(<App />);
  expect(await screen.findByText('Hello from Spring Boot')).toBeInTheDocument();
});
