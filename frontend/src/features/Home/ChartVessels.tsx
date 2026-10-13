import { useNavigate } from 'react-router-dom'

import type { VesselSummary } from '../../api/types'
import { CHART_COLORS, COLORS } from '../../theme'
import { getPhotoUrl, getVesselPath } from '../../utils/vesselKey'

/** Where the vessels sit on the chart (in its 1440×620 coordinates, before its 60 px offset). */
const SPOTS = [
  // Moved 100 px left of the mockup, so that the card is not cut by the right edge of common screens
  { boat: COLORS.ink, card: [1136, 228], heading: 225, line: [1136, 290], position: [1098, 312], sky: '#CFE0EA' },
  {
    boat: CHART_COLORS.photoCount,
    card: [968, 52],
    heading: 305,
    line: [968, 114],
    position: [952, 232],
    sky: '#E6D9C6'
  },
  { boat: '#2D5B7A', card: [1070, 404], heading: 165, line: [1070, 466], position: [1012, 468], sky: '#D5E3DA' }
] as const

/**
 * The latest vessels, drawn on the chart of the home page with a card each. Decorative: the same vessels are listed
 * below the search, so it is hidden from screen readers and from keyboard navigation.
 */
export function ChartVessels({ vessels }: { vessels: VesselSummary[] }) {
  const navigate = useNavigate()

  return (
    <svg aria-hidden="true" preserveAspectRatio="xMidYMid slice" viewBox="0 0 1440 620">
      <defs>
        <filter height="150%" id="cardShadow" width="140%" x="-20%" y="-20%">
          <feDropShadow dx="0" dy="3" floodColor={COLORS.ink} floodOpacity="0.22" stdDeviation="4" />
        </filter>
      </defs>
      <g transform="translate(60 0)">
        {SPOTS.slice(0, vessels.length).map((spot, index) => {
          const vessel = vessels[index]
          if (!vessel) {
            return null
          }
          const [x, y] = spot.position
          const [cardX, cardY] = spot.card
          const path = getVesselPath(vessel)
          const name = truncate(vessel.name ?? 'Unnamed vessel', 20)
          const key = vessel.cfr ? `CFR ${vessel.cfr}` : `MMSI ${vessel.mmsi}`
          const clipId = `chart-vessel-${index}`

          return (
            <g key={path}>
              <path
                d={`M${x} ${y} L${spot.line[0]} ${spot.line[1]}`}
                fill="none"
                stroke={COLORS.ink}
                strokeWidth="1.2"
              />
              <circle cx={x} cy={y} fill="none" r="24" stroke={COLORS.accent} strokeWidth="2" />
              <g transform={`translate(${x} ${y}) rotate(${spot.heading})`}>
                <path
                  d="M-5 14 L-14 46 M5 14 L14 46"
                  fill="none"
                  opacity="0.9"
                  stroke={COLORS.white}
                  strokeWidth="2.2"
                />
                <path
                  d="M0 14 V60"
                  fill="none"
                  opacity="0.7"
                  stroke={COLORS.ink}
                  strokeDasharray="3 4"
                  strokeWidth="1"
                />
                <path
                  d="M0 -15 C6 -7 6 9 5 14 H-5 C-6 9 -6 -7 0 -15 Z"
                  fill={spot.boat}
                  stroke={COLORS.white}
                  strokeWidth="1.4"
                />
                <rect fill={COLORS.white} height="7" width="6" x="-3" y="-2" />
              </g>
              <a
                aria-label={vessel.name ?? key}
                href={path}
                onClick={event => {
                  event.preventDefault()
                  navigate(path)
                }}
                style={{ cursor: 'pointer', pointerEvents: 'auto' }}
                tabIndex={-1}
              >
                <g transform={`translate(${cardX} ${cardY})`}>
                  <rect fill={COLORS.white} filter="url(#cardShadow)" height="124" rx="10" width="156" />
                  <clipPath id={clipId}>
                    <rect height="74" rx="6" width="144" x="6" y="6" />
                  </clipPath>
                  <g clipPath={`url(#${clipId})`}>
                    <rect fill={spot.sky} height="74" width="144" x="6" y="6" />
                    {vessel.mainPhoto ? (
                      <image
                        height="74"
                        href={getPhotoUrl(vessel.mainPhoto.urls, '320')}
                        preserveAspectRatio="xMidYMid slice"
                        width="144"
                        x="6"
                        y="6"
                      />
                    ) : (
                      <>
                        <rect fill={CHART_COLORS.sea} height="22" width="144" x="6" y="58" />
                        <path d="M30 52 H124 L114 66 H40 Z" fill={spot.boat} />
                        <path d="M70 52 V38 H96 L104 52 Z" fill={COLORS.white} stroke={COLORS.ink} strokeWidth="1.2" />
                      </>
                    )}
                  </g>
                  <text fill={COLORS.ink} fontFamily="IBM Plex Sans" fontSize="13" fontWeight="600" x="10" y="98">
                    {name}
                  </text>
                  <text fill={CHART_COLORS.cardMuted} fontFamily="IBM Plex Sans" fontSize="10" x="10" y="114">
                    {key}
                  </text>
                  <g fill="none" stroke={CHART_COLORS.photoCount} transform="translate(118 89)">
                    <rect height="10" rx="2" strokeWidth="1.3" width="14" x="0" y="2" />
                    <circle cx="7" cy="7" r="2.6" strokeWidth="1.2" />
                    <path d="M4 2 L5.5 0 H8.5 L10 2" strokeWidth="1.2" />
                  </g>
                  <text
                    fill={CHART_COLORS.photoCount}
                    fontFamily="IBM Plex Sans"
                    fontSize="10"
                    fontWeight="600"
                    textAnchor="end"
                    x="146"
                    y="114"
                  >
                    {vessel.photoCount} photo{vessel.photoCount === 1 ? '' : 's'}
                  </text>
                </g>
              </a>
            </g>
          )
        })}
      </g>
    </svg>
  )
}

function truncate(value: string, length: number): string {
  return value.length > length ? `${value.slice(0, length - 1)}…` : value
}
