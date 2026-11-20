import styled from 'styled-components'

import type { PortVessel } from '../api/types'
import { COLORS } from '../theme'
import { GEAR_FAMILIES, GearIcon, getFamilyOfGear } from './gears'
import { INDUSTRIAL_LENGTH } from './impacts'
import { formatPercent } from './texts'

/** Coastal, offshore and industrial vessels, as in the reports on the French fleet */
export const SIZE_CLASSES = [
  { color: '#8FB4C9', label: 'Moins de 12 m', max: 12, name: 'Petite pêche' },
  { color: '#2D6A92', label: '12 à 24 m', max: INDUSTRIAL_LENGTH, name: 'Pêche au large' },
  { color: '#0B2233', label: '24 m et plus', max: Infinity, name: 'Pêche industrielle' }
]

/**
 * What kind of port it is, at a glance: its fleet by size and by gear. From the EU fleet register, for the vessels
 * Open Vessel Photos knows.
 */
export function PortPortrait({ vessels }: { vessels: PortVessel[] }) {
  const lengths = vessels.flatMap(vessel => (vessel.lengthMeters === null ? [] : [vessel.lengthMeters]))
  if (lengths.length === 0) {
    return null
  }
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
      <Note>
        Source : registre de la flotte de l’UE. {getReliability(vessels)} <a href="/methodologie">Méthodologie</a>
      </Note>
    </Portrait>
  )
}

/**
 * How the home ports of the vessels were found, e.g. "Port d’attache : 12 navires vus à quai le plus souvent (81 % de
 * leurs positions à quai en moyenne), 3 d’après leurs escales (Global Fishing Watch), 5 d’après leur quartier
 * d’immatriculation seulement." Same as PortsPagesController.
 */
function getReliability(vessels: PortVessel[]): string {
  const observed = vessels.filter(vessel => vessel.homePortSource !== 'GFW' && vessel.homePortSource !== 'REGISTER')
  const fromVisits = vessels.filter(vessel => vessel.homePortSource === 'GFW').length
  const fromRegistration = vessels.filter(vessel => vessel.homePortSource === 'REGISTER').length
  const shares = observed
    .filter(vessel => vessel.observationsTotal > 0)
    .map(vessel => vessel.observationsAtPort / vessel.observationsTotal)
  const average = shares.length > 0 ? shares.reduce((sum, share) => sum + share, 0) / shares.length : undefined
  const parts = [
    observed.length > 0 &&
      `${observed.length} navire${observed.length > 1 ? 's' : ''} vu${observed.length > 1 ? 's' : ''} à quai le plus souvent${
        average === undefined ? '' : ` (${formatPercent(average)} de leurs positions à quai en moyenne)`
      }`,
    fromVisits > 0 && `${fromVisits} d’après leurs escales (Global Fishing Watch)`,
    fromRegistration > 0 && `${fromRegistration} d’après leur quartier d’immatriculation seulement`
  ].filter(Boolean)

  return parts.length > 0 ? `Port d’attache : ${parts.join(', ')}.` : ''
}

const Portrait = styled.section`
  margin-top: 14px;
  padding: 14px;
  border: 1px solid ${COLORS.cardBorder};
  border-radius: 6px;
  background: ${COLORS.white};
`

const Subtitle = styled.h3`
  margin: 14px 0 6px;

  &:first-child {
    margin-top: 0;
  }

  padding-bottom: 4px;
  border-bottom: 1px solid ${COLORS.divider};
  color: ${COLORS.ink};
  font-size: 12px;
  font-weight: 600;
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
  line-height: 1.5;

  a {
    color: ${COLORS.muted};
  }
`
