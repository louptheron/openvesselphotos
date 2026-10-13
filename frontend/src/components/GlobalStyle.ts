import { createGlobalStyle } from 'styled-components'

import { COLORS, FONTS } from '../theme'

export const GlobalStyle = createGlobalStyle`
  *, *::before, *::after { box-sizing: border-box; }

  body {
    margin: 0;
    background: ${COLORS.ground};
    color: ${COLORS.ink};
    font-family: ${FONTS.sans};
    -webkit-font-smoothing: antialiased;
  }

  a { color: ${COLORS.link}; }
  a:hover { color: ${COLORS.linkHover}; }

  :focus-visible { outline: 3px solid ${COLORS.accent}; outline-offset: 2px; }

  .visually-hidden {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip: rect(0 0 0 0);
    white-space: nowrap;
  }
`
