import styled from 'styled-components'

import type { PortVessel } from '../api/types'
import { COLORS, FONTS } from '../theme'
import { GEAR_FAMILIES, GearIcon, getFamilyOfGear } from './gears'
import { INDUSTRIAL_LENGTH } from './impacts'
import { formatNumber } from './texts'

const CURRENT_YEAR = new Date().getFullYear()

/** Coastal, offshore and industrial vessels, as in the reports on the French fleet */
export const SIZE_CLASSES = [
  { color: '#8FB4C9', label: 'Moins de 12 m', max: 12, name: 'Petite pêche' },
  { color: '#2D6A92', label: '12 à 24 m', max: INDUSTRIAL_LENGTH, name: 'Pêche au large' },
  { color: '#0B2233', label: '24 m et plus', max: Infinity, name: 'Pêche industrielle' }
]

/**
 * What kind of port it is, at a glance: its fleet by size and by gear, how old and how big its vessels are. From the
 * EU fleet register, for the vessels Open Vessel Photos knows.
 */
export function PortPortrait({ vessels }: { vessels: PortVessel[] }) {
  const lengths = vessels.flatMap(vessel => (vessel.lengthMeters === null ? [] : [vessel.lengthMeters]))
  if (lengths.length === 0) {
    return null
  }
  const ages = vessels.flatMap(vessel => (vessel.yearBuilt === null ? [] : [CURRENT_YEAR - vessel.yearBuilt]))
  const tonnage = vessels.reduce((sum, vessel) => sum + (vessel.tonnageGt ?? 0), 0)
  const sizes = SIZE_CLASSES.map((sizeClass, index) => ({
    ...sizeClass,
    count: lengths.filter(length => length < sizeClass.max && length >= (SIZE_CLASSES[index - 1]?.max ?? 0)).length
  }))
  const gears = GEAR_FAMILIES.map(family => ({
    count: vessels.filter(vessel => getFamilyOfGear(vessel.mainGear)?.id === family.id).length,
    family
  }))
    .filter(({ count }) => count > 0)
    // oxlint-disable-next-line unicorn/no-array-sort
    .sort((a, b) => b.count - a.count)
  const maxGearCount = Math.max(1, ...gears.map(({ count }) => count))
  const unknown = vessels.length - lengths.length

  return (
    <Portrait aria-label="Portrait du port">
      <Figures>
        <Figure>
          <strong>{formatNumber(Math.round(average(lengths) * 10) / 10)} m</strong>
          <span>longueur moyenne</span>
        </Figure>
        {ages.length > 0 && (
          <Figure>
            <strong>{Math.round(average(ages))} ans</strong>
            <span>âge moyen</span>
          </Figure>
        )}
        {tonnage > 0 && (
          <Figure>
            <strong>{formatNumber(Math.round(tonnage))}</strong>
            <span>jauge brute totale (UMS)</span>
          </Figure>
        )}
      </Figures>

      <Subtitle>Taille des navires</Subtitle>
      <SizeBar aria-hidden>
        {sizes
          .filter(({ count }) => count > 0)
          .map(({ color, count, name }) => (
            <span key={name} style={{ background: color, flexGrow: count }} title={`${name} : ${count}`} />
          ))}
      </SizeBar>
      <Legend>
        {sizes.map(({ color, count, label, name }) => (
          <li key={name}>
            <i style={{ background: color }} />
            <span>
              {name} <small>{label}</small>
            </span>
            <b>{count}</b>
          </li>
        ))}
      </Legend>

      {gears.length > 0 && (
        <>
          <Subtitle>Engins de pêche</Subtitle>
          <Gears>
            {gears.map(({ count, family }) => (
              <li key={family.id}>
                <GearIcon family={family} size={18} />
                <span>{family.label}</span>
                <GearBar>
                  <span style={{ width: `${(count / maxGearCount) * 100}%` }} />
                </GearBar>
                <b>{count}</b>
              </li>
            ))}
          </Gears>
        </>
      )}
      {unknown > 0 && (
        <Note>
          {unknown} navire{unknown > 1 ? 's' : ''} sans données du registre de la flotte de l’UE.
        </Note>
      )}
    </Portrait>
  )
}

function average(values: number[]): number {
  return values.reduce((sum, value) => sum + value, 0) / values.length
}

const Portrait = styled.section`
  margin-top: 14px;
  padding: 14px;
  border: 1px solid ${COLORS.cardBorder};
  border-radius: 14px;
  background: ${COLORS.white};
`

const Figures = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(80px, 1fr));
  gap: 8px;
`

const Figure = styled.div`
  display: flex;
  flex-direction: column;

  strong {
    color: ${COLORS.ink};
    font-family: ${FONTS.display};
    font-size: 18px;
    font-variant-numeric: tabular-nums;
  }

  span {
    color: ${COLORS.muted};
    font-size: 11.5px;
  }
`

const Subtitle = styled.h3`
  margin: 14px 0 6px;
  color: ${COLORS.muted};
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
`

const SizeBar = styled.div`
  display: flex;
  gap: 2px;
  height: 10px;
  overflow: hidden;
  border-radius: 5px;

  span {
    min-width: 4px;
  }
`

const Legend = styled.ul`
  display: grid;
  gap: 3px;
  margin: 8px 0 0;
  padding: 0;
  list-style: none;
  font-size: 12.5px;

  li {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  i {
    flex: none;
    width: 10px;
    height: 10px;
    border-radius: 3px;
  }

  span {
    flex: 1;
    color: ${COLORS.ink};
  }

  small {
    color: ${COLORS.muted};
    font-size: 11px;
  }

  b {
    font-variant-numeric: tabular-nums;
  }
`

const Gears = styled.ul`
  display: grid;
  gap: 5px;
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 12.5px;

  li {
    display: grid;
    grid-template-columns: 18px 110px 1fr 24px;
    align-items: center;
    gap: 8px;
    color: ${COLORS.ink};
  }

  svg {
    color: ${COLORS.link};
  }

  b {
    font-variant-numeric: tabular-nums;
    text-align: right;
  }
`

const GearBar = styled.span`
  height: 6px;
  overflow: hidden;
  border-radius: 3px;
  background: ${COLORS.divider};

  span {
    display: block;
    height: 100%;
    border-radius: 3px;
    background: ${COLORS.link};
  }
`

const Note = styled.p`
  margin: 10px 0 0;
  color: ${COLORS.muted};
  font-size: 11.5px;
`
