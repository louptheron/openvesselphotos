import { useState } from 'react'
import styled from 'styled-components'

import {
  getErrorMessage,
  useFindHomePortsFromPortVisitsMutation,
  useGetHomePortsFromPortVisitsQuery
} from '../../api/api'
import { ErrorMessage, SecondaryButton, SuccessMessage } from '../../components/ui'
import { useTranslation } from '../../i18n'
import { COLORS } from '../../theme'

/** How often the progress of a running search is refreshed */
const POLLING_INTERVAL_MS = 3000

/**
 * Gives a home port to the vessels of the EU fleet register without one, from their port visits on Global Fishing
 * Watch. The search runs in the background on the server: its progress is polled while it runs.
 */
export function HomePortsFromPortVisits() {
  const [isPolling, setIsPolling] = useState(false)
  const { data: status } = useGetHomePortsFromPortVisitsQuery(undefined, {
    pollingInterval: isPolling ? POLLING_INTERVAL_MS : 0
  })
  const [start, { error, isLoading }] = useFindHomePortsFromPortVisitsMutation()
  const { t } = useTranslation()

  // Polls while a search runs, including one started before the page was opened
  const isRunning = status?.isRunning ?? false
  if (isRunning !== isPolling) {
    setIsPolling(isRunning)
  }

  return (
    <Section>
      <div>
        <SecondaryButton disabled={isLoading || isRunning} onClick={() => start()} type="button">
          {isLoading ? t.admin.importing : t.admin.homePortsFromPortVisitsStart}
        </SecondaryButton>
      </div>
      {error && <ErrorMessage>{getErrorMessage(error, t)}</ErrorMessage>}
      {status?.startedAt && (
        <SuccessMessage aria-live="polite">
          <strong>{isRunning ? t.admin.photoImportRunning : t.admin.photoImportFinished}</strong>{' '}
          {status.total === 0 ? t.admin.homePortsFromPortVisitsNone : t.admin.homePortsFromPortVisitsProgress(status)}
          {isRunning && <Progress max={status.total} value={status.done} />}
          {status.unknownPorts.length > 0 && (
            <p>
              {t.admin.homePortsFromPortVisitsUnknownPorts} {status.unknownPorts.join(', ')}
            </p>
          )}
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
