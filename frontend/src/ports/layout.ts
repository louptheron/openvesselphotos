import { css } from 'styled-components'

import { COLORS } from '../theme'

/** The floating panel, on the left of the map on wide screens */
export const PANEL_WIDTH = 360
export const PANEL_MARGIN = 16
/** Below it, the panel is a sheet at the bottom of the screen */
export const MOBILE_QUERY = '(max-width: 760px)'
/** Share of the screen height the sheet takes on phones */
export const SHEET_HEIGHT = 0.46

/** The floating panels over the map, and the pages beyond it: one look for the whole site */
export const glass = css`
  border: 1px solid ${COLORS.border};
  background: #fbfaf6;
  box-shadow: 0 1px 2px rgba(11, 34, 51, 0.1);
`
