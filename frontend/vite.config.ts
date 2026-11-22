import react from '@vitejs/plugin-react'
import { defineConfig, type Plugin } from 'vite'

/** The Spring Boot backend, see `make run-back`; on the ports host, as browsers resolve *.localhost to the machine */
const BACKEND_PORT = 8880

/**
 * Serves the ports site's entry (ports.html) on http://ports.localhost:3000, as the backend does on its own domain:
 * the two sites share this app's code, but each has its own HTML entry. As in production, each page is filled with
 * what the backend renders for it, its metadata and content (see PortsPagesController): the pages beyond the map show
 * it as it is (see ContentPage). Without the backend, the bare entry.
 */
function portsSiteInDevelopment(): Plugin {
  return {
    configureServer(server) {
      server.middlewares.use((incoming, outgoing, next) => {
        const request = incoming as unknown as { headers: Record<string, string | undefined>; url?: string }
        if (!request.headers.host?.startsWith('ports.') || !request.headers.accept?.includes('text/html')) {
          next()

          return
        }
        const url = request.url ?? '/'
        const fillPage = async () => {
          // The entry as Vite serves it, with its development scripts
          const entry = await fetch(`http://localhost:${server.config.server.port}/ports.html`).then(response =>
            response.text()
          )
          const page = await fetch(`http://ports.localhost:${BACKEND_PORT}${url}`, {
            headers: { accept: 'text/html' },
            redirect: 'manual'
          }).catch(() => undefined)
          const location = page?.headers.get('location')
          if (page && location) {
            // e.g. /FRQUY moved to /ports/erquy
            outgoing.writeHead(page.status, { Location: new URL(location, page.url).pathname }).end()

            return
          }
          const html = page ? await page.text() : ''
          // Without the built frontend, the backend renders a bare page: its head is all metadata
          const head = /<head>([\s\S]*)<\/head>/.exec(html)?.[1]
          // and its content all of #root
          const body = /<div id="root">([\s\S]*)<\/div>\s*<\/body>/.exec(html)?.[1] ?? ''
          const filled = entry.replace(SEO_HEAD, match => head ?? match).replace('<!-- seo:body -->', () => body)
          outgoing.writeHead(page?.status ?? 200, { 'Content-Type': 'text/html; charset=utf-8' }).end(filled)
        }
        fillPage().catch(next)
      })
    },
    name: 'ports-site-in-development'
  }
}

const SEO_HEAD = /<!-- seo:head -->[\s\S]*?<!-- \/seo:head -->/

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
