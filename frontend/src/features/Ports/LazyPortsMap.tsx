import { lazy } from 'react'
import styled from 'styled-components'

import { COLORS } from '../../theme'

/** Loaded with Leaflet only where a map is shown. */
export const PortsMap = lazy(() => import('./PortsMap').then(module => ({ default: module.PortsMap })))

export const MapPlaceholder = styled.div<{ $isCompact?: boolean }>`
  height: ${p => (p.$isCompact ? '260px' : '560px')};
  border-radius: 12px;
  border: 1px solid ${COLORS.border};
  background: #fafaf8;

  @media (max-width: 600px) {
    height: ${p => (p.$isCompact ? '220px' : '440px')};
  }
`
