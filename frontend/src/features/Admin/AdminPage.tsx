import { type FormEvent, useState } from 'react'
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
import { ReviewQueuePanel } from './ReviewQueuePanel'

export function AdminPage() {
  const { data: queue, error, isLoading } = useGetReviewQueueQuery()
  const [logout] = useLogoutMutation()

  if (isLoading) {
    return <Container>Loading…</Container>
  }
  if (getErrorStatus(error) === 401) {
    return <LoginForm />
  }
  if (!queue) {
    return (
      <Container>
        <ErrorMessage>{getErrorMessage(error)}</ErrorMessage>
      </Container>
    )
  }

  return (
    <Page>
      <Header>
        <Title as="h1">Review queue</Title>
        <TextButton onClick={() => logout()} type="button">
          Sign out
        </TextButton>
      </Header>
      <ReviewQueuePanel queue={queue} />
    </Page>
  )
}

function LoginForm() {
  const [password, setPassword] = useState('')
  const [login, { error, isLoading }] = useLoginMutation()

  const submit = (event: FormEvent) => {
    event.preventDefault()
    login(password)
  }

  return (
    <Page>
      <LoginCard as="form" onSubmit={submit}>
        <Title as="h1">Review</Title>
        <p>Sign in with the admin password to review contributions.</p>
        <Field>
          <Label htmlFor="password">Admin password</Label>
          <Input
            autoComplete="current-password"
            id="password"
            onChange={event => setPassword(event.target.value)}
            type="password"
            value={password}
          />
        </Field>
        {error && <ErrorMessage>{getErrorMessage(error)}</ErrorMessage>}
        <PrimaryButton disabled={!password || isLoading} type="submit">
          Sign in
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
