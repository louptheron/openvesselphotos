import { Link } from 'react-router-dom'

import { Container, Lead, Title } from '../../components/ui'
import { usePageMeta } from '../../hooks/usePageMeta'
import { useTranslation } from '../../i18n'

export function NotFoundPage() {
  const { t } = useTranslation()
  usePageMeta(t.notFound.title)

  return (
    <Container style={{ display: 'flex', flexDirection: 'column', gap: 12, paddingBottom: 96, paddingTop: 48 }}>
      <Title>{t.notFound.title}</Title>
      <Lead>
        {t.notFound.text} <Link to="/">{t.notFound.link}</Link>
      </Lead>
    </Container>
  )
}
