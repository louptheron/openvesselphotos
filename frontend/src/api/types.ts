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

export type Vessel = {
  cfr: string | null
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
  /** "website", "direct", "other" or the host of a site */
  source: string
}

export type UsageStatistics = {
  activeApiKeys: number
  activeApiKeysTotal: number
  apiCalls: number
  apiCallsWithKey: number
  apiKeys: { calls: number; id: number; lastUsedAt: string | null; name: string; organisation: string | null }[]
  apiSources: SourceUsage[]
  daily: DailyUsage[]
  days: number
  imageSources: SourceUsage[]
  imageViews: number
  imageViewsElsewhere: number
  photoCount: number
  siteCount: number
  vesselCount: number
  vesselsAdded: number
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
}
