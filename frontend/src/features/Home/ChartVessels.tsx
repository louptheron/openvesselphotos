import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import type { VesselSummary } from '../../api/types'
import { CHART_COLORS, COLORS } from '../../theme'
import { getPhotoUrl, getVesselPath } from '../../utils/vesselKey'
import { COMMONS_VESSELS } from './commonsVessels'

/** Where the vessels sit on the chart (in its 1440×620 coordinates, before its 60 px offset), as in the mockup. */
const SPOTS = [
  { boat: COLORS.ink, card: [1236, 228], heading: 225, line: [1236, 290], position: [1098, 312], sky: '#CFE0EA' },
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

type Card = {
  /** Shown in the bottom right corner */
  count: string
  /** External link (Wikimedia Commons) or a vessel page */
  href: string
  isExternal: boolean
  name: string
  photo: string | undefined
  subtitle: string
}

function toCard(vessel: VesselSummary | undefined, index: number): Card | undefined {
  if (vessel) {
    return {
      count: `${vessel.photoCount} photo${vessel.photoCount === 1 ? '' : 's'}`,
      href: getVesselPath(vessel),
      isExternal: false,
      name: vessel.name ?? 'Unnamed vessel',
      photo: vessel.mainPhoto ? getPhotoUrl(vessel.mainPhoto.urls, '320') : undefined,
      subtitle: vessel.cfr ? `CFR ${vessel.cfr}` : `MMSI ${vessel.mmsi}`
    }
  }
  const commonsVessel = COMMONS_VESSELS[index]

  return (
    commonsVessel && {
      count: 'Commons',
      href: commonsVessel.fileUrl,
      isExternal: true,
      name: commonsVessel.name,
      photo: commonsVessel.photo,
      subtitle: commonsVessel.registration
    }
  )
}

/**
 * The latest vessels, drawn on the chart of the home page with a card each, as in the mockup. While the library has
 * fewer than 3 vessels, real vessels photographed on Wikimedia Commons fill the remaining spots (see the credits under
 * the chart). Decorative: the library's vessels are also listed below the search, so it is hidden from screen readers
 * and from keyboard navigation.
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
        {SPOTS.map((spot, index) => {
          const card = toCard(vessels[index], index)
          if (!card) {
            return null
          }
          const [x, y] = spot.position
          const [cardX, cardY] = spot.card
          const clipId = `chart-vessel-${index}`

          return (
            <g key={card.href}>
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
              <CardLink
                aria-label={card.name}
                href={card.href}
                onClick={event => {
                  if (!card.isExternal) {
                    event.preventDefault()
                    navigate(card.href)
                  }
                }}
                rel={card.isExternal ? 'noreferrer' : undefined}
                tabIndex={-1}
                target={card.isExternal ? '_blank' : undefined}
              >
                <g transform={`translate(${cardX} ${cardY})`}>
                  <rect fill={COLORS.white} filter="url(#cardShadow)" height="124" rx="10" width="156" />
                  <clipPath id={clipId}>
                    <rect height="74" rx="6" width="144" x="6" y="6" />
                  </clipPath>
                  <g clipPath={`url(#${clipId})`}>
                    <rect fill={spot.sky} height="74" width="144" x="6" y="6" />
                    {card.photo ? (
                      <image
                        height="74"
                        href={card.photo}
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
                        <path
                          d="M84 38 V20 M84 24 L58 46 M84 24 L112 46"
                          fill="none"
                          stroke={COLORS.ink}
                          strokeWidth="1.3"
                        />
                        <path d="M44 52 V36 H54 V52" fill="none" stroke={COLORS.ink} strokeWidth="1.6" />
                      </>
                    )}
                  </g>
                  <text fill={COLORS.ink} fontFamily="IBM Plex Sans" fontSize="13" fontWeight="600" x="10" y="98">
                    {truncate(card.name, 20)}
                  </text>
                  <text fill={CHART_COLORS.cardMuted} fontFamily="IBM Plex Sans" fontSize="10" x="10" y="114">
                    {card.subtitle}
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
                    {card.count}
                  </text>
                </g>
              </CardLink>
            </g>
          )
        })}
      </g>
    </svg>
  )
}

const CardLink = styled.a`
  cursor: pointer;
  pointer-events: auto;
`

function truncate(value: string, length: number): string {
  return value.length > length ? `${value.slice(0, length - 1)}…` : value
}
