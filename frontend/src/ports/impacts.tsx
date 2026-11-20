import styled from 'styled-components'

import { COLORS } from '../theme'
import type { Impact } from './gears'

export const SOURCES = {
  bloomGuide: {
    label: 'BLOOM, « Les méthodes de pêche » (2020)',
    url: 'https://www.bloomassociation.org/wp-content/uploads/2020/03/BLOOM-Methodes-de-peche.pdf'
  },
  bloomOligarchs: {
    label: 'BLOOM, « Les oligarques de la mer » (2026)',
    url: 'https://bloomassociation.org/wp-content/uploads/2026/06/Oligarques-de-la-mer.pdf'
  },
  shiftCarbon: {
    label: 'The Shift Project & BLOOM, « Bilan des émissions carbone de la pêche française » (2025)',
    url: 'https://theshiftproject.org/app/uploads/2025/11/Bilan-des-emissions-carbone-de-la-peche-Rapport-final-BLOOM-x-Shift.pdf'
  }
}

/** The fleets of the carbon report (2022 data): demersal or pelagic trawls and seines, dredges, static gears */
export type CarbonGroup = 'demersal' | 'dredge' | 'pelagic' | 'static'

/** Vessels of 24 m or more: industrial fishing, as defined by the Institut Agro and BLOOM */
export const INDUSTRIAL_LENGTH = 24

/**
 * Greenhouse gas emissions per kg landed, in kg CO₂e, for coastal (under 12 m), offshore (12 to 24 m) and industrial
 * (24 m or more) vessels: The Shift Project & BLOOM (2025), figure 43, page 117.
 */
const KG_CO2E_PER_KG_LANDED: Record<CarbonGroup, [number, number, number]> = {
  demersal: [4.3, 4.3, 3.9],
  dredge: [0.4, 1.3, 1.4],
  pelagic: [1.3, 0.8, 1.8],
  static: [2.6, 2.5, 2.0]
}

/** Per kg of edible product, all sizes: same report, figure 44, page 118 */
export const KG_CO2E_PER_KG_EDIBLE: Record<CarbonGroup, number> = {
  demersal: 9.6,
  dredge: 1.2,
  pelagic: 3.0,
  static: 5.1
}

/** Other animal proteins, per kg edible (Agribalyse, agriculture phase only): same figure */
export const PROTEIN_COMPARISONS = [
  { label: 'bœuf', value: 28.3 },
  { label: 'porc', value: 6.3 },
  { label: 'poulet', value: 3.7 }
]

/**
 * Bottom trawls and dredges stir up the carbon stored in the sediments: 104 to 883 kt CO₂e a year for the French fleet
 * according to the models, as much as all its fuel at most. Same report, page 94.
 */
export const SEABED_CARBON = '104 à 883 kt CO₂e par an'

export function getCarbonRange(group: CarbonGroup): [number, number] {
  const values = KG_CO2E_PER_KG_LANDED[group]

  return [Math.min(...values), Math.max(...values)]
}

export function formatKg(value: number): string {
  return value.toLocaleString('fr', { maximumFractionDigits: 1, minimumFractionDigits: 1 })
}

export function formatCarbon([min, max]: [number, number]): string {
  return min === max ? formatKg(min) : `${formatKg(min)} à ${formatKg(max)}`
}

/** Low under 1.5 kg CO₂e per kg landed, high from 3 */
export function getCarbonLevel([min, max]: [number, number]): Impact {
  const value = (min + max) / 2

  return value >= 3 ? 'high' : value >= 1.5 ? 'mixed' : 'low'
}

export const IMPACT_COLORS: Record<Impact, { background: string; dot: string; text: string }> = {
  high: { background: '#FBEAE4', dot: '#C2410C', text: '#8A2E0B' },
  low: { background: '#E3F2E8', dot: '#1D7A46', text: '#14532D' },
  mixed: { background: '#FDF3DC', dot: '#C68A0C', text: '#6B4A06' }
}

/** A coloured dot before a short label, e.g. "● Fort impact" */
export const ImpactPill = styled.span<{ $impact: Impact }>`
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 1px 6px;
  border-radius: 3px;
  background: ${({ $impact }) => IMPACT_COLORS[$impact].background};
  color: ${({ $impact }) => IMPACT_COLORS[$impact].text};
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;

  &::before {
    content: '';
    flex: none;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: ${({ $impact }) => IMPACT_COLORS[$impact].dot};
  }
`

/** Neutral, for facts that are not a rating: industrial fishing, owner */
export const FactPill = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 1px 6px;
  border: 1px solid ${COLORS.ink};
  border-radius: 3px;
  color: ${COLORS.ink};
  font-size: 11px;
  font-weight: 500;
  line-height: 1.35;
`
