import { describe, expect, it } from 'vitest'

import { detectKey, getNamedPhotoUrl, getPhotoSlug, getPhotoUrl, getVesselPath, isValidKey } from './vesselKey'

describe('utils/vesselKey', () => {
  it('detectKey() should recognize a CFR and an MMSI', () => {
    expect(detectKey(' fra 000-123456')).toEqual({ key: 'FRA000123456', keyType: 'cfr' })
    expect(detectKey('227000101')).toEqual({ key: '227000101', keyType: 'mmsi' })
    expect(detectKey('Ker Avel')).toBeUndefined()
  })

  it('isValidKey() should check the format of each key type', () => {
    expect(isValidKey('cfr', 'FRA000123456')).toBe(true)
    expect(isValidKey('mmsi', 'FRA000123456')).toBe(false)
    expect(isValidKey('mmsi', '22700010')).toBe(false)
  })

  it('getVesselPath() should prefer the CFR', () => {
    expect(getVesselPath({ cfr: 'FRA000123456', mmsi: '227000101' })).toBe('/vessels/cfr/FRA000123456')
    expect(getVesselPath({ cfr: null, mmsi: '227000101' })).toBe('/vessels/mmsi/227000101')
  })

  it('getPhotoUrl() should add the size', () => {
    expect(getPhotoUrl({ cfr: null, mmsi: 'https://x/v1/mmsi/1/photos/1.jpg' }, '320')).toBe(
      'https://x/v1/mmsi/1/photos/1.jpg?size=320'
    )
  })

  it('getPhotoSlug() should name the photos after the vessel, as the server does', () => {
    expect(getPhotoSlug({ name: 'LOLA', registration: 'LR 539755' })).toBe('lola-lr-539755')
    expect(getPhotoSlug({ name: "DAMA'LYJO — Île d'Yeu", registration: null })).toBe('dama-lyjo-ile-d-yeu')
    expect(getPhotoSlug({ name: null, registration: null })).toBe('vessel')
  })

  it('getNamedPhotoUrl() should add the slug before the extension', () => {
    expect(getNamedPhotoUrl({ cfr: 'https://x.org/v1/cfr/FRA000539755/photos/1.jpg', mmsi: null }, 'lola', '800')).toBe(
      'https://x.org/v1/cfr/FRA000539755/photos/1/lola.jpg?size=800'
    )
  })
})
