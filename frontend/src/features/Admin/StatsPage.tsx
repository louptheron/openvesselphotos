import { useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { getErrorMessage, getErrorStatus, useGetUsageQuery } from '../../api/api'
import type { DailyUsage, SourceUsage } from '../../api/types'
import { Card, Container, ErrorMessage, Muted, Pill, SectionTitle, Title } from '../../components/ui'
import { usePageMeta } from '../../hooks/usePageMeta'
import { useTranslation } from '../../i18n'
import { COLORS, FONTS } from '../../theme'
import { LoginForm } from './AdminPage'

const PERIODS = [7, 30, 90, 365]

/** Usage of the photos (by the sites showing them) and of the API (by key), for the admin. */
export function StatsPage() {
  const [days, setDays] = useState(30)
  const { data: stats, error, isFetching, isLoading } = useGetUsageQuery(days)
  const { formatDate, language, t } = useTranslation()
  usePageMeta(t.stats.pageTitle)
  const format = (value: number) => value.toLocaleString(language === 'fr' ? 'fr-FR' : 'en-GB')

  if (getErrorStatus(error) === 401) {
    return <LoginForm />
  }

  const sourceName = (source: string) =>
    ({ direct: t.stats.direct, other: t.stats.other, website: t.stats.website })[source] ?? source

  return (
    <Page>
      <Header>
        <Title as="h1">{t.stats.title}</Title>
        <Link to="/admin">{t.stats.backToQueue}</Link>
      </Header>
      <Periods aria-label={t.stats.period}>
        {PERIODS.map(period => (
          <Pill
            key={period}
            $active={days === period}
            aria-pressed={days === period}
            onClick={() => setDays(period)}
            type="button"
          >
            {t.stats.lastDays(period)}
          </Pill>
        ))}
        {isFetching && <Muted>{t.common.loading}</Muted>}
      </Periods>
      {error && <ErrorMessage>{getErrorMessage(error, t)}</ErrorMessage>}
      {isLoading && <Muted>{t.common.loading}</Muted>}
      {stats && (
        <>
          <Figures>
            <Figure>
              <strong>{format(stats.imageViews)}</strong>
              <span>{t.stats.imageViews}</span>
              <Muted>{t.stats.imageViewsElsewhere(format(stats.imageViewsElsewhere), stats.siteCount)}</Muted>
            </Figure>
            <Figure>
              <strong>{format(stats.apiCalls)}</strong>
              <span>{t.stats.apiCalls}</span>
              <Muted>{t.stats.apiCallsWithKey(format(stats.apiCallsWithKey))}</Muted>
            </Figure>
            <Figure>
              <strong>
                {stats.activeApiKeys} / {stats.activeApiKeysTotal}
              </strong>
              <span>{t.stats.activeKeys}</span>
              <Muted>{t.stats.activeKeysHint}</Muted>
            </Figure>
            <Figure>
              <strong>{format(stats.photoCount)}</strong>
              <span>{t.stats.photos}</span>
              <Muted>{t.stats.vessels(format(stats.vesselCount))}</Muted>
            </Figure>
          </Figures>

          <Charts>
            <DailyChart color={COLORS.accent} daily={stats.daily} title={t.stats.imageViewsPerDay} value="imageViews" />
            <DailyChart color={COLORS.link} daily={stats.daily} title={t.stats.apiCallsPerDay} value="apiCalls" />
            <DailyChart
              color={COLORS.ink}
              daily={stats.daily}
              title={t.stats.vesselsAddedPerDay(format(stats.vesselsAdded))}
              value="vesselsAdded"
            />
          </Charts>

          <Tables>
            <SourceTable
              empty={t.stats.noData}
              format={format}
              name={sourceName}
              sources={stats.imageSources}
              title={t.stats.imageSources}
            />
            <section>
              <SectionTitle>{t.stats.apiKeys}</SectionTitle>
              {stats.apiKeys.length === 0 ? (
                <Muted>{t.stats.noKeyCalls}</Muted>
              ) : (
                <Table>
                  <thead>
                    <tr>
                      <th>{t.stats.key}</th>
                      <th>{t.stats.calls}</th>
                      <th>{t.stats.lastUsed}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {stats.apiKeys.map(key => (
                      <tr key={key.id}>
                        <td>
                          {key.name}
                          {key.organisation && <Muted> · {key.organisation}</Muted>}
                        </td>
                        <td>{format(key.calls)}</td>
                        <td>{formatDate(key.lastUsedAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              )}
            </section>
            <SourceTable
              empty={t.stats.noData}
              format={format}
              name={sourceName}
              sources={stats.apiSources}
              title={t.stats.apiSources}
            />
          </Tables>
          <Muted>{t.stats.note}</Muted>
        </>
      )}
    </Page>
  )
}

function DailyChart({
  color,
  daily,
  title,
  value
}: {
  color: string
  daily: DailyUsage[]
  title: string
  value: 'apiCalls' | 'imageViews' | 'vesselsAdded'
}) {
  const { formatDate } = useTranslation()
  const max = Math.max(1, ...daily.map(day => day[value]))
  const barWidth = 100 / daily.length

  return (
    <ChartCard>
      <h2>{title}</h2>
      <svg aria-hidden="true" preserveAspectRatio="none" viewBox="0 0 100 40">
        {daily.map((day, index) => {
          const height = (day[value] / max) * 38

          return (
            <rect
              key={day.day}
              fill={color}
              height={height}
              width={Math.max(barWidth * 0.75, 0.2)}
              x={index * barWidth}
              y={40 - height}
            >
              <title>{`${formatDate(day.day)} : ${day[value]}`}</title>
            </rect>
          )
        })}
      </svg>
      <Axis>
        <span>{formatDate(daily[0]?.day)}</span>
        <span>max {max.toLocaleString()}</span>
        <span>{formatDate(daily.at(-1)?.day)}</span>
      </Axis>
    </ChartCard>
  )
}

function SourceTable({
  empty,
  format,
  name,
  sources,
  title
}: {
  empty: string
  format: (value: number) => string
  name: (source: string) => string
  sources: SourceUsage[]
  title: string
}) {
  const { t } = useTranslation()

  return (
    <section>
      <SectionTitle>{title}</SectionTitle>
      {sources.length === 0 ? (
        <Muted>{empty}</Muted>
      ) : (
        <Table>
          <thead>
            <tr>
              <th>{t.stats.site}</th>
              <th>{t.stats.count}</th>
            </tr>
          </thead>
          <tbody>
            {sources.map(source => (
              <tr key={source.source}>
                <td>
                  {source.source.includes('.') ? (
                    <a href={`https://${source.source}`} rel="noreferrer nofollow" target="_blank">
                      {source.source}
                    </a>
                  ) : (
                    name(source.source)
                  )}
                </td>
                <td>{format(source.count)}</td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
    </section>
  )
}

const Page = styled(Container)`
  padding-top: 24px;
  padding-bottom: 48px;
  display: flex;
  flex-direction: column;
  gap: 20px;
`

const Header = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  h1 {
    font-size: 36px;
  }

  a {
    padding: 12px 0;
  }
`

const Periods = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
`

const Figures = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 14px;
`

const Figure = styled(Card)`
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;

  strong {
    font-family: ${FONTS.display};
    font-size: 34px;
    line-height: 1.1;
  }

  span {
    font-weight: 500;
  }
`

const Charts = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 14px;
`

const ChartCard = styled(Card)`
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;

  h2 {
    margin: 0;
    font-family: ${FONTS.display};
    font-size: 18px;
  }

  svg {
    width: 100%;
    height: 140px;
    border-bottom: 1px solid ${COLORS.border};
  }
`

const Axis = styled.div`
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: ${COLORS.muted};
`

const Tables = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;

  section {
    display: flex;
    flex-direction: column;
    gap: 10px;
    min-width: 0;
  }

  h2 {
    font-size: 20px;
  }
`

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;

  th,
  td {
    padding: 8px 6px;
    border-bottom: 1px solid ${COLORS.border};
    text-align: left;
    overflow-wrap: anywhere;
  }

  th:not(:first-child),
  td:not(:first-child) {
    text-align: right;
    white-space: nowrap;
  }

  th {
    font-weight: 600;
    color: ${COLORS.muted};
  }
`
