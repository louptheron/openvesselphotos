import styled from 'styled-components'

import { Container, SectionTitle, Title } from '../../components/ui'
import { usePageMeta } from '../../hooks/usePageMeta'
import { useTranslation } from '../../i18n'
import { COLORS } from '../../theme'

/** Forwarded to the publisher (see LegalNotice on the server, which has the same texts for search engines) */
const CONTACT_EMAIL = 'contact@openvesselphotos.org'
const SOURCE_CODE_URL = 'https://github.com/louptheron/openvesselphotos'

/** The legal notice (mentions légales), mandatory for a website published in France */
export function LegalPage() {
  const { t } = useTranslation()
  usePageMeta(t.legal.pageTitle)
  const contact = <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>

  return (
    <Page>
      <Title>{t.legal.title}</Title>
      <section>
        <SectionTitle>{t.legal.publisher}</SectionTitle>
        <p>
          {t.legal.publisherText} {contact}.
        </p>
      </section>
      <section>
        <SectionTitle>{t.legal.director}</SectionTitle>
        <p>Loup Theron.</p>
      </section>
      <section>
        <SectionTitle>{t.legal.host}</SectionTitle>
        <p>
          {t.legal.hostText}{' '}
          <a href="https://scalingo.com" rel="noreferrer" target="_blank">
            scalingo.com
          </a>
        </p>
      </section>
      <section>
        <SectionTitle>{t.legal.content}</SectionTitle>
        <p>
          {t.legal.contentText}{' '}
          <a href={SOURCE_CODE_URL} rel="noreferrer" target="_blank">
            {t.legal.sourceCode}
          </a>
          .
        </p>
      </section>
      <section>
        <SectionTitle>{t.legal.personalData}</SectionTitle>
        {t.legal.personalDataText.map(paragraph => (
          <p key={paragraph}>{paragraph}</p>
        ))}
        <p>
          {t.legal.rightsText} {contact}.
        </p>
      </section>
    </Page>
  )
}

const Page = styled(Container)`
  display: flex;
  flex-direction: column;
  gap: 28px;
  max-width: 760px;
  padding-top: 48px;
  padding-bottom: 96px;

  section {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  p {
    margin: 0;
    line-height: 1.6;
    color: ${COLORS.text};
  }
`
