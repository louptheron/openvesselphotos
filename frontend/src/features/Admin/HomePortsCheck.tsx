import { useState } from 'react'
import styled from 'styled-components'

import {
  getErrorMessage,
  useCheckHomePortsMutation,
  useGetHomePortChecksQuery,
  useGetHomePortsCheckQuery
} from '../../api/api'
import type { HomePortCheckResult } from '../../api/types'
import { ErrorMessage, Input, SecondaryButton, SuccessMessage } from '../../components/ui'
import { useTranslation } from '../../i18n'
import { COLORS } from '../../theme'

/** How often the progress of a running check is refreshed */
const POLLING_INTERVAL_MS = 3000
const RESULTS: HomePortCheckResult[] = [
  'DISAGREES',
  'NOT_ON_THE_MAP',
  'NEARBY',
  'AGREES',
  'TOO_FEW_VISITS',
  'NOT_FOUND'
]
/** Shown in the table; the others are in the CSV file */
const TO_REVIEW: HomePortCheckResult[] = ['DISAGREES', 'NOT_ON_THE_MAP']
const MAX_ROWS = 200

/**
 * Double checks the imported home ports with the port visits of the vessels on Global Fishing Watch. The check runs in
 * the background on the server: its progress is polled while it runs. It changes no home port.
 */
export function HomePortsCheck() {
  const [port, setPort] = useState('')
  const [isPolling, setIsPolling] = useState(false)
  const { data: status } = useGetHomePortsCheckQuery(undefined, {
    pollingInterval: isPolling ? POLLING_INTERVAL_MS : 0
  })
  const { data: checks = [], refetch } = useGetHomePortChecksQuery()
  const [start, { error, isLoading }] = useCheckHomePortsMutation()
  const { t } = useTranslation()

  // Polls while a check runs, including one started before the page was opened; the results once it ends
  const isRunning = status?.isRunning ?? false
  if (isRunning !== isPolling) {
    setIsPolling(isRunning)
    if (!isRunning) {
      refetch()
    }
  }
  const counts = RESULTS.map(result => ({ count: checks.filter(it => it.result === result).length, result })).filter(
    ({ count }) => count > 0
  )
  const toReview = checks.filter(check => TO_REVIEW.includes(check.result))

  return (
    <Section>
      <Form
        onSubmit={event => {
          event.preventDefault()
          start(port.trim().toUpperCase() || undefined)
        }}
      >
        <label>
          {t.admin.homePortsCheckPort}
          <Input maxLength={12} onChange={event => setPort(event.target.value)} placeholder="FRLRT" value={port} />
        </label>
        <SecondaryButton disabled={isLoading || isRunning} type="submit">
          {isLoading ? t.admin.importing : t.admin.homePortsCheckStart}
        </SecondaryButton>
      </Form>
      {error && <ErrorMessage>{getErrorMessage(error, t)}</ErrorMessage>}
      {status?.startedAt && (
        <SuccessMessage aria-live="polite">
          <strong>{isRunning ? t.admin.photoImportRunning : t.admin.photoImportFinished}</strong>{' '}
          {t.admin.homePortsCheckProgress(status)}
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
      {counts.length > 0 && (
        <>
          <Counts>
            {counts.map(({ count, result }) => (
              <li key={result}>
                <b>{count}</b> {t.admin.homePortsCheckResults[result]}
              </li>
            ))}
          </Counts>
          <a download href="/v1/admin/ports/home-ports/checks.csv">
            {t.admin.homePortsCheckDownload}
          </a>
        </>
      )}
      {toReview.length > 0 && (
        <>
          <strong>{t.admin.homePortsCheckToReview(toReview.length)}</strong>
          <TableWrapper>
            <Table>
              <thead>
                <tr>
                  <th>{t.admin.homePortsCheckVessel}</th>
                  <th>{t.admin.homePortsCheckImported}</th>
                  <th>{t.admin.homePortsCheckVisited}</th>
                  <th>{t.admin.homePortsCheckVisits}</th>
                </tr>
              </thead>
              <tbody>
                {toReview.slice(0, MAX_ROWS).map(check => (
                  <tr key={check.cfr}>
                    <td>
                      {check.vesselName ?? '—'}
                      <small>{check.cfr}</small>
                    </td>
                    <td>
                      {check.importedPortName ?? check.importedLocode}
                      <small>{check.importedLocode}</small>
                    </td>
                    <td>
                      {check.gfwPortName ?? '—'}
                      <small>
                        {[check.gfwLocode, check.distanceKm !== null ? `${Math.round(check.distanceKm)} km` : null]
                          .filter(Boolean)
                          .join(' · ')}
                      </small>
                    </td>
                    <td>
                      {t.admin.homePortsCheckVisitsCell(
                        check.visitsAtGfwPort,
                        check.visitsAtImported,
                        check.visitsTotal
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </TableWrapper>
        </>
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

const Form = styled.form`
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px;

  label {
    display: flex;
    flex-direction: column;
    gap: 4px;
    font-size: 13px;
  }
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

const Counts = styled.ul`
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 14px;
`

const TableWrapper = styled.div`
  max-height: 480px;
  overflow: auto;
  border: 1px solid ${COLORS.border};
`

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;

  th,
  td {
    padding: 6px 8px;
    border-bottom: 1px solid ${COLORS.border};
    text-align: left;
    vertical-align: top;
  }

  th {
    position: sticky;
    top: 0;
    background: ${COLORS.white};
  }

  small {
    display: block;
    color: ${COLORS.muted};
  }
`
