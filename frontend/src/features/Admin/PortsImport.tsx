import { type FormEvent, useState } from 'react'
import styled from 'styled-components'

import {
  getErrorMessage,
  useImportAuctionSalesMutation,
  useImportHomePortsMutation,
  useImportPortPositionsMutation,
  useImportVesselOwnersMutation
} from '../../api/api'
import { Card, ErrorMessage, Muted, SecondaryButton, SectionTitle, SuccessMessage } from '../../components/ui'
import { useTranslation } from '../../i18n'
import { COLORS } from '../../theme'

/** The data of the ports site: the home ports of the vessels, and the positions of the ports on the map. */
export function PortsImport() {
  const { t } = useTranslation()
  const [importHomePorts, homePorts] = useImportHomePortsMutation()
  const [importPortPositions, positions] = useImportPortPositionsMutation()
  const [importVesselOwners, owners] = useImportVesselOwnersMutation()
  const [importAuctionSales, auctionSales] = useImportAuctionSalesMutation()

  return (
    <Section>
      <SectionTitle as="h2">{t.admin.portsImportTitle}</SectionTitle>
      <Muted>{t.admin.portsImportText}</Muted>
      <FileForm
        id="home-ports-file"
        isLoading={homePorts.isLoading}
        label={t.admin.homePortsFile}
        onSubmit={importHomePorts}
      />
      {homePorts.error && <ErrorMessage>{getErrorMessage(homePorts.error, t)}</ErrorMessage>}
      {homePorts.data && (
        <SuccessMessage>
          {t.admin.homePortsResult(homePorts.data)}
          <Errors messages={homePorts.data.errors} />
        </SuccessMessage>
      )}
      <FileForm
        id="port-positions-file"
        isLoading={positions.isLoading}
        label={t.admin.portPositionsFile}
        onSubmit={importPortPositions}
      />
      {positions.error && <ErrorMessage>{getErrorMessage(positions.error, t)}</ErrorMessage>}
      {positions.data && (
        <SuccessMessage>
          {t.admin.portPositionsResult(positions.data)}
          <Errors messages={positions.data.errors} />
        </SuccessMessage>
      )}
      <FileForm
        id="vessel-owners-file"
        isLoading={owners.isLoading}
        label={t.admin.vesselOwnersFile}
        onSubmit={importVesselOwners}
      />
      {owners.error && <ErrorMessage>{getErrorMessage(owners.error, t)}</ErrorMessage>}
      {owners.data && (
        <SuccessMessage>
          {t.admin.vesselOwnersResult(owners.data)}
          <Errors messages={owners.data.errors} />
        </SuccessMessage>
      )}
      <Muted>{t.admin.auctionSalesHelp}</Muted>
      <FileForm
        accept=".xlsm,.xlsx"
        id="auction-sales-file"
        isLoading={auctionSales.isLoading}
        label={t.admin.auctionSalesFile}
        onSubmit={importAuctionSales}
      />
      {auctionSales.error && <ErrorMessage>{getErrorMessage(auctionSales.error, t)}</ErrorMessage>}
      {auctionSales.data && <SuccessMessage>{t.admin.auctionSalesResult(auctionSales.data)}</SuccessMessage>}
    </Section>
  )
}

type FileFormProps = {
  /** CSV by default */
  accept?: string
  id: string
  isLoading: boolean
  label: string
  onSubmit: (form: FormData) => unknown
}

function FileForm({ accept = '.csv,text/csv', id, isLoading, label, onSubmit }: FileFormProps) {
  const { t } = useTranslation()
  const [file, setFile] = useState<File>()

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (file) {
      const form = new FormData()
      form.append('file', file)
      onSubmit(form)
    }
  }

  return (
    <Form as="form" onSubmit={submit}>
      <label htmlFor={id}>{label}</label>
      <input accept={accept} id={id} onChange={event => setFile(event.target.files?.[0])} type="file" />
      <SecondaryButton disabled={!file || isLoading} type="submit">
        {isLoading ? t.admin.importing : t.admin.import}
      </SecondaryButton>
    </Form>
  )
}

function Errors({ messages }: { messages: string[] }) {
  if (messages.length === 0) {
    return null
  }

  return (
    <ErrorList>
      {messages.map(message => (
        <li key={message}>{message}</li>
      ))}
    </ErrorList>
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
    min-width: 120px;
    font-weight: 500;
  }

  input {
    flex: 1 1 240px;
    min-width: 0;
  }
`

const ErrorList = styled.ul`
  margin: 8px 0 0;
  padding-left: 18px;
  font-size: 13px;
  color: ${COLORS.reject};
`
