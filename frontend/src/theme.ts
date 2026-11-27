export const COLORS = {
  accent: '#E0662A',
  accentHover: '#C9561E',
  accentInk: '#1C0B02',
  accentSoft: '#FDF1EA',
  approve: '#1D5B34',
  border: '#DAD5CA',
  cardBorder: '#E4DFD4',
  codeBackground: '#06151F',
  codeText: '#D5E1E9',
  divider: '#EEEAE2',
  ground: '#F5F3EE',
  info: '#E8F1F6',
  ink: '#0B2233',
  inkSoft: '#B9C8D3',
  inputBorder: '#B9B2A3',
  link: '#0E4A6E',
  linkHover: '#08304A',
  muted: '#55697A',
  /** Panels floating over the map */
  panel: '#FBFAF6',
  /** Their edge: the ink, faded, so that it reads on the map and on the page alike */
  panelBorder: 'rgba(11, 34, 51, 0.22)',
  reject: '#A3361A',
  text: '#3D5363',
  white: '#FFFFFF'
}

/** Colors of the nautical chart illustrations. */
export const CHART_COLORS = {
  cardBorder: '#CFDDE5',
  cardMuted: '#4A5E6E',
  photoCount: '#B5501C',
  sea: '#8FB4C9',
  text: '#22384A',
  textMuted: '#3F5466',
  waveLine: '#9DBFD3'
}

/** A photo placeholder: sky with a band of sea at the bottom. */
export function getSkyAndSea(sky: string, seaFrom = 76): string {
  return `linear-gradient(to bottom, ${sky} 0 ${seaFrom}%, ${CHART_COLORS.sea} ${seaFrom}% 100%)`
}

export const FONTS = {
  display: "'Bricolage Grotesque Variable', sans-serif",
  mono: "'IBM Plex Mono', monospace",
  sans: "'IBM Plex Sans', system-ui, sans-serif"
}

/** Pastel backgrounds for photo placeholders. */
export const SKIES = ['#CFE0EA', '#E6D9C6', '#D5E3DA', '#DCD8E8']

/**
 * The one corner radius of the site: square corners, as on a printed chart or report. Rounded pills and cards read as
 * buttons or as a generic dashboard; the few round things (port dots, the sheet handle) are drawn round on purpose.
 */
export const RADIUS = '2px'

/** The type scale: few steps, far enough apart that a heading never looks like a label. */
export const TYPE = {
  /** Hairline notes, sources, credits */
  note: '11.5px',
  /** Labels, table cells, secondary text */
  small: '13px',
  /** Body text */
  body: '14.5px',
  /** Key figures */
  figure: '20px',
  /** The page title */
  title: '28px'
}
