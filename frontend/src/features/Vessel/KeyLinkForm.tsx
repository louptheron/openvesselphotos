import { type FormEvent, useState } from 'react'
import styled from 'styled-components'

import { getErrorMessage, useSuggestKeyLinkMutation } from '../../api/api'
import type { KeyType, Vessel } from '../../api/types'
import { ErrorMessage, Field, Input, Label, SecondaryButton, SuccessMessage, TextButton } from '../../components/ui'
import { useTranslation } from '../../i18n'
import { isValidKey, normalizeKey } from '../../utils/vesselKey'

/** Suggests the key (CFR or MMSI) the vessel is missing. */
export function KeyLinkForm({ vessel, vesselKey }: { vessel: Vessel; vesselKey: { key: string; keyType: KeyType } }) {
  const { t } = useTranslation()
  const missingKeyType: KeyType = vessel.cfr ? 'mmsi' : 'cfr'
  const [isOpen, setIsOpen] = useState(false)
  const [missingKey, setMissingKey] = useState('')
  const [contributor, setContributor] = useState('')
  const [suggestKeyLink, { error, isLoading, isSuccess }] = useSuggestKeyLinkMutation()
  const label = missingKeyType.toUpperCase()

  if (isSuccess) {
    return <SuccessMessage>{t.keyLink.thanks(label)}</SuccessMessage>
  }
  if (!isOpen) {
    return (
      <OpenButton onClick={() => setIsOpen(true)} type="button">
        {t.keyLink.open(label)}
      </OpenButton>
    )
  }

  const isValid = isValidKey(missingKeyType, missingKey)
  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (isValid) {
      suggestKeyLink({ ...vesselKey, contributor, missingKey: normalizeKey(missingKey), missingKeyType })
    }
  }

  return (
    <Form onSubmit={submit}>
      <Field>
        <Label htmlFor="missing-key">{label}</Label>
        <Input
          id="missing-key"
          onChange={event => setMissingKey(event.target.value)}
          placeholder={missingKeyType === 'cfr' ? 'FRA000123456' : '227000101'}
          value={missingKey}
        />
      </Field>
      <Field>
        <Label htmlFor="link-contributor">{t.keyLink.yourName}</Label>
        <Input id="link-contributor" onChange={event => setContributor(event.target.value)} value={contributor} />
      </Field>
      {error && <ErrorMessage>{getErrorMessage(error, t)}</ErrorMessage>}
      <div>
        <SecondaryButton disabled={!isValid || isLoading} type="submit">
          {t.keyLink.submit(label)}
        </SecondaryButton>
        <TextButton onClick={() => setIsOpen(false)} type="button">
          {t.common.cancel}
        </TextButton>
      </div>
    </Form>
  )
}

const OpenButton = styled(TextButton)`
  align-self: flex-start;
  padding: 0;
  font-size: 14px;
`

const Form = styled.form`
  display: flex;
  flex-direction: column;
  gap: 10px;
`
