import type { ReactNode } from 'react'

import type { Port, PortVessel } from '../api/types'
import type { CarbonGroup } from './impacts'

/**
 * The impact of a way of fishing on habitats and species, as rated by BLOOM's guide "Les méthodes de pêche"
 * (https://www.bloomassociation.org/wp-content/uploads/2020/03/BLOOM-Methodes-de-peche.pdf)
 */
export type Impact = 'high' | 'mixed' | 'low'

/**
 * Families of fishing gears, for people who don't know the FAO codes (ISSCFG) of the EU fleet register: each groups
 * the codes of a way of fishing, with a short explanation and a drawing.
 */
export type GearFamily = {
  /** For its carbon footprint, see [CARBON] */
  carbonGroup: CarbonGroup
  codes: string[]
  /** How it works and what it does to the ocean, after BLOOM's guide */
  description: string
  icon: ReactNode
  /** In the URL, e.g. ?engin=chalut-de-fond */
  id: string
  impact: Impact
  label: string
}

const stroke = {
  fill: 'none',
  stroke: 'currentColor',
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  strokeWidth: 1.6
} as const

/** The sea bed, under the bottom gears */
const seabed = <path d="M2 21.5c2-.8 4 .8 6 0s4-.8 6 0 4 .8 6 0 2-.4 2-.4" {...stroke} strokeWidth={1.2} />
/** A boat at the surface */
const boat = <path d="M2.5 6.5h5l-.8 1.8H3.3z" {...stroke} />

export const GEAR_FAMILIES: GearFamily[] = [
  {
    carbonGroup: 'demersal',
    codes: ['OTB', 'OTT', 'PTB', 'TBB', 'TBN', 'TBS', 'OT', 'TB'],
    description:
      'Un filet en forme d’entonnoir, tiré sur le fond et maintenu ouvert par des panneaux. Il est en contact avec les fonds marins et capture aussi des espèces non ciblées.',
    icon: (
      <>
        {boat}
        <path d="M6.5 7.5 13 16.5M6.5 7.5 11 18.5" {...stroke} strokeWidth={1.1} />
        <path d="M11 15.5 20 17.5l1.5 1.5-1.5 1.5-9-1z" {...stroke} />
        {seabed}
      </>
    ),
    id: 'chalut-de-fond',
    impact: 'high',
    label: 'Chalut de fond'
  },
  {
    carbonGroup: 'demersal',
    codes: ['SDN', 'SSC', 'SPR', 'SX'],
    description:
      'Senne danoise ou écossaise : des câbles posés sur le fond rassemblent les poissons, puis un filet en entonnoir les capture. Une technique très efficace sur les poissons de fond.',
    icon: (
      <>
        {boat}
        <path d="M6.5 7.5c-3 6-2 11 4 11.5M6.5 7.5c5 2 11 4 13 11" {...stroke} strokeWidth={1.1} />
        <path d="M10 17.5h9.5l-4.8 2.8z" {...stroke} />
        {seabed}
      </>
    ),
    id: 'senne-demersale',
    impact: 'high',
    label: 'Senne démersale'
  },
  {
    carbonGroup: 'pelagic',
    codes: ['OTM', 'PTM', 'TM', 'TMS'],
    description:
      'Un grand filet en forme d’entonnoir, tiré entre deux eaux, parfois par deux navires. Il cible les poissons en bancs, comme la sardine ou le maquereau, et peut capturer des mammifères marins.',
    icon: (
      <>
        <path d="M2.5 5.5h5l-.8 1.8H3.3z" {...stroke} />
        <path d="M6.5 6.5 11.5 11M6.5 6.5 11 14" {...stroke} strokeWidth={1.1} />
        <path d="M11 10.5 19.5 11.5l1.5 1.2-1.5 1.3-8.5.6z" {...stroke} />
        <path d="M14 18.5c1.5-1 3 1 4.5 0M8 20c1.2-.8 2.4.8 3.6 0" {...stroke} strokeWidth={1.1} />
      </>
    ),
    id: 'chalut-pelagique',
    impact: 'high',
    label: 'Chalut pélagique'
  },
  {
    carbonGroup: 'dredge',
    codes: ['DRB', 'DRH', 'DRM', 'DRX', 'HMD'],
    description:
      'Un cadre métallique muni d’une poche, tiré sur le fond pour récolter les coquillages, comme la coquille Saint-Jacques. Il est en contact avec les fonds marins.',
    icon: (
      <>
        {boat}
        <path d="M6.5 7 12 15" {...stroke} strokeWidth={1.1} />
        <path d="M11 14.5h8.5l-1 5H12z" {...stroke} />
        <path d="M12 19.5l-.6 1.2M14 19.5l-.4 1.2M16 19.5l-.2 1.2M18 19.5v1.2" {...stroke} strokeWidth={1.1} />
        {seabed}
      </>
    ),
    id: 'dragues',
    impact: 'high',
    label: 'Dragues'
  },
  {
    carbonGroup: 'pelagic',
    codes: ['PS', 'PS1', 'PS2', 'LA', 'SB', 'SV'],
    description:
      'Un grand filet vertical encercle un banc de poissons puis se referme par le bas. Peu de contact avec le fond ; autour des dispositifs de concentration de poissons (DCP), il capture aussi d’autres espèces.',
    icon: (
      <>
        {boat}
        <path d="M6 7.5c0 7 3 11.5 7.5 11.5S21 15 21 9.5c0-2-1-4-3-4H9.5" {...stroke} />
        <path d="M9 11c1 .7 2 .7 3 0M13.5 13.5c1 .7 2 .7 3 0M11 16c1 .7 2 .7 3 0" {...stroke} strokeWidth={1.1} />
      </>
    ),
    id: 'senne-tournante',
    impact: 'mixed',
    label: 'Senne tournante'
  },
  {
    carbonGroup: 'static',
    codes: ['GNS', 'GND', 'GNC', 'GNF', 'GTR', 'GTN', 'GN', 'GEN'],
    description:
      'Des filets posés au fond ou dérivants, dans lesquels les poissons se prennent en nageant. Selon les zones, ils peuvent capturer des oiseaux ou des mammifères marins.',
    icon: (
      <>
        <path d="M3 5.5h18" {...stroke} />
        <circle cx="6" cy="4.5" fill="currentColor" r="1.2" />
        <circle cx="12" cy="4.5" fill="currentColor" r="1.2" />
        <circle cx="18" cy="4.5" fill="currentColor" r="1.2" />
        <path
          d="M4 5.5v14M8 5.5v14M12 5.5v14M16 5.5v14M20 5.5v14M4 10h16M4 14.5h16M4 19h16"
          {...stroke}
          strokeWidth={1}
        />
        {seabed}
      </>
    ),
    id: 'filets',
    impact: 'mixed',
    label: 'Filets'
  },
  {
    carbonGroup: 'static',
    codes: ['LLS', 'LLD', 'LL'],
    description:
      'Une ligne principale portant de nombreux hameçons appâtés, posée au fond ou dérivante. Elle peut capturer aussi des oiseaux, des requins ou des tortues.',
    icon: (
      <>
        <path d="M3 4.5h18" {...stroke} />
        <path d="M5 4.5v5M9 4.5v7M13 4.5v5M17 4.5v7" {...stroke} strokeWidth={1.1} />
        <path
          d="M5 9.5v1a1.2 1.2 0 1 1-2.4 0M9 11.5v1a1.2 1.2 0 1 1-2.4 0M13 9.5v1a1.2 1.2 0 1 1-2.4 0M17 11.5v1a1.2 1.2 0 1 1-2.4 0"
          {...stroke}
          strokeWidth={1.1}
        />
        {seabed}
      </>
    ),
    id: 'palangres',
    impact: 'mixed',
    label: 'Palangres'
  },
  {
    carbonGroup: 'static',
    codes: ['LHP', 'LHM', 'LTL', 'LX', 'LVT'],
    description:
      'Une canne ou une ligne à quelques hameçons, tenue à la main ou traînée derrière le bateau. Une pêche sélective : les prises non ciblées peuvent être relâchées.',
    icon: (
      <>
        <path d="M4 20 16 4" {...stroke} />
        <path d="M16 4c2 3 3 7 3 10" {...stroke} strokeWidth={1.1} />
        <path d="M19 14v2.5a2 2 0 1 1-4 0" {...stroke} />
      </>
    ),
    id: 'lignes',
    impact: 'low',
    label: 'Lignes'
  },
  {
    carbonGroup: 'static',
    codes: ['FPO', 'FIX', 'FPN', 'FYK', 'FWR'],
    description:
      'Des pièges appâtés posés au fond, pour les crabes, homards, araignées ou seiches. Peu de contact avec les habitats, et les prises non ciblées sont relâchées vivantes.',
    icon: (
      <>
        <path d="M12 2.5v6" {...stroke} strokeWidth={1.1} />
        <circle cx="12" cy="2.5" fill="currentColor" r="1.2" />
        <path d="M4.5 19.5v-6.5a7.5 4.5 0 0 1 15 0v6.5z" {...stroke} />
        <path d="M8.5 19.5v-8M12 19.5V8.5M15.5 19.5v-8M4.5 15.5h15" {...stroke} strokeWidth={1} />
        {seabed}
      </>
    ),
    id: 'casiers',
    impact: 'low',
    label: 'Casiers'
  }
]

export const IMPACT_LABELS: Record<Impact, string> = {
  high: 'Fort impact',
  low: 'Faible impact',
  mixed: 'Impact ambigu'
}

export function getGearFamily(id: string | null): GearFamily | undefined {
  return GEAR_FAMILIES.find(family => family.id === id)
}

export function getFamilyOfGear(code: string | null): GearFamily | undefined {
  return code ? GEAR_FAMILIES.find(family => family.codes.includes(code)) : undefined
}

/** The port's vessels of this family of gears, or all its vessels without filter */
export function countVessels(port: Port, family: GearFamily | undefined): number {
  // Missing from a response cached before the gears were added
  const gearCounts: Partial<Record<string, number>> = port.gearCounts ?? {}

  return family ? family.codes.reduce((sum, code) => sum + (gearCounts[code] ?? 0), 0) : port.vesselCount
}

export function hasGear(vessel: PortVessel, family: GearFamily | undefined): boolean {
  return !family || (vessel.mainGear !== null && family.codes.includes(vessel.mainGear))
}

/** A drawing of the gear, in the current text color */
export function GearIcon({ family, size = 24 }: { family: GearFamily; size?: number }) {
  return (
    <svg aria-hidden height={size} viewBox="0 0 24 24" width={size}>
      {family.icon}
    </svg>
  )
}
