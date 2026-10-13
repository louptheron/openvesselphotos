import type { ReactNode } from 'react'
import styled from 'styled-components'

import chartUrl from '../assets/chart.svg'
import { CHART_COLORS, COLORS } from '../theme'

/** A section drawn over a nautical chart (Étel estuary), with its content in a translucent card. */
export function ChartSection({ children, overlay }: { children: ReactNode; overlay?: ReactNode }) {
  return (
    <Wrapper>
      <img alt="" aria-hidden="true" src={chartUrl} />
      {/* Fades the chart, so that the vessels drawn on it and the content stand out */}
      <Veil />
      {overlay}
      {children}
    </Wrapper>
  )
}

export const ChartCard = styled.div`
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid ${CHART_COLORS.cardBorder};
  border-radius: 18px;
  display: flex;
  flex-direction: column;
`

/** The wave above a dark band. */
export function WaveDivider() {
  return (
    <svg
      aria-hidden="true"
      height="44"
      preserveAspectRatio="none"
      style={{ display: 'block' }}
      viewBox="0 0 1440 44"
      width="100%"
    >
      <path
        d="M0 44 V24 C120 4 240 44 360 24 C480 4 600 44 720 24 C840 4 960 44 1080 24 C1200 4 1320 44 1440 24 V44 Z"
        fill={COLORS.ink}
      />
      <path
        d="M0 18 C120 -2 240 38 360 18 C480 -2 600 38 720 18 C840 -2 960 38 1080 18 C1200 -2 1320 38 1440 18"
        fill="none"
        stroke={CHART_COLORS.waveLine}
        strokeWidth="1.5"
      />
    </svg>
  )
}

const Veil = styled.div`
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.4);
`

const Wrapper = styled.div`
  position: relative;
  overflow: hidden;
  border-top: 1px solid ${COLORS.border};
  border-bottom: 1px solid ${COLORS.border};
  background: #eef3f4;

  /* The chart and its overlay cover the section, cropped like preserveAspectRatio="xMidYMid slice" */
  > img,
  > svg {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  > svg {
    pointer-events: none;
  }

  > section {
    position: relative;
  }
`
