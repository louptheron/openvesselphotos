import { type ChangeEvent, type FormEvent, useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import { getErrorMessage, useGetVesselQuery, useSubmitPhotoMutation } from '../../api/api'
import type { KeyType } from '../../api/types'
import {
  Card,
  Container,
  ErrorMessage,
  Field,
  Input,
  Label,
  Lead,
  Muted,
  Pill,
  PrimaryButton,
  Title
} from '../../components/ui'
import { usePageMeta } from '../../hooks/usePageMeta'
import { COLORS, FONTS } from '../../theme'
import { getVesselPath, isValidKey, normalizeKey } from '../../utils/vesselKey'

const KEY_TYPES: Record<KeyType, { hint: string; label: string; placeholder: string }> = {
  cfr: {
    hint: '3-letter country code + 9 characters, from the EU fleet register.',
    label: 'CFR number',
    placeholder: 'FRA000123456'
  },
  mmsi: { hint: '9 digits, the radio identity shown on AIS.', label: 'MMSI number', placeholder: '227000101' }
}

const LICENCES = [
  { description: 'Anyone can reuse it with your credit.', label: 'CC BY 4.0', value: 'CC-BY-4.0' },
  { description: 'Reuse with credit; changes shared alike.', label: 'CC BY-SA 4.0', value: 'CC-BY-SA-4.0' },
  { description: 'Public domain. No credit required.', label: 'CC0', value: 'CC0-1.0' }
]

const MAX_FILE_SIZE = 15 * 1024 * 1024

export function ContributePage() {
  const [searchParams] = useSearchParams()
  const initialKeyType: KeyType = searchParams.get('mmsi') && !searchParams.get('cfr') ? 'mmsi' : 'cfr'
  const [keyType, setKeyType] = useState<KeyType>(initialKeyType)
  const [key, setKey] = useState(searchParams.get(initialKeyType) ?? '')
  const [vesselName, setVesselName] = useState('')
  const [file, setFile] = useState<File>()
  const [takenOn, setTakenOn] = useState('')
  const [place, setPlace] = useState('')
  const [credit, setCredit] = useState('')
  const [licence, setLicence] = useState('CC-BY-4.0')
  const [hasRights, setHasRights] = useState(false)
  const [email, setEmail] = useState('')
  const [submitPhoto, { error, isLoading, isSuccess, reset }] = useSubmitPhotoMutation()
  const [today] = useState(() => new Date().toISOString().slice(0, 10))

  usePageMeta('Contribute a vessel photo')
  const normalizedKey = normalizeKey(key)
  const isKeyValid = isValidKey(keyType, key)
  const { data: vessel, isFetching: isLookingUp } = useGetVesselQuery(
    { key: normalizedKey, keyType },
    { skip: !isKeyValid }
  )
  const previewUrl = useMemo(() => (file ? URL.createObjectURL(file) : undefined), [file])
  useEffect(
    () => () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl)
      }
    },
    [previewUrl]
  )

  const fileError = file && file.size > MAX_FILE_SIZE ? 'This file is larger than 15 MB.' : undefined
  const licenceLabel = LICENCES.find(item => item.value === licence)?.label
  const canSubmit = isKeyValid && !!file && !fileError && credit.trim().length > 0 && hasRights && !isLoading

  const pickFile = (event: ChangeEvent<HTMLInputElement>) => setFile(event.target.files?.[0])

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (!canSubmit || !file) {
      return
    }
    const form = new FormData()
    form.append('file', file)
    form.append('credit', credit.trim())
    form.append('licence', licence)
    if (takenOn) {
      form.append('takenOn', takenOn)
    }
    if (place.trim()) {
      form.append('place', place.trim())
    }
    if (email.trim()) {
      form.append('email', email.trim())
    }
    if (!vessel && vesselName.trim()) {
      form.append('vesselName', vesselName.trim())
    }
    submitPhoto({ form, key: normalizedKey, keyType })
  }

  const startOver = () => {
    reset()
    setFile(undefined)
    setHasRights(false)
  }

  if (isSuccess) {
    return (
      <Intro>
        <Title>Thank you!</Title>
        <Lead>
          Your photo of {vessel?.name ?? `${keyType.toUpperCase()} ${normalizedKey}`} is in the review queue. It goes
          live once a volunteer has checked it.
        </Lead>
        <Buttons>
          <PrimaryButton onClick={startOver} type="button">
            Send another photo
          </PrimaryButton>
          {vessel && <Link to={getVesselPath(vessel)}>Back to {vessel.name ?? 'the vessel'}</Link>}
        </Buttons>
      </Intro>
    )
  }

  return (
    <>
      <Intro>
        <Title>Contribute a photo</Title>
        <Lead>
          Your own picture, an open licence, and the vessel's CFR or MMSI. It goes live once a volunteer has reviewed
          it.
        </Lead>
      </Intro>

      <Columns>
        <Form onSubmit={submit}>
          <Step>
            <legend>1 · Which vessel?</legend>
            <KeyTypes>
              <legend className="visually-hidden">Key type</legend>
              {(['cfr', 'mmsi'] as const).map(type => (
                <Pill
                  key={type}
                  $active={keyType === type}
                  aria-pressed={keyType === type}
                  onClick={() => {
                    setKeyType(type)
                    setKey('')
                  }}
                  type="button"
                >
                  {type.toUpperCase()}
                </Pill>
              ))}
            </KeyTypes>
            <Field>
              <Label htmlFor="key">{KEY_TYPES[keyType].label}</Label>
              <KeyInput
                aria-describedby="key-hint"
                aria-invalid={key.length > 0 && !isKeyValid}
                id="key"
                onChange={event => setKey(event.target.value)}
                placeholder={KEY_TYPES[keyType].placeholder}
                value={key}
              />
              <Muted id="key-hint">{KEY_TYPES[keyType].hint}</Muted>
            </Field>
            {isKeyValid && !isLookingUp && vessel && (
              <Match>
                <svg
                  aria-hidden="true"
                  fill="none"
                  height="20"
                  stroke={COLORS.link}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  viewBox="0 0 24 24"
                  width="20"
                >
                  <path d="M20 6L9 17l-5-5" />
                </svg>
                <span>
                  Matches <Link to={getVesselPath(vessel)}>{vessel.name ?? 'an unnamed vessel'}</Link> ·{' '}
                  <Mono>
                    CFR {vessel.cfr ?? '—'} · MMSI {vessel.mmsi ?? '—'}
                  </Mono>{' '}
                  · {vessel.photos.length} photo{vessel.photos.length === 1 ? '' : 's'}
                </span>
              </Match>
            )}
            {isKeyValid && !isLookingUp && !vessel && (
              <>
                <Muted>No vessel with this key yet — it will be created when your photo is approved.</Muted>
                <Field>
                  <Label htmlFor="vessel-name">Vessel name</Label>
                  <Input
                    id="vessel-name"
                    maxLength={100}
                    onChange={event => setVesselName(event.target.value)}
                    placeholder="e.g. Ker Avel"
                    value={vesselName}
                  />
                </Field>
              </>
            )}
          </Step>

          <Step>
            <legend>2 · The photo</legend>
            <input
              accept="image/jpeg,image/png"
              className="visually-hidden"
              id="file"
              onChange={pickFile}
              type="file"
            />
            <DropZone htmlFor="file">
              {previewUrl ? (
                <img alt="Your upload" src={previewUrl} />
              ) : (
                <svg
                  aria-hidden="true"
                  fill="none"
                  height="32"
                  stroke={COLORS.link}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="1.8"
                  viewBox="0 0 24 24"
                  width="32"
                >
                  <path d="M12 16V4" />
                  <path d="M7 9l5-5 5 5" />
                  <path d="M4 16v3a1 1 0 001 1h14a1 1 0 001-1v-3" />
                </svg>
              )}
              <strong>{file ? file.name : 'Choose a photo'}</strong>
              <Muted>JPEG or PNG · the vessel clearly visible · 15 MB max</Muted>
            </DropZone>
            {fileError && <ErrorMessage>{fileError}</ErrorMessage>}
            <TwoColumns>
              <Field>
                <Label htmlFor="taken">Date taken</Label>
                <Input
                  id="taken"
                  max={today}
                  onChange={event => setTakenOn(event.target.value)}
                  type="date"
                  value={takenOn}
                />
              </Field>
              <Field>
                <Label htmlFor="place">
                  Place <Optional>(optional)</Optional>
                </Label>
                <Input
                  id="place"
                  maxLength={200}
                  onChange={event => setPlace(event.target.value)}
                  placeholder="e.g. Concarneau harbour"
                  value={place}
                />
              </Field>
            </TwoColumns>
          </Step>

          <Step>
            <legend>3 · Credit and licence</legend>
            <Field>
              <Label htmlFor="credit">Credit as</Label>
              <Input
                id="credit"
                maxLength={100}
                onChange={event => setCredit(event.target.value)}
                placeholder="Your name or handle"
                value={credit}
              />
            </Field>
            <Licences aria-label="Licence" role="radiogroup">
              {LICENCES.map(item => (
                <LicenceOption key={item.value} $active={licence === item.value}>
                  <input
                    checked={licence === item.value}
                    name="licence"
                    onChange={() => setLicence(item.value)}
                    type="radio"
                    value={item.value}
                  />
                  <strong>{item.label}</strong>
                  <span>{item.description}</span>
                </LicenceOption>
              ))}
            </Licences>
            <Checkbox>
              <input checked={hasRights} onChange={event => setHasRights(event.target.checked)} type="checkbox" />
              <span>I took this photo, or I hold the right to publish it under {licenceLabel}.</span>
            </Checkbox>
          </Step>

          <Field>
            <Label htmlFor="email">
              Email <Optional>(optional, only seen by reviewers if they have a question)</Optional>
            </Label>
            <Input
              autoComplete="email"
              id="email"
              onChange={event => setEmail(event.target.value)}
              type="email"
              value={email}
            />
          </Field>
          {error && <ErrorMessage>{getErrorMessage(error)}</ErrorMessage>}
          <Buttons>
            <PrimaryButton disabled={!canSubmit} type="submit">
              {isLoading ? 'Sending…' : 'Send for review'}
            </PrimaryButton>
            <Muted>No account needed.</Muted>
          </Buttons>
        </Form>

        <Aside>
          <NextSteps>
            <h2>What happens next</h2>
            <ol>
              <li>
                <strong>Received.</strong> Your photo waits in the review queue.
              </li>
              <li>
                <strong>Reviewed.</strong> A volunteer checks the vessel matches its key and the licence is clear.
              </li>
              <li>
                <strong>Published.</strong> Live on the site and the API under both CFR and MMSI.
              </li>
            </ol>
          </NextSteps>
          <Tips>
            <h2>A good vessel photo</h2>
            <ul>
              <li>The whole hull in frame, side-on if you can</li>
              <li>Name or registration readable</li>
              <li>No people's faces up close</li>
              <li>Only photos you took yourself</li>
            </ul>
            <Muted>Location data (EXIF/GPS) is removed from every photo.</Muted>
          </Tips>
          <Muted>
            Prefer scripts?{' '}
            <Link to="/docs">
              POST /v1/{'{cfr|mmsi}'}/{'{key}'}/photos
            </Link>{' '}
            does the same.
          </Muted>
        </Aside>
      </Columns>
    </>
  )
}

const Intro = styled(Container).attrs({ as: 'section' })`
  padding-top: 32px;
  padding-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
`

const Columns = styled(Container).attrs({ as: 'section' })`
  padding-top: 16px;
  padding-bottom: 56px;
  display: flex;
  flex-wrap: wrap;
  gap: 32px;
  align-items: flex-start;
`

const Form = styled.form`
  flex: 999 1 560px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
`

const Step = styled.fieldset`
  margin: 0;
  padding: 20px;
  border: 1px solid ${COLORS.cardBorder};
  border-radius: 14px;
  background: ${COLORS.white};
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;

  legend {
    padding: 0 6px;
    font-family: ${FONTS.display};
    font-size: 20px;
    font-weight: 700;
  }
`

const KeyTypes = styled.fieldset`
  margin: 0;
  padding: 0;
  border: none;
  display: flex;
  gap: 6px;
`

const KeyInput = styled(Input)`
  min-height: 52px;
  border: 2px solid ${COLORS.ink};
  font-family: ${FONTS.mono};
  font-size: 16px;
`

const Mono = styled.span`
  font-family: ${FONTS.mono};
  font-size: 13px;
`

const Match = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  background: ${COLORS.info};
  font-size: 14px;

  svg {
    flex: none;
  }
`

const DropZone = styled.label`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 180px;
  border: 2px dashed ${COLORS.inputBorder};
  border-radius: 12px;
  background: #faf8f4;
  cursor: pointer;
  text-align: center;
  padding: 20px;

  img {
    max-width: 100%;
    max-height: 280px;
    border-radius: 8px;
  }

  input:focus-visible + & {
    outline: 3px solid ${COLORS.accent};
  }
`

const TwoColumns = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
`

const Optional = styled.span`
  color: ${COLORS.muted};
  font-weight: 400;
`

const Licences = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
`

const LicenceOption = styled.label<{ $active: boolean }>`
  position: relative;
  padding: 14px;
  border-radius: 12px;
  border: 2px solid ${p => (p.$active ? COLORS.accent : COLORS.cardBorder)};
  background: ${p => (p.$active ? COLORS.accentSoft : COLORS.white)};
  display: flex;
  flex-direction: column;
  gap: 6px;
  cursor: pointer;

  input {
    position: absolute;
    opacity: 0;
  }

  &:has(input:focus-visible) {
    outline: 3px solid ${COLORS.accent};
    outline-offset: 2px;
  }

  span {
    font-size: 13px;
    color: ${COLORS.text};
    line-height: 1.4;
  }
`

const Checkbox = styled.label`
  display: flex;
  gap: 10px;
  align-items: flex-start;
  font-size: 14px;
  line-height: 1.45;
  cursor: pointer;

  input {
    width: 20px;
    height: 20px;
    margin: 1px 0 0;
    accent-color: ${COLORS.ink};
    flex: none;
  }
`

const Buttons = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
`

const Aside = styled.aside`
  flex: 1 1 320px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
`

const NextSteps = styled.div`
  background: ${COLORS.ink};
  color: ${COLORS.inkSoft};
  border-radius: 14px;
  padding: 20px;

  h2 {
    margin: 0 0 14px;
    font-family: ${FONTS.display};
    font-size: 22px;
    color: ${COLORS.white};
  }

  ol {
    margin: 0;
    padding-left: 20px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    font-size: 14px;
    line-height: 1.45;
  }

  li::marker {
    color: ${COLORS.accent};
    font-weight: 600;
  }

  strong {
    color: ${COLORS.white};
    font-weight: 600;
  }
`

const Tips = styled(Card)`
  padding: 20px;

  h2 {
    margin: 0 0 10px;
    font-size: 16px;
  }

  ul {
    margin: 0 0 10px;
    padding-left: 18px;
    font-size: 14px;
    line-height: 1.6;
    color: ${COLORS.text};
  }
`
