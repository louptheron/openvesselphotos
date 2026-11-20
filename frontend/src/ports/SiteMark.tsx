import { COLORS } from '../theme'

/**
 * The mark of the site: a fish whose body holds bars of a chart, for the figures of the French fishing industry.
 * Flat and sober, so that it reads as a reference rather than an illustration. Same drawing as
 * public/ports-favicon.svg and ShareImageRenderer.drawMark.
 */
export function SiteMark({ size = 28 }: { size?: number }) {
  return (
    <svg aria-hidden fill="none" height={size} viewBox="0 0 32 32" width={size}>
      <rect fill={COLORS.ink} height="32" rx="6" width="32" />
      {/* Tail */}
      <path d="M9 16 4 10.5v11z" fill={COLORS.white} />
      {/* Body */}
      <path
        d="M8 16c3-5.5 7.5-7.5 11-7.5 4.5 0 7.5 3.5 9 7.5-1.5 4-4.5 7.5-9 7.5-3.5 0-8-2-11-7.5z"
        fill={COLORS.white}
      />
      {/* Bars of a chart */}
      <path d="M12.5 14.2h8M12.5 17.8h5" stroke={COLORS.ink} strokeLinecap="round" strokeWidth="1.8" />
      {/* Eye */}
      <circle cx="24.2" cy="14" fill={COLORS.accent} r="1.4" />
    </svg>
  )
}
