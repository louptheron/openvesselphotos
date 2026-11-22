/**
 * What each admin action changes, and what it relies on: the admin page shows it with each section, so that an admin
 * knows, before running an action, which other sections it affects and which ones to run again afterwards.
 */

export type AdminSectionId =
  | 'review'
  | 'vessels'
  | 'wikidata'
  | 'photos'
  | 'photoNotes'
  | 'homePorts'
  | 'homePortsFromPortReturns'
  | 'portPositions'
  | 'homePortsFromVisits'
  | 'homePortsFromRegistration'
  | 'homePortsCheck'
  | 'fishingGrounds'
  | 'auctionSales'
  | 'owners'

export type AdminGroupId = 'review' | 'vessels' | 'ports'

/**
 * - replacesAll: replaces all its data with the file's, nothing of the previous file is kept
 * - completes: adds what is missing, never overwrites
 * - updatesListed: changes only what the file lists
 * - publishes: published at once, without review
 * - recomputes: computes its data again from other sections
 * - readOnly: changes nothing, only reports
 * - decides: approves or rejects contributions
 */
export type AdminEffect =
  'completes' | 'decides' | 'publishes' | 'readOnly' | 'recomputes' | 'replacesAll' | 'updatesListed'

/** Where the public sites show the data */
export type SiteArea = 'portPages' | 'portsMap' | 'vesselPages'

export type AdminSectionMeta = {
  effect: AdminEffect
  group: AdminGroupId
  id: AdminSectionId
  /** Runs in the background, with a progress */
  isJob: boolean
  /** The public pages it changes */
  shownOn: SiteArea[]
  /** The sections whose data it reads: changing them may call for running it again */
  uses: AdminSectionId[]
}

/** In the order to run them on an empty database */
export const ADMIN_SECTIONS: AdminSectionMeta[] = [
  { effect: 'decides', group: 'review', id: 'review', isJob: false, shownOn: ['vesselPages', 'portPages'], uses: [] },
  {
    effect: 'completes',
    group: 'vessels',
    id: 'vessels',
    isJob: false,
    shownOn: ['vesselPages', 'portPages'],
    uses: []
  },
  { effect: 'publishes', group: 'vessels', id: 'photos', isJob: true, shownOn: ['vesselPages', 'portPages'], uses: [] },
  {
    effect: 'updatesListed',
    group: 'vessels',
    id: 'photoNotes',
    isJob: false,
    shownOn: ['vesselPages'],
    uses: ['photos']
  },
  {
    effect: 'completes',
    group: 'vessels',
    id: 'wikidata',
    isJob: true,
    shownOn: ['vesselPages'],
    uses: ['vessels', 'photos', 'review']
  },
  {
    effect: 'replacesAll',
    group: 'ports',
    id: 'homePorts',
    isJob: false,
    shownOn: ['portsMap', 'portPages'],
    uses: []
  },
  {
    effect: 'replacesAll',
    group: 'ports',
    id: 'homePortsFromPortReturns',
    isJob: false,
    shownOn: ['portsMap', 'portPages'],
    uses: []
  },
  {
    effect: 'updatesListed',
    group: 'ports',
    id: 'portPositions',
    isJob: false,
    shownOn: ['portsMap'],
    uses: ['homePorts']
  },
  {
    effect: 'completes',
    group: 'ports',
    id: 'homePortsFromVisits',
    isJob: true,
    shownOn: ['portsMap', 'portPages'],
    uses: ['vessels', 'homePorts', 'portPositions']
  },
  {
    effect: 'completes',
    group: 'ports',
    id: 'homePortsFromRegistration',
    isJob: false,
    shownOn: ['portsMap', 'portPages'],
    uses: ['vessels', 'homePorts', 'portPositions', 'homePortsFromVisits']
  },
  {
    effect: 'readOnly',
    group: 'ports',
    id: 'homePortsCheck',
    isJob: true,
    shownOn: [],
    uses: ['vessels', 'homePorts', 'portPositions']
  },
  {
    effect: 'recomputes',
    group: 'ports',
    id: 'fishingGrounds',
    isJob: true,
    shownOn: ['portPages'],
    uses: ['vessels', 'homePorts', 'homePortsFromPortReturns', 'homePortsFromVisits', 'homePortsFromRegistration']
  },
  {
    effect: 'replacesAll',
    group: 'ports',
    id: 'auctionSales',
    isJob: false,
    shownOn: ['portsMap', 'portPages'],
    uses: ['homePorts']
  },
  { effect: 'replacesAll', group: 'ports', id: 'owners', isJob: false, shownOn: ['portPages'], uses: ['vessels'] }
]

export const ADMIN_GROUPS: AdminGroupId[] = ['review', 'vessels', 'ports']

export function getSection(id: AdminSectionId): AdminSectionMeta {
  const section = ADMIN_SECTIONS.find(it => it.id === id)
  if (!section) {
    throw new Error(`No admin section ${id}`)
  }

  return section
}

/** The sections that read this one's data: background jobs to run again after it, others simply affected */
export function getDependents(id: AdminSectionId): AdminSectionMeta[] {
  return ADMIN_SECTIONS.filter(section => section.uses.includes(id))
}

/** The anchor of a section on the admin page */
export function sectionAnchor(id: AdminSectionId): string {
  return `admin-${id}`
}
