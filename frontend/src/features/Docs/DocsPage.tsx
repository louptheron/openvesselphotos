import { useEffect } from 'react'
import { Link, useLocation } from 'react-router-dom'
import styled from 'styled-components'

import { Card, Chip, CodeBlock, Container, Lead, SectionTitle, Title } from '../../components/ui'
import { usePageMeta } from '../../hooks/usePageMeta'
import { useTranslation } from '../../i18n'
import { COLORS, FONTS } from '../../theme'

/** In the order of the descriptions in the texts (docs.endpointDescriptions). */
const ENDPOINTS = [
  { method: 'GET', path: '/v1/{cfr|mmsi}/{key}' },
  { method: 'GET', path: '/v1/{cfr|mmsi}/{key}.jpg' },
  { method: 'GET', path: '/v1/{cfr|mmsi}/{key}/photos/{n}.jpg' },
  { method: 'GET', path: '/v1/search?q=ker avel' },
  { method: 'GET', path: '/v1/vessels/recent' },
  { method: 'GET', path: '/v1/vessels?page=1&flag=NOR' },
  { method: 'GET', path: '/v1/vessels/filters' },
  { method: 'GET', path: '/v1/export.json' },
  { method: 'POST', path: '/v1/{cfr|mmsi}/{key}/photos' },
  { method: 'POST', path: '/v1/{cfr|mmsi}/{key}/link' },
  { method: 'POST', path: '/v1/{cfr|mmsi}/{key}/photos/{n}/reports' },
  { method: 'POST', path: '/v1/api-key-requests' }
]

export function DocsPage() {
  const { hash } = useLocation()
  const origin = window.location.origin
  const { t } = useTranslation()
  usePageMeta(t.docs.pageTitle)

  useEffect(() => {
    if (hash) {
      document.getElementById(hash.slice(1))?.scrollIntoView()
    }
  }, [hash])

  return (
    <Page>
      <Chip>{t.docs.chip}</Chip>
      <Title>{t.docs.title}</Title>
      <Lead>{t.docs.lead}</Lead>

      <Keys>
        <Card>
          <small>{t.docs.keyCfr}</small>
          <strong>FRA000123456</strong>
          <code>/v1/cfr/FRA000123456/photos/1.jpg</code>
        </Card>
        <Card>
          <small>{t.docs.keyMmsi}</small>
          <strong>227000101</strong>
          <code>/v1/mmsi/227000101/photos/1.jpg</code>
        </Card>
      </Keys>

      <Section>
        <SectionTitle>{t.docs.endpoints}</SectionTitle>
        {ENDPOINTS.map((endpoint, index) => (
          <Endpoint key={endpoint.method + endpoint.path}>
            <div>
              <Method $isPost={endpoint.method === 'POST'}>{endpoint.method}</Method>
              <code>{endpoint.path}</code>
            </div>
            <p>{t.docs.endpointDescriptions[index]}</p>
          </Endpoint>
        ))}
      </Section>

      <Section>
        <SectionTitle>{t.docs.example}</SectionTitle>
        <CodeBlock>{`GET ${origin}/v1/cfr/FRA000123456

{
  "cfr": "FRA000123456",
  "mmsi": "227000101",
  "name": "Ker Avel",
  "registration": "GV 123456",
  "port": "Le Guilvinec",
  "flag": "FRA",
  "photos": [{
    "n": 1,
    "urls": {
      "cfr":  "${origin}/v1/cfr/FRA000123456/photos/1.jpg",
      "mmsi": "${origin}/v1/mmsi/227000101/photos/1.jpg"
    },
    "credit": "J. Le Bihan",
    "licence": "CC-BY-4.0",
    "licenceName": "CC BY 4.0",
    "attribution": "Photo: J. Le Bihan / Open Vessel Photos, CC BY 4.0",
    "takenOn": "2026-06-01",
    "place": "Concarneau",
    "width": 1600,
    "height": 1067
  }],
  "history": [...]
}`}</CodeBlock>
        <p>
          {t.docs.errorsIntro} <code>{'{"code": "NOT_FOUND", "message": "…"}'}</code> {t.docs.errorsOutro}
        </p>
      </Section>

      <Section id="api-key">
        <SectionTitle>{t.docs.apiKeyTitle}</SectionTitle>
        <p>
          {t.docs.apiKeyText1} <Link to="/api-key">{t.docs.apiKeyLink}</Link>
          {t.docs.apiKeyText2}
        </p>
        <CodeBlock>{`curl -X POST -H "x-api-key: ovp_…" \\
  -F file=@boat.jpg -F credit="Jane Doe" -F licence=CC-BY-4.0 \\
  ${origin}/v1/cfr/FRA000123456/photos`}</CodeBlock>
      </Section>

      <Section id="rate-limits">
        <SectionTitle>{t.docs.rateLimitsTitle}</SectionTitle>
        <p>{t.docs.rateLimitsText}</p>
      </Section>

      <Chips>
        {t.docs.chips.map(chip => (
          <Chip key={chip}>{chip}</Chip>
        ))}
      </Chips>

      <Section id="licences">
        <SectionTitle>{t.docs.licencesTitle}</SectionTitle>
        <p>{t.docs.licencesText}</p>
        <ul>
          <li>
            <a href="https://creativecommons.org/licenses/by/4.0/" rel="noreferrer" target="_blank">
              CC BY 4.0
            </a>{' '}
            {t.docs.licenceBy}
          </li>
          <li>
            <a href="https://creativecommons.org/licenses/by-sa/4.0/" rel="noreferrer" target="_blank">
              CC BY-SA 4.0
            </a>{' '}
            {t.docs.licenceBySa}
          </li>
          <li>
            <a href="https://creativecommons.org/publicdomain/zero/1.0/" rel="noreferrer" target="_blank">
              CC0
            </a>{' '}
            {t.docs.licenceCc0}
          </li>
        </ul>
        <p>{t.docs.licenceOlder}</p>
      </Section>

      <Section id="self-host">
        <SectionTitle>{t.docs.selfHostTitle}</SectionTitle>
        <p>{t.docs.selfHostText}</p>
        <CodeBlock>{`docker compose up`}</CodeBlock>
      </Section>
    </Page>
  )
}

const Page = styled(Container)`
  padding-top: 24px;
  padding-bottom: 56px;
  max-width: 960px;
  display: flex;
  flex-direction: column;
  gap: 22px;

  p,
  li {
    line-height: 1.55;
    color: ${COLORS.text};
  }

  code {
    font-family: ${FONTS.mono};
    font-size: 0.92em;
  }
`

const Keys = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;

  > div {
    padding: 14px 16px;
    border-radius: 12px;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  small,
  code {
    color: ${COLORS.muted};
    font-size: 12px;
  }

  strong {
    font-family: ${FONTS.mono};
    font-size: 15px;
    font-weight: 400;
  }
`

const Section = styled.section`
  display: flex;
  flex-direction: column;
  gap: 8px;
  scroll-margin-top: 16px;

  p,
  ul {
    margin: 0;
  }
`

const Endpoint = styled(Card)`
  padding: 12px 14px;
  border-radius: 10px;

  > div {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }

  code {
    font-size: 14px;
    overflow-wrap: anywhere;
  }

  p {
    margin: 4px 0 0;
    font-size: 13px;
  }
`

const Method = styled.span<{ $isPost: boolean }>`
  font-family: ${FONTS.mono};
  font-size: 11px;
  font-weight: 500;
  padding: 3px 8px;
  border-radius: 4px;
  background: ${p => (p.$isPost ? '#FBE2D3' : '#DDEEE3')};
  color: ${p => (p.$isPost ? '#8A3410' : '#1D5B34')};
`

const Chips = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
`
