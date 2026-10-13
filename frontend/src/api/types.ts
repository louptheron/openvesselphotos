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
  place: string | null
  takenOn: string | null
  urls: PhotoUrls
  width: number
}

export type VesselSummary = {
  cfr: string | null
  flag: string | null
  mainPhoto: Photo | null
  mmsi: string | null
  name: string | null
  photoCount: number
}

export type HistoryEvent = {
  by: string | null
  date: string
  description: string
}

export type Vessel = {
  cfr: string | null
  flag: string | null
  history: HistoryEvent[]
  mmsi: string | null
  name: string | null
  photos: Photo[]
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
  submittedVesselName: string | null
  takenOn: string | null
  width: number
}

export type ReviewQueue = {
  keyLinks: Array<{
    contributor: string | null
    createdAt: string | null
    id: number
    key: string
    keyType: KeyType
    vessel: AdminVessel
  }>
  photos: Array<{
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
