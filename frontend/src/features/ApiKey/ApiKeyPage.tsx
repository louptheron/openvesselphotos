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
import { COLORS, FONTS } from '../../theme'

export function ApiKeyPage() {
  usePageMeta('Request an API key')
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
      <Title>Request an API key</Title>
      <Lead>
        Reading the API is free and open. An API key is only needed to send data with a script: contribute photos,
        suggest keys or report photos through <code>POST</code> requests.
      </Lead>

      <Columns>
        {isSuccess ? (
          <SuccessMessage>
            <strong>Request received.</strong> A reviewer will send your API key to {email.trim()} once your request is
            approved.
          </SuccessMessage>
        ) : (
          <Form as="form" onSubmit={submit}>
            <Field>
              <Label htmlFor="api-name">Your name</Label>
              <Input
                autoComplete="name"
                id="api-name"
                maxLength={100}
                onChange={event => setName(event.target.value)}
                value={name}
              />
            </Field>
            <Field>
              <Label htmlFor="api-email">Email</Label>
              <Input
                autoComplete="email"
                id="api-email"
                onChange={event => setEmail(event.target.value)}
                type="email"
                value={email}
              />
              <Muted>The key is sent to this address.</Muted>
            </Field>
            <Field>
              <Label htmlFor="api-organisation">
                Organisation or project <Optional>(optional)</Optional>
              </Label>
              <Input
                id="api-organisation"
                maxLength={200}
                onChange={event => setOrganisation(event.target.value)}
                value={organisation}
              />
            </Field>
            <Field>
              <Label htmlFor="api-use">What will you use it for?</Label>
              <TextArea
                id="api-use"
                maxLength={1000}
                onChange={event => setIntendedUse(event.target.value)}
                placeholder="e.g. Import the photos taken by our harbour team, about 50 a month."
                value={intendedUse}
              />
            </Field>
            <Checkbox>
              <input checked={acceptsTerms} onChange={event => setAcceptsTerms(event.target.checked)} type="checkbox" />
              <span>
                I will only send photos their authors agreed to publish under an open licence, with their credit.
              </span>
            </Checkbox>
            {error && <ErrorMessage>{getErrorMessage(error)}</ErrorMessage>}
            <div>
              <PrimaryButton disabled={!canSubmit} type="submit">
                {isLoading ? 'Sending…' : 'Request a key'}
              </PrimaryButton>
            </div>
          </Form>
        )}

        <Aside>
          <h2>How to use it</h2>
          <p>
            Send the key in the <code>x-api-key</code> header of your POST requests:
          </p>
          <pre>{`curl -X POST \\
  -H "x-api-key: ovp_…" \\
  -F file=@boat.jpg \\
  -F credit="Jane Doe" \\
  -F licence=CC-BY-4.0 \\
  ${window.location.origin}/v1/cfr/FRA000123456/photos`}</pre>
          <p>
            Keep it secret: never put it in a web page or a mobile app. Everything sent is still reviewed before
            publication. See the <Link to="/docs">API docs</Link>.
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
