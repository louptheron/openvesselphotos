import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Navigate, Route, Routes, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import {
  useGetFishingEffortPeriodQuery,
  useGetPortFishingEffortQuery,
  useGetPortQuery,
  useGetPortsQuery
} from '../api/api'
import { usePageViewBeacon } from '../hooks/usePageViewBeacon'
import { of } from '../i18n/fr'
import { COLORS } from '../theme'
import { AuctionSales } from './AuctionSales'
import { ContentPage } from './ContentPage'
import { GearFilter } from './GearFilter'
import { type GearFamily, getGearFamily } from './gears'
import { MOBILE_QUERY, PANEL_MARGIN, PANEL_WIDTH, PORT_SHEET_HEIGHT, SHEET_HEIGHT, SHEET_TOP_OFFSET } from './layout'
import { PortPanel } from './PortPanel'
import { type MapPadding, PortsMap, type PortsMapHandle } from './PortsMap'
import type { Extent } from './regions'
import { getPortPath, SITE_NAME, SITE_TAGLINE, vessels } from './texts'

type Props = {
  /** The page as rendered by the server, for the pages beyond the map */
  serverHtml: string
}

export function PortsApp({ serverHtml }: Props) {
  usePageViewBeacon()

  return (
    <Routes>
      <Route element={<MapPage />} path="/" />
      {/* The same element, so that the map stays the same from a port to another */}
      <Route element={<MapPage />} path="/ports/:slug" />
      {/* The old URLs of the ports, by LOCODE (e.g. /FRQUY): the server moves them, so does the app */}
      <Route element={<LocodeRedirect serverHtml={serverHtml} />} path="/:locode" />
      <Route element={<ContentPage html={serverHtml} />} path="*" />
    </Routes>
  )
}

const LOCODE = /^[A-Z]{2}[A-Z0-9]{3}(-\d{1,4})?$/i

function LocodeRedirect({ serverHtml }: Props) {
  const { locode = '' } = useParams()
  const { search } = useLocation()
  const { data: ports, isLoading } = useGetPortsQuery(undefined, { skip: !LOCODE.test(locode) })
  const port = ports?.find(({ locode: portLocode }) => portLocode === locode.toUpperCase())

  if (port) {
    return <Navigate replace to={{ pathname: getPortPath(port), search }} />
  }

  return isLoading ? null : <ContentPage html={serverHtml} />
}

/** The map, under a floating panel listing the ports, or the vessels of the selected one */
function MapPage() {
  const { slug } = useParams()
  const navigate = useNavigate()
  const mapRef = useRef<PortsMapHandle>(null)
  const { data: ports = [], isError, isLoading } = useGetPortsQuery()
  const locode = useMemo(() => (slug ? ports.find(port => port.slug === slug)?.locode : undefined), [ports, slug])
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
      ? `Port de pêche ${of(selected.name)} : ${vessels(selected.vesselCount)}${selected.auction ? `, criée et chiffres ${selected.auction.year}` : ' de pêche professionnelle'} | ${SITE_NAME}`
      : `${SITE_NAME} — ${SITE_TAGLINE.toLowerCase()}`
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
    (selectedLocode?: string) => {
      const port = selectedLocode ? ports.find(({ locode: portLocode }) => portLocode === selectedLocode) : undefined
      navigate({ pathname: port ? getPortPath(port) : '/', search })
    },
    [navigate, ports, search]
  )
  const fitExtent = useCallback((extent: Extent) => mapRef.current?.fitExtent(extent), [])
  // The panel hides the left of the map on wide screens, its bottom half on phones
  const padding = useMemo<MapPadding>(
    () =>
      // The gear filter covers the top of the map
      isMobile
        ? [SHEET_TOP_OFFSET, 0, Math.round(window.innerHeight * (locode ? PORT_SHEET_HEIGHT : SHEET_HEIGHT)), 0]
        : // The cards on the right of the map hide its right side
          [76, auction || gear ? RIGHT_COLUMN_WIDTH : 0, 0, PANEL_WIDTH + PANEL_MARGIN * 2],
    [auction, gear, isMobile, locode]
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
  /* Figures line up in every list and table */
  font-variant-numeric: tabular-nums;
`
