import { useEffect } from 'react'
import { Route, Routes, useLocation } from 'react-router-dom'

import { Layout } from './components/Layout'
import { AdminPage } from './features/Admin/AdminPage'
import { ContributePage } from './features/Contribute/ContributePage'
import { DocsPage } from './features/Docs/DocsPage'
import { HomePage } from './features/Home/HomePage'
import { VesselPage } from './features/Vessel/VesselPage'

export function App() {
  const { pathname } = useLocation()
  useEffect(() => window.scrollTo(0, 0), [pathname])

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route element={<HomePage />} index />
        <Route element={<VesselPage />} path="vessels/:keyType/:key" />
        <Route element={<ContributePage />} path="contribute" />
        <Route element={<DocsPage />} path="docs" />
        <Route element={<AdminPage />} path="admin" />
        <Route element={<HomePage />} path="*" />
      </Route>
    </Routes>
  )
}
