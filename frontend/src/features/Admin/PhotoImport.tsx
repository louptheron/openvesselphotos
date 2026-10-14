import { type FormEvent, useState } from 'react'
import styled from 'styled-components'

import {
  getErrorMessage,
  useCancelPhotoImportMutation,
  useGetPhotoImportQuery,
  useImportPhotosMutation
} from '../../api/api'
import {
  Card,
  ErrorMessage,
  Muted,
  SecondaryButton,
  SectionTitle,
  SuccessMessage,
  TextButton
} from '../../components/ui'
import { COLORS } from '../../theme'
import { useTranslation } from '../../i18n'

/** How often the progress of a running import is refreshed */
const POLLING_INTERVAL_MS = 3000

/**
 * Publishes openly licensed photos without review from a CSV file (e.g. a Wikimedia Commons export). The import runs in
 * the background on the server: its progress is polled while it runs.
 */
export function PhotoImport() {
  const [file, setFile] = useState<File>()
  const [isPolling, setIsPolling] = useState(false)
  const { data: status } = useGetPhotoImportQuery(undefined, {
    pollingInterval: isPolling ? POLLING_INTERVAL_MS : 0
  })
  const [importPhotos, { error, isLoading }] = useImportPhotosMutation()
  const [cancelImport] = useCancelPhotoImportMutation()
  const { t } = useTranslation()

  // Polls while an import runs, including one started before the page was opened
  const isRunning = status?.isRunning ?? false
  if (isRunning !== isPolling) {
    setIsPolling(isRunning)
  }

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (file) {
      const form = new FormData()
      form.append('file', file)
      importPhotos(form)
    }
  }

  return (
    <Section>
      <SectionTitle as="h2">{t.admin.photoImportTitle}</SectionTitle>
      <Muted>{t.admin.photoImportText}</Muted>
      <Form as="form" onSubmit={submit}>
        <label htmlFor="photos-file">{t.admin.csvFile}</label>
        <input
          accept=".csv,text/csv"
          id="photos-file"
          onChange={event => setFile(event.target.files?.[0])}
          type="file"
        />
        <SecondaryButton disabled={!file || isLoading || isRunning} type="submit">
          {isLoading ? t.admin.importing : t.admin.import}
        </SecondaryButton>
      </Form>
      {error && <ErrorMessage>{getErrorMessage(error, t)}</ErrorMessage>}
      {status?.startedAt && (
        <SuccessMessage aria-live="polite">
          <strong>{isRunning ? t.admin.photoImportRunning : t.admin.photoImportFinished}</strong>{' '}
          {t.admin.photoImportProgress(status)}
          {isRunning && (
            <>
              <Progress max={status.total} value={status.done} />
              <TextButton onClick={() => cancelImport()} type="button">
                {t.admin.stopImport}
              </TextButton>
            </>
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

const Form = styled(Card)`
  padding: 16px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;

  label {
    font-weight: 500;
  }

  input {
    flex: 1 1 240px;
    min-width: 0;
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
