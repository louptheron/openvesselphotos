import { type FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { getErrorMessage, useRequestApiKeyMutation } from '../../api/api'
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
  SuccessMessage,
  TextArea,
  Title
} from '../../components/ui'
import { usePageMeta } from '../../hooks/usePageMeta'
import { useTranslation } from '../../i18n'
import { COLORS, FONTS } from '../../theme'

export function ApiKeyPage() {
  const { t } = useTranslation()
  usePageMeta(t.apiKey.pageTitle)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [organisation, setOrganisation] = useState('')
  const [intendedUse, setIntendedUse] = useState('')
  const [acceptsTerms, setAcceptsTerms] = useState(false)
  const [requestApiKey, { error, isLoading, isSuccess }] = useRequestApiKeyMutation()

  const canSubmit =
    name.trim().length > 0 && email.includes('@') && intendedUse.trim().length >= 10 && acceptsTerms && !isLoading

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (canSubmit) {
      requestApiKey({
        email: email.trim(),
        intendedUse: intendedUse.trim(),
        name: name.trim(),
        organisation: organisation.trim()
      })
    }
  }

  return (
    <Page>
      <Title>{t.apiKey.title}</Title>
      <Lead>{t.apiKey.lead}</Lead>

      <Columns>
        {isSuccess ? (
          <SuccessMessage>
            <strong>{t.apiKey.received}</strong> {t.apiKey.receivedText(email.trim())}
          </SuccessMessage>
        ) : (
          <Form as="form" onSubmit={submit}>
            <Field>
              <Label htmlFor="api-name">{t.apiKey.name}</Label>
              <Input
                autoComplete="name"
                id="api-name"
                maxLength={100}
                onChange={event => setName(event.target.value)}
                value={name}
              />
            </Field>
            <Field>
              <Label htmlFor="api-email">{t.apiKey.email}</Label>
              <Input
                autoComplete="email"
                id="api-email"
                onChange={event => setEmail(event.target.value)}
                type="email"
                value={email}
              />
              <Muted>{t.apiKey.emailHint}</Muted>
            </Field>
            <Field>
              <Label htmlFor="api-organisation">
                {t.apiKey.organisation} <Optional>{t.common.optional}</Optional>
              </Label>
              <Input
                id="api-organisation"
                maxLength={200}
                onChange={event => setOrganisation(event.target.value)}
                value={organisation}
              />
            </Field>
            <Field>
              <Label htmlFor="api-use">{t.apiKey.use}</Label>
              <TextArea
                id="api-use"
                maxLength={1000}
                onChange={event => setIntendedUse(event.target.value)}
                placeholder={t.apiKey.usePlaceholder}
                value={intendedUse}
              />
            </Field>
            <Checkbox>
              <input checked={acceptsTerms} onChange={event => setAcceptsTerms(event.target.checked)} type="checkbox" />
              <span>{t.apiKey.terms}</span>
            </Checkbox>
            {error && <ErrorMessage>{getErrorMessage(error, t)}</ErrorMessage>}
            <div>
              <PrimaryButton disabled={!canSubmit} type="submit">
                {isLoading ? t.common.sending : t.apiKey.submit}
              </PrimaryButton>
            </div>
          </Form>
        )}

        <Aside>
          <h2>{t.apiKey.howTitle}</h2>
          <p>{t.apiKey.howHeader}</p>
          <pre>{`curl -X POST \\
  -H "x-api-key: ovp_…" \\
  -F file=@boat.jpg \\
  -F credit="Jane Doe" \\
  -F licence=CC-BY-4.0 \\
  ${window.location.origin}/v1/cfr/FRA000123456/photos`}</pre>
          <p>
            {t.apiKey.howSecret} <Link to="/docs">{t.apiKey.apiDocs}</Link>.
          </p>
        </Aside>
      </Columns>
    </Page>
  )
}

const Page = styled(Container)`
  padding-top: 32px;
  padding-bottom: 56px;
  display: flex;
  flex-direction: column;
  gap: 12px;

  code {
    font-family: ${FONTS.mono};
    font-size: 0.92em;
  }
`

const Columns = styled.div`
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 32px;
  align-items: flex-start;

  > :first-child {
    flex: 999 1 480px;
    min-width: 0;
  }
`

const Form = styled(Card)`
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
`

const Optional = styled.span`
  color: ${COLORS.muted};
  font-weight: 400;
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

const Aside = styled.aside`
  flex: 1 1 320px;
  min-width: 0;
  background: ${COLORS.ink};
  color: ${COLORS.inkSoft};
  border-radius: 14px;
  padding: 20px;
  font-size: 14px;
  line-height: 1.5;

  h2 {
    margin: 0 0 8px;
    font-family: ${FONTS.display};
    font-size: 22px;
    color: ${COLORS.white};
  }

  p {
    margin: 0 0 10px;
  }

  code {
    color: ${COLORS.white};
  }

  a {
    color: ${COLORS.white};
  }

  pre {
    margin: 0 0 10px;
    padding: 12px;
    border-radius: 8px;
    background: ${COLORS.codeBackground};
    color: ${COLORS.codeText};
    font-family: ${FONTS.mono};
    font-size: 12px;
    overflow-x: auto;
  }
`
