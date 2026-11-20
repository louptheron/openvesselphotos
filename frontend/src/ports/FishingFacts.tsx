import styled from 'styled-components'

import { COLORS, FONTS } from '../theme'
import { SOURCES } from './impacts'

const INSTITUT_AGRO = {
  label: 'Institut Agro, « Évaluation des performances des flottilles de pêche : bilan France » (2025)',
  url: 'https://halieutique.institut-agro.fr/files/fichiers/pdf/performances.pdf'
}

/** Share of the French fleet, its catches and its jobs by vessel size, 2017–2021: Institut Agro (2025), cited by BLOOM */
const SHARES = [
  { color: '#8FB4C9', label: 'Moins de 12 m', values: [84.3, 23.66, 43.6] },
  { color: '#4F8DB3', label: '12 à 24 m', values: [12.2, 27.25, 29.3] },
  { color: '#1C5A80', label: '24 à 40 m', values: [2.8, 12.24, 15.7] },
  { color: '#0B2233', label: 'Plus de 40 m', values: [0.8, 36.84, 11.4] }
]
const MEASURES = ['Navires', 'Captures', 'Emplois']

/** The French fishing industry in a few figures, for newcomers */
export function FishingFacts() {
  return (
    <div>
      <Lead>
        La flotte française compte surtout de petits bateaux, mais une poignée de grands navires débarque une large part
        des captures.
      </Lead>
      <Chart aria-label="Part des navires, des captures et des emplois selon la taille des navires">
        {MEASURES.map((measure, index) => (
          <Row key={measure}>
            <RowLabel>{measure}</RowLabel>
            <Stack>
              {SHARES.map(({ color, label, values }) => (
                <span
                  key={label}
                  style={{ background: color, flexGrow: values[index] }}
                  title={`${label} : ${formatPercent(values[index] ?? 0)}`}
                >
                  {(values[index] ?? 0) >= 10 && formatPercent(values[index] ?? 0)}
                </span>
              ))}
            </Stack>
          </Row>
        ))}
      </Chart>
      <Legend>
        {SHARES.map(({ color, label }) => (
          <li key={label}>
            <i style={{ background: color }} />
            {label}
          </li>
        ))}
      </Legend>
      <Takeaway>
        Les navires de moins de 12 m forment 84 % de la flotte et 44 % des emplois, pour 24 % des captures. Ceux de plus
        de 40 m, moins de 1 % des navires, en débarquent 37 %.
      </Takeaway>

      <Figures>
        <Figure>
          <strong>12 316</strong>
          <span>marins embarqués sur les navires de pêche (2022)</span>
        </Figure>
        <Figure>
          <strong>527 000 t</strong>
          <span>de produits de la mer débarqués (2022)</span>
        </Figure>
        <Figure>
          <strong>1,14 Mt</strong>
          <span>de CO₂e émises par la flotte (2022)</span>
        </Figure>
      </Figures>
      <Sources>
        Sources :{' '}
        <a href={INSTITUT_AGRO.url} rel="noopener" target="_blank">
          Institut Agro
        </a>{' '}
        (2017–2021, via{' '}
        <a href={SOURCES.bloomOligarchs.url} rel="noopener" target="_blank">
          BLOOM
        </a>
        ),{' '}
        <a href={SOURCES.shiftCarbon.url} rel="noopener" target="_blank">
          The Shift Project &amp; BLOOM
        </a>{' '}
        (données CSTEP 2022).
      </Sources>
    </div>
  )
}

function formatPercent(value: number): string {
  return `${Math.round(value).toLocaleString('fr')} %`
}

const Lead = styled.p`
  margin: 0 0 12px;
  color: ${COLORS.text};
  font-size: 13.5px;
  line-height: 1.5;
`

const Chart = styled.div`
  display: grid;
  gap: 6px;
`

const Row = styled.div`
  display: grid;
  grid-template-columns: 64px 1fr;
  align-items: center;
  gap: 8px;
`

const RowLabel = styled.span`
  color: ${COLORS.ink};
  font-size: 12.5px;
  font-weight: 600;
`

const Stack = styled.div`
  display: flex;
  gap: 2px;
  height: 22px;
  overflow: hidden;
  border-radius: 6px;

  span {
    display: grid;
    place-items: center;
    min-width: 3px;
    color: ${COLORS.white};
    font-size: 10.5px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
  }

  span:first-child {
    color: ${COLORS.ink};
  }
`

const Legend = styled.ul`
  display: flex;
  flex-wrap: wrap;
  gap: 4px 12px;
  margin: 8px 0 0 72px;
  padding: 0;
  list-style: none;
  color: ${COLORS.muted};
  font-size: 11.5px;

  li {
    display: flex;
    align-items: center;
    gap: 5px;
  }

  i {
    width: 9px;
    height: 9px;
    border-radius: 3px;
  }
`

const Takeaway = styled.p`
  margin: 10px 0 0;
  color: ${COLORS.text};
  font-size: 12.5px;
  line-height: 1.45;
`

const Figures = styled.div`
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 14px;
`

const Figure = styled.div`
  padding: 10px;
  border: 1px solid ${COLORS.cardBorder};
  border-radius: 12px;
  background: ${COLORS.white};

  strong {
    display: block;
    color: ${COLORS.ink};
    font-family: ${FONTS.display};
    font-size: 17px;
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
  }

  span {
    display: block;
    margin-top: 2px;
    color: ${COLORS.muted};
    font-size: 11px;
    line-height: 1.3;
  }
`

const Sources = styled.p`
  margin: 10px 0 0;
  color: ${COLORS.muted};
  font-size: 11px;
  line-height: 1.45;

  a {
    color: ${COLORS.muted};
  }
`
