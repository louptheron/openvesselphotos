import { type FormEvent, useState } from 'react'
import styled from 'styled-components'

import { getErrorMessage, useImportVesselsMutation } from '../../api/api'
import { Card, ErrorMessage, Muted, SecondaryButton, SectionTitle, SuccessMessage } from '../../components/ui'
import { COLORS } from '../../theme'

/** Creates vessels without photos from a CSV file, e.g. an export of the EU fleet register. */
export function VesselImport() {
  const [file, setFile] = useState<File>()
  const [importVessels, { data: result, error, isLoading }] = useImportVesselsMutation()

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
      <SectionTitle as="h2">Import vessels</SectionTitle>
      <Muted>
        Creates vessels without photos, so that their pages exist and can be found. Accepts an export of the{' '}
        <a href="https://webgate.ec.europa.eu/fleet-europa/search_en" rel="noreferrer" target="_blank">
          EU fleet register
        </a>{' '}
        (CSV, current fleet of a country), or a CSV with the columns cfr, mmsi, name, registration, port. Existing
        vessels are never overwritten: only their missing data is filled in. 15 MB max.
      </Muted>
      <Form as="form" onSubmit={submit}>
        <label htmlFor="vessels-file">CSV file</label>
        <input
          accept=".csv,text/csv"
          id="vessels-file"
          onChange={event => setFile(event.target.files?.[0])}
          type="file"
        />
        <SecondaryButton disabled={!file || isLoading} type="submit">
          {isLoading ? 'Importing…' : 'Import'}
        </SecondaryButton>
      </Form>
      {error && <ErrorMessage>{getErrorMessage(error)}</ErrorMessage>}
      {result && (
        <SuccessMessage>
          {result.read} lines read: {result.created} vessels created, {result.updated} completed, {result.unchanged}{' '}
          already up to date, {result.skipped} skipped.
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
