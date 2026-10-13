import { type FormEvent, useState } from 'react'
import styled from 'styled-components'

import { getErrorMessage, useReportPhotoMutation } from '../../api/api'
import type { KeyType, ReportReason } from '../../api/types'
import {
  Card,
  ErrorMessage,
  Field,
  Label,
  SecondaryButton,
  Select,
  SuccessMessage,
  TextArea,
  TextButton
} from '../../components/ui'

const REASONS: Array<{ label: string; value: ReportReason }> = [
  { label: 'It is not this vessel', value: 'WRONG_VESSEL' },
  { label: 'It is my photo / not openly licensed', value: 'COPYRIGHT' },
  { label: 'It shows people or private details', value: 'PRIVACY' },
  { label: 'Other', value: 'OTHER' }
]

export function ReportForm({
  n,
  onClose,
  vesselKey
}: {
  n: number
  onClose: () => void
  vesselKey: { key: string; keyType: KeyType }
}) {
  const [reason, setReason] = useState<ReportReason>('WRONG_VESSEL')
  const [message, setMessage] = useState('')
  const [reportPhoto, { error, isLoading, isSuccess }] = useReportPhotoMutation()

  if (isSuccess) {
    return <SuccessMessage>Thanks, a reviewer will look at photo {n}.</SuccessMessage>
  }

  const submit = (event: FormEvent) => {
    event.preventDefault()
    reportPhoto({ ...vesselKey, message, n, reason })
  }

  return (
    <Form as="form" onSubmit={submit}>
      <h2>Report photo {n}</h2>
      <Field>
        <Label htmlFor="report-reason">Reason</Label>
        <Select id="report-reason" onChange={event => setReason(event.target.value as ReportReason)} value={reason}>
          {REASONS.map(item => (
            <option key={item.value} value={item.value}>
              {item.label}
            </option>
          ))}
        </Select>
      </Field>
      <Field>
        <Label htmlFor="report-message">Details (optional)</Label>
        <TextArea
          id="report-message"
          maxLength={1000}
          onChange={event => setMessage(event.target.value)}
          value={message}
        />
      </Field>
      {error && <ErrorMessage>{getErrorMessage(error)}</ErrorMessage>}
      <div>
        <SecondaryButton disabled={isLoading} type="submit">
          Send report
        </SecondaryButton>
        <TextButton onClick={onClose} type="button">
          Cancel
        </TextButton>
      </div>
    </Form>
  )
}

const Form = styled(Card)`
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 520px;

  h2 {
    margin: 0;
    font-size: 16px;
  }
`
