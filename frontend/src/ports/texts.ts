export const SITE_NAME = 'La Pêche française'
/** "Pêche" alone mostly means angling on French search engines: the site is about the fishing industry */
export const SITE_TAGLINE = 'Chiffres et données de la pêche professionnelle'

/** The page of a port, e.g. /ports/le-guilvinec (see PortsPagesController); by LOCODE until the API gives slugs */
export function getPortPath(port: { locode: string; slug?: string | null }): string {
  return port.slug ? `/ports/${port.slug}` : `/${port.locode}`
}

export function vessels(count: number): string {
  return count === 1 ? '1 navire' : `${count} navires`
}

/** e.g. "92 %" */
export function formatPercent(share: number): string {
  return `${Math.round(share * 100)} %`
}

/** e.g. "20 novembre 2026", from "2026-11-20" */
export function formatDate(day: string): string {
  return new Date(`${day}T12:00:00`).toLocaleDateString('fr', { day: 'numeric', month: 'long', year: 'numeric' })
}

/** e.g. "12 345" */
export function formatNumber(value: number): string {
  return value.toLocaleString('fr')
}

/** Removes accents and case, to search "peche" in "Pêche" */
export function normalize(value: string): string {
  return value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()
}
