import { useState } from 'react'
import styled from 'styled-components'

import type { AuctionSpecies, FishingOpportunity, PortAuction, StockStatus } from '../api/types'
import { of } from '../i18n/fr'
import { COLORS, FONTS } from '../theme'
import { STOCK_STYLES } from './stockStatus'
import { formatNumber } from './texts'

const MONTH_INITIALS = ['J', 'F', 'M', 'A', 'M', 'J', 'J', 'A', 'S', 'O', 'N', 'D']
const CURRENT_MONTH = new Date().getMonth()
const FRANCE_AGRIMER_URL = 'https://www.franceagrimer.fr'

/**
 * What the fish auction of the port sells, and when: FranceAgriMer's yearly figures of the sales declared at the
 * auctions (halles à marée), with the season of each species in all French auctions.
 */
export function AuctionSales({ auction, isFloating = false }: { auction: PortAuction; isFloating?: boolean }) {
  const [isFolded, setIsFolded] = useState(false)
  const change =
    auction.previousQuantityTonnes && auction.previousQuantityTonnes > 0
      ? auction.quantityTonnes / auction.previousQuantityTonnes - 1
      : undefined
  const maxQuantity = Math.max(1, ...auction.species.map(species => species.quantityTonnes))

  return (
    <Card $isFloating={isFloating} aria-label={`Criée ${of(auction.name)}`}>
      <Header>
        <h3>Criée {of(auction.name)}</h3>
        <Year>{auction.year}</Year>
        {isFloating && (
          <FoldButton
            aria-expanded={!isFolded}
            aria-label={isFolded ? 'Afficher les espèces' : 'Réduire'}
            onClick={() => setIsFolded(!isFolded)}
            title={isFolded ? 'Afficher les espèces' : 'Réduire'}
            type="button"
          >
            <svg
              aria-hidden
              fill="none"
              height="16"
              stroke="currentColor"
              strokeWidth="2"
              viewBox="0 0 24 24"
              width="16"
            >
              <path d={isFolded ? 'M6 9l6 6 6-6' : 'M6 15l6-6 6 6'} strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </FoldButton>
        )}
      </Header>
      <Figures>
        <Figure>
          <strong>{formatNumber(Math.round(auction.quantityTonnes))} t</strong>
          <span>vendues</span>
          {change !== undefined && (
            <Change>
              {change >= 0 ? '+' : '−'}
              {Math.abs(Math.round(change * 100))} % sur un an
            </Change>
          )}
        </Figure>
        <Figure>
          <strong>{formatMillions(auction.valueThousandEuros)}</strong>
          <span>de ventes</span>
        </Figure>
        {auction.priceEurosPerKg !== null && (
          <Figure>
            <strong>{formatPrice(auction.priceEurosPerKg)}</strong>
            <span>prix moyen</span>
          </Figure>
        )}
      </Figures>

      {!isFolded && auction.species.length > 0 && (
        <>
          <Subtitle>
            <span>Principales espèces</span>
            <Months aria-hidden>
              {MONTH_INITIALS.map((initial, index) => (
                <i data-current={index === CURRENT_MONTH} key={index}>
                  {initial}
                </i>
              ))}
            </Months>
          </Subtitle>
          <SpeciesList>
            {auction.species.map(species => (
              <SpeciesRow
                key={species.name}
                maxQuantity={maxQuantity}
                quotaYear={auction.fishingOpportunities?.year}
                species={species}
              />
            ))}
          </SpeciesList>
        </>
      )}
      {!isFolded && (
        <Note>
          Ventes déclarées à la criée en {auction.year}, y compris par des navires d’autres ports. Saisons : ventes
          mensuelles dans toutes les criées françaises. Source :{' '}
          <a href={FRANCE_AGRIMER_URL} rel="noopener" target="_blank">
            FranceAgriMer
          </a>
          .
          {auction.stockAssessment && (
            <>
              {' '}
              État des stocks de la façade :{' '}
              <a href={auction.stockAssessment.url} rel="noopener" target="_blank">
                Ifremer, bilan {auction.stockAssessment.edition}
              </a>{' '}
              (<a href="/methodologie#stocks">ce que veut dire chaque état</a>).
            </>
          )}
          {auction.fishingOpportunities && (
            <>
              {' '}
              Quotas :{' '}
              <a href={auction.fishingOpportunities.url} rel="noopener" target="_blank">
                TAC {auction.fishingOpportunities.year} du Conseil de l’UE
              </a>
              , part initiale de la France (<a href="/methodologie#quotas">les quotas par navire ne sont pas publics</a>
              ).
            </>
          )}
        </Note>
      )}
    </Card>
  )
}

function SpeciesRow({
  maxQuantity,
  quotaYear,
  species
}: {
  maxQuantity: number
  quotaYear: number | undefined
  species: AuctionSpecies
}) {
  const months = species.monthlyQuantitiesTonnes
  const maxMonth = months ? Math.max(...months) : 0
  const averageMonth = months ? months.reduce((sum, value) => sum + value, 0) / 12 : 0
  const isInSeason = months !== null && maxMonth > 0 && (months[CURRENT_MONTH] ?? 0) >= averageMonth

  return (
    <SpeciesItem>
      <SpeciesName>
        {capitalize(species.name)}
        {isInSeason && <InSeason title="Plus vendue que la moyenne ce mois-ci">de saison</InSeason>}
      </SpeciesName>
      <StockCell>{species.stockStatus && <StockBadge stockStatus={species.stockStatus} />}</StockCell>
      <Quantity>
        <span>
          <span style={{ width: `${(species.quantityTonnes / maxQuantity) * 100}%` }} />
        </span>
        <b>{formatNumber(Math.round(species.quantityTonnes))} t</b>
        {species.priceEurosPerKg !== null && <small>{formatPrice(species.priceEurosPerKg)}</small>}
      </Quantity>
      {months ? (
        <Season aria-label={`Saison : ventes de ${species.name} par mois en France`}>
          {months.map((value, index) => (
            <i
              data-current={index === CURRENT_MONTH}
              key={index}
              style={{ opacity: maxMonth > 0 ? 0.12 + (value / maxMonth) * 0.88 : 0.12 }}
              title={`${MONTH_INITIALS[index]} : ${formatNumber(Math.round(value))} t en France`}
            />
          ))}
        </Season>
      ) : (
        <NoSeason />
      )}
      {species.quotas && <QuotaLine quotas={species.quotas} year={quotaYear} />}
    </SpeciesItem>
  )
}

/**
 * France's quota of the TACs a species is fished under in the waters of the port, on a line of its own: a national
 * figure, not the port's, with each TAC in the title
 */
function QuotaLine({ quotas, year }: { quotas: FishingOpportunity[]; year: number | undefined }) {
  const franceTonnes = quotas.reduce((sum, quota) => sum + quota.franceTonnes, 0)
  const title = [
    'Part initiale de la France, avant échanges (TAC du Conseil de l’UE) :',
    ...quotas.map(
      quota =>
        `${quota.area} : ${formatNumber(Math.round(quota.franceTonnes))} t` +
        (quota.tacTonnes !== null ? ` sur un TAC de ${formatNumber(Math.round(quota.tacTonnes))} t` : '') +
        ` (${quota.tacCode})`
    )
  ].join('\n')

  return (
    <QuotaText title={title}>
      Quota France{year ? ` ${year}` : ''}
      {quotas.length > 1 ? ` (${quotas.length} zones)` : ''} : <b>{formatNumber(Math.round(franceTonnes))} t</b>
    </QuotaText>
  )
}

/** The status of the stocks a species is landed from in the waters of the port; nothing when not classified */
function StockBadge({ stockStatus }: { stockStatus: StockStatus }) {
  if (stockStatus.status === 'NON_CLASSIFIE') {
    return null
  }
  const style = STOCK_STYLES[stockStatus.status]
  const title = [
    stockStatus.status === 'VARIABLE'
      ? 'Stocks dans des états différents (Ifremer) :'
      : `Stock ${stockStatus.label} (Ifremer) :`,
    ...stockStatus.stocks.map(
      stock => `${stock.name} : ${stock.label} (${formatNumber(Math.round(stock.tonnes))} t débarquées)`
    )
  ].join('\n')

  return (
    <Stock $background={style.background} $color={style.color} title={title}>
      <span className="visually-hidden">Stock : </span>
      {style.label}
    </Stock>
  )
}

/** "thon germon" → "Thon germon": the workbook does not always capitalize */
function capitalize(name: string): string {
  return name.charAt(0).toUpperCase() + name.slice(1)
}

/** e.g. "44,6 M€", "830 k€" */
function formatMillions(thousandEuros: number): string {
  return thousandEuros >= 1000
    ? `${(thousandEuros / 1000).toLocaleString('fr', { maximumFractionDigits: 1 })} M€`
    : `${Math.round(thousandEuros).toLocaleString('fr')} k€`
}

/** e.g. "7,39 €/kg" */
function formatPrice(eurosPerKg: number): string {
  return `${eurosPerKg.toLocaleString('fr', { maximumFractionDigits: 2, minimumFractionDigits: 2 })} €/kg`
}

const SEASON_WIDTH = 96

const Card = styled.section<{ $isFloating: boolean }>`
  flex: none;
  margin-top: ${({ $isFloating }) => ($isFloating ? 0 : 14)}px;
  padding: 14px;
  border: 1px solid ${({ $isFloating }) => ($isFloating ? COLORS.border : COLORS.cardBorder)};
  border-radius: 6px;
  background: ${({ $isFloating }) => ($isFloating ? '#FBFAF6' : COLORS.white)};
  box-shadow: ${({ $isFloating }) => ($isFloating ? '0 1px 2px rgba(11, 34, 51, 0.1)' : 'none')};
`

const FoldButton = styled.button`
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  margin-left: 2px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: ${COLORS.muted};
  cursor: pointer;

  &:hover {
    background: ${COLORS.divider};
    color: ${COLORS.ink};
  }
`

const Header = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;

  h3 {
    flex: 1;
    margin: 0;
    color: ${COLORS.ink};
    font-family: ${FONTS.sans};
    font-size: 16px;
  }
`

const Year = styled.span`
  color: ${COLORS.muted};
  font-size: 12px;
  font-variant-numeric: tabular-nums;
`

const Figures = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(80px, 1fr));
  gap: 8px;
  margin-top: 10px;
`

const Figure = styled.div`
  display: flex;
  flex-direction: column;

  strong {
    color: ${COLORS.ink};
    font-family: ${FONTS.sans};
    font-size: 17px;
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
  }

  span {
    color: ${COLORS.muted};
    font-size: 11.5px;
  }
`

const Change = styled.span`
  color: ${COLORS.text} !important;
  font-size: 11px !important;
`

const Subtitle = styled.div`
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 8px;
  margin: 14px 0 6px;
  padding-bottom: 4px;
  border-bottom: 1px solid ${COLORS.divider};
  color: ${COLORS.ink};
  font-size: 12px;
  font-weight: 600;
`

const Months = styled.span`
  display: grid;
  flex: none;
  grid-template-columns: repeat(12, 1fr);
  width: ${SEASON_WIDTH}px;
  letter-spacing: 0;
  text-align: center;

  i {
    font-size: 9px;
    font-style: normal;
  }

  i[data-current='true'] {
    color: ${COLORS.accent};
  }
`

const SpeciesList = styled.ul`
  margin: 0;
  padding: 0;
  list-style: none;
`

/**
 * A grid, so that every row has the same columns whatever its content: the name and the stock status, the sales and
 * the season, then the quota across
 */
const SpeciesItem = styled.li`
  display: grid;
  /* The status pill can be wider than the season strip: both end on the right, under the months */
  grid-template-columns: minmax(0, 1fr) minmax(${SEASON_WIDTH}px, auto);
  align-items: center;
  gap: 4px 12px;
  padding: 8px 0;
  border-bottom: 1px solid ${COLORS.divider};

  &:last-child {
    border-bottom: none;
  }
`

const SpeciesName = styled.div`
  min-width: 0;
  color: ${COLORS.ink};
  font-size: 13.5px;
  font-weight: 600;
  line-height: 1.3;
`

const StockCell = styled.div`
  display: flex;
  justify-content: flex-end;
`

const InSeason = styled.span`
  margin-left: 6px;
  color: ${COLORS.approve};
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
`

const QuotaText = styled.span`
  grid-column: 1 / -1;
  color: ${COLORS.muted};
  font-size: 11.5px;
  cursor: help;

  b {
    color: ${COLORS.text};
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }
`

/** Right-aligned on the name line, so that the statuses make a column */
const Stock = styled.span<{ $background: string; $color: string }>`
  padding: 1px 6px;
  border-radius: 999px;
  background: ${({ $background }) => $background};
  color: ${({ $color }) => $color};
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
  white-space: nowrap;
  cursor: help;
`

const Quantity = styled.div`
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  color: ${COLORS.muted};
  font-size: 11.5px;

  > span {
    flex: 1;
    max-width: 70px;
    height: 5px;
    overflow: hidden;
    border-radius: 3px;
    background: ${COLORS.divider};
  }

  > span > span {
    display: block;
    height: 100%;
    border-radius: 3px;
    background: ${COLORS.link};
  }

  b {
    color: ${COLORS.ink};
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }

  small {
    font-size: 11px;
  }
`

/** A strip of 12 months, darker when more is sold */
const Season = styled.span`
  display: grid;
  justify-self: end;
  grid-template-columns: repeat(12, 1fr);
  gap: 1px;
  width: ${SEASON_WIDTH}px;
  height: 14px;

  i {
    border-radius: 2px;
    background: ${COLORS.link};
  }

  i[data-current='true'] {
    outline: 1.5px solid ${COLORS.accent};
    outline-offset: 0;
  }
`

const NoSeason = styled.span`
  justify-self: end;
  width: ${SEASON_WIDTH}px;
`

const Note = styled.p`
  margin: 12px 0 0;
  color: ${COLORS.muted};
  font-size: 11px;
  line-height: 1.45;

  a {
    color: ${COLORS.muted};
  }
`
