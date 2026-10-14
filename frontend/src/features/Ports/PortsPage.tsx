import { Suspense } from 'react'
import styled from 'styled-components'

import { useGetCoverageQuery, useGetPortsQuery } from '../../api/api'
import { CoverageBar } from '../../components/CoverageBar'
import { Chip, Container, Lead, Muted, Title } from '../../components/ui'
import { usePageMeta } from '../../hooks/usePageMeta'
import { useTranslation } from '../../i18n'
import { MapPlaceholder, PortsMap } from './LazyPortsMap'
import { PortsTable } from './PortsTable'
import { ScopeNote } from './ScopeNote'

export function PortsPage() {
  const { data: ports, isError, isLoading } = useGetPortsQuery()
  const { data: coverage } = useGetCoverageQuery()
  const { formatNumber, t } = useTranslation()
  usePageMeta(t.ports.pageTitle)

  return (
    <Page>
      <Chip>{t.ports.chip}</Chip>
      <Title>{t.ports.title}</Title>
      <Lead>{t.ports.lead}</Lead>
      {ports && ports.length > 0 && <ScopeNote ports={ports} />}
      {coverage && coverage.registerVessels > 0 && (
        <CoverageBar
          label={t.coverage.register(
            formatNumber(coverage.registerPhotographed),
            formatNumber(coverage.registerVessels),
            coverage.registerPhotographed
          )}
          photographed={coverage.registerPhotographed}
          total={coverage.registerVessels}
        />
      )}
      {isLoading && <MapPlaceholder aria-busy="true" />}
      {isError && <p>{t.ports.unavailable}</p>}
      {ports && ports.length === 0 && <Muted>{t.ports.none}</Muted>}
      {ports && ports.length > 0 && (
        <>
          <Suspense fallback={<MapPlaceholder />}>
            <PortsMap ports={ports} />
          </Suspense>
          <PortsTable ports={ports} />
        </>
      )}
    </Page>
  )
}

const Page = styled(Container)`
  padding-top: 40px;
  padding-bottom: 64px;
  display: flex;
  flex-direction: column;
  gap: 20px;
`
