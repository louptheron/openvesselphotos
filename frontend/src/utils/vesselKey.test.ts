import { describe, expect, it } from 'vitest'

import { detectKey, getPhotoUrl, getVesselPath, isValidKey } from './vesselKey'

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
})
