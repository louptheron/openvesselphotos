import { css } from 'styled-components'

import { COLORS } from '../theme'

/** The floating panel, on the left of the map on wide screens */
export const PANEL_WIDTH = 360
export const PANEL_MARGIN = 16
/** Below it, the panel is a sheet at the bottom of the screen */
export const MOBILE_QUERY = '(max-width: 760px)'
/** Share of the screen height the sheet takes on phones */
export const SHEET_HEIGHT = 0.6
/** Taller when it shows a port, which has more to read: the map above still shows the port */
export const PORT_SHEET_HEIGHT = 0.72
/** On phones, the gear filter bar covers the top of the map: the expanded sheet stops below it */
export const SHEET_TOP_OFFSET = 64

/** The floating panels over the map, and the pages beyond it: one look for the whole site */
export const glass = css`
  border: 1px solid ${COLORS.border};
  background: #fbfaf6;
  box-shadow: 0 1px 2px rgba(11, 34, 51, 0.1);
`
