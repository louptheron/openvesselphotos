import { COLORS } from '../theme'

export function LogoMark({ color = COLORS.ink, size = 30 }: { color?: string; size?: number }) {
  return (
    <svg
      aria-hidden="true"
      fill="none"
      height={size}
      stroke={color}
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth="2"
      viewBox="0 0 32 32"
      width={size}
    >
      <path d="M4 20h24l-4 7H8z" />
      <path d="M9 20v-8h10l5 8" />
      <path d="M13 12V6" />
      <path d="M3 29c3 0 3-1.5 6.5-1.5S13 29 16 29s3-1.5 6.5-1.5S26 29 29 29" stroke={COLORS.accent} />
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
