import { useState } from 'react'
import styled from 'styled-components'

import { getErrorMessage, useGetWikidataImportQuery, useImportWikidataMutation } from '../../api/api'
import { ErrorMessage, Muted, SecondaryButton, SectionTitle, SuccessMessage } from '../../components/ui'
import { COLORS } from '../../theme'
import { useTranslation } from '../../i18n'

/** How often the progress of a running import is refreshed */
const POLLING_INTERVAL_MS = 3000

/**
 * Completes the vessels with photos with what Wikidata knows of them, found by their MMSI. The import runs in the
 * background on the server: its progress is polled while it runs.
 */
export function WikidataImport() {
  const [isPolling, setIsPolling] = useState(false)
  const { data: status } = useGetWikidataImportQuery(undefined, {
    pollingInterval: isPolling ? POLLING_INTERVAL_MS : 0
  })
  const [importWikidata, { error, isLoading }] = useImportWikidataMutation()
  const { t } = useTranslation()

  // Polls while an import runs, including one started before the page was opened
  const isRunning = status?.isRunning ?? false
  if (isRunning !== isPolling) {
    setIsPolling(isRunning)
  }

  return (
    <Section>
      <SectionTitle as="h2">{t.admin.wikidataImportTitle}</SectionTitle>
      <Muted>{t.admin.wikidataImportText}</Muted>
      <div>
        <SecondaryButton disabled={isLoading || isRunning} onClick={() => importWikidata()} type="button">
          {isLoading ? t.admin.importing : t.admin.wikidataImportStart}
        </SecondaryButton>
      </div>
      {error && <ErrorMessage>{getErrorMessage(error, t)}</ErrorMessage>}
      {status?.startedAt && (
        <SuccessMessage aria-live="polite">
          <strong>{isRunning ? t.admin.photoImportRunning : t.admin.photoImportFinished}</strong>{' '}
          {t.admin.wikidataImportProgress(status)}
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
