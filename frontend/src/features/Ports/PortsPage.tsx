import { lazy, Suspense } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { useGetCoverageQuery, useGetPortsQuery } from '../../api/api'
import { CoverageBar } from '../../components/CoverageBar'
import { Card, Chip, Container, Lead, Muted, Title } from '../../components/ui'
import { usePageMeta } from '../../hooks/usePageMeta'
import { useTranslation } from '../../i18n'
import { COLORS, FONTS } from '../../theme'
import { getCoverageColor } from './coverageColors'

/** Loaded with its coastlines only where a map is shown. */
export const PortsMap = lazy(() => import('./PortsMap').then(module => ({ default: module.PortsMap })))

export function PortsPage() {
  const { data: ports, isError, isLoading } = useGetPortsQuery()
  const { data: coverage } = useGetCoverageQuery()
  const { formatNumber, language, t } = useTranslation()
  const countries = new Intl.DisplayNames([language], { type: 'region' })
  usePageMeta(t.ports.pageTitle)

  return (
    <Page>
      <Chip>{t.ports.chip}</Chip>
      <Title>{t.ports.title}</Title>
      <Lead>{t.ports.lead}</Lead>
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
      {isLoading && <Muted>{t.common.loading}</Muted>}
      {isError && <p>{t.ports.unavailable}</p>}
      {ports && ports.length === 0 && <p>{t.ports.none}</p>}
      {ports && ports.length > 0 && (
        <>
          <Suspense fallback={<MapPlaceholder />}>
            <PortsMap ports={ports} />
          </Suspense>
          <TableBox>
            <table>
              <caption className="visually-hidden">{t.ports.tableCaption}</caption>
              <thead>
                <tr>
                  <th scope="col">{t.ports.port}</th>
                  <th scope="col">{t.ports.country}</th>
                  <th scope="col">{t.ports.vessels}</th>
                  <th scope="col">{t.ports.photographed}</th>
                </tr>
              </thead>
              <tbody>
                {ports.map(port => {
                  const share = port.vessels > 0 ? port.photographed / port.vessels : 0

                  return (
                    <tr key={port.code}>
                      <td>
                        <Link to={`/ports/${port.code}`}>{port.name}</Link>
                      </td>
                      <td>{countries.of(port.country) ?? port.country}</td>
                      <td>{formatNumber(port.vessels)}</td>
                      <td>
                        <Share>
                          <span aria-hidden="true" style={{ background: getCoverageColor(share) }} />
                          {formatNumber(port.photographed)} ({Math.round(share * 100)}%)
                        </Share>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </TableBox>
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

export const MapPlaceholder = styled.div`
  aspect-ratio: 16 / 9;
  border-radius: 10px;
  background: #f8fbfc;
  border: 1px solid ${COLORS.border};
`

const TableBox = styled(Card)`
  overflow-x: auto;

  table {
    width: 100%;
    border-collapse: collapse;
    font-size: 15px;
  }

  th,
  td {
    padding: 10px 14px;
    text-align: left;
    border-bottom: 1px solid ${COLORS.divider};
    white-space: nowrap;
  }

  th {
    font-size: 13px;
    color: ${COLORS.muted};
    font-weight: 500;
  }

  td:nth-child(3),
  td:nth-child(4) {
    font-family: ${FONTS.mono};
    font-size: 14px;
  }
`

const Share = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 8px;

  > span {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    border: 1px solid ${COLORS.ink};
  }
`
