import type { StockStatusCode } from '../api/types'
import { COLORS } from '../theme'

/**
 * Ifremer's scale, from green (a stock in good state) to black (a collapsed one), as tinted pills that read apart from
 * the brand's orange and from the green "de saison". Shared by the port sheet (AuctionSales) and the species page,
 * rendered by the backend (PortsContentController.stockCell, data-stock attribute): same labels on both.
 */
export const STOCK_STYLES: Record<StockStatusCode, { background: string; color: string; label: string }> = {
  BON_ETAT: { background: '#E3F1E7', color: '#1D5B34', label: 'Bon état' },
  EFFONDRE: { background: '#26292C', color: '#FFFFFF', label: 'Effondré' },
  NON_CLASSIFIE: { background: 'transparent', color: COLORS.muted, label: 'Non classifié' },
  RECONSTITUABLE: { background: '#F8F0CF', color: '#6B5200', label: 'Reconstituable' },
  SURPECHE: { background: '#FBE6D8', color: '#9A3F0C', label: 'Surpêché' },
  SURPECHE_DEGRADE: { background: '#F7DEDB', color: '#962519', label: 'Surpêché, dégradé' },
  VARIABLE: { background: COLORS.divider, color: COLORS.text, label: 'Variable' }
}

/** The pill, for the HTML rendered by the backend */
export const STOCK_PILL_CSS = `
  padding: 1px 7px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
  line-height: 18px;
  white-space: nowrap;
  cursor: help;
`

export const STOCK_STATUS_RULES = (Object.keys(STOCK_STYLES) as StockStatusCode[])
  .map(
    code =>
      `.stock-status[data-stock='${code}'] { background: ${STOCK_STYLES[code].background}; color: ${STOCK_STYLES[code].color}; }`
  )
  .join('\n')
