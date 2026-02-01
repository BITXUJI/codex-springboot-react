import { render, screen } from '@testing-library/react';
import App from './App';
import * as api from './api/helloClient';
import { ApiError } from './api/helloClient';

describe('App', () => {
  let fetchHelloSpy: jest.SpyInstance;

  beforeEach(() => {
    fetchHelloSpy = jest
      .spyOn(api, 'fetchHello')
      .mockResolvedValue({ message: 'Hello from Spring Boot' });
  });

  afterEach(() => {
    fetchHelloSpy.mockRestore();
  });

  it('renders the hello message', async () => {
    render(<App />);
    expect(await screen.findByText('Hello from Spring Boot')).toBeInTheDocument();
  });

  it('renders the error message when the request fails', async () => {
    fetchHelloSpy.mockRejectedValueOnce(
      new ApiError('Network error. Please check your connection.'),
    );
    render(<App />);
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Network error. Please check your connection.',
    );
  });
});
