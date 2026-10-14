import { Link } from 'react-router-dom'
import styled from 'styled-components'

import type { Port } from '../../api/types'
import { useTranslation } from '../../i18n'
import { CHART_COLORS, COLORS, FONTS } from '../../theme'
import { COVERAGE_LEGEND, getCoverageColor } from './coverageColors'
import { FRAMES, getFrameId, getViewBox, INSET_FRAMES, project } from './projection'

type PlacedPort = Port & { point: [number, number] }

/** Colours of the nautical chart of the home page (src/assets/chart.svg). */
const CHART = {
  coast: '#7E745A',
  graticule: '#C3D5DF',
  land: '#EFE4BF',
  magenta: '#B0408A',
  sea: '#F8FBFC',
  // From the open sea to the coast, as the depth bands of the chart
  shallows: ['#E6F0F5', '#D4E6EF', '#BFD8E6']
}

/**
 * The ports of registration on a nautical chart: a circle per port, sized by its number of vessels and coloured by the
 * share of them with a photo. Mainland Europe, with the overseas regions in insets. Each port links to its page; the
 * table of the ports page gives the same data without the map.
 */
export function PortsMap({ ports }: { ports: Port[] }) {
  const { t } = useTranslation()
  const placed: PlacedPort[] = ports.flatMap(port =>
    port.latitude !== null && port.longitude !== null
      ? [{ ...port, point: project(port.longitude, port.latitude) }]
      : []
  )
  const maxVessels = Math.max(1, ...placed.map(port => port.vessels))
  const byFrame = new Map<string, PlacedPort[]>()
  placed.forEach(port => {
    const frameId = getFrameId(port.point)
    byFrame.set(frameId, [...(byFrame.get(frameId) ?? []), port])
  })
  const mainland = byFrame.get('europe') ?? []
  const insets = INSET_FRAMES.filter(id => byFrame.has(id))

  return (
    <Figure>
      <Maps $hasInsets={insets.length > 0}>
        {mainland.length > 0 && (
          <MapFrame frameId="europe" isMain maxVessels={maxVessels} ports={mainland} title={t.ports.mainland} />
        )}
        {insets.length > 0 && (
          <Insets>
            {insets.map(id => (
              <MapFrame
                key={id}
                frameId={id}
                maxVessels={maxVessels}
                ports={byFrame.get(id) ?? []}
                title={t.ports.regions[id]}
              />
            ))}
          </Insets>
        )}
      </Maps>
      <Legend>
        <span>{t.ports.legendCoverage}</span>
        <Gradient
          style={{
            background: `linear-gradient(to right, ${COVERAGE_LEGEND.map(
              (stop, index) => `${getCoverageColor(stop)} ${(index / (COVERAGE_LEGEND.length - 1)) * 100}%`
            ).join(', ')})`
          }}
        />
        <Ticks>
          {COVERAGE_LEGEND.map((stop, index) => (
            <span key={stop} style={{ left: `${(index / (COVERAGE_LEGEND.length - 1)) * 100}%` }}>
              {Math.round(stop * 100)}%
            </span>
          ))}
        </Ticks>
        <span>{t.ports.legendSize}</span>
        <Credits>
          {t.ports.credits}{' '}
          <a href="https://www.naturalearthdata.com" rel="noreferrer" target="_blank">
            Natural Earth
          </a>
          , UN/LOCODE
        </Credits>
      </Legend>
    </Figure>
  )
}

function MapFrame({
  frameId,
  isMain = false,
  maxVessels,
  ports,
  title
}: {
  frameId: string
  isMain?: boolean
  maxVessels: number
  ports: PlacedPort[]
  title: string
}) {
  const { formatNumber, t } = useTranslation()
  const frame = FRAMES[frameId]
  const [x0, y0, width, height] = getViewBox(
    ports.map(port => port.point),
    frameId
  )
  const step = isMain ? (width > 12 ? 4 : 2) : width > 2 ? 1 : 0.25
  const longitudes = range(Math.ceil(x0 / step) * step, x0 + width, step)
  const latitudes = range(-80, 80, step)
    .map(latitude => ({ latitude, y: project(0, latitude)[1] }))
    .filter(({ y }) => y > y0 && y < y0 + height)
  // Largest first, so that the small ports are drawn on top (a copy: toSorted() is not in ES2022)
  // oxlint-disable-next-line unicorn/no-array-sort
  const sorted = [...ports].sort((a, b) => b.vessels - a.vessels)
  const getSize = (port: PlacedPort) => Math.round((isMain ? 10 : 12) + 26 * Math.sqrt(port.vessels / maxVessels))
  // Pixels at the usual width of the frame: labels are placed for it, and stay readable when narrower
  const pixelWidth = isMain ? 860 : 170
  const scale = pixelWidth / width
  const labelled = getLabelledPorts(sorted, getSize, ([x, y]) => [(x - x0) * scale, (y - y0) * scale], [
    0,
    0,
    pixelWidth,
    height * scale
  ])
  const toPercent = ([x, y]: [number, number]) => ({
    left: `${((x - x0) / width) * 100}%`,
    top: `${((y - y0) / height) * 100}%`
  })

  return (
    <Frame>
      {!isMain && <FrameTitle>{title}</FrameTitle>}
      <Chart style={{ aspectRatio: `${width} / ${height}` }}>
        <Land aria-hidden="true" preserveAspectRatio="none" viewBox={`${x0} ${y0} ${width} ${height}`}>
          <rect fill={CHART.sea} height={height} width={width} x={x0} y={y0} />
          {longitudes.map(x => (
            <line
              key={`x${x}`}
              stroke={CHART.graticule}
              vectorEffect="non-scaling-stroke"
              x1={x}
              x2={x}
              y1={y0}
              y2={y0 + height}
            />
          ))}
          {latitudes.map(({ latitude, y }) => (
            <line
              key={`y${latitude}`}
              stroke={CHART.graticule}
              vectorEffect="non-scaling-stroke"
              x1={x0}
              x2={x0 + width}
              y1={y}
              y2={y}
            />
          ))}
          {CHART.shallows.map((color, index) => (
            <path
              key={color}
              d={frame?.land}
              fill="none"
              stroke={color}
              strokeLinejoin="round"
              strokeWidth={[22, 12, 5][index]}
              vectorEffect="non-scaling-stroke"
            />
          ))}
          <path
            d={frame?.land}
            fill={CHART.land}
            fillRule="evenodd"
            stroke={CHART.coast}
            strokeWidth="0.8"
            vectorEffect="non-scaling-stroke"
          />
        </Land>
        {isMain && (
          <>
            {longitudes.map(x => (
              <Graduation key={`x${x}`} style={{ left: `${((x - x0) / width) * 100}%`, top: 4 }}>
                {formatDegrees(x, 'E', 'W')}
              </Graduation>
            ))}
            {latitudes.map(({ latitude, y }) => (
              <Graduation key={`y${latitude}`} style={{ left: 4, top: `${((y - y0) / height) * 100}%` }}>
                {formatDegrees(latitude, 'N', 'S')}
              </Graduation>
            ))}
            <CompassRose />
          </>
        )}
        {sorted.map(port => {
          const share = port.vessels > 0 ? port.photographed / port.vessels : 0
          const size = getSize(port)
          const summary = t.ports.markerSummary(formatNumber(port.vessels), formatNumber(port.photographed))

          return (
            <Marker
              key={port.code}
              aria-label={`${port.name}: ${summary}`}
              style={{
                ...toPercent(port.point),
                ['--color' as string]: getCoverageColor(share),
                ['--size' as string]: `${size}px`
              }}
              to={`/ports/${port.code}`}
            >
              <Dot />
              {labelled.has(port.code) && (
                <Name $side={labelled.get(port.code) ?? 'right'} aria-hidden="true">
                  {port.name}
                </Name>
              )}
              <Tip aria-hidden="true">
                <strong>{port.name}</strong>
                <span>{summary}</span>
              </Tip>
            </Marker>
          )
        })}
      </Chart>
    </Frame>
  )
}

type Box = [number, number, number, number]

type LabelSide = 'left' | 'right'

/** The side of each port to label, largest first: on the right, else on the left, if it covers no label nor port. */
function getLabelledPorts(
  ports: PlacedPort[],
  getSize: (port: PlacedPort) => number,
  toPixels: (point: [number, number]) => [number, number],
  [frameLeft, frameTop, frameRight, frameBottom]: Box
): Map<string, LabelSide> {
  const dots: Box[] = ports.map(port => {
    const [x, y] = toPixels(port.point)
    const radius = getSize(port) / 2

    return [x - radius, y - radius, x + radius, y + radius]
  })
  const overlaps = ([left, top, right, bottom]: Box, [otherLeft, otherTop, otherRight, otherBottom]: Box) =>
    left < otherRight && right > otherLeft && top < otherBottom && bottom > otherTop
  const labels: Box[] = []
  const labelled = new Map<string, LabelSide>()
  ports.forEach((port, index) => {
    const [x, y] = toPixels(port.point)
    const gap = getSize(port) / 2 + 3
    // About 6.5 px per character at 11 px
    const labelWidth = port.name.length * 6.5
    const candidates: [LabelSide, Box][] = [
      ['right', [x + gap, y - 8, x + gap + labelWidth, y + 8]],
      ['left', [x - gap - labelWidth, y - 8, x - gap, y + 8]]
    ]
    const free = candidates.find(
      ([, label]) =>
        label[0] >= frameLeft &&
        label[2] <= frameRight &&
        label[1] >= frameTop &&
        label[3] <= frameBottom &&
        labels.every(other => !overlaps(label, other)) &&
        dots.every((dot, dotIndex) => dotIndex === index || !overlaps(label, dot))
    )
    if (free) {
      labels.push(free[1])
      labelled.set(port.code, free[0])
    }
  })

  return labelled
}

function range(from: number, to: number, step: number): number[] {
  const values: number[] = []
  for (let value = from; value <= to; value += step) {
    values.push(Math.round(value * 100) / 100)
  }

  return values
}

/** 4 -> "4°E", -4.5 -> "4°30′W" */
function formatDegrees(value: number, positive: string, negative: string): string {
  const absolute = Math.abs(value)
  const degrees = Math.floor(absolute)
  const minutes = Math.round((absolute - degrees) * 60)

  return `${degrees}°${minutes ? `${minutes}′` : ''}${value === 0 ? '' : value > 0 ? positive : negative}`
}

/** A compass rose, as on the chart (magnetic north in magenta). */
function CompassRose() {
  return (
    <Rose aria-hidden="true" viewBox="-50 -50 100 100">
      <circle fill="none" r="44" stroke={CHART.magenta} strokeWidth="1" />
      <circle fill="none" r="36" stroke={CHART.magenta} strokeDasharray="1 3" strokeWidth="3" />
      <path d="M0 -46 L6 0 L0 46 L-6 0 Z M-46 0 L0 -6 L46 0 L0 6 Z" fill={CHART.magenta} opacity="0.85" />
      <path d="M0 -46 L6 0 L0 0 Z M46 0 L0 6 L0 0 Z M0 46 L-6 0 L0 0 Z M-46 0 L0 -6 L0 0 Z" fill={COLORS.white} />
      <text fill={CHART.magenta} fontFamily={FONTS.sans} fontSize="13" fontWeight="600" textAnchor="middle" y="-29">
        N
      </text>
    </Rose>
  )
}

const Figure = styled.figure`
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
`

const Maps = styled.div<{ $hasInsets: boolean }>`
  display: grid;
  grid-template-columns: ${p => (p.$hasInsets ? 'minmax(0, 3fr) minmax(0, 1.3fr)' : 'minmax(0, 1fr)')};
  gap: 14px;
  align-items: start;

  @media (max-width: 760px) {
    grid-template-columns: minmax(0, 1fr);
  }
`

const Insets = styled.div`
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
`

const Frame = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
`

const FrameTitle = styled.span`
  font-size: 12px;
  font-weight: 600;
  color: ${CHART_COLORS.textMuted};
`

const Chart = styled.div`
  /* Smaller circles on phones, where the map is narrow */
  --marker-scale: 1;
  position: relative;

  @media (max-width: 600px) {
    --marker-scale: 0.6;
  }

  width: 100%;
  border: 1px solid ${CHART_COLORS.cardBorder};
  border-radius: 10px;
  overflow: hidden;
  background: ${CHART.sea};
`

const Land = styled.svg`
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
`

const Graduation = styled.span`
  position: absolute;
  font-family: ${FONTS.mono};
  font-size: 10px;
  color: ${CHART_COLORS.textMuted};
  pointer-events: none;
  white-space: nowrap;
`

const Rose = styled.svg`
  position: absolute;
  right: 12px;
  bottom: 12px;
  width: 64px;
  height: 64px;
  pointer-events: none;

  @media (max-width: 600px) {
    width: 44px;
    height: 44px;
  }
`

const Dot = styled.span`
  display: block;
  width: calc(var(--size) * var(--marker-scale));
  height: calc(var(--size) * var(--marker-scale));
  border-radius: 50%;
  background: var(--color);
  border: 1.5px solid ${COLORS.ink};
  opacity: 0.88;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.7);
  transition: transform 0.15s ease-out;
`

const Name = styled.span<{ $side: LabelSide }>`
  position: absolute;
  /* Offset from the opposite edge: "left" places a label on the right of its circle */
  ${p => (p.$side === 'right' ? 'left' : 'right')}: calc(var(--size) * var(--marker-scale) / 2 + 4px);
  top: 50%;
  transform: translateY(-50%);
  font-size: 11px;
  font-style: italic;
  font-weight: 600;
  color: ${CHART.magenta};
  white-space: nowrap;
  text-shadow:
    0 0 3px ${CHART.sea},
    0 0 3px ${CHART.sea};
  pointer-events: none;

  @media (max-width: 600px) {
    display: none;
  }
`

const Tip = styled.span`
  position: absolute;
  bottom: calc(var(--size) * var(--marker-scale) / 2 + 8px);
  left: 50%;
  transform: translateX(-50%);
  display: none;
  flex-direction: column;
  gap: 2px;
  padding: 6px 10px;
  border-radius: 8px;
  background: ${COLORS.ink};
  color: ${COLORS.white};
  font-size: 12px;
  white-space: nowrap;
  pointer-events: none;
`

const Marker = styled(Link)`
  position: absolute;
  transform: translate(-50%, -50%);
  text-decoration: none;

  &:hover,
  &:focus-visible {
    z-index: 2;

    ${Dot} {
      transform: scale(1.15);
      opacity: 1;
    }

    ${Tip} {
      display: flex;
    }
  }
`

const Legend = styled.figcaption`
  display: grid;
  grid-template-columns: auto minmax(120px, 220px) 1fr;
  grid-template-rows: auto auto;
  align-items: center;
  gap: 2px 12px;
  font-size: 13px;
  color: ${COLORS.text};

  > span:first-child {
    grid-row: 1;
  }

  @media (max-width: 600px) {
    grid-template-columns: minmax(0, 1fr);
  }
`

const Gradient = styled.span`
  display: block;
  height: 10px;
  border-radius: 5px;
  border: 1px solid ${COLORS.ink};
`

const Ticks = styled.span`
  position: relative;
  grid-column: 2;
  grid-row: 2;
  height: 14px;
  font-family: ${FONTS.mono};
  font-size: 10px;
  color: ${COLORS.muted};

  > span {
    position: absolute;
    transform: translateX(-50%);
  }

  @media (max-width: 600px) {
    grid-column: 1;
    grid-row: auto;
  }
`

const Credits = styled.span`
  grid-column: 1 / -1;
  font-size: 12px;
  color: ${COLORS.muted};
`
