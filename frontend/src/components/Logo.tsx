import { COLORS } from '../theme'

/**
 * A camera's frame corners around a vessel on the horizon: photos of vessels. Drawn in one color, with the horizon in
 * the accent; the same drawing as public/favicon.svg.
 */
export function LogoMark({ color = COLORS.ink, size = 30 }: { color?: string; size?: number }) {
  return (
    <svg aria-hidden="true" fill="none" height={size} viewBox="0 0 32 32" width={size}>
      <path
        d="M3.5 10V3.5H10M22 3.5h6.5V10M28.5 22v6.5H22M10 28.5H3.5V22"
        stroke={color}
        strokeLinecap="square"
        strokeWidth="2.4"
      />
      {/* Hull and wheelhouse */}
      <path d="M8 18.5h16.5l-3 4.5H10.5zM11.5 18.5V14h6l2.5 4.5z" fill={color} />
      <path d="M7 25.5h18" stroke={COLORS.accent} strokeLinecap="round" strokeWidth="2.2" />
    </svg>
  )
}

/** Drawing shown where a vessel has no photo. */
export function BoatPlaceholder({ width = 150 }: { width?: number }) {
  return (
    <svg
      aria-hidden="true"
      fill="none"
      height={(width * 16) / 30}
      stroke={COLORS.ink}
      strokeLinejoin="round"
      strokeWidth={0.7}
      viewBox="0 0 30 16"
      width={width}
    >
      <path d="M2 10h26l-3 4.5H5z" fill={COLORS.white} />
      <path d="M7 10V5h9l4 5" fill={COLORS.white} />
      <path d="M10 5V1.5h2" />
    </svg>
  )
}
