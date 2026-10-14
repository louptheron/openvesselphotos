import 'leaflet/dist/leaflet.css'

import L from 'leaflet'
import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import type { Port } from '../../api/types'
import { Pill } from '../../components/ui'
import { useTranslation } from '../../i18n'
import { COLORS, FONTS } from '../../theme'
import { COVERAGE_LEGEND, getCoverageColor } from './coverageColors'
import { getShare } from './portsTable'
import { type Bounds, getBounds, getRegionId, OVERSEAS_REGIONS, type OverseasRegionId } from './regions'

/**
 * CARTO's light basemap (Positron) with the key set at build time (VITE_CARTO_BASEMAP_KEY), else OpenStreetMap's
 * standard tiles, e.g. in development. Both draw OpenStreetMap data.
 */
const CARTO_KEY: string | undefined = import.meta.env.VITE_CARTO_BASEMAP_KEY || undefined
const OSM_ATTRIBUTION = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
const TILES = CARTO_KEY
  ? {
      attribution: `${OSM_ATTRIBUTION} &copy; <a href="https://carto.com/attributions">CARTO</a>`,
      url: `https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png?key=${encodeURIComponent(CARTO_KEY)}`
    }
  : { attribution: OSM_ATTRIBUTION, url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png' }

/** From this zoom level, the names of the ports show under their circle. */
const NAMES_ZOOM = 7.5

/** Below this size, a circle shows no number (its tooltip does). */
const MIN_SIZE_WITH_COUNT = 24

/** Smaller circles when zoomed out, where nearby ports overlap: 60% at the zoom of France, full size from 8. */
function getMarkerScale(zoom: number): number {
  return Math.min(1, Math.max(0.5, 0.6 + (zoom - 5) * 0.14))
}

const PADDING: L.PointExpression = [40, 40]

type PlacedPort = Port & { latitude: number; longitude: number }

/**
 * The ports on a map: a circle per port, sized by its number of vessels (written inside), coloured by the share of
 * them with a photo and ringed with that share. Each port links to its page. The table of the ports page gives the
 * same data without the map.
 */
export function PortsMap({ isCompact = false, ports }: { isCompact?: boolean; ports: Port[] }) {
  const { formatNumber, language, t } = useTranslation()
  const navigate = useNavigate()
  const containerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<L.Map>()
  const [hasNames, setHasNames] = useState(isCompact)
  const [markerScale, setMarkerScale] = useState(1)
  const placed = ports.filter((port): port is PlacedPort => port.latitude !== null && port.longitude !== null)
  const byRegion = groupByRegion(placed)
  const mainlandBounds = getBounds(byRegion.get('mainland')?.map(toPoint) ?? [])
  const firstBounds = mainlandBounds ?? getBounds(placed.map(toPoint))
  const overseas = OVERSEAS_REGIONS.filter(region => byRegion.has(region.id))
  const maxVessels = Math.max(1, ...placed.map(port => port.vessels))
  // Rebuilds the markers when the data or the language changes, not on every render
  const markersKey = JSON.stringify([placed, language, markerScale])

  useEffect(() => {
    if (!containerRef.current) {
      return undefined
    }
    const map = L.map(containerRef.current, {
      // The page scrolls over the map: zoom with the buttons, a double click or a pinch
      scrollWheelZoom: false,
      zoomSnap: 0.5
    })
    L.tileLayer(TILES.url, { attribution: TILES.attribution, maxZoom: 18, subdomains: 'abcd' }).addTo(map)
    // One port: centred on it; several: the mainland ones (the overseas ones have their buttons)
    if (!firstBounds) {
      map.setView([46.5, 2.5], 5)
    } else if (placed.length === 1) {
      map.setView(toLatLng(firstBounds[0]), 9)
    } else {
      map.fitBounds(toLatLngBounds(firstBounds), { padding: PADDING })
    }
    mapRef.current = map
    const updateZoom = () => {
      setHasNames(isCompact || map.getZoom() >= NAMES_ZOOM)
      setMarkerScale(isCompact ? 1 : getMarkerScale(map.getZoom()))
    }
    map.on('zoomend', updateZoom)
    updateZoom()

    return () => {
      map.remove()
      mapRef.current = undefined
    }
    // oxlint-disable-next-line react/exhaustive-deps -- the map is created once; the markers follow the data
  }, [])

  useEffect(() => {
    const map = mapRef.current
    if (!map) {
      return undefined
    }
    const layer = L.layerGroup()
    // Largest first, so that the small ports are drawn over the large ones (a copy: toSorted() is not in ES2022)
    // oxlint-disable-next-line unicorn/no-array-sort
    const sorted = [...placed].sort((a, b) => b.vessels - a.vessels)
    sorted.forEach((port, index) => {
      const share = getShare(port)
      const size = Math.round(((isCompact ? 36 : 24) + 34 * Math.sqrt(port.vessels / maxVessels)) * markerScale)
      const summary = t.ports.markerSummary(formatNumber(port.vessels), formatNumber(port.photographed))
      const marker = L.marker(toLatLng(toPoint(port)), {
        icon: L.divIcon({
          className: 'port-marker',
          html: createMarkerContent(port, size, share),
          iconAnchor: [size / 2, size / 2],
          iconSize: [size, size]
        }),
        keyboard: !isCompact,
        riseOnHover: true,
        title: `${port.name}: ${summary}`,
        zIndexOffset: index
      })
      marker.bindTooltip(createTooltipContent(port.name, summary, t.ports.shareWithPhoto(Math.round(share * 100))), {
        direction: 'top',
        offset: [0, -size / 2]
      })
      if (!isCompact) {
        marker.on('click', () => navigate(`/ports/${port.code}`))
      }
      layer.addLayer(marker)
    })
    layer.addTo(map)

    return () => {
      layer.remove()
    }
    // oxlint-disable-next-line react/exhaustive-deps -- markersKey stands for the ports, the language and the zoom
  }, [markersKey])

  const show = (bounds: Bounds) =>
    mapRef.current?.flyToBounds(toLatLngBounds(bounds), { duration: 0.8, maxZoom: 9, padding: PADDING })

  return (
    <Figure>
      {!isCompact && overseas.length > 0 && (
        <Regions aria-label={t.ports.regionsLabel} role="toolbar">
          {mainlandBounds && (
            <Pill $active={false} onClick={() => show(mainlandBounds)} type="button">
              {t.ports.mainland} · {formatNumber(countVessels(byRegion.get('mainland')))}
            </Pill>
          )}
          {overseas.map(region => (
            <Pill key={region.id} $active={false} onClick={() => show(region.bounds)} type="button">
              {t.ports.regions[region.id]} · {formatNumber(countVessels(byRegion.get(region.id)))}
            </Pill>
          ))}
        </Regions>
      )}
      <MapBox ref={containerRef} $hasNames={hasNames} $isCompact={isCompact} />
      {!isCompact && (
        <Legend>
          <LegendItem>
            <SampleRing aria-hidden="true">
              <span>12</span>
            </SampleRing>
            {t.ports.legendCircle}
          </LegendItem>
          <LegendItem>
            <Gradient
              aria-hidden="true"
              style={{
                background: `linear-gradient(to right, ${COVERAGE_LEGEND.map(
                  (stop, index) => `${getCoverageColor(stop)} ${(index / (COVERAGE_LEGEND.length - 1)) * 100}%`
                ).join(', ')})`
              }}
            />
            <span>
              {t.ports.legendColor} ({COVERAGE_LEGEND.map(stop => `${Math.round(stop * 100)}%`).join(' → ')})
            </span>
          </LegendItem>
        </Legend>
      )}
    </Figure>
  )
}

/** The ring with the number of vessels, and the name (shown when zoomed in). Leaflet positions the icon itself. */
function createMarkerContent(port: PlacedPort, size: number, share: number): HTMLElement {
  const content = document.createElement('span')
  content.className = 'port-marker__content'
  content.style.setProperty('--size', `${size}px`)
  content.style.setProperty('--share', `${(share * 100).toFixed(1)}%`)
  content.style.setProperty('--color', getCoverageColor(share))
  const ring = document.createElement('span')
  ring.className = 'port-marker__ring'
  const count = document.createElement('span')
  count.textContent = size >= MIN_SIZE_WITH_COUNT ? formatCount(port.vessels) : ''
  ring.append(count)
  const name = document.createElement('span')
  name.className = 'port-marker__name'
  name.textContent = port.name
  content.append(ring, name)

  return content
}

function createTooltipContent(name: string, summary: string, share: string): HTMLElement {
  const content = document.createElement('div')
  const title = document.createElement('strong')
  title.textContent = name
  content.append(title, document.createElement('br'), summary, document.createElement('br'), share)

  return content
}

/** [longitude, latitude] */
function toPoint(port: PlacedPort): [number, number] {
  return [port.longitude, port.latitude]
}

/** Leaflet takes [latitude, longitude] */
function toLatLng([longitude, latitude]: [number, number]): L.LatLngTuple {
  return [latitude, longitude]
}

function toLatLngBounds([southWest, northEast]: Bounds): L.LatLngBoundsExpression {
  return [toLatLng(southWest), toLatLng(northEast)]
}

function groupByRegion(ports: PlacedPort[]): Map<OverseasRegionId | 'mainland', PlacedPort[]> {
  const groups = new Map<OverseasRegionId | 'mainland', PlacedPort[]>()
  ports.forEach(port => {
    const id = getRegionId(port.latitude, port.longitude)
    groups.set(id, [...(groups.get(id) ?? []), port])
  })

  return groups
}

function countVessels(ports: PlacedPort[] | undefined): number {
  return (ports ?? []).reduce((sum, port) => sum + port.vessels, 0)
}

/** 431 -> "431", 1234 -> "1.2k": fits in the smallest circles */
function formatCount(value: number): string {
  return value < 1000 ? String(value) : `${(value / 1000).toFixed(1).replace(/\.0$/, '')}k`
}

const Figure = styled.figure`
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
`

const Regions = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
`

/** A ring showing the share of vessels with a photo (--share, in green), around a disc coloured by it (--color). */
const RING = `
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: conic-gradient(${COLORS.approve} var(--share), rgba(255, 255, 255, 0.95) 0);
  box-shadow: 0 1px 4px rgba(11, 34, 51, 0.35);

  &::before {
    content: '';
    position: absolute;
    inset: max(2px, min(4px, calc(var(--size, 30px) * 0.12)));
    border-radius: 50%;
    background: var(--color);
  }

  > span {
    position: relative;
    font-weight: 700;
    color: ${COLORS.white};
    text-shadow: 0 1px 2px rgba(11, 34, 51, 0.6);
  }
`

const MapBox = styled.div<{ $hasNames: boolean; $isCompact: boolean }>`
  /* Keeps Leaflet's panes (z-index 400 and more) under the site's header */
  position: relative;
  z-index: 0;
  height: ${p => (p.$isCompact ? '260px' : '560px')};
  border-radius: 12px;
  border: 1px solid ${COLORS.border};
  background: #f2f3f0;
  font-family: ${FONTS.sans};

  @media (max-width: 600px) {
    height: ${p => (p.$isCompact ? '220px' : '440px')};
  }

  .port-marker {
    background: none;
    border: none;
    cursor: ${p => (p.$isCompact ? 'default' : 'pointer')};

    &:focus-visible {
      outline: none;
    }
  }

  .port-marker__content {
    display: block;
    width: var(--size);
    height: var(--size);
  }

  .port-marker__ring {
    ${RING}
    width: 100%;
    height: 100%;
    font-size: max(9px, calc(var(--size) * 0.32));
    transition: transform 0.15s ease-out;
  }

  .port-marker:hover .port-marker__ring,
  .port-marker:focus-visible .port-marker__ring {
    transform: scale(1.12);
  }

  .port-marker:focus-visible .port-marker__ring {
    outline: 3px solid ${COLORS.ink};
    outline-offset: 2px;
  }

  .port-marker__name {
    display: ${p => (p.$hasNames ? 'block' : 'none')};
    position: absolute;
    top: calc(100% + 2px);
    left: 50%;
    transform: translateX(-50%);
    font-size: 12px;
    font-weight: 600;
    color: ${COLORS.ink};
    white-space: nowrap;
    text-shadow:
      0 0 3px ${COLORS.white},
      0 0 3px ${COLORS.white},
      0 0 3px ${COLORS.white};
    pointer-events: none;
  }

  .leaflet-tooltip {
    font-family: ${FONTS.sans};
    font-size: 13px;
    line-height: 1.45;
  }
`

const Legend = styled.figcaption`
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
  font-size: 13px;
  color: ${COLORS.text};
`

const LegendItem = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 8px;
`

const SampleRing = styled.span`
  ${RING}
  --share: 30%;
  --color: ${getCoverageColor(0.3)};
  flex: none;
  width: 30px;
  height: 30px;
  font-size: 10px;
`

const Gradient = styled.span`
  display: block;
  flex: none;
  width: 120px;
  height: 10px;
  border-radius: 5px;
  border: 1px solid ${COLORS.ink};
`
