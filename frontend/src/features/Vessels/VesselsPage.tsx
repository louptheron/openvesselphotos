import { Link, useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import { useGetVesselsPageQuery } from '../../api/api'
import { VesselCard, VesselGrid } from '../../components/VesselCard'
import { Container, Lead, Muted, Title } from '../../components/ui'
import { usePageMeta } from '../../hooks/usePageMeta'
import { useTranslation } from '../../i18n'
import { NotFoundPage } from '../NotFound/NotFoundPage'

/** Every vessel with photos, by name: also links each vessel page for search engines (see PagesController). */
export function VesselsPage() {
  const [searchParams] = useSearchParams()
  const page = Number(searchParams.get('page') ?? 1)
  const { t } = useTranslation()
  const { data, isError, isFetching } = useGetVesselsPageQuery(page, { skip: !Number.isInteger(page) || page < 1 })
  usePageMeta(data && t.vessels.pageTitle(data.page, data.pageCount))

  if (!Number.isInteger(page) || page < 1 || (data && data.vessels.length === 0 && page > 1)) {
    return <NotFoundPage />
  }

  return (
    <Wrapper>
      <div>
        <Title>{t.vessels.title}</Title>
        {data && <Lead>{t.vessels.lead(data.total)}</Lead>}
      </div>
      {isError && <p>{t.vessels.unavailable}</p>}
      {isFetching && <Muted>{t.common.loading}</Muted>}
      {data && (
        <>
          <VesselGrid>
            {data.vessels.map((vessel, index) => (
              <VesselCard key={vessel.cfr ?? vessel.mmsi} index={index} vessel={vessel} />
            ))}
          </VesselGrid>
          {data.pageCount > 1 && (
            <Pagination aria-label={t.vessels.pagination}>
              {page > 1 ? <Link to={pagePath(page - 1)}>{t.vessels.previous}</Link> : <span />}
              <Muted>{t.vessels.pageOf(page, data.pageCount)}</Muted>
              {page < data.pageCount ? <Link to={pagePath(page + 1)}>{t.vessels.next}</Link> : <span />}
            </Pagination>
          )}
        </>
      )}
    </Wrapper>
  )
}

/** Same URLs as the server's (PagesController.directoryPath) */
function pagePath(page: number): string {
  return page <= 1 ? '/vessels' : `/vessels?page=${page}`
}

const Wrapper = styled(Container)`
  padding-top: 48px;
  padding-bottom: 96px;
  display: flex;
  flex-direction: column;
  gap: 24px;
`

const Pagination = styled.nav`
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
`
