import { useEffect } from 'react'
import { Route, Routes, useLocation } from 'react-router-dom'

import { Layout } from './components/Layout'
import { AdminPage } from './features/Admin/AdminPage'
import { StatsPage } from './features/Admin/StatsPage'
import { ApiKeyPage } from './features/ApiKey/ApiKeyPage'
import { ContributePage } from './features/Contribute/ContributePage'
import { DocsPage } from './features/Docs/DocsPage'
import { HomePage } from './features/Home/HomePage'
import { LegalPage } from './features/Legal/LegalPage'
import { NotFoundPage } from './features/NotFound/NotFoundPage'
import { VesselPage } from './features/Vessel/VesselPage'
import { VesselsPage } from './features/Vessels/VesselsPage'
import { usePageViewBeacon } from './hooks/usePageViewBeacon'

export function App() {
  const { pathname } = useLocation()
  // The admin's own pages are not counted
  usePageViewBeacon({ skip: /^(\/fr)?\/admin(\/|$)/.test(pathname) })
  // Scroll to the top on each page change. The braces matter: scrollTo returns a Promise in recent browsers, which
  // React would take for the effect's clean-up function and call on the next page change, unmounting the app.
  useEffect(() => {
    window.scrollTo(0, 0)
    // oxlint-disable-next-line react/exhaustive-effect-dependencies
  }, [pathname])

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route element={<HomePage />} index />
        <Route element={<VesselsPage />} path="vessels" />
        <Route element={<VesselPage />} path="vessels/:keyType/:key" />
        <Route element={<ContributePage />} path="contribute" />
        <Route element={<DocsPage />} path="docs" />
        <Route element={<ApiKeyPage />} path="api-key" />
        <Route element={<LegalPage />} path="legal" />
        <Route element={<AdminPage />} path="admin" />
        <Route element={<StatsPage />} path="admin/stats" />
        <Route element={<NotFoundPage />} path="*" />
      </Route>
    </Routes>
  )
}
