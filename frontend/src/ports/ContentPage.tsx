import styled, { css } from 'styled-components'

import { COLORS } from '../theme'
import { glass, MOBILE_QUERY, PANEL_MARGIN } from './layout'
import { MapBackdrop } from './MapBackdrop'
import { SiteMark } from './SiteMark'
import { STOCK_PILL_CSS, STOCK_STATUS_RULES } from './stockStatus'
import { SITE_NAME, SITE_TAGLINE } from './texts'

type Props = {
  /** The page as rendered by the server (see PortsContentController): escaped there */
  html: string
}

/** The pages of the site, in the header of every page beyond the map */
const SECTIONS = [
  { label: 'Carte des ports', path: '/' },
  { label: 'Espèces', path: '/especes' },
  { label: 'Petite pêche', path: '/petite-peche-cotiere' },
  { label: 'Méthodologie', path: '/methodologie' },
  { label: 'Mises à jour', path: '/mises-a-jour' }
]

/**
 * The pages beyond the map (species, small-scale fisheries, methodology…), framed as the map is: the same floating
 * panels over its basemap, the same mark, the same pills. Their content is shown as the server rendered it for search
 * engines, so that people and crawlers read the same text. Their links are plain links: each page is rendered by the
 * server.
 */
export function ContentPage({ html }: Props) {
  const { pathname } = window.location
  const current = SECTIONS.find(({ path }) => path !== '/' && pathname.startsWith(path))

  return (
    <Page>
      <MapBackdrop />
      <Header>
        <Brand href="/">
          <BrandMark>
            <SiteMark size={32} />
          </BrandMark>
          <span>
            <BrandName>{SITE_NAME}</BrandName>
            <BrandTagline>{SITE_TAGLINE}</BrandTagline>
          </span>
        </Brand>
        <Nav aria-label="Pages du site">
          {SECTIONS.map(({ label, path }) => (
            <NavLink
              $isCurrent={current?.path === path}
              aria-current={current?.path === path ? 'page' : undefined}
              href={path}
              key={path}
            >
              {label}
            </NavLink>
          ))}
        </Nav>
      </Header>
      <Panel>
        {/* oxlint-disable-next-line react/no-danger -- The server escapes every value of the page */}
        <Content dangerouslySetInnerHTML={{ __html: html }} />
        <Footer>
          <a href="/">Carte des ports</a> · <a href="/especes">Espèces et prix</a> ·{' '}
          <a href="/methodologie">Méthodologie et sources</a> · Photos de{' '}
          <a href="https://openvesselphotos.org/fr" rel="noopener" target="_blank">
            Open Vessel Photos
          </a>{' '}
          · Fond de carte © OpenStreetMap
        </Footer>
      </Panel>
    </Page>
  )
}

const Page = styled.div`
  position: relative;
  min-height: 100vh;
  padding: ${PANEL_MARGIN}px;
  color: ${COLORS.text};
  font-variant-numeric: tabular-nums;

  @media ${MOBILE_QUERY} {
    padding: 8px;
  }
`

/** As the panel and the gear filter of the map, floating over it */
const Header = styled.header`
  ${glass}
  position: sticky;
  top: ${PANEL_MARGIN}px;
  z-index: 2;
  display: flex;
  flex-wrap: wrap;
  gap: 10px 24px;
  align-items: center;
  justify-content: space-between;
  max-width: 1120px;
  margin: 0 auto;
  padding: 10px 12px 10px 14px;
  border-radius: 6px;

  @media ${MOBILE_QUERY} {
    position: static;
  }
`

const Brand = styled.a`
  display: flex;
  align-items: center;
  gap: 12px;
  color: ${COLORS.ink};
  text-decoration: none;
`

const BrandMark = styled.span`
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 4px;
  background: ${COLORS.ink};
`

const BrandName = styled.span`
  display: block;
  font-size: 19px;
  font-weight: 600;
  letter-spacing: -0.01em;
`

const BrandTagline = styled.span`
  display: block;
  color: ${COLORS.muted};
  font-size: 12.5px;
`

/** As the gear filter of the map: the current page in ink. On phones, one line to swipe. */
const Nav = styled.nav`
  display: flex;
  flex-wrap: wrap;
  gap: 2px;

  @media ${MOBILE_QUERY} {
    flex-wrap: nowrap;
    width: 100%;
    overflow-x: auto;
    scrollbar-width: none;
  }
`

const NavLink = styled.a<{ $isCurrent: boolean }>`
  flex: none;
  padding: 8px 12px;
  border-radius: 4px;
  color: ${COLORS.ink};
  font-size: 13.5px;
  font-weight: 500;
  text-decoration: none;
  transition: background 0.15s;

  &:hover {
    background: ${COLORS.white};
  }

  ${({ $isCurrent }) =>
    $isCurrent &&
    css`
      &,
      &:hover {
        background: ${COLORS.ink};
        color: ${COLORS.white};
      }
    `}
`

const Panel = styled.main`
  ${glass}
  position: relative;
  z-index: 1;
  max-width: 860px;
  margin: ${PANEL_MARGIN}px auto 0;
  border-radius: 6px;
`

const Footer = styled.footer`
  padding: 12px 32px 14px;
  border-top: 1px solid ${COLORS.divider};
  color: ${COLORS.muted};
  font-size: 12px;

  a {
    color: ${COLORS.muted};
  }

  @media ${MOBILE_QUERY} {
    padding: 12px 16px 14px;
  }
`

const Content = styled.div`
  padding: 24px 32px 40px;
  font-size: 15px;
  line-height: 1.6;

  h1,
  h2,
  h3 {
    color: ${COLORS.ink};
    font-weight: 600;
    line-height: 1.25;
  }

  h1 {
    font-size: 30px;
    letter-spacing: -0.01em;
  }

  h2 {
    margin-top: 36px;
    font-size: 21px;
  }

  h3 {
    margin-top: 20px;
    font-size: 16px;
  }

  a {
    color: ${COLORS.link};
  }

  a:hover {
    color: ${COLORS.linkHover};
  }

  nav:first-child {
    color: ${COLORS.muted};
    font-size: 13px;
  }

  table {
    width: 100%;
    border-collapse: collapse;
    font-variant-numeric: tabular-nums;
  }

  th,
  td {
    padding: 6px 8px;
    border-bottom: 1px solid ${COLORS.divider};
    text-align: left;
  }

  td {
    text-align: right;
    white-space: nowrap;
  }

  thead th {
    color: ${COLORS.muted};
    font-size: 12.5px;
    font-weight: 500;
  }

  /* The status of the stocks of each species (see stockStatus) */
  th.stock,
  td.stock {
    text-align: left;
  }

  .stock-status {
    ${STOCK_PILL_CSS}
    font-size: 12.5px;
  }

  ${STOCK_STATUS_RULES}

  /* Key figures (see ContentCharts.tiles) */
  .stat-tiles {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
    gap: 12px;
    margin: 24px 0 6px;
  }

  .stat-tile {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 14px 16px;
    border: 1px solid ${COLORS.cardBorder};
    border-radius: 8px;
    background: ${COLORS.white};
    line-height: 1.35;
  }

  .stat-tile strong {
    color: ${COLORS.ink};
    font-size: 30px;
    font-variant-numeric: proportional-nums;
    font-weight: 600;
    letter-spacing: -0.01em;
  }

  .stat-tile span {
    color: ${COLORS.muted};
    font-size: 13px;
  }

  /* Charts (see ContentCharts) */
  .chart {
    margin: 20px 0 28px;
    padding: 16px 18px 12px;
    border: 1px solid ${COLORS.cardBorder};
    border-radius: 8px;
    background: ${COLORS.white};
  }

  .chart-title {
    margin-bottom: 12px;
    color: ${COLORS.ink};
    font-size: 15px;
    font-weight: 600;
    line-height: 1.4;
  }

  .chart-legend {
    display: flex;
    flex-wrap: wrap;
    gap: 6px 16px;
    margin: 0 0 12px;
    padding: 0;
    color: ${COLORS.text};
    font-size: 13px;
    list-style: none;
  }

  .chart-legend li {
    display: flex;
    gap: 6px;
    align-items: center;
  }

  .chart-legend i {
    width: 10px;
    height: 10px;
    border-radius: 2px;
  }

  .chart-row {
    display: grid;
    /* The value column has the same width on every row, so that the tracks line up */
    grid-template-columns: minmax(120px, 34%) 1fr 8.5em;
    gap: 12px;
    align-items: center;
    margin: 8px 0;
    font-size: 13.5px;
    line-height: 1.3;
  }

  .chart-label {
    color: ${COLORS.text};
  }

  .chart-value {
    color: ${COLORS.ink};
    font-weight: 500;
    text-align: right;
    white-space: nowrap;
  }

  /* A whole split into parts: no baseline, a 2px gap between parts */
  .chart-stack {
    display: flex;
    grid-column: 2 / 4;
    gap: 2px;
    height: 28px;
  }

  .chart-part {
    display: flex;
    align-items: center;
    overflow: hidden;
    padding-left: 6px;
    border-radius: 4px;
    font-size: 12.5px;
    font-weight: 500;
    white-space: nowrap;
  }

  .chart-track {
    position: relative;
    height: 14px;
    border-radius: 4px;
    background: ${COLORS.divider};
  }

  .chart-bar,
  .chart-range {
    position: absolute;
    top: 0;
    bottom: 0;
    min-width: 8px;
    border-radius: 4px;
  }

  .chart-bar {
    left: 0;
    border-radius: 0 4px 4px 0;
  }

  .chart-note,
  .chart-source {
    margin: 8px 0 0;
    color: ${COLORS.muted};
    font-size: 12.5px;
    line-height: 1.5;
  }

  .chart details {
    margin-top: 6px;
    font-size: 12.5px;
  }

  .chart summary {
    color: ${COLORS.link};
    cursor: pointer;
  }

  .chart table {
    margin-top: 6px;
  }

  @media (width <= 560px) {
    padding: 20px 16px 32px;

    h1 {
      font-size: 24px;
    }

    /* The label above its bar */
    .chart-row {
      grid-template-columns: 1fr 8.5em;
      gap: 4px 10px;
    }

    .chart-label {
      grid-column: 1 / 3;
    }

    .chart-stack {
      grid-column: 1 / 3;
    }

    th,
    td {
      padding: 6px 4px;
      font-size: 13.5px;
    }
  }
`
