import styled from 'styled-components'

import { COLORS, FONTS } from '../theme'
import { SiteMark } from './SiteMark'
import { SITE_NAME, SITE_TAGLINE } from './texts'

type Props = {
  /** The page as rendered by the server (see PortsContentController): escaped there */
  html: string
}

/**
 * The pages beyond the map (species, methodology): shown as the server rendered them for search engines, so that
 * people and crawlers read the same content. Their links are plain links: each page is rendered by the server.
 */
export function ContentPage({ html }: Props) {
  return (
    <Page>
      <Header>
        <a href="/">
          <SiteMark size={32} />
          <span>
            <strong>{SITE_NAME}</strong>
            <small>{SITE_TAGLINE}</small>
          </span>
        </a>
      </Header>
      {/* oxlint-disable-next-line react/no-danger -- The server escapes every value of the page */}
      <Content dangerouslySetInnerHTML={{ __html: html }} />
    </Page>
  )
}

const Page = styled.div`
  min-height: 100vh;
  background: ${COLORS.ground};
  color: ${COLORS.text};
  font-family: ${FONTS.sans};
  font-variant-numeric: tabular-nums;
`

const Header = styled.header`
  padding: 16px;
  border-bottom: 1px solid ${COLORS.divider};
  background: ${COLORS.white};

  a {
    display: inline-flex;
    gap: 10px;
    align-items: center;
    color: ${COLORS.ink};
    text-decoration: none;
  }

  span {
    display: flex;
    flex-direction: column;
  }

  strong {
    font-family: ${FONTS.sans};
    font-size: 17px;
  }

  small {
    color: ${COLORS.muted};
    font-size: 12.5px;
  }
`

const Content = styled.div`
  max-width: 760px;
  margin: 0 auto;
  padding: 24px 16px 64px;
  font-size: 15px;
  line-height: 1.6;

  h1,
  h2,
  h3 {
    color: ${COLORS.ink};
    font-family: ${FONTS.sans};
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

  @media (width <= 560px) {
    h1 {
      font-size: 24px;
    }

    th,
    td {
      padding: 6px 4px;
      font-size: 13.5px;
    }
  }
`
