import { useEffect } from 'react'
import { Link, useLocation } from 'react-router-dom'
import styled from 'styled-components'

import { Card, Chip, CodeBlock, Container, Lead, SectionTitle, Title } from '../../components/ui'
import { usePageMeta } from '../../hooks/usePageMeta'
import { COLORS, FONTS } from '../../theme'

const ENDPOINTS = [
  {
    description: 'Vessel, both of its keys, and all its published photos with credit and licence.',
    method: 'GET',
    path: '/v1/{cfr|mmsi}/{key}'
  },
  {
    description: 'The main photo as an image, ready for an <img> tag. ?size=320|800|full (default 800).',
    method: 'GET',
    path: '/v1/{cfr|mmsi}/{key}.jpg'
  },
  {
    description: 'A specific photo; the same n under either key is the same file. Same ?size parameter.',
    method: 'GET',
    path: '/v1/{cfr|mmsi}/{key}/photos/{n}.jpg'
  },
  {
    description: 'Search by name, or by the beginning of a CFR or MMSI. Up to 24 results.',
    method: 'GET',
    path: '/v1/search?q=ker avel'
  },
  {
    description: 'Vessels with the most recently published photos. ?limit=12 (max 48).',
    method: 'GET',
    path: '/v1/vessels/recent'
  },
  {
    description: 'The whole dataset: every vessel with its photo URLs, credits and licences.',
    method: 'GET',
    path: '/v1/export.json'
  },
  {
    description:
      'Contribute a photo (multipart: file, credit, licence, takenOn?, place?, email?, vesselName?), under whichever key you know. Published after review. Needs an API key.',
    method: 'POST',
    path: '/v1/{cfr|mmsi}/{key}/photos'
  },
  {
    description:
      'Suggest the missing key, e.g. {"keyType": "cfr", "key": "FRA000123456"}. Applied after review. Needs an API key.',
    method: 'POST',
    path: '/v1/{cfr|mmsi}/{key}/link'
  },
  {
    description:
      'Report a photo: {"reason": "WRONG_VESSEL|COPYRIGHT|PRIVACY|OTHER", "message": "…"}. Needs an API key.',
    method: 'POST',
    path: '/v1/{cfr|mmsi}/{key}/photos/{n}/reports'
  },
  {
    description: 'Request an API key: {"name", "email", "organisation"?, "intendedUse"}. Sent by email once approved.',
    method: 'POST',
    path: '/v1/api-key-requests'
  }
]

export function DocsPage() {
  const { hash } = useLocation()
  const origin = window.location.origin
  usePageMeta('Free vessel photo API, by CFR or MMSI')

  useEffect(() => {
    if (hash) {
      document.getElementById(hash.slice(1))?.scrollIntoView()
    }
  }, [hash])

  return (
    <Page>
      <Chip>API v1 · no key required</Chip>
      <Title>The vessel photo API</Title>
      <Lead>
        CFR and MMSI are both first-class keys: every URL exists under each of them and returns the same vessel and
        photos. A vessel can have one key or both — a non-EU ship with only an MMSI works just as well.
      </Lead>

      <Keys>
        <Card>
          <small>Key · CFR (EU fleet register)</small>
          <strong>FRA000123456</strong>
          <code>/v1/cfr/FRA000123456/photos/1.jpg</code>
        </Card>
        <Card>
          <small>Key · MMSI (radio identity)</small>
          <strong>227000101</strong>
          <code>/v1/mmsi/227000101/photos/1.jpg</code>
        </Card>
      </Keys>

      <Section>
        <SectionTitle>Endpoints</SectionTitle>
        {ENDPOINTS.map(endpoint => (
          <Endpoint key={endpoint.method + endpoint.path}>
            <div>
              <Method $isPost={endpoint.method === 'POST'}>{endpoint.method}</Method>
              <code>{endpoint.path}</code>
            </div>
            <p>{endpoint.description}</p>
          </Endpoint>
        ))}
      </Section>

      <Section>
        <SectionTitle>Example</SectionTitle>
        <CodeBlock>{`GET ${origin}/v1/cfr/FRA000123456

{
  "cfr": "FRA000123456",
  "mmsi": "227000101",
  "name": "Ker Avel",
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
    "width": 2400,
    "height": 1600
  }],
  "history": [...]
}`}</CodeBlock>
        <p>
          Errors come back as <code>{'{"code": "NOT_FOUND", "message": "…"}'}</code> with a 400, 404 or 409 status.
        </p>
      </Section>

      <Section id="api-key">
        <SectionTitle>API key</SectionTitle>
        <p>
          Every <code>GET</code> is open, without key. <code>POST</code> requests need an API key in the{' '}
          <code>x-api-key</code> header: <Link to="/api-key">request one</Link>, it is free. Without it, they return a{' '}
          <code>401</code>.
        </p>
        <CodeBlock>{`curl -X POST -H "x-api-key: ovp_…" \\
  -F file=@boat.jpg -F credit="Jane Doe" -F licence=CC-BY-4.0 \\
  ${origin}/v1/cfr/FRA000123456/photos`}</CodeBlock>
      </Section>

      <Section id="rate-limits">
        <SectionTitle>Rate limits</SectionTitle>
        <p>
          Up to 600 <code>GET</code> per minute per IP, and 1,000 <code>POST</code> per hour per API key. Every response
          tells where you stand in <code>X-RateLimit-Limit</code>, <code>X-RateLimit-Remaining</code> and{' '}
          <code>X-RateLimit-Reset</code> (seconds). Above the limit, requests return a <code>429</code> with a{' '}
          <code>Retry-After</code> header. Need more? Download the whole dataset from <code>/v1/export.json</code>, or
          ask for a higher limit.
        </p>
      </Section>

      <Chips>
        <Chip>CORS enabled</Chip>
        <Chip>Reading needs no API key</Chip>
        <Chip>Always show the credit</Chip>
      </Chips>

      <Section id="licences">
        <SectionTitle>Licences</SectionTitle>
        <p>
          Every photo is published by its author under one of these licences. Reuse it freely, as long as you show the
          credit line given in <code>attribution</code> when the licence asks for it.
        </p>
        <ul>
          <li>
            <a href="https://creativecommons.org/licenses/by/4.0/" rel="noreferrer" target="_blank">
              CC BY 4.0
            </a>{' '}
            — reuse with credit.
          </li>
          <li>
            <a href="https://creativecommons.org/licenses/by-sa/4.0/" rel="noreferrer" target="_blank">
              CC BY-SA 4.0
            </a>{' '}
            — reuse with credit; changes shared under the same licence.
          </li>
          <li>
            <a href="https://creativecommons.org/publicdomain/zero/1.0/" rel="noreferrer" target="_blank">
              CC0
            </a>{' '}
            — public domain, no credit required.
          </li>
        </ul>
      </Section>

      <Section id="self-host">
        <SectionTitle>Self-host it</SectionTitle>
        <p>
          The service is one container (Spring Boot serving the API and this web app) and one PostgreSQL database, which
          also stores the images. Set <code>DATABASE_URL</code> and <code>ADMIN_PASSWORD</code>, and run it.
        </p>
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
