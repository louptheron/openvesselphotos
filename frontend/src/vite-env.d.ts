/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** CARTO basemap key, for the ports map; OpenStreetMap's standard tiles without it */
  readonly VITE_CARTO_BASEMAP_KEY?: string
}
