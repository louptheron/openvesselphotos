import { useMemo } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import { useGetVesselFilterOptionsQuery, useGetVesselsPageQuery } from '../../api/api'
import type { FilterOption, VesselFilter } from '../../api/types'
import { VesselCard, VesselGrid } from '../../components/VesselCard'
import { Container, Field, Label, Lead, Muted, Select, TextButton, Title } from '../../components/ui'
import { usePageMeta } from '../../hooks/usePageMeta'
import { useTranslation } from '../../i18n'
import { NotFoundPage } from '../NotFound/NotFoundPage'

/** The query parameters of the filters, as on the server (PagesController.directoryPath) */
const FILTER_KEYS = ['flag', 'port', 'vesselType', 'mainGear'] as const

type FilterKey = (typeof FILTER_KEYS)[number]

/**
 * Every vessel with photos, by name, filtered by flag, port, type and gear: also links each vessel page for search
 * engines (see PagesController). The filters are menus, not links, so that crawlers don't follow every combination.
 */
export function VesselsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const page = Number(searchParams.get('page') ?? 1)
  const filter: VesselFilter = Object.fromEntries(
    FILTER_KEYS.flatMap(key => (searchParams.get(key) ? [[key, searchParams.get(key)]] : []))
  )
  const isFiltered = Object.keys(filter).length > 0
  const isValidPage = Number.isInteger(page) && page >= 1
  const { language, t } = useTranslation()
  const { data, isError, isFetching } = useGetVesselsPageQuery({ ...filter, page }, { skip: !isValidPage })
  const { data: options } = useGetVesselFilterOptionsQuery()
  const title = isFiltered ? t.vessels.filteredTitle : t.vessels.title
  usePageMeta(data && (isFiltered ? title : t.vessels.pageTitle(data.page, data.pageCount)))

  const countryNames = useMemo(() => new Intl.DisplayNames([language], { type: 'region' }), [language])
  const labels: Record<FilterKey, (value: string, option?: FilterOption) => string> = {
    flag: (value, option) => (option?.countryCode && countryNames.of(option.countryCode)) || value,
    mainGear: value => t.register.gears[value] ?? value,
    port: value => value,
    vesselType: value => t.register.vesselTypes[value] ?? value
  }
  const filters: { key: FilterKey; label: string; options: FilterOption[] }[] = [
    { key: 'flag', label: t.vessels.flag, options: options?.flags ?? [] },
    { key: 'port', label: t.register.frenchPort, options: options?.ports ?? [] },
    { key: 'vesselType', label: t.register.type, options: options?.vesselTypes ?? [] },
    { key: 'mainGear', label: t.register.mainGear, options: options?.mainGears ?? [] }
  ]

  /** A filter's new value, back to the first page */
  const setFilter = (key: FilterKey, value: string) => {
    const params = new URLSearchParams(searchParams)
    params.delete('page')
    if (value) {
      params.set(key, value)
    } else {
      params.delete(key)
    }
    setSearchParams(params)
  }

  const pagePath = (number: number): string => {
    const params = new URLSearchParams(searchParams)
    params.delete('page')
    if (number > 1) {
      params.set('page', String(number))
    }
    const query = params.toString()

    return query ? `/vessels?${query}` : '/vessels'
  }

  if (!isValidPage || (data && data.vessels.length === 0 && page > 1)) {
    return <NotFoundPage />
  }

  return (
    <Wrapper>
      <div>
        <Title>{title}</Title>
        {data && <Lead>{t.vessels.lead(data.total)}</Lead>}
      </div>
      <Filters aria-label={t.vessels.filters}>
        {filters.map(({ key, label, options: values }) => (
          <Field key={key}>
            <Label htmlFor={`filter-${key}`}>{label}</Label>
            <Select
              disabled={values.length === 0 && !filter[key]}
              id={`filter-${key}`}
              onChange={event => setFilter(key, event.target.value)}
              value={filter[key] ?? ''}
            >
              <option value="">{t.vessels.any}</option>
              {values
                .map(option => ({ label: labels[key](option.value, option), option }))
                // Sorts the copy made by map(), by translated label
                // oxlint-disable-next-line unicorn/no-array-sort
                .sort((a, b) => a.label.localeCompare(b.label, language))
                .map(({ label: optionLabel, option }) => (
                  <option key={option.value} value={option.value}>
                    {optionLabel} ({option.count})
                  </option>
                ))}
            </Select>
          </Field>
        ))}
        {isFiltered && (
          <TextButton onClick={() => setSearchParams({})} type="button">
            {t.vessels.clearFilters}
          </TextButton>
        )}
      </Filters>
      {isError && <p>{t.vessels.unavailable}</p>}
      {isFetching && <Muted>{t.common.loading}</Muted>}
      {data && data.vessels.length === 0 && <p>{t.vessels.noMatch}</p>}
      {data && data.vessels.length > 0 && (
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

const Wrapper = styled(Container)`
  padding-top: 48px;
  padding-bottom: 96px;
  display: flex;
  flex-direction: column;
  gap: 24px;
`

const Filters = styled.section`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  align-items: end;
  gap: 12px 16px;
`

const Pagination = styled.nav`
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
`
