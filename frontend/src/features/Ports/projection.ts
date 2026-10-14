import coastlines from './coastlines.json'

/** A map frame: its land as an SVG path, and its bounds [west, north, east, south] in projected coordinates. */
type Frame = { bounds: [number, number, number, number]; land: string }

/** Built by scripts/coastlines.py from Natural Earth: mainland Europe, then the regions drawn in insets. */
export const FRAMES = coastlines as unknown as Record<string, Frame>

export const INSET_FRAMES = ['GP', 'MQ', 'GF', 'RE', 'YT', 'PM', 'IC', 'AZ', 'MD'] as const

/** Mercator, in degrees: x is the longitude, y grows southwards (as in SVG). Same as scripts/coastlines.py. */
export function project(longitude: number, latitude: number): [number, number] {
  return [longitude, -(180 / Math.PI) * Math.log(Math.tan(Math.PI / 4 + (latitude * Math.PI) / 360))]
}

/** The inset showing this point, else the mainland map. */
export function getFrameId([x, y]: [number, number]): string {
  return (
    INSET_FRAMES.find(id => {
      const [west, north, east, south] = FRAMES[id]?.bounds ?? [0, 0, 0, 0]

      return x >= west && x <= east && y >= north && y <= south
    }) ?? 'europe'
  )
}

/** [x, y, width, height] around the points, with a margin, between the given proportions and within the frame. */
export function getViewBox(
  points: [number, number][],
  frameId: string,
  { maxRatio = 1.5, minRatio = 1, minWidth = 4 } = {}
): [number, number, number, number] {
  const frame = FRAMES[frameId]
  if (!frame) {
    return [0, 0, 1, 1]
  }
  if (frameId !== 'europe' || points.length === 0) {
    const [west, north, east, south] = frame.bounds

    return [west, north, east - west, south - north]
  }
  const xs = points.map(([x]) => x)
  const ys = points.map(([, y]) => y)
  let [x0, x1, y0, y1] = [Math.min(...xs), Math.max(...xs), Math.min(...ys), Math.max(...ys)]
  const margin = Math.max(x1 - x0, y1 - y0, minWidth) * 0.08 + 0.5
  x0 -= margin
  x1 += margin
  y0 -= margin
  y1 += margin
  if (x1 - x0 < minWidth) {
    ;[x0, x1] = widen(x0, x1, minWidth)
  }
  if ((x1 - x0) / (y1 - y0) < minRatio) {
    ;[x0, x1] = widen(x0, x1, (y1 - y0) * minRatio)
  } else if ((x1 - x0) / (y1 - y0) > maxRatio) {
    ;[y0, y1] = widen(y0, y1, (x1 - x0) / maxRatio)
  }

  return [x0, y0, x1 - x0, y1 - y0]
}

/** The interval [from, to] widened to the size, around the same center. */
function widen(from: number, to: number, size: number): [number, number] {
  const center = (from + to) / 2

  return [center - size / 2, center + size / 2]
}
