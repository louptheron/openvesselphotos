import { Link, NavLink, Outlet } from 'react-router-dom'
import styled from 'styled-components'

import { COLORS, FONTS } from '../theme'
import { LogoMark } from './Logo'
import { Container } from './ui'

export function Layout() {
  return (
    <>
      <Header>
        <Brand to="/">
          <LogoMark />
          <span>Open Vessel Photos</span>
        </Brand>
        <Nav aria-label="Main">
          <NavItem end to="/">
            Search
          </NavItem>
          <NavItem to="/docs">API</NavItem>
          <NavItem to="/contribute">Contribute</NavItem>
          {/* A plain link: as a NavLink, it would be highlighted together with "API" on /docs */}
          <NavItem as={Link} to="/docs#licences">
            Licences
          </NavItem>
        </Nav>
      </Header>
      <main>
        <Outlet />
      </main>
      <Footer>
        <FooterInner>
          <span>Open Vessel Photos · open-source code · photos under their own open licences</span>
          <span>
            <a href="/v1/export.json">Download the dataset</a> · <Link to="/docs">API</Link> ·{' '}
            <Link to="/api-key">API key</Link> · <Link to="/admin">Review</Link>
          </span>
        </FooterInner>
      </Footer>
    </>
  )
}

const Header = styled(Container).attrs({ as: 'header' })`
  padding-top: 18px;
  padding-bottom: 18px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
`

const Brand = styled(Link)`
  display: flex;
  align-items: center;
  min-height: 44px;
  gap: 10px;
  text-decoration: none;
  color: ${COLORS.ink};
  font-family: ${FONTS.display};
  font-weight: 700;
  font-size: 22px;

  &:hover {
    color: ${COLORS.ink};
  }
`

const Nav = styled.nav`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  font-size: 15px;
`

const NavItem = styled(NavLink)`
  text-decoration: none;
  color: ${COLORS.ink};
  padding: 12px;

  &.active {
    font-weight: 600;
    box-shadow: inset 0 -2px 0 ${COLORS.accent};
  }
`

const Footer = styled.footer`
  border-top: 1px solid ${COLORS.border};
  margin-top: 24px;
`

const FooterInner = styled(Container)`
  padding-top: 20px;
  padding-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  color: ${COLORS.muted};
`
