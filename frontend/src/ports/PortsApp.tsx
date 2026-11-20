import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Route, Routes, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import {
  useGetFishingEffortPeriodQuery,
  useGetPortFishingEffortQuery,
  useGetPortQuery,
  useGetPortsQuery
} from '../api/api'
import { of } from '../i18n/fr'
import { COLORS } from '../theme'
import { AuctionSales } from './AuctionSales'
import { GearFilter } from './GearFilter'
import { type GearFamily, getGearFamily } from './gears'
import { MOBILE_QUERY, PANEL_MARGIN, PANEL_WIDTH, SHEET_HEIGHT } from './layout'
import { PortPanel } from './PortPanel'
import { type MapPadding, PortsMap, type PortsMapHandle } from './PortsMap'
import type { Extent } from './regions'
import { SITE_NAME, vessels } from './texts'

export function PortsApp() {
  return (
    <Routes>
      {/* One route, so that the map stays the same from a port to another */}
      <Route element={<MapPage />} path="/:locode?" />
    </Routes>
  )
}

/** The map, under a floating panel listing the ports, or the vessels of the selected one */
function MapPage() {
  const { locode } = useParams()
  const navigate = useNavigate()
  const mapRef = useRef<PortsMapHandle>(null)
  const { data: ports = [], isError, isLoading } = useGetPortsQuery()
  const { data: fishingEffort } = useGetFishingEffortPeriodQuery()
  // The same request as the panel's: done once
  const { data: portDetails } = useGetPortQuery(locode ?? '', { skip: !locode })
  const auction = locode && portDetails?.port.locode === locode ? portDetails.auction : null
  const selected = useMemo(() => ports.find(port => port.locode === locode), [locode, ports])
  const isMobile = useMediaQuery(MOBILE_QUERY)
  const [highlightedLocode, setHighlightedLocode] = useState<string>()
  // The gear filter is in the URL (e.g. ?engin=chalut-de-fond): kept from a port to another, and shareable
  const [searchParams, setSearchParams] = useSearchParams()
  const { search } = useLocation()
  const gear = getGearFamily(searchParams.get(GEAR_PARAM))
  // Where the vessels of the port fished, for the trawls only (see GetFishingEffort)
  const isPortEffortWanted =
    Boolean(locode && fishingEffort?.isEnabled) && (!gear || (fishingEffort?.gearFamilies.includes(gear.id) ?? false))
  const { data: portFishingEffort } = useGetPortFishingEffortQuery(
    { gear: gear?.id, locode: locode ?? '' },
    { skip: !isPortEffortWanted }
  )
  const setGear = useCallback(
    (family: GearFamily | undefined) => setSearchParams(family ? { [GEAR_PARAM]: family.id } : {}, { replace: true }),
    [setSearchParams]
  )

  // The server sets the title on the first load of each page (see PortsPagesController)
  useEffect(() => {
    document.title = selected
      ? `Port de pêche ${of(selected.name)} : ${vessels(selected.vesselCount)}${selected.auction ? ' et sa criée' : ''} | ${SITE_NAME}`
      : `${SITE_NAME} : la flotte, les ports et les criées`
  }, [selected])

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && locode) {
        navigate({ pathname: '/', search })
      }
    }
    window.addEventListener('keydown', onKeyDown)

    return () => window.removeEventListener('keydown', onKeyDown)
  }, [locode, navigate, search])

  const select = useCallback(
    (selectedLocode?: string) => navigate({ pathname: selectedLocode ? `/${selectedLocode}` : '/', search }),
    [navigate, search]
  )
  const fitExtent = useCallback((extent: Extent) => mapRef.current?.fitExtent(extent), [])
  // The panel hides the left of the map on wide screens, its bottom half on phones
  const padding = useMemo<MapPadding>(
    () =>
      // The gear filter covers the top of the map
      isMobile
        ? [64, 0, Math.round(window.innerHeight * SHEET_HEIGHT), 0]
        : // The cards on the right of the map hide its right side
          [76, auction || gear ? RIGHT_COLUMN_WIDTH : 0, 0, PANEL_WIDTH + PANEL_MARGIN * 2],
    [auction, gear, isMobile]
  )

  return (
    <Page>
      <PortsMap
        fishingEffort={fishingEffort}
        portFishingEffort={portFishingEffort}
        gear={gear}
        highlightedLocode={locode ? undefined : highlightedLocode}
        onSelect={select}
        padding={padding}
        ports={ports}
        ref={mapRef}
        selectedLocode={selected?.locode}
      />
      <GearFilter onChange={setGear} ports={ports} selected={gear}>
        {auction && <AuctionSales auction={auction} isFloating key={locode} />}
      </GearFilter>
      <PortPanel
        gear={gear}
        isError={isError}
        isLoading={isLoading}
        locode={locode}
        onFitExtent={fitExtent}
        onHighlight={setHighlightedLocode}
        onSelect={select}
        ports={ports}
      />
    </Page>
  )
}

const GEAR_PARAM = 'engin'
/** The column of cards on the right of the map, with its margins */
const RIGHT_COLUMN_WIDTH = 360 + 72 + 16

function useMediaQuery(query: string): boolean {
  const [matches, setMatches] = useState(() => window.matchMedia(query).matches)

  useEffect(() => {
    const list = window.matchMedia(query)
    const onChange = () => setMatches(list.matches)
    list.addEventListener('change', onChange)

    return () => list.removeEventListener('change', onChange)
  }, [query])

  return matches
}

const Page = styled.div`
  position: fixed;
  inset: 0;
  overflow: hidden;
  background: ${COLORS.ground};
`
