import { describe, expect, it } from 'vitest'

import { getCoverageColor } from './coverageColors'
import { getFrameId, getViewBox, project } from './projection'

describe('project', () => {
  it('keeps the longitude and puts the north up', () => {
    const [x, y] = project(-4.48, 48.4)

    expect(x).toBe(-4.48)
    expect(y).toBeLessThan(project(-4.48, 43.3)[1])
    expect(project(0, 0)[1]).toBeCloseTo(0)
  })
})

describe('getFrameId', () => {
  it('draws the overseas ports in their insets', () => {
    expect(getFrameId(project(-61.05, 14.6))).toBe('MQ')
    expect(getFrameId(project(55.29, -20.94))).toBe('RE')
    expect(getFrameId(project(-4.48, 48.4))).toBe('europe')
  })
})

describe('getViewBox', () => {
  it('frames the mainland ports with a margin, within the given proportions', () => {
    const [x, y, width, height] = getViewBox([project(-4.48, 48.4), project(7.27, 43.7)], 'europe')

    expect(x).toBeLessThan(-4.48)
    expect(x + width).toBeGreaterThan(7.27)
    expect(y).toBeLessThan(project(0, 48.4)[1])
    expect(width / height).toBeGreaterThanOrEqual(1)
    expect(width / height).toBeLessThanOrEqual(1.5 + 1e-9)
  })
})

describe('getCoverageColor', () => {
  it('goes from orange to green', () => {
    expect(getCoverageColor(0)).toBe('rgb(224, 102, 42)')
    expect(getCoverageColor(0.1)).toBe('rgb(226, 194, 74)')
    expect(getCoverageColor(1)).toBe('rgb(47, 125, 79)')
    expect(getCoverageColor(0.05)).toBe('rgb(225, 148, 58)')
  })
})
