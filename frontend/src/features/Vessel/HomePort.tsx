import { useState } from 'react'
import styled from 'styled-components'

import type { KeyType, Vessel } from '../../api/types'
import { HomePortCorrectionForm } from '../../components/HomePortCorrectionForm'
import { TextButton } from '../../components/ui'
import { useTranslation } from '../../i18n'
import { COLORS } from '../../theme'

/**
 * The port the vessel is based at, linked to its page on the ports site, and a way to correct it. Home ports are known
 * for French vessels only, by their CFR.
 */
export function HomePort({ vessel, vesselKey }: { vessel: Vessel; vesselKey: { key: string; keyType: KeyType } }) {
  const { t } = useTranslation()
  const [isOpen, setIsOpen] = useState(false)
  const homePort = vessel.homePort
  if (!homePort && !vessel.cfr?.startsWith('FRA')) {
    return null
  }

  return (
    <Wrapper>
      {homePort && (
        <Line>
          <span>{t.homePort.label}</span>
          <a href={homePort.pageUrl} rel="noopener">
            {homePort.name}
          </a>
          <Source>{t.homePort.sources[homePort.source]}</Source>
        </Line>
      )}
      {isOpen ? (
        <HomePortCorrectionForm
          currentLocode={homePort?.locode}
          onCancel={() => setIsOpen(false)}
          t={t}
          vesselKey={vesselKey}
        />
      ) : (
        <OpenButton onClick={() => setIsOpen(true)} type="button">
          {homePort ? t.homePort.open : t.homePort.openWithout}
        </OpenButton>
      )}
    </Wrapper>
  )
}

const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  gap: 6px;
`

const Line = styled.p`
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px 8px;
  font-size: 15px;

  > span:first-child {
    font-weight: 600;
  }
`

const Source = styled.span`
  font-size: 13px;
  color: ${COLORS.muted};
`

const OpenButton = styled(TextButton)`
  align-self: flex-start;
  min-height: 32px;
  padding: 0;
  font-size: 14px;
`
