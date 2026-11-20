import react from '@vitejs/plugin-react'
import { defineConfig, type Plugin } from 'vite'

/**
 * Serves the ports site's entry (ports.html) on http://ports.localhost:3000, as the backend does on its own domain:
 * the two sites share this app's code, but each has its own HTML entry.
 */
function portsSiteInDevelopment(): Plugin {
  return {
    configureServer(server) {
      server.middlewares.use((incoming, _, next) => {
        const request = incoming as unknown as { headers: Record<string, string | undefined>; url?: string }
        if (request.headers.host?.startsWith('ports.') && request.headers.accept?.includes('text/html')) {
          request.url = '/ports.html'
        }
        next()
      })
    },
    name: 'ports-site-in-development'
  }
}

// https://vitejs.dev/config/
export default defineConfig({
  build: {
    minify: true,
    outDir: './build',
    // Open Vessel Photos and the ports site (see PortsPagesController)
    rolldownOptions: {
      input: { main: 'index.html', ports: 'ports.html' }
    },
    sourcemap: true,
    target: 'esnext'
  },
  plugins: [react(), portsSiteInDevelopment()],
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
