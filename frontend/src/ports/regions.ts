import type { Port } from '../api/types'

/** [west, south, east, north] in degrees */
export type Extent = [number, number, number, number]

export type Region = {
  extent: Extent
  name: string
}

/** Shortcuts to the areas of the map with ports, beyond the view of metropolitan France */
export const REGIONS: Region[] = [
  { extent: [-5.6, 41.3, 9.7, 51.2], name: 'Métropole' },
  { extent: [-11, 49, 9, 58.5], name: 'Mer du Nord & Manche' },
  { extent: [-10, 35.8, 4, 44], name: 'Espagne' },
  { extent: [-62.5, 3.5, -51.5, 15.5], name: 'Antilles & Guyane' },
  { extent: [-56.6, 46.6, -55.9, 47.3], name: 'Saint-Pierre-et-Miquelon' },
  { extent: [-18.5, 3.5, -2.5, 16], name: 'Afrique de l’Ouest' },
  { extent: [42, -26, 58, -3.5], name: 'Océan Indien' }
]

export function isInExtent(port: Port, [west, south, east, north]: Extent): boolean {
  return (
    port.longitude !== null &&
    port.latitude !== null &&
    port.longitude >= west &&
    port.longitude <= east &&
    port.latitude >= south &&
    port.latitude <= north
  )
}
