import { type ReactNode, useMemo, useState } from 'react'
import styled, { css, keyframes } from 'styled-components'

import type { Port } from '../api/types'
import { COLORS, FONTS } from '../theme'
import { GEAR_FAMILIES, type GearFamily, GearIcon, IMPACT_LABELS, countVessels } from './gears'
import {
  type CarbonGroup,
  IMPACT_COLORS,
  ImpactPill,
  KG_CO2E_PER_KG_EDIBLE,
  PROTEIN_COMPARISONS,
  SEABED_CARBON,
  SOURCES,
  formatCarbon,
  formatKg,
  getCarbonLevel,
  getCarbonRange
} from './impacts'
import { MOBILE_QUERY, PANEL_MARGIN, PANEL_WIDTH } from './layout'
import { formatNumber, vessels } from './texts'

type Props = {
  /** Cards shown on the right of the map, under the gear's explanation, e.g. the fish auction of the port */
  children?: ReactNode
  onChange: (family: GearFamily | undefined) => void
  ports: Port[]
  selected: GearFamily | undefined
}

/** Over the map: the ways of fishing, each with a drawing, to show only the vessels using one */
export function GearFilter({ children, onChange, ports, selected }: Props) {
  const families = useMemo(
    () =>
      GEAR_FAMILIES.map(family => ({
        count: ports.reduce((sum, port) => sum + countVessels(port, family), 0),
        family
      })).filter(({ count }) => count > 0),
    [ports]
  )
  const rightColumn =
    (selected && families.length > 0) || children ? (
      <RightColumn>
        {selected && families.length > 0 && (
          <GearExplanation
            count={families.find(({ family }) => family.id === selected.id)?.count ?? 0}
            family={selected}
            key={selected.id}
            onClear={() => onChange(undefined)}
          />
        )}
        {children}
      </RightColumn>
    ) : null
  if (families.length === 0) {
    return <Wrapper>{rightColumn}</Wrapper>
  }

  return (
    <Wrapper>
      <Bar aria-label="Filtrer par engin de pêche" role="toolbar">
        <Option $isSelected={!selected} aria-pressed={!selected} onClick={() => onChange(undefined)} type="button">
          <Icon $isSelected={!selected}>
            <svg
              aria-hidden
              fill="none"
              height="20"
              stroke="currentColor"
              strokeWidth="1.6"
              viewBox="0 0 24 24"
              width="20"
            >
              <circle cx="7" cy="7" r="3" />
              <circle cx="17" cy="7" r="3" />
              <circle cx="7" cy="17" r="3" />
              <circle cx="17" cy="17" r="3" />
            </svg>
          </Icon>
          <Text>
            <Label>Tous</Label>
          </Text>
        </Option>
        {families.map(({ count, family }) => {
          const isSelected = selected?.id === family.id

          return (
            <Option
              $isSelected={isSelected}
              aria-pressed={isSelected}
              key={family.id}
              onClick={() => onChange(isSelected ? undefined : family)}
              title={family.description}
              type="button"
            >
              <Icon $isSelected={isSelected}>
                <GearIcon family={family} size={22} />
                <ImpactDot $impact={family.impact} title={IMPACT_LABELS[family.impact]} />
              </Icon>
              <Text>
                <Label>{family.label}</Label>
                <Count>{vessels(count)}</Count>
              </Text>
            </Option>
          )
        })}
      </Bar>
      {rightColumn}
    </Wrapper>
  )
}

/** On the right of the map, so that the map stays usable: the gear's impact and carbon, foldable to its title */
function GearExplanation({ count, family, onClear }: { count: number; family: GearFamily; onClear: () => void }) {
  const [isFolded, setIsFolded] = useState(false)

  return (
    <Explanation aria-live="polite">
      <ExplanationHeader>
        <ExplanationIcon>
          <GearIcon family={family} size={30} />
        </ExplanationIcon>
        <ExplanationTitle>
          {family.label}
          <span>
            {formatNumber(count)} navires · {family.codes.slice(0, 3).join(', ')}
          </span>
        </ExplanationTitle>
        <HeaderButton
          aria-expanded={!isFolded}
          aria-label={isFolded ? 'Afficher les détails' : 'Réduire'}
          onClick={() => setIsFolded(!isFolded)}
          title={isFolded ? 'Afficher les détails' : 'Réduire'}
          type="button"
        >
          <svg aria-hidden fill="none" height="16" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" width="16">
            <path d={isFolded ? 'M6 9l6 6 6-6' : 'M6 15l6-6 6 6'} strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </HeaderButton>
        <HeaderButton aria-label="Retirer le filtre" onClick={onClear} title="Retirer le filtre" type="button">
          <svg aria-hidden fill="none" height="16" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" width="16">
            <path d="M6 6l12 12M18 6 6 18" strokeLinecap="round" />
          </svg>
        </HeaderButton>
      </ExplanationHeader>
      <Pills>
        <ImpactPill $impact={family.impact}>{IMPACT_LABELS[family.impact]} pour les habitats et les espèces</ImpactPill>
      </Pills>
      {!isFolded && (
        <>
          <p>{family.description}</p>
          <CarbonFacts
            carbonGroup={family.carbonGroup}
            hasSeabedCarbon={family.carbonGroup === 'demersal' || family.id === 'dragues'}
          />
          <Sources>
            Sources :{' '}
            <a href={SOURCES.bloomGuide.url} rel="noopener" target="_blank">
              BLOOM
            </a>
            ,{' '}
            <a href={SOURCES.shiftCarbon.url} rel="noopener" target="_blank">
              The Shift Project &amp; BLOOM
            </a>
          </Sources>
        </>
      )}
    </Explanation>
  )
}

const appear = keyframes`
  from { opacity: 0; transform: translateY(-6px); }
  to { opacity: 1; transform: none; }
`

const glass = css`
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(250, 249, 246, 0.9);
  box-shadow:
    0 1px 2px rgba(11, 34, 51, 0.06),
    0 12px 32px rgba(11, 34, 51, 0.16);
  backdrop-filter: blur(20px) saturate(1.4);
`

const Wrapper = styled.div`
  position: absolute;
  top: ${PANEL_MARGIN}px;
  right: 72px;
  left: ${PANEL_WIDTH + PANEL_MARGIN * 2}px;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  pointer-events: none;

  > * {
    pointer-events: auto;
  }

  @media ${MOBILE_QUERY} {
    top: 12px;
    right: 64px;
    left: 12px;
  }
`

/** Wraps on a second line when the map is narrow; on phones, one line to swipe, fading on the right */
const Bar = styled.div`
  ${glass}
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
  max-width: 100%;
  padding: 4px;
  border-radius: 16px;

  @media ${MOBILE_QUERY} {
    flex-wrap: nowrap;
    overflow-x: auto;
    scrollbar-width: none;
    mask-image: linear-gradient(to right, #000 calc(100% - 32px), transparent);

    &::-webkit-scrollbar {
      display: none;
    }
  }
`

const Option = styled.button<{ $isSelected: boolean }>`
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 4px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: ${COLORS.ink};
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s;

  &:hover {
    background: ${COLORS.white};
  }

  ${({ $isSelected }) =>
    $isSelected &&
    css`
      &,
      &:hover {
        background: ${COLORS.ink};
        color: ${COLORS.white};
      }
    `}
`

const Icon = styled.span<{ $isSelected: boolean }>`
  position: relative;
  flex: none;
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: ${({ $isSelected }) => ($isSelected ? 'rgba(255, 255, 255, 0.12)' : COLORS.info)};
  color: ${({ $isSelected }) => ($isSelected ? COLORS.white : COLORS.link)};
  transition: background 0.15s;
`

const Text = styled.span`
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  white-space: nowrap;
`

const Label = styled.span`
  font-size: 13px;
  font-weight: 600;
`

const Count = styled.span`
  font-size: 11px;
  opacity: 0.7;
`

/** The gear's impact level, on the corner of its drawing */
const ImpactDot = styled.span<{ $impact: keyof typeof IMPACT_COLORS }>`
  position: absolute;
  top: -2px;
  right: -2px;
  width: 10px;
  height: 10px;
  border: 2px solid ${COLORS.white};
  border-radius: 50%;
  background: ${({ $impact }) => IMPACT_COLORS[$impact].dot};
`

const Pills = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
`

const Sources = styled.div`
  margin-top: 10px;
  color: ${COLORS.muted};
  font-size: 11.5px;

  a {
    color: ${COLORS.muted};
  }
`

/** On the right of the map, under the gear bar: the gear's explanation and the port's cards, scrolling when long */
const RightColumn = styled.div`
  align-self: flex-end;
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 360px;
  max-width: 100%;
  max-height: calc(100vh - 110px);
  overflow-y: auto;
  border-radius: 18px;
  scrollbar-width: none;

  @media ${MOBILE_QUERY} {
    display: none;
  }
`

const Explanation = styled.div`
  ${glass}
  flex: none;
  padding: 12px 14px 14px;
  border-radius: 18px;
  animation: ${appear} 0.25s ease-out;

  p {
    margin: 8px 0 0;
    color: ${COLORS.text};
    font-size: 13px;
    line-height: 1.45;
  }

  @media (prefers-reduced-motion: reduce) {
    animation: none;
  }

  @media ${MOBILE_QUERY} {
    display: none;
  }
`

const ExplanationHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
`

const ExplanationIcon = styled.span`
  flex: none;
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 11px;
  background: linear-gradient(160deg, #dcebf4, ${COLORS.info});
  color: ${COLORS.link};
`

const ExplanationTitle = styled.div`
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  color: ${COLORS.ink};
  font-family: ${FONTS.display};
  font-size: 17px;
  font-weight: 700;

  span {
    color: ${COLORS.muted};
    font-family: ${FONTS.sans};
    font-size: 12px;
    font-weight: 500;
  }
`

const HeaderButton = styled.button`
  flex: none;
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
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

/** Emissions per kg landed, compared with meat per kg edible, and the carbon of the sediments for bottom gears */
function CarbonFacts({ carbonGroup, hasSeabedCarbon }: { carbonGroup: CarbonGroup; hasSeabedCarbon: boolean }) {
  const range = getCarbonRange(carbonGroup)
  const edible = KG_CO2E_PER_KG_EDIBLE[carbonGroup]
  const level = getCarbonLevel(range)
  const scale = Math.max(...PROTEIN_COMPARISONS.map(({ value }) => value))

  return (
    <Carbon>
      <CarbonHeader>
        <strong>Empreinte carbone</strong>
        <ImpactPill $impact={level}>{formatCarbon(range)} kg CO₂e par kg pêché</ImpactPill>
      </CarbonHeader>
      <Comparison aria-label="Par kg consommable, comparé aux viandes">
        {[
          { isGear: true, label: 'ce poisson', value: edible },
          ...PROTEIN_COMPARISONS.map(item => ({ ...item, isGear: false }))
        ]
          .sort((a, b) => b.value - a.value)
          .map(({ isGear, label, value }) => (
            <ComparisonRow $isGear={isGear} key={label}>
              <span>{label}</span>
              <ComparisonBar>
                <span style={{ width: `${Math.max(2, (value / scale) * 100)}%` }} />
              </ComparisonBar>
              <span>{formatKg(value)}</span>
            </ComparisonRow>
          ))}
      </Comparison>
      <CarbonNote>kg CO₂e par kg consommable, en moyenne pour toutes tailles de navires.</CarbonNote>
      {hasSeabedCarbon && (
        <CarbonNote>
          Le contact avec le fond remet aussi en suspension du carbone stocké dans les sédiments : estimé à{' '}
          {SEABED_CARBON}
          pour la flotte française.
        </CarbonNote>
      )}
    </Carbon>
  )
}

const Carbon = styled.div`
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  background: ${COLORS.white};
`

const CarbonHeader = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-bottom: 8px;
  color: ${COLORS.ink};
  font-size: 13px;
`

const Comparison = styled.div`
  display: grid;
  gap: 4px;
`

const ComparisonRow = styled.div<{ $isGear: boolean }>`
  display: grid;
  grid-template-columns: 76px 1fr 32px;
  align-items: center;
  gap: 8px;
  color: ${({ $isGear }) => ($isGear ? COLORS.ink : COLORS.muted)};
  font-size: 12px;
  font-weight: ${({ $isGear }) => ($isGear ? 700 : 400)};

  span:last-child {
    font-variant-numeric: tabular-nums;
    text-align: right;
  }

  ${({ $isGear }) => $isGear && `--bar: ${COLORS.accent};`}
`

const ComparisonBar = styled.span`
  height: 6px;
  overflow: hidden;
  border-radius: 3px;
  background: ${COLORS.divider};

  span {
    display: block;
    height: 100%;
    border-radius: 3px;
    background: var(--bar, ${COLORS.inkSoft});
  }
`

const CarbonNote = styled.p`
  margin: 6px 0 0 !important;
  color: ${COLORS.muted} !important;
  font-size: 11.5px !important;
  line-height: 1.4 !important;
`
