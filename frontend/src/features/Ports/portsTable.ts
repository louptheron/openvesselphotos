import type { Port } from '../../api/types'
import { getRegisterCountry } from './regions'

export const PORT_SORTS = ['name', 'vessels', 'photographed', 'share'] as const

export type PortSort = (typeof PORT_SORTS)[number]

export const PORTS_PER_PAGE = 20

export function getShare(port: Pick<Port, 'photographed' | 'vessels'>): number {
  return port.vessels > 0 ? port.photographed / port.vessels : 0
}

/** Ports whose name or code contains the query, of the given member state. */
export function filterPorts(ports: Port[], query: string, country: string | undefined): Port[] {
  const normalizedQuery = normalize(query)

  return ports.filter(
    port =>
      (!country || getRegisterCountry(port) === country) &&
      (!normalizedQuery ||
        normalize(port.name).includes(normalizedQuery) ||
        port.code.toLowerCase().includes(normalizedQuery))
  )
}

/** Sorted by the column, then by name. */
export function sortPorts(ports: Port[], sort: PortSort, isDescending: boolean, locale: string): Port[] {
  const byName = (a: Port, b: Port) => a.name.localeCompare(b.name, locale)
  const compare: Record<PortSort, (a: Port, b: Port) => number> = {
    name: byName,
    photographed: (a, b) => a.photographed - b.photographed,
    share: (a, b) => getShare(a) - getShare(b),
    vessels: (a, b) => a.vessels - b.vessels
  }

  // A copy, sorted (toSorted() is not in ES2022)
  // oxlint-disable-next-line unicorn/no-array-sort
  return [...ports].sort((a, b) => (isDescending ? -1 : 1) * compare[sort](a, b) || byName(a, b))
}

/** The page (from 1, kept within the pages) and the number of pages. */
export function paginate<T>(items: T[], page: number, size = PORTS_PER_PAGE) {
  const pageCount = Math.max(1, Math.ceil(items.length / size))
  const current = Math.min(Math.max(1, Math.floor(page) || 1), pageCount)

  return { items: items.slice((current - 1) * size, current * size), page: current, pageCount }
}

/** Lower case, without accents: "Sète" matches "sete". */
function normalize(value: string): string {
  return value.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim()
}
