import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

type CspMode = 'dev' | 'preview';

const API_PROXY = {
  '/api': 'http://localhost:8080',
};

const baseDirectives = [
  "default-src 'self'",
  "img-src 'self' data:",
  "font-src 'self'",
  "object-src 'none'",
  "base-uri 'self'",
  "frame-ancestors 'none'",
  "form-action 'self'",
];

/**
 * Builds the CSP value for each runtime mode.
 *
 * <pre>
 * Responsibilities:
 * 1) Keep shared directives in one place to avoid policy drift.
 * 2) Allow Vite dev tooling in "dev" mode (inline/eval scripts + ws HMR).
 * 3) Enforce stricter directives in "preview" mode to approximate production.
 * </pre>
 */
function buildCspPolicy(mode: CspMode): string {
  const directives = [...baseDirectives];

  if (mode === 'dev') {
    // Vite dev server injects inline preamble and uses eval/websocket for HMR.
    directives.push(
      "script-src 'self' 'unsafe-inline' 'unsafe-eval'",
      "style-src 'self' 'unsafe-inline'",
      "connect-src 'self' ws://127.0.0.1:5173 ws://localhost:5173",
    );
    return directives.join('; ');
  }

  directives.push("script-src 'self'", "style-src 'self'", "connect-src 'self'");
  return directives.join('; ');
}

/**
 * Creates security headers for Vite server modes.
 *
 * <pre>
 * Usage:
 * 1) Build the mode-specific CSP policy string.
 * 2) Attach the policy to the Content-Security-Policy response header.
 * 3) Reuse the returned object in both dev and preview config blocks.
 * </pre>
 */
function buildSecurityHeaders(mode: CspMode): Record<string, string> {
  return {
    'Content-Security-Policy': buildCspPolicy(mode),
  };
}

// Proxy API requests to the backend during local development.
export default defineConfig({
  plugins: [react()],
  server: {
    headers: buildSecurityHeaders('dev'),
    proxy: API_PROXY,
  },
  preview: {
    headers: buildSecurityHeaders('preview'),
    proxy: API_PROXY,
  },
});
