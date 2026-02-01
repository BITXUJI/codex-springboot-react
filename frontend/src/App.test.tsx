import { render, screen } from '@testing-library/react';
import App from './App';
import * as api from './api/helloClient';
import { ApiError } from './api/helloClient';

/** Tests for the App component. */
describe('App', () => {
  /** Spy for the hello API call. */
  let fetchHelloSpy: jest.SpyInstance;

  /** Stubs the hello call to succeed by default. */
  beforeEach(() => {
    fetchHelloSpy = jest
      .spyOn(api, 'fetchHello')
      .mockResolvedValue({ message: 'Hello from Spring Boot' });
  });

  /** Restores the hello spy after each test. */
  afterEach(() => {
    fetchHelloSpy.mockRestore();
  });

  /**
   * Hello message is rendered.
   *
   * <pre>
   * Theme: Rendering
   * Test view: Hello message is rendered
   * Test conditions: API resolves with message
   * Test result: Message text appears
   * </pre>
   */
  it('renders the hello message', async () => {
    render(<App />);
    expect(await screen.findByText('Hello from Spring Boot')).toBeInTheDocument();
  });

  /**
   * API errors are rendered.
   *
   * <pre>
   * Theme: Error rendering
   * Test view: API errors are rendered
   * Test conditions: API rejects with ApiError
   * Test result: Alert displays API error message
   * </pre>
   */
  it('renders the error message when the request fails', async () => {
    fetchHelloSpy.mockRejectedValueOnce(
      new ApiError('Network error. Please check your connection.'),
    );
    render(<App />);
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Network error. Please check your connection.',
    );
  });

  /**
   * Generic errors are rendered.
   *
   * <pre>
   * Theme: Error rendering
   * Test view: Generic errors are rendered
   * Test conditions: API rejects with Error
   * Test result: Alert displays error message
   * </pre>
   */
  it('renders standard error messages for generic errors', async () => {
    fetchHelloSpy.mockRejectedValueOnce(new Error('Boom'));
    render(<App />);
    expect(await screen.findByRole('alert')).toHaveTextContent('Boom');
  });

  /**
   * Non-error rejections fall back to a generic message.
   *
   * <pre>
   * Theme: Error rendering
   * Test view: Non-error rejections fall back to a generic message
   * Test conditions: API rejects with non-error value
   * Test result: Alert displays fallback message
   * </pre>
   */
  it('renders a fallback message for non-error rejections', async () => {
    fetchHelloSpy.mockRejectedValueOnce('nope');
    render(<App />);
    expect(await screen.findByRole('alert')).toHaveTextContent('Unexpected error');
  });
});
