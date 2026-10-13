import type { KeyType } from '../api/types'

const CFR_REGEX = /^[A-Z]{3}[A-Z0-9]{9}$/
const MMSI_REGEX = /^[0-9]{9}$/

export function normalizeKey(raw: string): string {
  return raw.toUpperCase().replace(/[\s.-]/g, '')
}

export function isValidKey(keyType: KeyType, raw: string): boolean {
  return (keyType === 'cfr' ? CFR_REGEX : MMSI_REGEX).test(normalizeKey(raw))
}

/** The key a search query looks like, if any. */
export function detectKey(raw: string): { key: string; keyType: KeyType } | undefined {
  const key = normalizeKey(raw)
  if (MMSI_REGEX.test(key)) {
    return { key, keyType: 'mmsi' }
  }
  if (CFR_REGEX.test(key)) {
    return { key, keyType: 'cfr' }
  }

  return undefined
}

/** Page path of a vessel, under its CFR when it has one. */
export function getVesselPath(vessel: { cfr: string | null; mmsi: string | null }): string {
  return vessel.cfr ? `/vessels/cfr/${vessel.cfr}` : `/vessels/mmsi/${vessel.mmsi}`
}

export function getPhotoUrl(urls: { cfr: string | null; mmsi: string | null }, size?: '320' | '800' | 'full'): string {
  const url = urls.cfr ?? urls.mmsi ?? ''

  return size ? `${url}?size=${size}` : url
}
