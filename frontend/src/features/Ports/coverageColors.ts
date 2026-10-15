/** Coverage colours, as the chart's buoys: orange while no vessel is photographed, then yellow, then green. */
const COVERAGE_STOPS: [number, [number, number, number]][] = [
  [0, [224, 102, 42]],
  [0.1, [226, 194, 74]],
  [0.5, [141, 187, 122]],
  [1, [47, 125, 79]]
]

export function getCoverageColor(share: number): string {
  const index = COVERAGE_STOPS.findIndex(([stop]) => stop >= share)
  if (index <= 0) {
    return `rgb(${COVERAGE_STOPS[Math.max(index, 0)]?.[1].join(', ')})`
  }
  const [fromStop, from] = COVERAGE_STOPS[index - 1] ?? COVERAGE_STOPS[0]!
  const [toStop, to] = COVERAGE_STOPS[index] ?? COVERAGE_STOPS[0]!
  const ratio = (share - fromStop) / (toStop - fromStop)

  return `rgb(${from.map((value, i) => Math.round(value + ((to[i] ?? value) - value) * ratio)).join(', ')})`
}

export const COVERAGE_LEGEND = COVERAGE_STOPS.map(([stop]) => stop)
