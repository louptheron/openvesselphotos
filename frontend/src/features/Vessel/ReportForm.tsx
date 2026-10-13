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
import { useTranslation } from '../../i18n'

const REASONS: ReportReason[] = ['WRONG_VESSEL', 'COPYRIGHT', 'PRIVACY', 'OTHER']

export function ReportForm({
  n,
  onClose,
  vesselKey
}: {
  n: number
  onClose: () => void
  vesselKey: { key: string; keyType: KeyType }
}) {
  const { t } = useTranslation()
  const [reason, setReason] = useState<ReportReason>('WRONG_VESSEL')
  const [message, setMessage] = useState('')
  const [reportPhoto, { error, isLoading, isSuccess }] = useReportPhotoMutation()

  if (isSuccess) {
    return <SuccessMessage>{t.report.thanks(n)}</SuccessMessage>
  }

  const submit = (event: FormEvent) => {
    event.preventDefault()
    reportPhoto({ ...vesselKey, message, n, reason })
  }

  return (
    <Form as="form" onSubmit={submit}>
      <h2>{t.report.title(n)}</h2>
      <Field>
        <Label htmlFor="report-reason">{t.report.reason}</Label>
        <Select id="report-reason" onChange={event => setReason(event.target.value as ReportReason)} value={reason}>
          {REASONS.map(item => (
            <option key={item} value={item}>
              {t.report.reasons[item]}
            </option>
          ))}
        </Select>
      </Field>
      <Field>
        <Label htmlFor="report-message">{t.report.details}</Label>
        <TextArea
          id="report-message"
          maxLength={1000}
          onChange={event => setMessage(event.target.value)}
          value={message}
        />
      </Field>
      {error && <ErrorMessage>{getErrorMessage(error, t)}</ErrorMessage>}
      <div>
        <SecondaryButton disabled={isLoading} type="submit">
          {t.report.submit}
        </SecondaryButton>
        <TextButton onClick={onClose} type="button">
          {t.common.cancel}
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
