/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  // Tailwind подключён плагином; пустой конфиг не даёт Vite искать postcss.config в родительских папках
  css: { postcss: {} },
  server: {
    port: 5173,
    // В режиме real запросы /api уходят на Spring Boot
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
    css: false,
  },
});
