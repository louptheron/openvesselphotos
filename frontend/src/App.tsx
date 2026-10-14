import { useEffect } from 'react'
import { Route, Routes, useLocation } from 'react-router-dom'

import { Layout } from './components/Layout'
import { AdminPage } from './features/Admin/AdminPage'
import { StatsPage } from './features/Admin/StatsPage'
import { ApiKeyPage } from './features/ApiKey/ApiKeyPage'
import { ContributePage } from './features/Contribute/ContributePage'
import { DocsPage } from './features/Docs/DocsPage'
import { HomePage } from './features/Home/HomePage'
import { NotFoundPage } from './features/NotFound/NotFoundPage'
import { PortPage } from './features/Ports/PortPage'
import { PortsPage } from './features/Ports/PortsPage'
import { VesselPage } from './features/Vessel/VesselPage'

export function App() {
  const { pathname } = useLocation()
  // Scroll to the top on each page change (in a block: recent browsers return a promise, which React would call)
  useEffect(() => {
    window.scrollTo(0, 0)
    // oxlint-disable-next-line react/exhaustive-effect-dependencies
  }, [pathname])

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route element={<HomePage />} index />
        <Route element={<VesselPage />} path="vessels/:keyType/:key" />
        <Route element={<PortsPage />} path="ports" />
        <Route element={<PortPage />} path="ports/:code" />
        <Route element={<ContributePage />} path="contribute" />
        <Route element={<DocsPage />} path="docs" />
        <Route element={<ApiKeyPage />} path="api-key" />
        <Route element={<AdminPage />} path="admin" />
        <Route element={<StatsPage />} path="admin/stats" />
        <Route element={<NotFoundPage />} path="*" />
      </Route>
    </Routes>
  )
}
