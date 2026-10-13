import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vitejs.dev/config/
export default defineConfig({
  build: {
    minify: true,
    outDir: './build',
    sourcemap: true,
    target: 'esnext'
  },
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 3000,
    proxy: {
      // The Spring Boot backend, see `make run-back`
      '/v1': {
        target: 'http://localhost:8880'
      }
    }
  }
})
