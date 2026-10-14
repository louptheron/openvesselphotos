import { describe, expect, it } from 'vitest'

import type { Port } from '../../api/types'
import { filterPorts, paginate, sortPorts } from './portsTable'
import { getRegionId, getRegisterCountries } from './regions'

const port = (code: string, name: string, vessels: number, photographed: number): Port => ({
  code,
  country: code.slice(0, 2),
  latitude: 0,
  longitude: 0,
  name,
  photographed,
  vessels
})

const PORTS = [
  port('FRSET', 'Sète', 431, 0),
  port('FRBES', 'Brest', 134, 10),
  port('MQFDF', 'Fort-de-France', 771, 1),
  port('ESVGO', 'Vigo', 300, 30)
]

describe('filterPorts', () => {
  it('matches names without accents, and codes', () => {
    expect(filterPorts(PORTS, 'sete', undefined).map(p => p.code)).toEqual(['FRSET'])
    expect(filterPorts(PORTS, 'frbes', undefined).map(p => p.code)).toEqual(['FRBES'])
  })

  it("counts overseas ports in their member state's register", () => {
    expect(filterPorts(PORTS, '', 'FR').map(p => p.code)).toEqual(['FRSET', 'FRBES', 'MQFDF'])
  })
})

describe('sortPorts', () => {
  it('sorts by any column, in both directions', () => {
    expect(sortPorts(PORTS, 'vessels', true, 'en').map(p => p.code)).toEqual(['MQFDF', 'FRSET', 'ESVGO', 'FRBES'])
    expect(sortPorts(PORTS, 'share', true, 'en').map(p => p.code)).toEqual(['ESVGO', 'FRBES', 'MQFDF', 'FRSET'])
    expect(sortPorts(PORTS, 'name', false, 'fr').map(p => p.code)).toEqual(['FRBES', 'MQFDF', 'FRSET', 'ESVGO'])
  })
})

describe('paginate', () => {
  it('keeps the page within the pages', () => {
    const items = Array.from({ length: 45 }, (_, index) => index)

    expect(paginate(items, 3)).toEqual({ items: [40, 41, 42, 43, 44], page: 3, pageCount: 3 })
    expect(paginate(items, 9).page).toBe(3)
    expect(paginate(items, Number.NaN).page).toBe(1)
    expect(paginate([], 1)).toEqual({ items: [], page: 1, pageCount: 1 })
  })
})

describe('regions', () => {
  it('finds the overseas regions, and the member states imported', () => {
    expect(getRegionId(14.6, -61.05)).toBe('MQ')
    expect(getRegionId(48.4, -4.48)).toBe('mainland')
    expect(getRegisterCountries(PORTS)).toEqual(['ES', 'FR'])
  })
})
