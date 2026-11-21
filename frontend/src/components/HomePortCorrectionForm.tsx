import { type FormEvent, useId, useMemo, useState } from 'react'
import styled from 'styled-components'

import { getErrorMessage, useGetPortsQuery, useSuggestHomePortMutation } from '../api/api'
import type { KeyType, Port } from '../api/types'
import type { Messages } from '../i18n/en'
import { ErrorMessage, Field, Input, Label, SecondaryButton, SuccessMessage, TextArea, TextButton } from './ui'

type Props = {
  /** The vessel's current home port, not offered */
  currentLocode?: string | null | undefined
  onCancel: () => void
  /** The messages of the site: translated on Open Vessel Photos, in French on the ports site */
  t: Messages
  vesselKey: { key: string; keyType: KeyType }
}

/**
 * Suggests the port a vessel is really based at, on the vessel page of Open Vessel Photos and the port pages of the
 * ports site: it becomes its home port once the admin has checked it.
 */
export function HomePortCorrectionForm({ currentLocode, onCancel, t, vesselKey }: Props) {
  const id = useId()
  const { data: ports = [] } = useGetPortsQuery()
  const [portInput, setPortInput] = useState('')
  const [comment, setComment] = useState('')
  const [contributor, setContributor] = useState('')
  const [hasTriedSubmit, setHasTriedSubmit] = useState(false)
  const [suggestHomePort, { error, isLoading, isSuccess }] = useSuggestHomePortMutation()

  const options = useMemo(
    () =>
      ports
        .filter(port => port.locode !== currentLocode)
        .map(port => ({ label: getOptionLabel(port), port }))
        .sort((a, b) => a.label.localeCompare(b.label, 'fr')),
    [currentLocode, ports]
  )
  const port = findPort(options, portInput)

  if (isSuccess && port) {
    return <SuccessMessage>{t.homePort.thanks(port.name)}</SuccessMessage>
  }

  const submit = (event: FormEvent) => {
    event.preventDefault()
    setHasTriedSubmit(true)
    if (port) {
      suggestHomePort({ ...vesselKey, comment, contributor, locode: port.locode })
    }
  }

  return (
    <Form onSubmit={submit}>
      <Field>
        <Label htmlFor={`${id}-port`}>{t.homePort.port}</Label>
        <Input
          autoComplete="off"
          // eslint-disable-next-line jsx-a11y/no-autofocus -- opened on purpose, by its button
          autoFocus
          id={`${id}-port`}
          list={`${id}-ports`}
          onChange={event => setPortInput(event.target.value)}
          placeholder={t.homePort.portPlaceholder}
          value={portInput}
        />
        <datalist id={`${id}-ports`}>
          {options.map(option => (
            <option key={option.port.locode} value={option.label}>
              {option.label}
            </option>
          ))}
        </datalist>
      </Field>
      <Field>
        <Label htmlFor={`${id}-comment`}>{t.homePort.comment}</Label>
        <TextArea
          id={`${id}-comment`}
          maxLength={500}
          onChange={event => setComment(event.target.value)}
          placeholder={t.homePort.commentPlaceholder}
          value={comment}
        />
      </Field>
      <Field>
        <Label htmlFor={`${id}-contributor`}>{t.homePort.yourName}</Label>
        <Input
          id={`${id}-contributor`}
          maxLength={100}
          onChange={event => setContributor(event.target.value)}
          value={contributor}
        />
      </Field>
      {hasTriedSubmit && !port && <ErrorMessage>{t.homePort.unknownPort}</ErrorMessage>}
      {error && <ErrorMessage>{getErrorMessage(error, t)}</ErrorMessage>}
      <Buttons>
        <SecondaryButton disabled={isLoading} type="submit">
          {t.homePort.submit}
        </SecondaryButton>
        <TextButton onClick={onCancel} type="button">
          {t.common.cancel}
        </TextButton>
      </Buttons>
    </Form>
  )
}

/** e.g. "Quiberon (FRQUI)": the LOCODE tells apart the ports of the same name */
function getOptionLabel(port: Port): string {
  return `${port.name} (${port.locode})`
}

/** The option picked, or else the only port of the typed name */
function findPort(options: Array<{ label: string; port: Port }>, input: string): Port | undefined {
  const typed = normalize(input)
  if (!typed) {
    return undefined
  }
  const picked = options.find(({ label }) => normalize(label) === typed)
  if (picked) {
    return picked.port
  }
  const sameName = options.filter(({ port }) => normalize(port.name) === typed)

  return sameName.length === 1 ? sameName[0]?.port : undefined
}

/** Without case, accents, or apostrophe and dash differences */
function normalize(text: string): string {
  return text
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[’'\-\s]+/g, ' ')
    .trim()
    .toLowerCase()
}

const Form = styled.form`
  display: flex;
  flex-direction: column;
  gap: 10px;
`

const Buttons = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
`
