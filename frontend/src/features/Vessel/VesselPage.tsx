import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import styled from 'styled-components'

import { getErrorStatus, useGetVesselQuery } from '../../api/api'
import type { KeyType, Photo, Vessel } from '../../api/types'
import { BoatPlaceholder } from '../../components/Logo'
import { VesselCard, VesselGrid } from '../../components/VesselCard'
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
import { usePageMeta } from '../../hooks/usePageMeta'
import { useTranslation } from '../../i18n'
import { COLORS, FONTS, SKIES, getSkyAndSea } from '../../theme'
import { getNamedPhotoUrl, getPhotoSlug, getPhotoUrl, normalizeKey } from '../../utils/vesselKey'
import { KeyLinkForm } from './KeyLinkForm'
import { RegisterFacts } from './RegisterFacts'
import { ReportForm } from './ReportForm'

/** Licences under which crediting the author is welcome but not required */
const LICENCES_WITHOUT_CREDIT = new Set(['CC0-1.0', 'LicenseRef-PublicDomain'])

const SIZES = [
  { label: '320 px', value: '320' },
  { label: '800 px', value: '800' },
  { label: undefined, value: 'full' }
] as const

type Size = (typeof SIZES)[number]['value']

export function VesselPage() {
  const params = useParams<{ key: string; keyType: string }>()
  const keyType: KeyType = params.keyType === 'mmsi' ? 'mmsi' : 'cfr'
  const key = normalizeKey(params.key ?? '')
  const { data: vessel, error, isLoading } = useGetVesselQuery({ key, keyType })
  const { t } = useTranslation()
  const keys = vessel
    ? [vessel.cfr && `CFR ${vessel.cfr}`, vessel.mmsi && `MMSI ${vessel.mmsi}`].filter(Boolean).join(', ')
    : ''
  const vesselType = vessel?.register?.vesselType ? t.register.vesselTypes[vessel.register.vesselType] : undefined
  usePageMeta(
    vessel
      ? t.vessel.pageTitle(
          vessel.name ? `${vessel.name} (${vessel.registration ?? keys})` : keys,
          vessel.photos.length,
          [vesselType, vessel.port].filter((detail): detail is string => !!detail)
        )
      : isLoading
        ? undefined
        : t.vessel.notFoundPageTitle(`${keyType.toUpperCase()} ${key}`)
  )

  if (isLoading) {
    return <Container>{t.common.loading}</Container>
  }
  if (!vessel) {
    const status = getErrorStatus(error)

    return (
      <NotFound>
        <h1>
          {status === 404 || status === 400
            ? t.vessel.notFoundTitle(`${keyType.toUpperCase()} ${key}`)
            : t.vessel.unavailable}
        </h1>
        {status === 404 && (
          <p>
            {t.vessel.notFoundText}{' '}
            <Link rel="nofollow" to={`/contribute?${keyType}=${key}`}>
              {t.vessel.contributeFirst}
            </Link>
          </p>
        )}
        {status === 400 && (
          <p>
            {t.vessel.invalidKey(keyType.toUpperCase())} <Link to="/">{t.vessel.backToSearch}</Link>
          </p>
        )}
        {status !== 404 && status !== 400 && <p>{t.vessel.tryLater}</p>}
      </NotFound>
    )
  }

  return <VesselDetails key={`${keyType}-${key}`} vessel={vessel} />
}

function VesselDetails({ vessel }: { vessel: Vessel }) {
  const { formatDate, t } = useTranslation()
  const [pickedIndex, setPickedIndex] = useState(0)
  const [size, setSize] = useState<Size>('800')
  const [copied, setCopied] = useState(false)
  const [isReporting, setIsReporting] = useState(false)
  const photo: Photo | undefined = vessel.photos[pickedIndex] ?? vessel.photos[0]
  const name = vessel.name ?? t.common.unnamedVessel
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
      <Breadcrumb aria-label={t.vessel.breadcrumb}>
        <Link to="/">{t.vessel.search}</Link> <span aria-hidden="true">›</span> {name}
      </Breadcrumb>

      <Main>
        <Gallery>
          {photo ? (
            <Figure>
              <img
                alt={t.vessel.photoAlt(
                  vessel.registration ? `${name} (${vessel.registration})` : name,
                  vessel.register?.vesselType ? t.register.vesselTypes[vessel.register.vesselType] : undefined,
                  photo.place,
                  photo.credit
                )}
                src={getNamedPhotoUrl(photo.urls, getPhotoSlug(vessel), '800')}
                srcSet={`${getNamedPhotoUrl(photo.urls, getPhotoSlug(vessel), '800')} 800w, ${getNamedPhotoUrl(photo.urls, getPhotoSlug(vessel), 'full')} 1600w`}
                sizes="(max-width: 900px) 100vw, 760px"
              />
              <figcaption>
                <span>
                  © {photo.credit} · {photo.licenceName}
                  {photo.source && (
                    <>
                      {' · '}
                      <a href={photo.source} rel="noreferrer" target="_blank">
                        {t.vessel.source}
                      </a>
                    </>
                  )}
                </span>
                <span>
                  {t.vessel.photoOf(photo.n, vessel.photos.length)}
                  {photo.takenOn && ` · ${t.vessel.taken(formatDate(photo.takenOn))}`}
                  {photo.place && ` · ${photo.place}`}
                </span>
                {photo.note && <span>{photo.note}</span>}
              </figcaption>
            </Figure>
          ) : (
            <EmptyFigure>
              <BoatPlaceholder width={320} />
              <p>{t.vessel.noPhotoOf(name)}</p>
              <PrimaryButton as={Link} rel="nofollow" to={`/contribute?${contributeQuery}`}>
                {t.vessel.addFirstPhoto}
              </PrimaryButton>
            </EmptyFigure>
          )}

          <Thumbnails aria-label={t.vessel.photosLabel}>
            {vessel.photos.map((item, index) => (
              <Thumbnail
                key={item.n}
                $active={item.n === photo?.n}
                aria-label={t.vessel.showPhoto(item.n)}
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
            <AddPhoto rel="nofollow" to={`/contribute?${contributeQuery}`}>
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
              {t.vessel.addPhoto}
            </AddPhoto>
          </Thumbnails>

          {photo &&
            (isReporting ? (
              <ReportForm key={photo.n} n={photo.n} onClose={() => setIsReporting(false)} vesselKey={vesselKey} />
            ) : (
              <ReportButton onClick={() => setIsReporting(true)} type="button">
                {t.vessel.reportPhoto(photo.n)}
              </ReportButton>
            ))}
        </Gallery>

        <Aside>
          <Heading>
            {vessel.flag && <FlagChip>{t.vessel.flag(vessel.flag)}</FlagChip>}
            <h1>{name}</h1>
            {(vessel.registration || vessel.port) && (
              <Registration>{[vessel.registration, vessel.port].filter(Boolean).join(' · ')}</Registration>
            )}
          </Heading>

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
          <RegisterFacts vessel={vessel} />

          {photo && (
            <UsePanel>
              <h2>{t.vessel.useThisPhoto}</h2>
              <Muted>
                {t.vessel.creditLine}
                {LICENCES_WITHOUT_CREDIT.has(photo.licence) && t.vessel.creditLineOptional}
              </Muted>
              <Attribution>{photo.attribution}</Attribution>
              <Sizes>
                <legend className="visually-hidden">{t.vessel.imageSize}</legend>
                {SIZES.map(item => (
                  <Pill
                    key={item.value}
                    $active={size === item.value}
                    aria-pressed={size === item.value}
                    onClick={() => setSize(item.value)}
                    type="button"
                  >
                    {item.label ?? t.vessel.sizeFull}
                  </Pill>
                ))}
              </Sizes>
              <Muted>{t.vessel.imageUrl}</Muted>
              <CodeBlock>{imageUrls.join('\n')}</CodeBlock>
              <Actions>
                <PrimaryButton onClick={copy} type="button">
                  {copied ? t.common.copied : t.vessel.copyCreditAndUrl}
                </PrimaryButton>
                <SecondaryButton
                  as="a"
                  href={`/v1/${vesselKey.keyType}/${vesselKey.key}`}
                  rel="noreferrer"
                  target="_blank"
                >
                  {t.vessel.json}
                </SecondaryButton>
              </Actions>
            </UsePanel>
          )}
        </Aside>
      </Main>

      {vessel.related.length > 0 && (
        <Related aria-labelledby="related-title">
          <SectionTitle id="related-title">
            {vessel.port && vessel.related.every(other => other.port === vessel.port)
              ? t.vessel.relatedFromPort(vessel.port)
              : t.vessel.related}
          </SectionTitle>
          <VesselGrid>
            {vessel.related.map((other, index) => (
              <VesselCard key={other.cfr ?? other.mmsi} index={index} vessel={other} />
            ))}
          </VesselGrid>
          <Link to="/vessels">{t.layout.allVessels}</Link>
        </Related>
      )}

      {vessel.history.length > 0 && (
        <History>
          <SectionTitle>{t.vessel.history}</SectionTitle>
          <TableBox>
            <table>
              <thead>
                <tr>
                  <th scope="col">{t.vessel.historyDate}</th>
                  <th scope="col">{t.vessel.historyChange}</th>
                  <th scope="col">{t.vessel.historyBy}</th>
                </tr>
              </thead>
              <tbody>
                {vessel.history.map(event => (
                  <tr key={`${event.date}-${event.description}`}>
                    <td>{formatDate(event.date)}</td>
                    <td>{t.vessel.historyEvent(event)}</td>
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

const Related = styled(Container).attrs({ as: 'section' })`
  padding-top: 32px;
  padding-bottom: 32px;
  display: flex;
  flex-direction: column;
  gap: 16px;
`

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
  font-size: 14px;
  color: ${COLORS.muted};

  a {
    display: inline-block;
    padding: 12px 0;
  }
`

const Main = styled(Container).attrs({ as: 'section' })`
  padding-top: 20px;
  padding-bottom: 40px;
  display: flex;
  flex-wrap: wrap;
  gap: 32px;
  align-items: flex-start;

  /* Phones and tablets: one column, with the vessel's name before its photos */
  @media (max-width: 959px) {
    flex-direction: column;
    flex-wrap: nowrap;
    align-items: stretch;
    gap: 18px;
  }
`

const Gallery = styled.div`
  flex: 999 1 560px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;

  /* In the one-column layout, the flex-basis would become a minimum height */
  @media (max-width: 959px) {
    flex: none;
  }
`

const Figure = styled.figure`
  margin: 0;
  border-radius: 8px;
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

const Registration = styled.p`
  margin: 6px 0 0;
  font-size: 17px;
  color: ${COLORS.text};
`

const EmptyFigure = styled.div`
  height: 360px;
  border-radius: 8px;
  background: ${getSkyAndSea(SKIES[0] ?? COLORS.info, 82)};
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
  border-radius: 6px;
  border: 3px solid ${p => (p.$active ? COLORS.accent : 'transparent')};
  background: ${getSkyAndSea(SKIES[0] ?? COLORS.info, 82)};
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
  border-radius: 6px;
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
  min-height: 44px;
  border: none;
  background: none;
  padding: 0;
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

  /* Its blocks join the page's column, so that the heading can move above the photos */
  @media (max-width: 959px) {
    display: contents;
  }
`

const Heading = styled.div`
  h1 {
    margin: 6px 0 0;
    font-family: ${FONTS.display};
    font-weight: 700;
    font-size: clamp(34px, 9vw, 48px);
    line-height: 1.02;
    overflow-wrap: anywhere;
  }

  @media (max-width: 959px) {
    order: -1;
  }
`

const FlagChip = styled.span`
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 6px;
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
    border-radius: 6px;
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

  pre {
    font-size: 12px;
  }
`

const Sizes = styled.fieldset`
  margin: 0;
  padding: 0;
  border: none;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
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
