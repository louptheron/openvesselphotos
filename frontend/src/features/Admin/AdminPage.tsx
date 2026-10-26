import { type FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import {
  getErrorMessage,
  getErrorStatus,
  useGetReviewQueueQuery,
  useLoginMutation,
  useLogoutMutation
} from '../../api/api'
import {
  Card,
  Container,
  ErrorMessage,
  Field,
  Input,
  Label,
  PrimaryButton,
  TextButton,
  Title
} from '../../components/ui'
import { usePageMeta } from '../../hooks/usePageMeta'
import { useTranslation } from '../../i18n'
import { ReviewQueuePanel } from './ReviewQueuePanel'
import { PhotoImport } from './PhotoImport'
import { PhotoNotes } from './PhotoNotes'
import { VesselImport } from './VesselImport'
import { WikidataImport } from './WikidataImport'

export function AdminPage() {
  const { data: queue, error, isLoading } = useGetReviewQueueQuery()
  const [logout] = useLogoutMutation()
  const { t } = useTranslation()
  usePageMeta(t.admin.pageTitle)

  if (isLoading) {
    return <Container>{t.common.loading}</Container>
  }
  if (getErrorStatus(error) === 401) {
    return <LoginForm />
  }
  if (!queue) {
    return (
      <Container>
        <ErrorMessage>{getErrorMessage(error, t)}</ErrorMessage>
      </Container>
    )
  }

  return (
    <Page>
      <Header>
        <Title as="h1">{t.admin.queueTitle}</Title>
        <HeaderLinks>
          <Link to="/admin/stats">{t.admin.statistics}</Link>
          <TextButton onClick={() => logout()} type="button">
            {t.admin.signOut}
          </TextButton>
        </HeaderLinks>
      </Header>
      <ReviewQueuePanel queue={queue} />
      <VesselImport />
      <WikidataImport />
      <PhotoImport />
      <PhotoNotes />
    </Page>
  )
}

export function LoginForm() {
  const [password, setPassword] = useState('')
  const [login, { error, isLoading }] = useLoginMutation()
  const { t } = useTranslation()

  const submit = (event: FormEvent) => {
    event.preventDefault()
    login(password)
  }

  return (
    <Page>
      <LoginCard as="form" onSubmit={submit}>
        <Title as="h1">{t.admin.loginTitle}</Title>
        <p>{t.admin.loginText}</p>
        <Field>
          <Label htmlFor="password">{t.admin.password}</Label>
          <Input
            autoComplete="current-password"
            id="password"
            onChange={event => setPassword(event.target.value)}
            type="password"
            value={password}
          />
        </Field>
        {error && <ErrorMessage>{getErrorMessage(error, t)}</ErrorMessage>}
        <PrimaryButton disabled={!password || isLoading} type="submit">
          {t.admin.signIn}
        </PrimaryButton>
      </LoginCard>
    </Page>
  )
}

const Page = styled(Container)`
  padding-top: 24px;
  padding-bottom: 48px;
`

const Header = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;

  h1 {
    font-size: 36px;
  }
`

const HeaderLinks = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
`

const LoginCard = styled(Card)`
  max-width: 420px;
  margin: 24px auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;

  h1 {
    font-size: 32px;
  }

  p {
    margin: 0;
  }
`
