import { Link, NavLink, Outlet } from 'react-router-dom'
import styled from 'styled-components'

import { COLORS, FONTS } from '../theme'
import { LogoMark } from './Logo'
import { useTranslation } from '../i18n'
import { Container } from './ui'

export function Layout() {
  const { language, setLanguage, t } = useTranslation()

  return (
    <>
      <Header>
        <Brand to="/">
          <LogoMark />
          <span>Open Vessel Photos</span>
        </Brand>
        <Nav aria-label={t.layout.navLabel}>
          <NavItem end to="/">
            {t.layout.search}
          </NavItem>
          <NavItem to="/docs">{t.layout.api}</NavItem>
          <NavItem to="/contribute">{t.layout.contribute}</NavItem>
        </Nav>
        <LanguageButton
          aria-label={t.language.switchLabel}
          lang={language === 'en' ? 'fr' : 'en'}
          onClick={() => setLanguage(language === 'en' ? 'fr' : 'en')}
          type="button"
        >
          {t.language.switchText}
        </LanguageButton>
      </Header>
      <main>
        <Outlet />
      </main>
      <Footer>
        <FooterInner>
          <span>
            Open Vessel Photos ·{' '}
            <a href="https://github.com/louptheron/openvesselphotos" rel="noreferrer" target="_blank">
              {t.layout.footerCode}
            </a>{' '}
            {t.layout.footerUnder}{' '}
            <a href="https://github.com/louptheron/openvesselphotos/blob/main/LICENSE" rel="noreferrer" target="_blank">
              AGPL-3.0
            </a>{' '}
            · {t.layout.footerPhotos}
          </span>
          <span>
            <Link to="/vessels">{t.layout.allVessels}</Link> · <a href="/v1/export.json">{t.layout.dataset}</a> ·{' '}
            <Link to="/docs">{t.layout.api}</Link> · <Link to="/api-key">{t.layout.apiKey}</Link> ·{' '}
            <Link to="/admin">{t.layout.review}</Link>
          </span>
        </FooterInner>
      </Footer>
    </>
  )
}

/** The widest screen where the logo, the links and the language button don't fit on one row */
const NARROW_HEADER_MAX_WIDTH = 679

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
  /* Smaller on the narrowest phones, to keep the language button on the same row */
  font-size: clamp(18px, 5.6vw, 22px);

  &:hover {
    color: ${COLORS.ink};
  }
`

const Nav = styled.nav`
  margin-left: auto;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  font-size: 15px;

  /* Below the width where everything fits on one row, the links go under the logo and the language button */
  @media (max-width: ${NARROW_HEADER_MAX_WIDTH}px) {
    order: 1;
    width: 100%;
    margin-left: -8px;
    gap: 0;
  }
`

const NavItem = styled(NavLink)`
  text-decoration: none;
  color: ${COLORS.ink};
  padding: 12px;

  @media (max-width: ${NARROW_HEADER_MAX_WIDTH}px) {
    padding: 12px 8px;
  }

  &.active {
    font-weight: 600;
    box-shadow: inset 0 -2px 0 ${COLORS.accent};
  }
`

const LanguageButton = styled.button`
  min-width: 44px;
  min-height: 44px;
  padding: 0 10px;
  border: 1px solid ${COLORS.inputBorder};
  border-radius: 22px;
  background: ${COLORS.white};
  color: ${COLORS.ink};
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;

  @media (max-width: ${NARROW_HEADER_MAX_WIDTH}px) {
    margin-left: auto;
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
