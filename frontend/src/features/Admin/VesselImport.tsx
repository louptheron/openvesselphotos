import { type FormEvent, useState } from 'react'
import styled from 'styled-components'

import { getErrorMessage, useImportVesselsMutation } from '../../api/api'
import { Card, ErrorMessage, Muted, SecondaryButton, SectionTitle, SuccessMessage } from '../../components/ui'
import { COLORS } from '../../theme'
import { useTranslation } from '../../i18n'

/** Creates vessels without photos from a CSV file, e.g. an export of the EU fleet register. */
export function VesselImport() {
  const [file, setFile] = useState<File>()
  const [importVessels, { data: result, error, isLoading }] = useImportVesselsMutation()
  const { t } = useTranslation()

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (file) {
      const form = new FormData()
      form.append('file', file)
      importVessels(form)
    }
  }

  return (
    <Section>
      <SectionTitle as="h2">{t.admin.importTitle}</SectionTitle>
      <Muted>
        {t.admin.importText1}{' '}
        <a href="https://webgate.ec.europa.eu/fleet-europa/search_en" rel="noreferrer" target="_blank">
          {t.admin.importRegister}
        </a>{' '}
        {t.admin.importText2}
      </Muted>
      <Form as="form" onSubmit={submit}>
        <label htmlFor="vessels-file">{t.admin.csvFile}</label>
        <input
          accept=".csv,text/csv"
          id="vessels-file"
          onChange={event => setFile(event.target.files?.[0])}
          type="file"
        />
        <SecondaryButton disabled={!file || isLoading} type="submit">
          {isLoading ? t.admin.importing : t.admin.import}
        </SecondaryButton>
      </Form>
      {error && <ErrorMessage>{getErrorMessage(error, t)}</ErrorMessage>}
      {result && (
        <SuccessMessage>
          {t.admin.importResult(result)}
          {result.errors.length > 0 && (
            <Errors>
              {result.errors.map(message => (
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

const Errors = styled.ul`
  margin: 8px 0 0;
  padding-left: 18px;
  font-size: 13px;
  color: ${COLORS.reject};
`
