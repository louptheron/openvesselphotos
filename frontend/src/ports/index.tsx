import '@fontsource-variable/bricolage-grotesque'
import '@fontsource/ibm-plex-mono/400.css'
import '@fontsource/ibm-plex-sans/400.css'
import '@fontsource/ibm-plex-sans/500.css'
import '@fontsource/ibm-plex-sans/600.css'
import 'ol/ol.css'

import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Provider } from 'react-redux'
import { BrowserRouter } from 'react-router-dom'

import { GlobalStyle } from '../components/GlobalStyle'
import { store } from '../store'
import { PortsApp } from './PortsApp'

/** The ports site, in French on its own domain: the French fishing vessels by home port (see PortsPagesController). */
createRoot(document.getElementById('root') as HTMLElement).render(
  <StrictMode>
    <Provider store={store}>
      <BrowserRouter>
        <GlobalStyle />
        <PortsApp />
      </BrowserRouter>
    </Provider>
  </StrictMode>
)
