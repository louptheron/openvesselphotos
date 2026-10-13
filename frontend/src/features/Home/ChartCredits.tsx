import { Fragment } from 'react'
import styled from 'styled-components'

import { useTranslation } from '../../i18n'
import { CHART_COLORS } from '../../theme'
import { COMMONS_VESSELS } from './commonsVessels'

/** Credits of the Wikimedia Commons photos shown on the chart, as their licences require. */
export function ChartCredits({ libraryVesselCount }: { libraryVesselCount: number }) {
  const { t } = useTranslation()
  const shown = COMMONS_VESSELS.slice(libraryVesselCount)
  if (shown.length === 0) {
    return null
  }

  return (
    <Credits>
      {t.chart.creditsIntro}{' '}
      {shown.map((vessel, index) => (
        <Fragment key={vessel.fileUrl}>
          {index > 0 && ', '}
          <a href={vessel.fileUrl} rel="noreferrer" target="_blank">
            {vessel.name}
          </a>{' '}
          {t.chart.creditsBy} {vessel.author} (
          <a href={vessel.licenceUrl} rel="noreferrer" target="_blank">
            {vessel.licence}
          </a>
          )
        </Fragment>
      ))}
      {t.chart.creditsOutro}
    </Credits>
  )
}

const Credits = styled.p`
  position: absolute;
  right: 0;
  bottom: 0;
  margin: 0;
  max-width: min(640px, 100%);
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.85);
  font-size: 11px;
  line-height: 1.4;
  color: ${CHART_COLORS.textMuted};
  pointer-events: auto;

  a {
    color: inherit;
  }
`
