import styled from 'styled-components'

import { useTranslation } from '../i18n'
import { COLORS, FONTS } from '../theme'

/** The share of vessels with a photo, as a progress bar with its figures. */
export function CoverageBar({
  label,
  photographed,
  total
}: {
  /** e.g. "1,234 of 5,843 vessels of the EU fleet register have a photo." */
  label: string
  photographed: number
  total: number
}) {
  const { language, t } = useTranslation()
  const share = total > 0 ? photographed / total : 0

  return (
    <Wrapper>
      <Figures>
        <Percent>{formatShare(share, language === 'fr' ? 'fr-FR' : 'en-GB')}</Percent>
        <span>{label}</span>
      </Figures>
      <progress aria-label={t.coverage.barLabel} className="visually-hidden" max={total} value={photographed} />
      <Track aria-hidden="true">
        {/* At least a sliver, so that the first photos show */}
        <Fill style={{ width: `${photographed > 0 ? Math.max(share * 100, 1) : 0}%` }} />
      </Track>
    </Wrapper>
  )
}

/** "21%" or "21 %", and "<1%" for the first photos rather than "0%". */
function formatShare(share: number, locale: string): string {
  const format = new Intl.NumberFormat(locale, { maximumFractionDigits: 0, style: 'percent' })

  return share > 0 && share < 0.01 ? `<${format.format(0.01)}` : format.format(share)
}

const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
`

const Figures = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px 12px;
  color: ${COLORS.text};
  font-size: 15px;
  line-height: 1.4;
`

const Percent = styled.strong`
  font-family: ${FONTS.display};
  font-size: 28px;
  line-height: 1;
  color: ${COLORS.ink};
`

const Track = styled.div`
  height: 10px;
  border-radius: 5px;
  background: #d4e6ef;
  border: 1px solid #93b1c3;
  overflow: hidden;
`

const Fill = styled.div`
  height: 100%;
  background: ${COLORS.accent};
  border-radius: 5px;
  transition: width 0.6s ease-out;
`
