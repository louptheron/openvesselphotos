import { useState } from 'react'
import styled from 'styled-components'

import { getErrorMessage, useSyncFishingActivityMutation, useGetFishingActivitySyncQuery } from '../../api/api'
import { ErrorMessage, SecondaryButton, SuccessMessage } from '../../components/ui'
import { useTranslation } from '../../i18n'
import { COLORS } from '../../theme'

/** How often the progress of a running synchronisation is refreshed */
const POLLING_INTERVAL_MS = 3000

/**
 * Where the vessels of the ports site fished, for the map of each port, from Global Fishing Watch. The synchronisation
 * runs in the background on the server: its progress is polled while it runs.
 */
export function FishingActivitySync() {
  const [isPolling, setIsPolling] = useState(false)
  const { data: status } = useGetFishingActivitySyncQuery(undefined, {
    pollingInterval: isPolling ? POLLING_INTERVAL_MS : 0
  })
  const [start, { error, isLoading }] = useSyncFishingActivityMutation()
  const { t } = useTranslation()

  // Polls while a synchronisation runs, including one started before the page was opened
  const isRunning = status?.isRunning ?? false
  if (isRunning !== isPolling) {
    setIsPolling(isRunning)
  }

  return (
    <Section>
      <div>
        <SecondaryButton disabled={isLoading || isRunning} onClick={() => start()} type="button">
          {isLoading ? t.admin.importing : t.admin.fishingActivityStart}
        </SecondaryButton>
      </div>
      {error && <ErrorMessage>{getErrorMessage(error, t)}</ErrorMessage>}
      {status?.startedAt && (
        <SuccessMessage aria-live="polite">
          <strong>{isRunning ? t.admin.photoImportRunning : t.admin.photoImportFinished}</strong>{' '}
          {t.admin.fishingActivityProgress(status)}
          {isRunning && <Progress max={status.total} value={status.done} />}
          {status.errors.length > 0 && (
            <Errors>
              {status.errors.map(message => (
                <li key={message}>{message}</li>
              ))}
            </Errors>
          )}
        </SuccessMessage>
      )}
    </Section>
  )
}

const Section = styled.section`
  margin-top: 32px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 760px;
`

const Progress = styled.progress`
  display: block;
  width: 100%;
  margin-top: 10px;
  accent-color: ${COLORS.accent};
`

const Errors = styled.ul`
  margin: 8px 0 0;
  padding-left: 18px;
  font-size: 13px;
  color: ${COLORS.reject};
`
