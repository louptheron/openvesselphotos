import { Suspense, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import styled from 'styled-components'

import { getErrorStatus, useGetPortQuery, useGetPortVesselsQuery } from '../../api/api'
import { CoverageBar } from '../../components/CoverageBar'
import { VesselCard } from '../../components/VesselCard'
import { Chip, Container, Muted, SecondaryButton, SectionTitle } from '../../components/ui'
import { usePageMeta } from '../../hooks/usePageMeta'
import { useTranslation } from '../../i18n'
import { COLORS, FONTS } from '../../theme'
import { getVesselPath } from '../../utils/vesselKey'
import { MapPlaceholder, PortsMap } from './LazyPortsMap'

/** Same as the backend (GetPortVessels.PAGE_SIZE): a shorter page is the last one. */
const PAGE_SIZE = 48

export function PortPage() {
  const code = (useParams<{ code: string }>().code ?? '').toUpperCase()
  const { data: port, error, isLoading } = useGetPortQuery(code)
  const { formatNumber, language, t } = useTranslation()
  usePageMeta(port ? t.ports.portPageTitle(port.name, port.code) : isLoading ? undefined : t.ports.notFound(code))

  if (isLoading) {
    return <Page>{t.common.loading}</Page>
  }
  if (!port) {
    return (
      <Page>
        <h1>{getErrorStatus(error) === 404 ? t.ports.notFound(code) : t.ports.unavailable}</h1>
        <p>
          <Link to="/ports">{t.ports.allPorts}</Link>
        </p>
      </Page>
    )
  }
  const wanted = port.vessels - port.photographed

  return (
    <>
      <Breadcrumb aria-label={t.vessel.breadcrumb}>
        <Link to="/ports">{t.ports.title}</Link> <span aria-hidden="true">›</span> {port.name}
      </Breadcrumb>
      <Page>
        <Top>
          <Heading>
            <Chip>{new Intl.DisplayNames([language], { type: 'region' }).of(port.country) ?? port.country}</Chip>
            <h1>{port.name}</h1>
            <Muted>UN/LOCODE {port.code}</Muted>
            <CoverageBar
              label={t.ports.coverage(formatNumber(port.photographed), formatNumber(port.vessels), port.photographed)}
              photographed={port.photographed}
              total={port.vessels}
            />
          </Heading>
          {port.latitude !== null && (
            <Suspense fallback={<MapPlaceholder $isCompact />}>
              <PortsMap isCompact ports={[port]} />
            </Suspense>
          )}
        </Top>

        {port.photographed > 0 && (
          <section>
            <SectionTitle>{t.ports.photographedTitle(formatNumber(port.photographed))}</SectionTitle>
            <PagedVessels code={port.code} isPhotographed total={port.photographed} />
          </section>
        )}
        {wanted > 0 && (
          <section>
            <SectionTitle>{t.ports.wantedTitle(formatNumber(wanted))}</SectionTitle>
            <Muted>{t.ports.wantedText}</Muted>
            <PagedVessels code={port.code} isPhotographed={false} total={wanted} />
          </section>
        )}
      </Page>
    </>
  )
}

/** The vessels of a port, a page at a time, with a "show more" button. */
function PagedVessels({ code, isPhotographed, total }: { code: string; isPhotographed: boolean; total: number }) {
  const [pageCount, setPageCount] = useState(1)
  const { t } = useTranslation()

  return (
    <>
      {isPhotographed ? (
        <Grid>
          {Array.from({ length: pageCount }, (_, page) => (
            <VesselsPage key={page} code={code} isPhotographed page={page} />
          ))}
        </Grid>
      ) : (
        <Wanted>
          {Array.from({ length: pageCount }, (_, page) => (
            <VesselsPage key={page} code={code} isPhotographed={false} page={page} />
          ))}
        </Wanted>
      )}
      {pageCount * PAGE_SIZE < total && (
        <MoreButton onClick={() => setPageCount(count => count + 1)} type="button">
          {t.ports.showMore}
        </MoreButton>
      )}
    </>
  )
}

function VesselsPage({ code, isPhotographed, page }: { code: string; isPhotographed: boolean; page: number }) {
  const { data: vessels } = useGetPortVesselsQuery({ code, page, photographed: isPhotographed })
  const { t } = useTranslation()

  if (!vessels) {
    return null
  }
  if (isPhotographed) {
    return vessels.map((vessel, index) => (
      <VesselCard key={vessel.cfr ?? vessel.mmsi} index={page * PAGE_SIZE + index} vessel={vessel} />
    ))
  }

  return vessels.map(vessel => (
    <li key={vessel.cfr ?? vessel.mmsi}>
      <Link to={getVesselPath(vessel)}>{vessel.name ?? t.common.unnamedVessel}</Link>
      <span>{vessel.registration ?? vessel.cfr}</span>
      <Link to={`/contribute?${vessel.cfr ? `cfr=${vessel.cfr}` : `mmsi=${vessel.mmsi}`}`}>{t.ports.addPhoto}</Link>
    </li>
  ))
}

const Breadcrumb = styled(Container).attrs({ as: 'nav' })`
  padding-top: 20px;
  font-size: 14px;
  color: ${COLORS.muted};
`

const Page = styled(Container)`
  padding-top: 24px;
  padding-bottom: 64px;
  display: flex;
  flex-direction: column;
  gap: 28px;

  > section {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }
`

const Top = styled.div`
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
  gap: 24px;
  align-items: start;

  @media (max-width: 760px) {
    grid-template-columns: minmax(0, 1fr);
  }
`

const Heading = styled.div`
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;

  > div:last-child {
    align-self: stretch;
    margin-top: 12px;
  }

  h1 {
    margin: 0;
    font-family: ${FONTS.display};
    font-size: clamp(32px, 5vw, 52px);
    line-height: 1.05;
  }
`

const Grid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 18px;
`

const Wanted = styled.ul`
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 0 24px;

  li {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto auto;
    align-items: baseline;
    gap: 12px;
    padding: 9px 0;
    border-bottom: 1px solid ${COLORS.divider};

    > a:first-child {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-weight: 500;
    }

    > span {
      font-family: ${FONTS.mono};
      font-size: 13px;
      color: ${COLORS.muted};
    }

    > a:last-child {
      font-size: 14px;
    }
  }
`

const MoreButton = styled(SecondaryButton)`
  align-self: flex-start;
`
