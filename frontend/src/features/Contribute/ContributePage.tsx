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
  PrimaryButton,
  Title
} from '../../components/ui'
import { usePageMeta } from '../../hooks/usePageMeta'
import { useTranslation } from '../../i18n'
import { ChartCard, ChartSection } from '../../components/Chart'
import { CHART_COLORS, COLORS, FONTS } from '../../theme'
import { getVesselPath, isValidKey, normalizeKey } from '../../utils/vesselKey'

const KEY_TYPES: KeyType[] = ['cfr', 'mmsi']

const KEY_PLACEHOLDERS: Record<KeyType, string> = { cfr: 'FRA000123456', mmsi: '227000101' }

const LICENCES = [
  { label: 'CC BY 4.0', value: 'CC-BY-4.0' },
  { label: 'CC BY-SA 4.0', value: 'CC-BY-SA-4.0' },
  { label: 'CC0', value: 'CC0-1.0' }
]

const MAX_FILE_SIZE = 15 * 1024 * 1024

export function ContributePage() {
  const [searchParams] = useSearchParams()
  // A vessel can have both keys: one is enough, both are welcome
  const [keys, setKeys] = useState<Record<KeyType, string>>({
    cfr: searchParams.get('cfr') ?? '',
    mmsi: searchParams.get('mmsi') ?? ''
  })
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

  const { t } = useTranslation()
  usePageMeta(t.contribute.pageTitle)
  const isFilled = (type: KeyType) => keys[type].trim().length > 0
  const isValid = (type: KeyType) => isValidKey(type, keys[type])
  const isKeyValid = (isValid('cfr') || isValid('mmsi')) && KEY_TYPES.every(type => !isFilled(type) || isValid(type))
  // Sent under the CFR when there is one, with the MMSI as the other key
  const keyType: KeyType = isValid('cfr') ? 'cfr' : 'mmsi'
  const normalizedKey = normalizeKey(keys[keyType])
  const otherKeyType: KeyType = keyType === 'cfr' ? 'mmsi' : 'cfr'
  const otherKey = isValid(otherKeyType) ? normalizeKey(keys[otherKeyType]) : undefined
  const cfrLookup = useGetVesselQuery({ key: normalizeKey(keys.cfr), keyType: 'cfr' }, { skip: !isValid('cfr') })
  const mmsiLookup = useGetVesselQuery({ key: normalizeKey(keys.mmsi), keyType: 'mmsi' }, { skip: !isValid('mmsi') })
  const isLookingUp = cfrLookup.isFetching || mmsiLookup.isFetching
  const cfrVessel = isValid('cfr') ? cfrLookup.currentData : undefined
  const mmsiVessel = isValid('mmsi') ? mmsiLookup.currentData : undefined
  const vessel = cfrVessel ?? mmsiVessel
  // Both keys must be those of the same vessel
  const hasKeyConflict =
    (!!cfrVessel && !!mmsiVessel && cfrVessel.cfr !== mmsiVessel.cfr) ||
    (!!vessel && KEY_TYPES.some(type => isValid(type) && !!vessel[type] && vessel[type] !== normalizeKey(keys[type])))
  const previewUrl = useMemo(() => (file ? URL.createObjectURL(file) : undefined), [file])
  useEffect(
    () => () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl)
      }
    },
    [previewUrl]
  )

  const fileError = file && file.size > MAX_FILE_SIZE ? t.contribute.fileTooLarge : undefined
  const licenceLabel = LICENCES.find(item => item.value === licence)?.label
  const canSubmit =
    isKeyValid && !hasKeyConflict && !!file && !fileError && credit.trim().length > 0 && hasRights && !isLoading

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
    if (otherKey) {
      form.append(otherKeyType, otherKey)
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
        <Title>{t.contribute.thanksTitle}</Title>
        <Lead>{t.contribute.thanksText(vessel?.name ?? `${keyType.toUpperCase()} ${normalizedKey}`)}</Lead>
        <Buttons>
          <PrimaryButton onClick={startOver} type="button">
            {t.contribute.sendAnother}
          </PrimaryButton>
          {vessel && (
            <Link to={getVesselPath(vessel)}>{t.contribute.backTo(vessel.name ?? t.contribute.theVessel)}</Link>
          )}
        </Buttons>
      </Intro>
    )
  }

  return (
    <>
      <ChartSection>
        <ChartIntro>
          <IntroCard>
            <Title>{t.contribute.title}</Title>
            <Lead>{t.contribute.lead}</Lead>
          </IntroCard>
        </ChartIntro>
      </ChartSection>

      <Columns>
        <Form onSubmit={submit}>
          <Step>
            <legend>{t.contribute.step1}</legend>
            <Muted>{t.contribute.keysHint}</Muted>
            <KeyFields>
              {KEY_TYPES.map(type => (
                <Field key={type}>
                  <Label htmlFor={`key-${type}`}>
                    {type === 'cfr' ? t.contribute.cfrLabel : t.contribute.mmsiLabel}
                  </Label>
                  <KeyInput
                    aria-describedby={`key-${type}-hint`}
                    aria-invalid={isFilled(type) && !isValid(type)}
                    id={`key-${type}`}
                    onChange={event => setKeys({ ...keys, [type]: event.target.value })}
                    placeholder={KEY_PLACEHOLDERS[type]}
                    value={keys[type]}
                  />
                  <Muted id={`key-${type}-hint`}>{type === 'cfr' ? t.contribute.cfrHint : t.contribute.mmsiHint}</Muted>
                </Field>
              ))}
            </KeyFields>
            {isKeyValid && !isLookingUp && hasKeyConflict && <ErrorMessage>{t.contribute.keysConflict}</ErrorMessage>}
            {isKeyValid && !isLookingUp && !hasKeyConflict && vessel && (
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
                  {t.contribute.matches}{' '}
                  <Link to={getVesselPath(vessel)}>{vessel.name ?? t.contribute.anUnnamedVessel}</Link> ·{' '}
                  <Mono>
                    CFR {vessel.cfr ?? '—'} · MMSI {vessel.mmsi ?? '—'}
                  </Mono>{' '}
                  · {t.common.photos(vessel.photos.length)}
                </span>
              </Match>
            )}
            {isKeyValid && !isLookingUp && !vessel && (
              <>
                <Muted>{t.contribute.noVesselYet}</Muted>
                <Field>
                  <Label htmlFor="vessel-name">{t.contribute.vesselName}</Label>
                  <Input
                    id="vessel-name"
                    maxLength={100}
                    onChange={event => setVesselName(event.target.value)}
                    placeholder={t.contribute.vesselNamePlaceholder}
                    value={vesselName}
                  />
                </Field>
              </>
            )}
          </Step>

          <Step>
            <legend>{t.contribute.step2}</legend>
            <input
              accept="image/jpeg,image/png"
              className="visually-hidden"
              id="file"
              onChange={pickFile}
              type="file"
            />
            <DropZone htmlFor="file">
              {previewUrl ? (
                <img alt={t.contribute.yourUpload} src={previewUrl} />
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
              <strong>{file ? file.name : t.contribute.choosePhoto}</strong>
              <Muted>{t.contribute.fileHint}</Muted>
            </DropZone>
            {fileError && <ErrorMessage>{fileError}</ErrorMessage>}
            <TwoColumns>
              <Field>
                <Label htmlFor="taken">{t.contribute.dateTaken}</Label>
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
                  {t.contribute.place} <Optional>{t.common.optional}</Optional>
                </Label>
                <Input
                  id="place"
                  maxLength={200}
                  onChange={event => setPlace(event.target.value)}
                  placeholder={t.contribute.placePlaceholder}
                  value={place}
                />
              </Field>
            </TwoColumns>
          </Step>

          <Step>
            <legend>{t.contribute.step3}</legend>
            <Field>
              <Label htmlFor="credit">{t.contribute.creditAs}</Label>
              <Input
                id="credit"
                maxLength={100}
                onChange={event => setCredit(event.target.value)}
                placeholder={t.contribute.creditPlaceholder}
                value={credit}
              />
            </Field>
            <Licences aria-label={t.contribute.licence} role="radiogroup">
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
                  <span>{t.contribute.licences[item.value]}</span>
                </LicenceOption>
              ))}
            </Licences>
            <Checkbox>
              <input checked={hasRights} onChange={event => setHasRights(event.target.checked)} type="checkbox" />
              <span>{t.contribute.rights(licenceLabel ?? '')}</span>
            </Checkbox>
          </Step>

          <Field>
            <Label htmlFor="email">
              {t.contribute.email} <Optional>{t.contribute.emailHint}</Optional>
            </Label>
            <Input
              autoComplete="email"
              id="email"
              onChange={event => setEmail(event.target.value)}
              type="email"
              value={email}
            />
          </Field>
          {error && <ErrorMessage>{getErrorMessage(error, t)}</ErrorMessage>}
          <Buttons>
            <PrimaryButton disabled={!canSubmit} type="submit">
              {isLoading ? t.common.sending : t.contribute.submit}
            </PrimaryButton>
            <Muted>{t.contribute.noAccount}</Muted>
          </Buttons>
        </Form>

        <Aside>
          <NextSteps>
            <h2>{t.contribute.nextTitle}</h2>
            <ol>
              <li>
                <strong>{t.contribute.nextReceived}</strong> {t.contribute.nextReceivedText}
              </li>
              <li>
                <strong>{t.contribute.nextReviewed}</strong> {t.contribute.nextReviewedText}
              </li>
              <li>
                <strong>{t.contribute.nextPublished}</strong> {t.contribute.nextPublishedText}
              </li>
            </ol>
          </NextSteps>
          <Tips>
            <h2>{t.contribute.tipsTitle}</h2>
            <ul>
              {t.contribute.tips.map(tip => (
                <li key={tip}>{tip}</li>
              ))}
            </ul>
            <Muted>{t.contribute.exif}</Muted>
          </Tips>
          <Muted>
            {t.contribute.scripts}{' '}
            <Link to="/docs">
              POST /v1/{'{cfr|mmsi}'}/{'{key}'}/photos
            </Link>{' '}
            {t.contribute.scriptsEnd}
          </Muted>
        </Aside>
      </Columns>
    </>
  )
}

const ChartIntro = styled(Container).attrs({ as: 'section' })`
  padding-top: 36px;
  padding-bottom: 40px;

  @media (max-width: 600px) {
    padding-top: 20px;
    padding-bottom: 20px;
  }
`

const IntroCard = styled(ChartCard)`
  max-width: 700px;
  padding: 26px 28px;
  gap: 10px;
  border-radius: 16px;

  p {
    color: ${CHART_COLORS.text};
  }
`

const Intro = styled(Container).attrs({ as: 'section' })`
  padding-top: 32px;
  padding-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
`

const Columns = styled(Container).attrs({ as: 'section' })`
  padding-top: 36px;
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

const KeyFields = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
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
