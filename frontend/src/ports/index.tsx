import '@fontsource/ibm-plex-mono/400.css'
import '@fontsource/ibm-plex-sans/400.css'
import '@fontsource/ibm-plex-sans/500.css'
import '@fontsource/ibm-plex-sans/600.css'
import '@fontsource/ibm-plex-sans/700.css'
import 'ol/ol.css'

import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Provider } from 'react-redux'
import { BrowserRouter } from 'react-router-dom'

import { GlobalStyle } from '../components/GlobalStyle'
import { store } from '../store'
import { PortsApp } from './PortsApp'

const root = document.getElementById('root') as HTMLElement
// The page as rendered by the server, shown as it is on the pages beyond the map (see ContentPage)
const serverHtml = root.innerHTML

/** The ports site, in French on its own domain: the French fishing vessels by home port (see PortsPagesController). */
createRoot(root).render(
  <StrictMode>
    <Provider store={store}>
      <BrowserRouter>
        <GlobalStyle />
        <PortsApp serverHtml={serverHtml} />
      </BrowserRouter>
    </Provider>
  </StrictMode>
)
