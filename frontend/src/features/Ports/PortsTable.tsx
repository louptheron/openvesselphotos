import { Link, useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import type { Port } from '../../api/types'
import { Card, Input, Muted, SecondaryButton, Select } from '../../components/ui'
import { useTranslation } from '../../i18n'
import { COLORS, FONTS } from '../../theme'
import { getCoverageColor } from './coverageColors'
import { filterPorts, getShare, paginate, PORT_SORTS, type PortSort, sortPorts } from './portsTable'
import { getRegisterCountries, getRegisterCountry } from './regions'

/** All the ports: searchable, sortable by column and paginated, with the state in the URL (?q=&country=&sort=&page=). */
export function PortsTable({ ports }: { ports: Port[] }) {
  const [searchParams, setSearchParams] = useSearchParams()
  const { formatNumber, language, t } = useTranslation()
  const query = searchParams.get('q') ?? ''
  const country = searchParams.get('country') ?? undefined
  const sortParam = searchParams.get('sort') ?? '-vessels'
  const sortName = sortParam.replace(/^-/, '')
  const sort: PortSort = PORT_SORTS.find(item => item === sortName) ?? 'vessels'
  const isDescending = sortParam.startsWith('-')
  const countries = getRegisterCountries(ports)
  const countryNames = new Intl.DisplayNames([language], { type: 'region' })
  const filtered = filterPorts(ports, query, country)
  const { items, page, pageCount } = paginate(
    sortPorts(filtered, sort, isDescending, language),
    Number(searchParams.get('page') ?? 1)
  )

  /** Changes some parameters, back to the first page unless the page is given. */
  const update = (changes: Record<string, string | undefined>) => {
    const next = new URLSearchParams(searchParams)
    next.delete('page')
    Object.entries(changes).forEach(([key, value]) => (value ? next.set(key, value) : next.delete(key)))
    setSearchParams(next, { preventScrollReset: true, replace: true })
  }
  const sortBy = (column: PortSort) =>
    // A second click on the column reverses the order; numbers start with the largest
    update({
      sort: column === sort ? `${isDescending ? '' : '-'}${column}` : column === 'name' ? 'name' : `-${column}`
    })

  const header = (column: PortSort, label: string) => (
    <th aria-sort={column === sort ? (isDescending ? 'descending' : 'ascending') : 'none'} scope="col">
      <SortButton onClick={() => sortBy(column)} type="button">
        {label}
        <span aria-hidden="true">{column === sort ? (isDescending ? ' ↓' : ' ↑') : ''}</span>
      </SortButton>
    </th>
  )

  return (
    <Section aria-labelledby="ports-table-title">
      <h2 id="ports-table-title">{t.ports.tableTitle(formatNumber(ports.length))}</h2>
      <Filters>
        <label className="visually-hidden" htmlFor="ports-search">
          {t.ports.searchLabel}
        </label>
        <Input
          id="ports-search"
          onChange={event => update({ q: event.target.value })}
          placeholder={t.ports.searchPlaceholder}
          type="search"
          value={query}
        />
        {countries.length > 1 && (
          <>
            <label className="visually-hidden" htmlFor="ports-country">
              {t.ports.country}
            </label>
            <Select
              id="ports-country"
              onChange={event => update({ country: event.target.value })}
              value={country ?? ''}
            >
              <option value="">{t.ports.allCountries}</option>
              {countries.map(code => (
                <option key={code} value={code}>
                  {countryNames.of(code) ?? code}
                </option>
              ))}
            </Select>
          </>
        )}
        <Muted aria-live="polite">{t.ports.shown(formatNumber(filtered.length), formatNumber(ports.length))}</Muted>
      </Filters>

      {filtered.length === 0 ? (
        <p>{t.ports.noMatch(query)}</p>
      ) : (
        <TableBox>
          <table>
            <caption className="visually-hidden">{t.ports.tableCaption}</caption>
            <thead>
              <tr>
                {header('name', t.ports.port)}
                <th scope="col">{t.ports.country}</th>
                {header('vessels', t.ports.vessels)}
                {header('photographed', t.ports.photographed)}
                {header('share', t.ports.share)}
              </tr>
            </thead>
            <tbody>
              {items.map(port => {
                const share = getShare(port)

                return (
                  <tr key={port.code}>
                    <td>
                      <Link to={`/ports/${port.code}`}>{port.name}</Link>
                      <Code>{port.code}</Code>
                    </td>
                    <td>
                      {countryNames.of(port.country) ?? port.country}
                      {port.country !== getRegisterCountry(port) && (
                        <Muted> ({countryNames.of(getRegisterCountry(port))})</Muted>
                      )}
                    </td>
                    <td>{formatNumber(port.vessels)}</td>
                    <td>{formatNumber(port.photographed)}</td>
                    <td>
                      <ShareBar aria-hidden="true">
                        <span style={{ background: getCoverageColor(share), width: `${Math.max(share * 100, 2)}%` }} />
                      </ShareBar>
                      {Math.round(share * 100)}%
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </TableBox>
      )}

      {pageCount > 1 && (
        <Pagination aria-label={t.ports.pagination}>
          <SecondaryButton disabled={page <= 1} onClick={() => update({ page: String(page - 1) })} type="button">
            {t.ports.previous}
          </SecondaryButton>
          <span>{t.ports.pageOf(page, pageCount)}</span>
          <SecondaryButton
            disabled={page >= pageCount}
            onClick={() => update({ page: String(page + 1) })}
            type="button"
          >
            {t.ports.next}
          </SecondaryButton>
        </Pagination>
      )}
    </Section>
  )
}

const Section = styled.section`
  display: flex;
  flex-direction: column;
  gap: 14px;

  h2 {
    margin: 0;
    font-family: ${FONTS.display};
    font-size: 26px;
  }
`

const Filters = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 14px;

  input {
    flex: 1 1 260px;
    max-width: 420px;
  }
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

  td:nth-child(n + 3) {
    font-family: ${FONTS.mono};
    font-size: 14px;
  }

  tbody tr:hover {
    background: ${COLORS.ground};
  }
`

const SortButton = styled.button`
  border: none;
  background: none;
  padding: 0;
  font: inherit;
  color: inherit;
  cursor: pointer;

  &:hover {
    color: ${COLORS.ink};
    text-decoration: underline;
  }
`

const Code = styled.span`
  margin-left: 8px;
  font-family: ${FONTS.mono};
  font-size: 12px;
  color: ${COLORS.muted};
`

const ShareBar = styled.span`
  display: inline-block;
  vertical-align: middle;
  width: 60px;
  height: 8px;
  margin-right: 8px;
  border-radius: 4px;
  background: ${COLORS.divider};
  overflow: hidden;

  > span {
    display: block;
    height: 100%;
  }
`

const Pagination = styled.nav`
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 14px;
  color: ${COLORS.text};
`
