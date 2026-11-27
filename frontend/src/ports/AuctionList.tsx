import { useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import type { Port } from '../api/types'
import { COLORS, FONTS, RADIUS } from '../theme'
import { AUCTION_COLOR } from './PortsMap'
import { formatNumber, getPortPath } from './texts'

const SHOWN_AT_FIRST = 8

type Props = {
  onHighlight: (locode: string | undefined) => void
  ports: Port[]
  /** The query string of the page, kept in the links (e.g. the gear filter) */
  search: string
}

/** The fish auctions (criées), by value of their sales, each linking to its port */
export function AuctionList({ onHighlight, ports, search }: Props) {
  const [isExpanded, setIsExpanded] = useState(false)
  const auctions = ports
    .flatMap(port => (port.auction ? [{ auction: port.auction, port }] : []))
    // oxlint-disable-next-line unicorn/no-array-sort
    .sort((a, b) => b.auction.valueThousandEuros - a.auction.valueThousandEuros)
  if (auctions.length === 0) {
    return null
  }
  const maxValue = auctions[0]?.auction.valueThousandEuros ?? 1
  const totalValue = auctions.reduce((sum, { auction }) => sum + auction.valueThousandEuros, 0)
  const shown = isExpanded ? auctions : auctions.slice(0, SHOWN_AT_FIRST)

  return (
    <div>
      <Lead>
        Le poisson débarqué est vendu aux enchères dans {auctions.length} criées, pour {formatMillions(totalValue)} en{' '}
        {auctions[0]?.auction.year}. Sur la carte, ces ports sont entourés <Ring aria-hidden />.
      </Lead>
      <List>
        {shown.map(({ auction, port }, index) => (
          <li key={port.locode}>
            <Row
              onBlur={() => onHighlight(undefined)}
              onClick={() => onHighlight(undefined)}
              onFocus={() => onHighlight(port.locode)}
              onMouseEnter={() => onHighlight(port.locode)}
              onMouseLeave={() => onHighlight(undefined)}
              to={{ pathname: getPortPath(port), search }}
            >
              <Rank>{index + 1}</Rank>
              <Name>
                <span>{auction.name}</span>
                <small>{formatNumber(Math.round(auction.quantityTonnes))} t</small>
              </Name>
              <Value>{formatMillions(auction.valueThousandEuros)}</Value>
              <Bar aria-hidden style={{ width: `${(auction.valueThousandEuros / maxValue) * 100}%` }} />
            </Row>
          </li>
        ))}
      </List>
      {auctions.length > SHOWN_AT_FIRST && (
        <More onClick={() => setIsExpanded(!isExpanded)} type="button">
          {isExpanded ? 'Voir moins' : `Voir les ${auctions.length} criées`}
        </More>
      )}
      <Note>Ventes déclarées en halles à marée, toutes espèces. Source : FranceAgriMer.</Note>
    </div>
  )
}

/** e.g. "44,6 M€" */
function formatMillions(thousandEuros: number): string {
  return `${(thousandEuros / 1000).toLocaleString('fr', { maximumFractionDigits: 1 })} M€`
}

const Lead = styled.p`
  margin: 0 0 8px;
  color: ${COLORS.text};
  font-size: 13.5px;
  line-height: 1.5;
`

const Ring = styled.span`
  display: inline-block;
  width: 11px;
  height: 11px;
  border: 3px solid ${AUCTION_COLOR};
  border-radius: 50%;
  background: #2d6a92;
  vertical-align: -1px;
`

const List = styled.ol`
  margin: 0 -8px;
  padding: 0;
  list-style: none;
`

const Row = styled(Link)`
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 8px 11px;
  border-radius: ${RADIUS};
  color: ${COLORS.ink};
  text-decoration: none;

  &:hover {
    background: ${COLORS.white};
    color: ${COLORS.ink};
  }
`

const Rank = styled.span`
  flex: none;
  width: 18px;
  color: ${COLORS.muted};
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  text-align: right;
`

const Name = styled.span`
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: baseline;
  gap: 8px;

  span {
    overflow: hidden;
    font-size: 14px;
    font-weight: 500;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  small {
    flex: none;
    color: ${COLORS.muted};
    font-size: 11.5px;
  }
`

const Value = styled.span`
  font-family: ${FONTS.sans};
  font-size: 13.5px;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  white-space: nowrap;
`

const Bar = styled.span`
  position: absolute;
  bottom: 5px;
  left: 36px;
  max-width: calc(100% - 44px);
  height: 3px;
  border-radius: ${RADIUS};
  background: ${AUCTION_COLOR};
  opacity: 0.7;
`

const More = styled.button`
  margin-top: 4px;
  padding: 6px 0;
  border: none;
  background: none;
  color: ${COLORS.link};
  font: 600 13px ${FONTS.sans};
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
`

const Note = styled.p`
  margin: 6px 0 0;
  color: ${COLORS.muted};
  font-size: 11px;
`
