import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'

import type { Messages } from '../i18n/en'

import type {
  ApiKey,
  ApiKeyRequest,
  ApprovedApiKey,
  FishingActivitySyncStatus,
  HomePortCheck,
  HomePortsCheckStatus,
  FishingEffortPeriod,
  HomePortsFromPortVisitsStatus,
  HomePortsFromRegistrationResult,
  HomePortImportResult,
  KeyType,
  PhotoImportStatus,
  PhotoNotesResult,
  Port,
  PortsDataQuality,
  PortDetails,
  PortFishingEffort,
  PortPositionImportResult,
  PortVesselSearchResult,
  ReportReason,
  ReviewQueue,
  VesselImportResult,
  Submission,
  UsageStatistics,
  Vessel,
  VesselFilter,
  VesselFilterOptions,
  VesselSummary,
  VesselsPage,
  WikidataImportStatus
} from './types'

type VesselKeyParams = { key: string; keyType: KeyType }

export const api = createApi({
  baseQuery: fetchBaseQuery({ baseUrl: '/v1' }),
  endpoints: builder => ({
    // Public
    getRecentVessels: builder.query<VesselSummary[], void>({
      providesTags: ['Vessels'],
      query: () => '/vessels/recent'
    }),
    getVessel: builder.query<Vessel, VesselKeyParams>({
      providesTags: ['Vessels'],
      query: ({ key, keyType }) => `/${keyType}/${encodeURIComponent(key)}`
    }),
    getPort: builder.query<PortDetails, string>({
      providesTags: ['Ports'],
      query: locode => `/ports/${encodeURIComponent(locode)}`
    }),
    searchPortVessels: builder.query<PortVesselSearchResult[], string>({
      providesTags: ['Ports'],
      query: q => ({ params: { q }, url: '/ports/vessels' })
    }),
    getPortFishingEffort: builder.query<PortFishingEffort, { gear: string | undefined; locode: string }>({
      providesTags: ['Ports'],
      query: ({ gear, locode }) => ({
        params: gear ? { engin: gear } : {},
        url: `/ports/${encodeURIComponent(locode)}/fishing-effort`
      })
    }),
    getFishingActivitySync: builder.query<FishingActivitySyncStatus, void>({
      providesTags: ['FishingActivitySync'],
      query: () => '/admin/ports/fishing-activity'
    }),
    syncFishingActivity: builder.mutation<FishingActivitySyncStatus, void>({
      invalidatesTags: ['FishingActivitySync', 'Ports'],
      query: () => ({ method: 'POST', url: '/admin/ports/fishing-activity' })
    }),
    getHomePortsCheck: builder.query<HomePortsCheckStatus, void>({
      providesTags: ['HomePortsCheck'],
      query: () => '/admin/ports/home-ports/check'
    }),
    getHomePortChecks: builder.query<HomePortCheck[], void>({
      providesTags: ['HomePortsCheck'],
      query: () => '/admin/ports/home-ports/checks'
    }),
    checkHomePorts: builder.mutation<HomePortsCheckStatus, string | undefined>({
      invalidatesTags: ['HomePortsCheck'],
      query: port => ({ method: 'POST', params: port ? { port } : {}, url: '/admin/ports/home-ports/check' })
    }),
    getFishingEffortPeriod: builder.query<FishingEffortPeriod, void>({
      query: () => '/ports/fishing-effort'
    }),
    getHomePortsFromPortVisits: builder.query<HomePortsFromPortVisitsStatus, void>({
      providesTags: ['HomePortsFromPortVisits'],
      query: () => '/admin/ports/home-ports/from-port-visits'
    }),
    findHomePortsFromPortVisits: builder.mutation<HomePortsFromPortVisitsStatus, void>({
      invalidatesTags: ['HomePortsFromPortVisits', 'Ports'],
      query: () => ({ method: 'POST', url: '/admin/ports/home-ports/from-port-visits' })
    }),
    findHomePortsFromRegistration: builder.mutation<HomePortsFromRegistrationResult, void>({
      invalidatesTags: ['Ports'],
      query: () => ({ method: 'POST', url: '/admin/ports/home-ports/from-registration' })
    }),
    getPortsDataQuality: builder.query<PortsDataQuality, void>({
      query: () => '/ports/data-quality'
    }),
    getPorts: builder.query<Port[], void>({
      providesTags: ['Ports'],
      query: () => '/ports'
    }),
    getVesselFilterOptions: builder.query<VesselFilterOptions, void>({
      providesTags: ['Vessels'],
      query: () => '/vessels/filters'
    }),
    getVesselsPage: builder.query<VesselsPage, VesselFilter & { page: number }>({
      providesTags: ['Vessels'],
      query: params => ({ params, url: '/vessels' })
    }),
    requestApiKey: builder.mutation<Submission, ApiKeyRequest>({
      query: body => ({ body, method: 'POST', url: '/api-key-requests' })
    }),
    reportPhoto: builder.mutation<Submission, VesselKeyParams & { message: string; n: number; reason: ReportReason }>({
      query: ({ key, keyType, message, n, reason }) => ({
        body: { message, reason },
        method: 'POST',
        url: `/${keyType}/${encodeURIComponent(key)}/photos/${n}/reports`
      })
    }),
    searchVessels: builder.query<VesselSummary[], string>({
      providesTags: ['Vessels'],
      query: q => ({ params: { q }, url: '/search' })
    }),
    submitPhoto: builder.mutation<Submission, VesselKeyParams & { form: FormData }>({
      query: ({ form, key, keyType }) => ({
        body: form,
        method: 'POST',
        url: `/${keyType}/${encodeURIComponent(key)}/photos`
      })
    }),
    suggestHomePort: builder.mutation<
      Submission,
      VesselKeyParams & { comment: string; contributor: string; locode: string }
    >({
      query: ({ comment, contributor, key, keyType, locode }) => ({
        body: { comment, contributor, locode },
        method: 'POST',
        url: `/${keyType}/${encodeURIComponent(key)}/home-port`
      })
    }),
    suggestKeyLink: builder.mutation<
      Submission,
      VesselKeyParams & { contributor: string; missingKey: string; missingKeyType: KeyType }
    >({
      query: ({ contributor, key, keyType, missingKey, missingKeyType }) => ({
        body: { contributor, key: missingKey, keyType: missingKeyType },
        method: 'POST',
        url: `/${keyType}/${encodeURIComponent(key)}/link`
      })
    }),

    // Admin (the session is an HttpOnly cookie)
    adminAction: builder.mutation<void, { body?: object; path: string }>({
      invalidatesTags: ['ApiKeys', 'Queue', 'Vessels'],
      query: ({ body, path }) => ({ body: body ?? {}, method: 'POST', url: `/admin/${path}` })
    }),
    approveApiKey: builder.mutation<ApprovedApiKey, number>({
      invalidatesTags: ['ApiKeys', 'Queue'],
      query: id => ({ method: 'POST', url: `/admin/api-keys/${id}/approve` })
    }),
    getActiveApiKeys: builder.query<ApiKey[], void>({
      providesTags: ['ApiKeys'],
      query: () => '/admin/api-keys'
    }),
    importHomePorts: builder.mutation<HomePortImportResult, FormData>({
      invalidatesTags: ['Ports'],
      query: form => ({ body: form, method: 'POST', url: '/admin/ports/home-ports/import' })
    }),
    importPortPositions: builder.mutation<PortPositionImportResult, FormData>({
      invalidatesTags: ['Ports'],
      query: form => ({ body: form, method: 'POST', url: '/admin/ports/positions/import' })
    }),
    importAuctionSales: builder.mutation<
      { auctions: number; matched: string[]; seasonalSpecies: number; unmatched: string[]; year: number },
      FormData
    >({
      invalidatesTags: ['Ports'],
      query: form => ({ body: form, method: 'POST', url: '/admin/ports/auction-sales/import' })
    }),
    importVesselOwners: builder.mutation<
      { errors: string[]; imported: number; read: number; skipped: number },
      FormData
    >({
      invalidatesTags: ['Ports'],
      query: form => ({ body: form, method: 'POST', url: '/admin/ports/owners/import' })
    }),
    importVessels: builder.mutation<VesselImportResult, FormData>({
      invalidatesTags: ['Vessels'],
      query: form => ({ body: form, method: 'POST', url: '/admin/vessels/import' })
    }),
    getWikidataImport: builder.query<WikidataImportStatus, void>({
      providesTags: ['WikidataImport'],
      query: () => '/admin/vessels/wikidata'
    }),
    importWikidata: builder.mutation<WikidataImportStatus, void>({
      invalidatesTags: ['WikidataImport', 'Vessels'],
      query: () => ({ method: 'POST', url: '/admin/vessels/wikidata' })
    }),
    cancelPhotoImport: builder.mutation<void, void>({
      invalidatesTags: ['PhotoImport'],
      query: () => ({ method: 'POST', url: '/admin/photos/import/cancel' })
    }),
    getPhotoImport: builder.query<PhotoImportStatus, void>({
      providesTags: ['PhotoImport'],
      query: () => '/admin/photos/import'
    }),
    importPhotos: builder.mutation<PhotoImportStatus, FormData>({
      invalidatesTags: ['PhotoImport', 'Vessels'],
      query: form => ({ body: form, method: 'POST', url: '/admin/photos/import' })
    }),
    setPhotoNotes: builder.mutation<PhotoNotesResult, FormData>({
      invalidatesTags: ['Vessels'],
      query: form => ({ body: form, method: 'POST', url: '/admin/photos/notes' })
    }),
    getUsage: builder.query<UsageStatistics, number>({
      query: days => ({ params: { days }, url: '/admin/usage' })
    }),
    getReviewQueue: builder.query<ReviewQueue, void>({
      providesTags: ['Queue'],
      query: () => '/admin/queue'
    }),
    login: builder.mutation<void, string>({
      invalidatesTags: ['Queue'],
      query: password => ({ body: { password }, method: 'POST', url: '/admin/login' })
    }),
    logout: builder.mutation<void, void>({
      invalidatesTags: ['Queue'],
      query: () => ({ method: 'POST', url: '/admin/logout' })
    })
  }),
  reducerPath: 'api',
  tagTypes: [
    'ApiKeys',
    'FishingActivitySync',
    'HomePortsCheck',
    'HomePortsFromPortVisits',
    'PhotoImport',
    'Ports',
    'Queue',
    'Vessels',
    'WikidataImport'
  ]
})

export const {
  useAdminActionMutation,
  useApproveApiKeyMutation,
  useCancelPhotoImportMutation,
  useCheckHomePortsMutation,
  useFindHomePortsFromPortVisitsMutation,
  useFindHomePortsFromRegistrationMutation,
  useGetActiveApiKeysQuery,
  useGetFishingActivitySyncQuery,
  useGetFishingEffortPeriodQuery,
  useGetHomePortChecksQuery,
  useGetHomePortsCheckQuery,
  useGetHomePortsFromPortVisitsQuery,
  useGetPhotoImportQuery,
  useGetPortFishingEffortQuery,
  useGetPortQuery,
  useGetPortsDataQualityQuery,
  useGetPortsQuery,
  useGetRecentVesselsQuery,
  useGetReviewQueueQuery,
  useGetUsageQuery,
  useGetVesselFilterOptionsQuery,
  useGetVesselQuery,
  useGetVesselsPageQuery,
  useGetWikidataImportQuery,
  useImportAuctionSalesMutation,
  useImportHomePortsMutation,
  useImportPhotosMutation,
  useImportPortPositionsMutation,
  useImportVesselOwnersMutation,
  useImportVesselsMutation,
  useImportWikidataMutation,
  useLazyGetVesselQuery,
  useLoginMutation,
  useLogoutMutation,
  useReportPhotoMutation,
  useRequestApiKeyMutation,
  useSearchPortVesselsQuery,
  useSyncFishingActivityMutation,
  useSearchVesselsQuery,
  useSetPhotoNotesMutation,
  useSubmitPhotoMutation,
  useSuggestHomePortMutation,
  useSuggestKeyLinkMutation
} = api

/** The message of an RTK Query error: translated from its code, or the backend's own message (in English). */
export function getErrorMessage(error: unknown, t: Messages): string {
  if (error && typeof error === 'object' && 'data' in error) {
    const { data } = error as { data: unknown }
    if (data && typeof data === 'object') {
      const code = 'code' in data && typeof data.code === 'string' ? data.code : undefined
      const translated = code ? t.errors.byCode[code] : undefined
      if (translated) {
        return translated
      }
      if ('message' in data && typeof data.message === 'string') {
        return data.message
      }
    }
  }

  return t.errors.generic
}

export function getErrorStatus(error: unknown): number | string | undefined {
  return error && typeof error === 'object' && 'status' in error
    ? (error as { status: number | string }).status
    : undefined
}
