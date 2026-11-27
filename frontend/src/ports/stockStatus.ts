import type { StockStatusCode } from '../api/types'
import { COLORS } from '../theme'

/**
 * Ifremer's scale, from green (a stock in good state) to black (a collapsed one), as a colored square before the label,
 * as in a printed table: the label carries the meaning, the color only repeats it (WCAG 1.4.1), and no filled pill that
 * would read as a button. Every color has at least 4.5:1 of contrast on the panels. Shared by the port sheet
 * (AuctionSales) and the species page, rendered by the backend (PortsContentController.stockCell, data-stock
 * attribute): same labels on both.
 */
export const STOCK_STYLES: Record<StockStatusCode, { color: string; label: string; mark: string }> = {
  BON_ETAT: { color: '#1D5B34', label: 'Bon état', mark: '#2E8A4E' },
  EFFONDRE: { color: '#26292C', label: 'Effondré', mark: '#26292C' },
  NON_CLASSIFIE: { color: COLORS.muted, label: 'Non classifié', mark: 'transparent' },
  RECONSTITUABLE: { color: '#6B5200', label: 'Reconstituable', mark: '#D9A90F' },
  SURPECHE: { color: '#9A3F0C', label: 'Surpêché', mark: '#E0662A' },
  SURPECHE_DEGRADE: { color: '#962519', label: 'Surpêché, dégradé', mark: '#B8301F' },
  VARIABLE: { color: COLORS.text, label: 'Variable', mark: COLORS.inputBorder }
}

/** The label after its square, for the HTML rendered by the backend (the color comes from STOCK_STATUS_RULES) */
export const STOCK_MARK_CSS = `
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  font-weight: 600;
  line-height: 18px;
  white-space: nowrap;
  cursor: help;

  &::before {
    content: '';
    flex: none;
    width: 8px;
    height: 8px;
    background: var(--stock-mark);
  }
`

export const STOCK_STATUS_RULES = (Object.keys(STOCK_STYLES) as StockStatusCode[])
  .map(
    code =>
      `.stock-status[data-stock='${code}'] { color: ${STOCK_STYLES[code].color}; --stock-mark: ${STOCK_STYLES[code].mark}; }`
  )
  .join('\n')
