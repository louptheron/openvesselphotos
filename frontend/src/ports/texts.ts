export const SITE_NAME = 'La Pêche française'
export const SITE_TAGLINE = 'La flotte, les ports, les criées'

export function vessels(count: number): string {
  return count === 1 ? '1 navire' : `${count} navires`
}

/** e.g. "12 345" */
export function formatNumber(value: number): string {
  return value.toLocaleString('fr')
}

/** Removes accents and case, to search "peche" in "Pêche" */
export function normalize(value: string): string {
  return value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()
}
