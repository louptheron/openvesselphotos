import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  build: {
    outDir: 'build'
  },
  plugins: [react()],
  server: {
    proxy: {
      // The Spring Boot backend, see `make dev-back`
      '/v1': 'http://localhost:8880'
    }
  }
})
