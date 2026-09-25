import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    // En développement, le front parle au backend sans CORS : /api est relayé.
    proxy: {
      '/api': { target: process.env.VITE_PROXY_CIBLE ?? 'http://localhost:8080', changeOrigin: true },
    },
  },
});
