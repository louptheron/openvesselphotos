export type KeyType = 'cfr' | 'mmsi'

export type PhotoUrls = {
  cfr: string | null
  mmsi: string | null
}

export type Photo = {
  attribution: string
  credit: string
  height: number
  licence: string
  licenceName: string
  n: number
  /** e.g. "Photographed under its former name LE CRI DES FLOTS" */
  note: string | null
  place: string | null
  /** Page of the original photo, for a photo imported from Wikimedia Commons or Flickr */
  source: string | null
  takenOn: string | null
  urls: PhotoUrls
  width: number
}

/** Filters of the vessel directory (query parameters of /v1/vessels and /vessels) */
export type VesselFilter = {
  /** ISO 3166-1 alpha-3, from the CFR or else the MMSI */
  flag?: string
  /** Port of registration ("quartier d'immatriculation" in France) */
  port?: string
  vesselType?: string
  mainGear?: string
}

export type FilterOption = {
  /** For flags: ISO 3166-1 alpha-2, to name the country */
  countryCode: string | null
  count: number
  value: string
}

export type VesselFilterOptions = {
  flags: FilterOption[]
  mainGears: FilterOption[]
  ports: FilterOption[]
  vesselTypes: FilterOption[]
}

/** A page of the vessel directory: vessels with photos, by name */
export type VesselsPage = {
  page: number
  pageCount: number
  /** Vessels with photos, on all pages */
  total: number
  vessels: VesselSummary[]
}

export type VesselSummary = {
  cfr: string | null
  flag: string | null
  mainPhoto: Photo | null
  mmsi: string | null
  name: string | null
  photoCount: number
  port: string | null
  /** Marking painted on the hull, e.g. "LO 752221" */
  registration: string | null
}

export type HistoryEvent = {
  by: string | null
  date: string
  /** In English: the other fields give a translated text (see the vessel page) */
  description: string
  key: string | null
  licence: string | null
  linkedTo: string | null
  photoNumber: number | null
  type: 'KEY_LINKED' | 'PHOTO_PUBLISHED' | 'VESSEL_ADDED'
}

/** Technical data of a vessel, from the EU fleet register. */
export type RegisterData = {
  /** International radio call sign (IRCS) */
  callSign: string | null
  enginePowerKw: number | null
  /** Day of the last import of the register that gave this data */
  importedOn: string | null
  lengthMeters: number | null
  /** FAO code (ISSCFG), e.g. OTB for bottom otter trawls */
  mainGear: string | null
  otherGears: string[]
  tonnageGt: number | null
  /** FAO code (ISSCFV), e.g. TO for a trawler; FX when not specified */
  vesselType: string | null
  yearBuilt: number | null
}

/** What Wikidata knows of a vessel, found by its MMSI. */
export type WikidataData = {
  /** e.g. "Q7868346" */
  id: string
  /** Day of the last import that gave this data */
  importedOn: string | null
  imo: string | null
  lengthMeters: number | null
  /** Names the vessel had or has, other than its name here, e.g. a former name */
  otherNames: string[]
  shipTypeEn: string | null
  shipTypeFr: string | null
  url: string
  /** Year of construction, or else of entry into service */
  yearBuilt: number | null
}

export type Vessel = {
  cfr: string | null
  /** ISO 3166-1 alpha-2 of the flag, from the CFR or else the MMSI's MID */
  countryCode: string | null
  flag: string | null
  history: HistoryEvent[]
  mmsi: string | null
  name: string | null
  photos: Photo[]
  port: string | null
  register: RegisterData | null
  registration: string | null
  /** Other vessels with photos, from the same port or under the same flag */
  related: VesselSummary[]
  /** What Wikidata knows of the vessel, if found by its MMSI */
  wikidata: WikidataData | null
  /** The port the vessel is based at, as shown on the ports site (missing from older responses) */
  homePort?: VesselHomePort | null
}

/**
 * IMPORT: from the imported observations; GFW: the port it visited most in a year, on Global Fishing Watch; REGISTER:
 * the port of its quartier d'immatriculation, the coarsest; MANUAL: suggested by a visitor, checked by the admin;
 * RETURNS: the port it returned to most, from a file of returns to port
 */
export type HomePortSource = 'GFW' | 'IMPORT' | 'MANUAL' | 'REGISTER' | 'RETURNS'

export type VesselHomePort = {
  locode: string
  name: string
  /** Its page on the ports site */
  pageUrl: string
  source: HomePortSource
}

export type Submission = {
  id: number
  status: string
}

export type ApiError = {
  code: string
  message: string
}

export type ReportReason = 'WRONG_VESSEL' | 'COPYRIGHT' | 'PRIVACY' | 'OTHER'

export type AdminVessel = {
  cfr: string | null
  mmsi: string | null
  name: string | null
}

export type AdminPhoto = {
  contributorEmail: string | null
  /** Pseudonymous id of the contributor in the application of the API key */
  contributorId: string | null
  createdAt: string | null
  credit: string
  height: number
  id: number
  licence: string
  licenceName: string
  number: number | null
  place: string | null
  submittedKey: string
  submittedKeyType: KeyType
  /** The key of the other type, when the contributor gave both */
  submittedOtherKey: string | null
  submittedVesselName: string | null
  takenOn: string | null
  width: number
}

export type ApiKey = {
  createdAt: string | null
  email: string
  id: number
  intendedUse: string
  keyPrefix: string | null
  lastUsedAt: string | null
  name: string
  organisation: string | null
  /** Public id of the application, for ?app= on the contribute page */
  publicId: string | null
  reviewedAt: string | null
  status: 'ACTIVE' | 'PENDING' | 'REJECTED' | 'REVOKED'
}

export type ApprovedApiKey = {
  apiKey: ApiKey
  key: string
}

export type ApiKeyRequest = {
  email: string
  intendedUse: string
  name: string
  organisation: string
}

export type VesselImportResult = {
  created: number
  errors: string[]
  outOfFleet: number
  read: number
  skipped: number
  unchanged: number
  updated: number
}

export type PhotoNotesResult = {
  errors: string[]
  read: number
  unchanged: number
  updated: number
}

export type DailyUsage = {
  apiCalls: number
  day: string
  imageViews: number
  vesselsAdded: number
}

export type SourceUsage = {
  count: number
  /** "website" (Open Vessel Photos), "ports-website" (La Pêche française), "direct", "other" or the host of a site */
  source: string
}

export type SiteDailyUsage = {
  apiCalls: number
  day: string
  imageViews: number
  pageViews: number
}

/** The usage of one website by its own pages */
export type SiteUsage = {
  apiCalls: number
  daily: SiteDailyUsage[]
  imageViews: number
  pageViews: number
  site: 'LA_PECHE_FRANCAISE' | 'OPEN_VESSEL_PHOTOS'
  /** path: e.g. "/vessels/cfr/FRA000123456", or "other" */
  topPages: { count: number; path: string }[]
}

export type UsageStatistics = {
  activeApiKeys: number
  activeApiKeysTotal: number
  apiCalls: number
  apiCallsWithKey: number
  apiKeys: {
    calls: number
    id: number
    imageViews: number
    lastUsedAt: string | null
    name: string
    organisation: string | null
  }[]
  apiSources: SourceUsage[]
  daily: DailyUsage[]
  days: number
  imageSources: SourceUsage[]
  imageViews: number
  imageViewsElsewhere: number
  photoCount: number
  siteCount: number
  sites: SiteUsage[]
  vesselCount: number
  vesselsAdded: number
}

export type WikidataImportStatus = {
  done: number
  errors: string[]
  failed: number
  finishedAt: string | null
  /** Vessels found on Wikidata */
  found: number
  isRunning: boolean
  startedAt: string | null
  /** Vessels with photos and an MMSI */
  total: number
}

export type PhotoImportStatus = {
  alreadyImported: number
  done: number
  errors: string[]
  failed: number
  finishedAt: string | null
  imported: number
  invalid: number
  isRunning: boolean
  startedAt: string | null
  total: number
}

export type ReviewQueue = {
  apiKeyRequests: ApiKey[]
  keyLinks: Array<{
    contributor: string | null
    createdAt: string | null
    id: number
    key: string
    keyType: KeyType
    vessel: AdminVessel
  }>
  photos: Array<{
    apiKeyName: string | null
    photo: AdminPhoto
    vessel: AdminVessel | null
  }>
  reports: Array<{
    createdAt: string | null
    id: number
    message: string | null
    photo: AdminPhoto
    reason: ReportReason
    vessel: AdminVessel
  }>
  homePorts: PendingHomePort[]
}

/** A home port suggested by a visitor, for the admin to check */
export type PendingHomePort = {
  cfr: string
  comment: string | null
  contributor: string | null
  createdAt: string | null
  currentPort: { locode: string; name: string } | null
  currentSource: HomePortSource | null
  id: number
  /** Whether Open Vessel Photos has a page for the vessel */
  isKnownVessel: boolean
  suggestedPort: { locode: string; name: string }
  vesselName: string | null
}

/** A port of the ports site, with the vessels based at it (see PortsController) */
export type Port = {
  country: string
  latitude: number | null
  locode: string
  longitude: number | null
  name: string
  vesselCount: number
  /** Vessels by main gear (FAO code, e.g. OTB), for those whose gear is known */
  gearCounts: Record<string, number>
  /** The fish auction of the port: its sales of all species in its last year (missing from older responses) */
  auction?: AuctionTotal | null
  /** In the URL of its page, e.g. "le-guilvinec" for /ports/le-guilvinec; null in a port's details */
  slug?: string | null
}

export type AuctionTotal = {
  name: string
  quantityTonnes: number
  valueThousandEuros: number
  year: number
}

export type PortVessel = {
  cfr: string
  lengthMeters: number | null
  mainGear: string | null
  mainPhoto: Photo | null
  meanDistanceKm: number | null
  name: string | null
  observationsAtPort: number
  observationsTotal: number
  /** The vessel's page on Open Vessel Photos, when it has one */
  pageUrl: string | null
  photoCount: number
  /** The company owning the vessel, from a public source */
  owner: { group: string | null; name: string; sourceUrl: string } | null
  registration: string | null
  homePortSource?: HomePortSource
  /** Gross tonnage */
  tonnageGt: number | null
  vesselType: string | null
  yearBuilt: number | null
}

/** A vessel of the ports site found by its name or CFR */
export type PortVesselSearchResult = {
  cfr: string
  name: string | null
  portLocode: string
  portName: string
}

export type PortDetails = {
  /** The fish auction of the port, when it has one (FranceAgriMer) */
  auction: PortAuction | null
  port: Port
  vessels: PortVessel[]
}

/** Sales declared at the fish auction of a port in its last year */
export type PortAuction = {
  name: string
  previousQuantityTonnes: number | null
  priceEurosPerKg: number | null
  quantityTonnes: number
  /** The main species sold, by value */
  species: AuctionSpecies[]
  /** The Council regulation, the source of the TACs of the species; null when none has one */
  fishingOpportunities: FishingOpportunities | null
  /** Ifremer's assessment, the source of the stock statuses of the species; null when none has one */
  stockAssessment: StockAssessment | null
  valueThousandEuros: number
  year: number
}

export type StockAssessment = {
  /** e.g. 2025 for the "Bilan 2025", of the landings of 2024 */
  edition: number
  url: string
}

export type FishingOpportunities = {
  url: string
  /** The year of the TACs, e.g. 2026 */
  year: number
}

/** A TAC the Union fixes for a stock, with the initial quota of France (before exchanges) */
export type FishingOpportunity = {
  /** e.g. "Manche Est (7d)" */
  area: string
  franceTonnes: number
  /** e.g. "SOL/07D." */
  tacCode: string
  /** null when the Union does not set it, e.g. in Norwegian waters */
  tacTonnes: number | null
}

/** A stock status from Ifremer, or VARIABLE when the main stocks of a species differ */
export type StockStatusCode =
  'BON_ETAT' | 'EFFONDRE' | 'NON_CLASSIFIE' | 'RECONSTITUABLE' | 'SURPECHE' | 'SURPECHE_DEGRADE' | 'VARIABLE'

/** The status of the stocks a species is landed from in the waters of a port */
export type StockStatus = {
  /** e.g. "en bon état", "variable selon le stock" */
  label: string
  status: StockStatusCode
  stocks: FishStock[]
}

export type FishStock = {
  label: string
  /** As Ifremer names it, e.g. "Sole commune (Solea solea), Division VIId" */
  name: string
  region: string
  status: StockStatusCode
  /** French landings from the stock in the region */
  tonnes: number
}

export type AuctionSpecies = {
  /** Its sales in all French auctions by month, from January: its season */
  monthlyQuantitiesTonnes: number[] | null
  name: string
  previousQuantityTonnes: number | null
  priceEurosPerKg: number | null
  quantityTonnes: number
  /** The TACs it is fished under in the waters of the port, France's largest quota first; null when it has none */
  quotas: FishingOpportunity[] | null
  /** The status of the stocks it is landed from in the waters of the port (Ifremer); null when not assessed */
  stockStatus: StockStatus | null
  valueThousandEuros: number
}

export type HomePortImportResult = {
  errors: string[]
  imported: number
  ports: number
  read: number
  skipped: number
  withoutPort: number
}

export type PortReturnsImportResult = {
  errors: string[]
  imported: number
  ports: number
  read: number
  skipped: number
  /** Vessels returning as often to two ports or more, by CFR: left out */
  ties: string[]
}

export type PortPositionImportResult = {
  errors: string[]
  read: number
  skipped: number
  unknown: number
  updated: number
}

/** How much of the French fleet has a known home port, and when the data last changed (see /mises-a-jour) */
export type PortsDataQuality = {
  fromObservations: number
  fromPortVisits: number
  /** From the quartier d'immatriculation, when no other source gives a home port */
  fromRegistration: number
  /** e.g. 2026-11-20 */
  lastUpdate: string | null
  registerVessels: number
  /** From 0 to 1; null without register */
  share: number | null
  withHomePort: number
}

/** The fishing activity map: the last 12 complete months, when a Global Fishing Watch token is set */
export type FishingEffortPeriod = {
  from: string
  /** The gear families drawn: the trawls only, the other vessels mostly lacking AIS */
  gearFamilies: string[]
  isEnabled: boolean
  to: string
}

/** Where the vessels of a port fished over the last 12 months */
export type PortFishingEffort = {
  /** [longitude, latitude, hours of fishing] of each cell of 0.05° */
  cells: [number, number, number][]
  trackedVessels: number
}

export type FishingActivitySyncStatus = {
  done: number
  errors: string[]
  failed: number
  finishedAt: string | null
  found: number
  isRunning: boolean
  startedAt: string | null
  total: number
  withFishing: number
}

export type HomePortsFromRegistrationResult = {
  candidates: number
  /** e.g. "CAEN → Port-en-Bessin (FRPEB), 12" */
  matched: string[]
  placed: number
  /** e.g. "SAINT PIERRE (14)" */
  unmatched: string[]
  withoutPlace: number
}

export type HomePortsFromPortVisitsStatus = {
  done: number
  errors: string[]
  failed: number
  farFromPorts: number
  finishedAt: string | null
  found: number
  isRunning: boolean
  placed: number
  startedAt: string | null
  total: number
  unknownPorts: string[]
  withoutVisits: number
}

export type HomePortCheckResult = 'AGREES' | 'DISAGREES' | 'NEARBY' | 'NOT_FOUND' | 'NOT_ON_THE_MAP' | 'TOO_FEW_VISITS'

export type HomePortsCheckStatus = {
  countsByResult: Partial<Record<HomePortCheckResult, number>>
  done: number
  errors: string[]
  failed: number
  finishedAt: string | null
  isRunning: boolean
  startedAt: string | null
  total: number
}

/** An imported home port checked against the port the vessel visited most in a year, on Global Fishing Watch */
export type HomePortCheck = {
  cfr: string
  checkedOn: string
  distanceKm: number | null
  gfwLocode: string | null
  gfwPortName: string | null
  importedLocode: string
  importedPortName: string | null
  result: HomePortCheckResult
  vesselName: string | null
  visitsAtGfwPort: number
  visitsAtImported: number
  visitsTotal: number
}
