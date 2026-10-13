import { Link } from 'react-router-dom'

import { Container, Lead, Title } from '../../components/ui'
import { usePageMeta } from '../../hooks/usePageMeta'

export function NotFoundPage() {
  usePageMeta('Page not found')

  return (
    <Container style={{ display: 'flex', flexDirection: 'column', gap: 12, paddingBottom: 96, paddingTop: 48 }}>
      <Title>Page not found</Title>
      <Lead>
        This page doesn't exist. <Link to="/">Search vessel photos</Link>
      </Lead>
    </Container>
  )
}
