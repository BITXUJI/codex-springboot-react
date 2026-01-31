import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Proxy API requests to the backend during local development.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
});
