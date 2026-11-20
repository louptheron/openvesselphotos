import { COLORS } from '../theme'

/**
 * A stern trawler seen from the side: raised bow, wheelhouse forward, mast with its radar, and the gantry at the stern
 * towing the trawl warp. Same drawing as public/ports-favicon.svg.
 */
export function TrawlerLogo({ size = 28 }: { size?: number }) {
  return (
    <svg aria-hidden fill="none" height={size} viewBox="0 0 32 32" width={size}>
      {/* Trawl warp, from the gantry into the sea */}
      <path
        d="M27.6 9.2 31 21.5"
        stroke={COLORS.inkSoft}
        strokeDasharray="1.4 1.3"
        strokeLinecap="round"
        strokeWidth="0.9"
      />
      {/* Mast, radar and stay */}
      <path d="M11 11.5V4.2M8.6 5.6h4.8M11 6.4l9.5 5.6" stroke={COLORS.white} strokeLinecap="round" strokeWidth="1.3" />
      {/* Gantry */}
      <path d="M24.6 17.6 26.2 9.2h2.6l-1.1 8.4" stroke={COLORS.white} strokeLinejoin="round" strokeWidth="1.5" />
      {/* Wheelhouse, with its windows */}
      <path d="M6.6 17.6v-5.4c0-.6.4-1 1-1h6.9c.5 0 .9.3 1 .8l1.2 5.6z" fill={COLORS.white} />
      <path d="M8.1 12.9h1.7v1.6H8.1zM10.8 12.9h1.7v1.6h-1.7zM13.5 12.9h1.4l.3 1.6h-1.7z" fill={COLORS.ink} />
      {/* Hull, raised at the bow */}
      <path d="M1.6 15.8 4.4 17.6h26l-2.6 5.6c-.3.6-.9 1-1.6 1H6.9c-.7 0-1.3-.4-1.6-1z" fill={COLORS.white} />
      {/* Waterline */}
      <path d="M5.1 21.6h24.2" stroke={COLORS.accent} strokeWidth="1.6" />
      {/* Sea */}
      <path
        d="M1.5 27.4c1.6 0 1.6-1 3.2-1s1.6 1 3.2 1 1.6-1 3.2-1 1.6 1 3.2 1 1.6-1 3.2-1 1.6 1 3.2 1 1.6-1 3.2-1 1.6 1 3.2 1 1.6-1 3.1-1"
        stroke={COLORS.accent}
        strokeLinecap="round"
        strokeWidth="1.4"
      />
    </svg>
  )
}
