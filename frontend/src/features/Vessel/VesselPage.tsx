import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import styled from 'styled-components'

import { getErrorStatus, useGetVesselQuery } from '../../api/api'
import type { KeyType, Photo, Vessel } from '../../api/types'
import { BoatPlaceholder } from '../../components/Logo'
import {
  Card,
  CodeBlock,
  Container,
  Muted,
  Pill,
  PrimaryButton,
  SecondaryButton,
  SectionTitle
} from '../../components/ui'
import { COLORS, FONTS, SKIES } from '../../theme'
import { getPhotoUrl, normalizeKey } from '../../utils/vesselKey'
import { KeyLinkForm } from './KeyLinkForm'
import { ReportForm } from './ReportForm'

const SIZES = [
  { label: '320 px', value: '320' },
  { label: '800 px', value: '800' },
  { label: 'Full', value: 'full' }
] as const

type Size = (typeof SIZES)[number]['value']

export function VesselPage() {
  const params = useParams<{ key: string; keyType: string }>()
  const keyType: KeyType = params.keyType === 'mmsi' ? 'mmsi' : 'cfr'
  const key = normalizeKey(params.key ?? '')
  const { data: vessel, error, isLoading } = useGetVesselQuery({ key, keyType })

  if (isLoading) {
    return <Container>Loading…</Container>
  }
  if (!vessel) {
    const status = getErrorStatus(error)

    return (
      <NotFound>
        <h1>
          {status === 404 || status === 400 ? `No vessel with ${keyType.toUpperCase()} ${key} yet` : 'Unavailable'}
        </h1>
        {status === 404 && (
          <p>
            Nobody has shared a photo of it so far.{' '}
            <Link to={`/contribute?${keyType}=${key}`}>Contribute the first photo →</Link>
          </p>
        )}
        {status === 400 && (
          <p>
            This doesn't look like a valid {keyType.toUpperCase()}. <Link to="/">Back to search</Link>
          </p>
        )}
        {status !== 404 && status !== 400 && <p>Please try again later.</p>}
      </NotFound>
    )
  }

  return <VesselDetails key={`${keyType}-${key}`} vessel={vessel} />
}

function VesselDetails({ vessel }: { vessel: Vessel }) {
  const [pickedIndex, setPickedIndex] = useState(0)
  const [size, setSize] = useState<Size>('800')
  const [copied, setCopied] = useState(false)
  const [isReporting, setIsReporting] = useState(false)
  const photo: Photo | undefined = vessel.photos[pickedIndex] ?? vessel.photos[0]
  const name = vessel.name ?? 'Unnamed vessel'
  const vesselKey = vessel.cfr
    ? { key: vessel.cfr, keyType: 'cfr' as const }
    : { key: vessel.mmsi ?? '', keyType: 'mmsi' as const }
  const contributeQuery = vessel.cfr ? `cfr=${vessel.cfr}` : `mmsi=${vessel.mmsi}`

  const imageUrls = photo ? [photo.urls.cfr, photo.urls.mmsi].filter(Boolean).map(url => `${url}?size=${size}`) : []

  const copy = async () => {
    if (!photo) {
      return
    }
    await navigator.clipboard.writeText(`${photo.attribution}\n${imageUrls[0]}`)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <>
      <Breadcrumb aria-label="Breadcrumb">
        <Link to="/">Search</Link> <span aria-hidden="true">›</span> {name}
      </Breadcrumb>

      <Main>
        <Gallery>
          {photo ? (
            <Figure>
              <img
                alt={`${name}, photo ${photo.n} by ${photo.credit}`}
                src={getPhotoUrl(photo.urls, '800')}
                srcSet={`${getPhotoUrl(photo.urls, '800')} 800w, ${getPhotoUrl(photo.urls, 'full')} 2400w`}
                sizes="(max-width: 900px) 100vw, 760px"
              />
              <figcaption>
                <span>
                  © {photo.credit} · {photo.licenceName}
                </span>
                <span>
                  Photo {photo.n} of {vessel.photos.length}
                  {photo.takenOn && ` · taken ${photo.takenOn}`}
                  {photo.place && ` · ${photo.place}`}
                </span>
              </figcaption>
            </Figure>
          ) : (
            <EmptyFigure>
              <BoatPlaceholder width={320} />
              <p>No photo available.</p>
            </EmptyFigure>
          )}

          <Thumbnails aria-label="Photos of this vessel">
            {vessel.photos.map((item, index) => (
              <Thumbnail
                key={item.n}
                $active={item.n === photo?.n}
                aria-label={`Show photo ${item.n}`}
                aria-pressed={item.n === photo?.n}
                onClick={() => {
                  setPickedIndex(index)
                  setIsReporting(false)
                }}
                type="button"
              >
                <img alt="" loading="lazy" src={getPhotoUrl(item.urls, '320')} />
              </Thumbnail>
            ))}
            <AddPhoto to={`/contribute?${contributeQuery}`}>
              <svg
                aria-hidden="true"
                fill="none"
                height="20"
                stroke="currentColor"
                strokeLinecap="round"
                strokeWidth="2"
                viewBox="0 0 24 24"
                width="20"
              >
                <path d="M12 5v14" />
                <path d="M5 12h14" />
              </svg>
              Add a photo
            </AddPhoto>
          </Thumbnails>

          {photo &&
            (isReporting ? (
              <ReportForm key={photo.n} n={photo.n} onClose={() => setIsReporting(false)} vesselKey={vesselKey} />
            ) : (
              <ReportButton onClick={() => setIsReporting(true)} type="button">
                Report photo {photo.n}
              </ReportButton>
            ))}
        </Gallery>

        <Aside>
          <div>
            {vessel.flag && <FlagChip>Flag · {vessel.flag} (from CFR)</FlagChip>}
            <h1>{name}</h1>
          </div>

          <Keys>
            <Card as="div">
              <dt>CFR</dt>
              <dd>{vessel.cfr ?? '—'}</dd>
            </Card>
            <Card as="div">
              <dt>MMSI</dt>
              <dd>{vessel.mmsi ?? '—'}</dd>
            </Card>
          </Keys>
          {(!vessel.cfr || !vessel.mmsi) && <KeyLinkForm vessel={vessel} vesselKey={vesselKey} />}

          {photo && (
            <UsePanel>
              <h2>Use this photo</h2>
              <Muted>
                Credit line — required by the licence{photo.licence === 'CC0-1.0' && ' (CC0: optional but appreciated)'}
              </Muted>
              <Attribution>{photo.attribution}</Attribution>
              <div aria-label="Image size" role="group">
                {SIZES.map(item => (
                  <Pill
                    key={item.value}
                    $active={size === item.value}
                    aria-pressed={size === item.value}
                    onClick={() => setSize(item.value)}
                    type="button"
                  >
                    {item.label}
                  </Pill>
                ))}
              </div>
              <Muted>Image URL, either key works</Muted>
              <CodeBlock>{imageUrls.join('\n')}</CodeBlock>
              <Actions>
                <PrimaryButton onClick={copy} type="button">
                  {copied ? 'Copied' : 'Copy credit + URL'}
                </PrimaryButton>
                <SecondaryButton
                  as="a"
                  href={`/v1/${vesselKey.keyType}/${vesselKey.key}`}
                  rel="noreferrer"
                  target="_blank"
                >
                  JSON for this vessel
                </SecondaryButton>
              </Actions>
            </UsePanel>
          )}
        </Aside>
      </Main>

      {vessel.history.length > 0 && (
        <History>
          <SectionTitle>History</SectionTitle>
          <TableBox>
            <table>
              <thead>
                <tr>
                  <th scope="col">Date</th>
                  <th scope="col">Change</th>
                  <th scope="col">By</th>
                </tr>
              </thead>
              <tbody>
                {vessel.history.map(event => (
                  <tr key={`${event.date}-${event.description}`}>
                    <td>{event.date.slice(0, 10)}</td>
                    <td>{event.description}</td>
                    <td>{event.by ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </TableBox>
        </History>
      )}
    </>
  )
}

const NotFound = styled(Container)`
  padding-top: 48px;
  padding-bottom: 96px;

  h1 {
    font-family: ${FONTS.display};
    font-size: 36px;
    margin: 0 0 12px;
  }
`

const Breadcrumb = styled(Container).attrs({ as: 'nav' })`
  padding-top: 8px;
  font-size: 14px;
  color: ${COLORS.muted};
`

const Main = styled(Container).attrs({ as: 'section' })`
  padding-top: 20px;
  padding-bottom: 40px;
  display: flex;
  flex-wrap: wrap;
  gap: 32px;
  align-items: flex-start;
`

const Gallery = styled.div`
  flex: 999 1 560px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
`

const Figure = styled.figure`
  margin: 0;
  border-radius: 16px;
  overflow: hidden;
  background: ${SKIES[0]};

  img {
    display: block;
    width: 100%;
    max-height: 560px;
    object-fit: contain;
  }

  figcaption {
    background: ${COLORS.ink};
    color: ${COLORS.white};
    padding: 12px 16px;
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    gap: 8px;
    font-size: 14px;
  }
`

const EmptyFigure = styled.div`
  height: 360px;
  border-radius: 16px;
  background: ${SKIES[0]};
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: ${COLORS.text};
`

const Thumbnails = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
`

const Thumbnail = styled.button<{ $active: boolean }>`
  width: 128px;
  height: 84px;
  padding: 0;
  border-radius: 10px;
  border: 3px solid ${p => (p.$active ? COLORS.accent : 'transparent')};
  background: ${SKIES[0]};
  overflow: hidden;
  cursor: pointer;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
`

const AddPhoto = styled(Link)`
  width: 128px;
  height: 84px;
  border-radius: 10px;
  border: 2px dashed ${COLORS.inputBorder};
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
`

const ReportButton = styled.button`
  align-self: flex-start;
  border: none;
  background: none;
  padding: 8px 0;
  font: inherit;
  font-size: 13px;
  color: ${COLORS.muted};
  text-decoration: underline;
  cursor: pointer;
`

const Aside = styled.aside`
  flex: 1 1 340px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;

  h1 {
    margin: 6px 0 0;
    font-family: ${FONTS.display};
    font-weight: 700;
    font-size: 48px;
    line-height: 1.02;
  }
`

const FlagChip = styled.span`
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 12px;
  background: ${COLORS.white};
  border: 1px solid ${COLORS.border};
`

const Keys = styled.dl`
  margin: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;

  > div {
    padding: 12px 14px;
    border-radius: 12px;
  }

  dt {
    font-size: 12px;
    color: ${COLORS.muted};
  }

  dd {
    margin: 4px 0 0;
    font-family: ${FONTS.mono};
    font-size: 15px;
  }
`

const UsePanel = styled(Card)`
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;

  h2 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
  }

  [role='group'] {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  pre {
    font-size: 12px;
  }
`

const Attribution = styled.div`
  font-family: ${FONTS.mono};
  font-size: 13px;
  background: ${COLORS.ground};
  border-radius: 8px;
  padding: 10px 12px;
`

const Actions = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
`

const History = styled(Container).attrs({ as: 'section' })`
  padding-bottom: 48px;
  display: flex;
  flex-direction: column;
  gap: 14px;
`

const TableBox = styled(Card)`
  overflow-x: auto;

  table {
    width: 100%;
    min-width: 560px;
    border-collapse: collapse;
    font-size: 14px;
  }

  th {
    text-align: left;
    color: ${COLORS.muted};
    font-size: 12px;
    font-weight: 500;
  }

  th,
  td {
    padding: 12px 16px;
    border-bottom: 1px solid ${COLORS.divider};
  }

  td:first-child {
    font-family: ${FONTS.mono};
    font-size: 13px;
    white-space: nowrap;
  }

  td:last-child {
    color: ${COLORS.text};
  }
`
