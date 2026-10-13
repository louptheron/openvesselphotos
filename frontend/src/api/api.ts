import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'

import type {
  ApiKey,
  ApiKeyRequest,
  ApprovedApiKey,
  KeyType,
  ReportReason,
  ReviewQueue,
  Submission,
  Vessel,
  VesselSummary
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
  tagTypes: ['ApiKeys', 'Queue', 'Vessels']
})

export const {
  useAdminActionMutation,
  useApproveApiKeyMutation,
  useGetActiveApiKeysQuery,
  useGetRecentVesselsQuery,
  useGetReviewQueueQuery,
  useGetVesselQuery,
  useLazyGetVesselQuery,
  useLoginMutation,
  useLogoutMutation,
  useReportPhotoMutation,
  useRequestApiKeyMutation,
  useSearchVesselsQuery,
  useSubmitPhotoMutation,
  useSuggestKeyLinkMutation
} = api

/** Extracts the backend message from an RTK Query error. */
export function getErrorMessage(error: unknown): string {
  if (error && typeof error === 'object' && 'data' in error) {
    const { data } = error as { data: unknown }
    if (data && typeof data === 'object' && 'message' in data && typeof data.message === 'string') {
      return data.message
    }
  }

  return 'Something went wrong. Please try again.'
}

export function getErrorStatus(error: unknown): number | string | undefined {
  return error && typeof error === 'object' && 'status' in error
    ? (error as { status: number | string }).status
    : undefined
}
