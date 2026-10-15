import type { Port } from '../../api/types'

/** [[west, south], [east, north]], as MapLibre bounds. */
export type Bounds = [[number, number], [number, number]]

/** Outermost regions, far from the mainland: the map has a button to show each of them. */
export const OVERSEAS_REGIONS: { bounds: Bounds; id: OverseasRegionId }[] = [
  {
    bounds: [
      [-61.85, 15.8],
      [-60.95, 16.55]
    ],
    id: 'GP'
  },
  {
    bounds: [
      [-61.25, 14.35],
      [-60.8, 14.9]
    ],
    id: 'MQ'
  },
  {
    bounds: [
      [-54.6, 2.1],
      [-51.6, 5.8]
    ],
    id: 'GF'
  },
  {
    bounds: [
      [55.2, -21.4],
      [55.85, -20.85]
    ],
    id: 'RE'
  },
  {
    bounds: [
      [44.95, -13.05],
      [45.35, -12.6]
    ],
    id: 'YT'
  },
  {
    bounds: [
      [-56.45, 46.75],
      [-56.1, 47.15]
    ],
    id: 'PM'
  },
  {
    bounds: [
      [-18.2, 27.6],
      [-13.3, 29.5]
    ],
    id: 'IC'
  },
  {
    bounds: [
      [-31.3, 36.9],
      [-25, 39.8]
    ],
    id: 'AZ'
  },
  {
    bounds: [
      [-17.3, 32.6],
      [-16.2, 33.15]
    ],
    id: 'MD'
  }
]

export type OverseasRegionId = 'AZ' | 'GF' | 'GP' | 'IC' | 'MD' | 'MQ' | 'PM' | 'RE' | 'YT'

/** The overseas region of a port, or "mainland". */
export function getRegionId(latitude: number, longitude: number): OverseasRegionId | 'mainland' {
  const region = OVERSEAS_REGIONS.find(
    ({ bounds: [[west, south], [east, north]] }) =>
      latitude >= south && latitude <= north && longitude >= west && longitude <= east
  )

  return region?.id ?? 'mainland'
}

/** The bounds of the [longitude, latitude] points, or undefined without any. */
export function getBounds(points: [number, number][]): Bounds | undefined {
  if (points.length === 0) {
    return undefined
  }
  const longitudes = points.map(([longitude]) => longitude)
  const latitudes = points.map(([, latitude]) => latitude)

  return [
    [Math.min(...longitudes), Math.min(...latitudes)],
    [Math.max(...longitudes), Math.max(...latitudes)]
  ]
}

/** French overseas regions and territories have their own ISO codes, but France's fleet register. */
const FRENCH_TERRITORIES = new Set(['FR', 'GP', 'MQ', 'GF', 'RE', 'YT', 'PM', 'BL', 'MF'])

/** The member state whose register a port belongs to, e.g. FR for Martinique (MQ). */
export function getRegisterCountry(port: Pick<Port, 'country'>): string {
  return FRENCH_TERRITORIES.has(port.country) ? 'FR' : port.country
}

/** The member states whose register is imported, sorted. */
export function getRegisterCountries(ports: Pick<Port, 'country'>[]): string[] {
  // A copy, sorted (toSorted() is not in ES2022)
  // oxlint-disable-next-line unicorn/no-array-sort
  return [...new Set(ports.map(getRegisterCountry))].sort()
}
