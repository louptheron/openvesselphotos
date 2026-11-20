import Feature from 'ol/Feature'
import OlMap from 'ol/Map'
import Overlay from 'ol/Overlay'
import View from 'ol/View'
import Attribution from 'ol/control/Attribution'
import ScaleLine from 'ol/control/ScaleLine'
import Point from 'ol/geom/Point'
import VectorLayer from 'ol/layer/Vector'
import WebGLVectorLayer from 'ol/layer/WebGLVector'
import { fromLonLat, transformExtent } from 'ol/proj'
import VectorSource from 'ol/source/Vector'
import type { FlatStyle } from 'ol/style/flat'
import { Fill, Stroke, Style, Text } from 'ol/style'
import { MapboxVectorLayer } from 'ol-mapbox-style'
import { createPortal } from 'react-dom'
import { forwardRef, useEffect, useImperativeHandle, useMemo, useRef, useState } from 'react'
import styled, { keyframes } from 'styled-components'

import type { Port } from '../api/types'
import { COLORS, FONTS } from '../theme'
import { type GearFamily, countVessels } from './gears'
import { MOBILE_QUERY, PANEL_MARGIN, PANEL_WIDTH } from './layout'
import { formatNumber, vessels } from './texts'

/** Free vector tiles from OpenStreetMap data, no key needed (openfreemap.org) */
const BASEMAP_STYLE_URL = 'https://tiles.openfreemap.org/styles/positron'
const PORT_ZOOM = 10.5
/** Port names are shown from this zoom level, the vessel counts of the biggest ports at any level */
const LABEL_ZOOM = 8.5

export type MapPadding = [top: number, right: number, bottom: number, left: number]

export type PortsMapHandle = {
  /** [west, south, east, north] in degrees */
  fitExtent: (extent: [number, number, number, number]) => void
}

type Props = {
  /** Shows only the vessels using this family of gears */
  gear: GearFamily | undefined
  /** A port hovered in the panel, highlighted on the map */
  highlightedLocode?: string | undefined
  onSelect: (locode: string | undefined) => void
  /** The area hidden by the panel, so that the map centers what stays visible */
  padding: MapPadding
  ports: Port[]
  selectedLocode: string | undefined
}

/**
 * The ports on a vector basemap. The ports are drawn with WebGL: their size, color and hover state are computed on the
 * GPU from their vessel count and from style variables, so hovering or selecting a port only updates two variables.
 */
export const PortsMap = forwardRef<PortsMapHandle, Props>(function PortsMap(
  { gear, highlightedLocode, onSelect, padding, ports, selectedLocode },
  handle
) {
  const elementRef = useRef<HTMLDivElement>(null)
  // OpenLayers moves the overlays' elements into the map: React renders into them through portals, never removing them
  const [tooltipElement] = useState(() => document.createElement('div'))
  const [pulseElement] = useState(() => document.createElement('div'))
  const mapRef = useRef<OlMap>(undefined)
  const portsLayerRef = useRef<WebGLVectorLayer>(undefined)
  const tooltipOverlayRef = useRef<Overlay>(undefined)
  const pulseOverlayRef = useRef<Overlay>(undefined)
  const sourceRef = useRef(new VectorSource<Feature<Point>>())
  const onSelectRef = useRef(onSelect)
  const selectedLocodeRef = useRef(selectedLocode)
  const [hovered, setHovered] = useState<Port>()

  useEffect(() => {
    onSelectRef.current = onSelect
    selectedLocodeRef.current = selectedLocode
  }, [onSelect, selectedLocode])

  useImperativeHandle(handle, () => ({
    fitExtent: extent =>
      mapRef.current?.getView().fit(transformExtent(extent, 'EPSG:4326', 'EPSG:3857'), {
        duration: 700,
        maxZoom: PORT_ZOOM,
        padding: [24, 24, 24, 24]
      })
  }))

  useEffect(() => {
    if (!elementRef.current) {
      return undefined
    }
    const portsLayer = new WebGLVectorLayer({
      source: sourceRef.current,
      style: PORT_STYLE,
      variables: { hovered: '', selected: '' }
    })
    const labelsLayer = new VectorLayer({
      declutter: true,
      source: sourceRef.current,
      style: (feature, resolution) =>
        getLabelStyle(feature.get('port') as Port, feature.get('vesselCount') as number, resolution)
    })
    const tooltip = new Overlay({ element: tooltipElement, offset: [0, -18], positioning: 'bottom-center' })
    const pulse = new Overlay({ element: pulseElement, positioning: 'center-center', stopEvent: false })
    const map = new OlMap({
      controls: [new Attribution({ collapsible: true }), new ScaleLine({ bar: false, minWidth: 80 })],
      layers: [new MapboxVectorLayer({ styleUrl: BASEMAP_STYLE_URL }), portsLayer, labelsLayer],
      overlays: [tooltip, pulse],
      target: elementRef.current,
      view: new View({
        center: fromLonLat(METROPOLITAN_FRANCE_CENTER),
        maxZoom: 16,
        minZoom: 2,
        zoom: 5.6
      })
    })

    const portAt = (pixel: number[]): Port | undefined =>
      map.forEachFeatureAtPixel(pixel, feature => feature.get('port') as Port, {
        hitTolerance: 4,
        layerFilter: layer => layer === portsLayer
      })
    map.on('singleclick', event => onSelectRef.current(portAt(event.pixel)?.locode))
    map.on('pointermove', event => {
      if (event.dragging) {
        return
      }
      const port = portAt(event.pixel)
      setHovered(current => (current?.locode === port?.locode ? current : port))
    })
    map.getViewport()?.addEventListener('pointerleave', () => setHovered(undefined))

    mapRef.current = map
    portsLayerRef.current = portsLayer
    tooltipOverlayRef.current = tooltip
    pulseOverlayRef.current = pulse

    return () => map.setTarget(undefined)
  }, [pulseElement, tooltipElement])

  // Frames metropolitan France beside the panel on the first load, unless a port is selected
  const hasFramedRef = useRef(false)
  useEffect(() => {
    const view = mapRef.current?.getView()
    if (!view || hasFramedRef.current || ports.length === 0) {
      return
    }
    hasFramedRef.current = true
    mapRef.current?.updateSize()
    view.padding = [...padding]
    if (!selectedLocode) {
      view.fit(transformExtent(METROPOLITAN_FRANCE, 'EPSG:4326', 'EPSG:3857'), { padding: [32, 32, 32, 32] })
    }
  }, [padding, ports, selectedLocode])

  // With a gear filter, each port is sized by its vessels using that gear, and those without any are hidden
  useEffect(() => {
    sourceRef.current.clear()
    sourceRef.current.addFeatures(
      ports
        .filter(port => port.latitude !== null && port.longitude !== null)
        .map(port => ({ count: countVessels(port, gear), port }))
        .filter(({ count, port }) => count > 0 || port.locode === selectedLocodeRef.current)
        // The biggest ports first, below the smaller ones, which stay visible and clickable
        // oxlint-disable-next-line unicorn/no-array-sort
        .sort((a, b) => b.count - a.count)
        .map(
          ({ count, port }) =>
            new Feature({
              geometry: new Point(fromLonLat([port.longitude ?? 0, port.latitude ?? 0])),
              locode: port.locode,
              port,
              vesselCount: Math.max(count, 1),
              // A ring around the ports with a fish auction
              hasAuction: port.auction ? 1 : 0
            })
        )
    )
  }, [gear, ports])

  // The port under the pointer, or the one hovered in the panel's list
  const shown = useMemo(
    () => hovered ?? ports.find(port => port.locode === highlightedLocode),
    [highlightedLocode, hovered, ports]
  )

  useEffect(() => {
    portsLayerRef.current?.updateStyleVariables({ hovered: shown?.locode ?? '' })
    tooltipOverlayRef.current?.setPosition(shown && shown.locode !== selectedLocode ? positionOf(shown) : undefined)
  }, [selectedLocode, shown])

  useEffect(() => {
    mapRef.current?.getViewport()?.style.setProperty('cursor', hovered ? 'pointer' : '')
  }, [hovered])

  useEffect(() => {
    const view = mapRef.current?.getView()
    if (view) {
      view.padding = [...padding]
    }
  }, [padding])

  // Flies to the selected port, unless it is in view already at a close zoom
  useEffect(() => {
    portsLayerRef.current?.updateStyleVariables({ selected: selectedLocode ?? '' })
    const port = ports.find(found => found.locode === selectedLocode)
    const position = port ? positionOf(port) : undefined
    pulseOverlayRef.current?.setPosition(position)
    const map = mapRef.current
    if (!map || !position) {
      return
    }
    const view = map.getView()
    const isInView = (view.getZoom() ?? 0) >= PORT_ZOOM - 1.5 && isVisible(map, position, padding)
    if (!isInView) {
      view.animate({ center: position, duration: 900, zoom: Math.max(view.getZoom() ?? 0, PORT_ZOOM) })
    }
  }, [padding, ports, selectedLocode])

  const zoomBy = (delta: number) => {
    const view = mapRef.current?.getView()
    view?.animate({ duration: 250, zoom: (view.getZoom() ?? 0) + delta })
  }

  return (
    <>
      <MapElement aria-label="Carte des ports de pêche" ref={elementRef} role="application" />
      <Controls>
        <ControlGroup>
          <ControlButton aria-label="Zoomer" onClick={() => zoomBy(1)} title="Zoomer" type="button">
            <svg
              aria-hidden
              fill="none"
              height="18"
              stroke="currentColor"
              strokeWidth="2"
              viewBox="0 0 24 24"
              width="18"
            >
              <path d="M12 5v14M5 12h14" strokeLinecap="round" />
            </svg>
          </ControlButton>
          <ControlButton aria-label="Dézoomer" onClick={() => zoomBy(-1)} title="Dézoomer" type="button">
            <svg
              aria-hidden
              fill="none"
              height="18"
              stroke="currentColor"
              strokeWidth="2"
              viewBox="0 0 24 24"
              width="18"
            >
              <path d="M5 12h14" strokeLinecap="round" />
            </svg>
          </ControlButton>
        </ControlGroup>
        <ControlGroup>
          <ControlButton
            aria-label="Recentrer sur la France"
            onClick={() =>
              mapRef.current?.getView().fit(transformExtent(METROPOLITAN_FRANCE, 'EPSG:4326', 'EPSG:3857'), {
                duration: 700,
                padding: [32, 32, 32, 32]
              })
            }
            title="Recentrer sur la France"
            type="button"
          >
            <svg
              aria-hidden
              fill="none"
              height="18"
              stroke="currentColor"
              strokeWidth="2"
              viewBox="0 0 24 24"
              width="18"
            >
              <circle cx="12" cy="12" r="4" />
              <path d="M12 2v3M12 19v3M2 12h3M19 12h3" strokeLinecap="round" />
            </svg>
          </ControlButton>
        </ControlGroup>
      </Controls>
      {createPortal(
        <Tooltip>
          {shown && (
            <>
              <strong>{shown.name}</strong>
              <span>
                {gear
                  ? `${vessels(countVessels(shown, gear))} · ${gear.label.toLowerCase()}`
                  : vessels(shown.vesselCount)}
              </span>
              {shown.auction && <AuctionLine>Criée · {formatTonnes(shown.auction.quantityTonnes)} vendues</AuctionLine>}
            </>
          )}
        </Tooltip>,
        tooltipElement
      )}
      {createPortal(<Pulse />, pulseElement)}
      <Legend aria-label="Légende">
        <span>
          <i />
          Port d’attache
        </span>
        <span>
          <i data-auction />
          Port avec criée
        </span>
      </Legend>
    </>
  )
})

const METROPOLITAN_FRANCE_CENTER = [2.4, 46.6]
/** [west, south, east, north] */
const METROPOLITAN_FRANCE: [number, number, number, number] = [-5.2, 41.3, 9.6, 51.1]

function positionOf(port: Port): number[] {
  return fromLonLat([port.longitude ?? 0, port.latitude ?? 0])
}

/** Whether a position is in the part of the map not hidden by the panel */
function isVisible(map: OlMap, position: number[], padding: MapPadding): boolean {
  const pixel = map.getPixelFromCoordinate(position)
  const size = map.getSize()
  if (!pixel || !size) {
    return false
  }
  const [x = 0, y = 0] = pixel
  const [width = 0, height = 0] = size

  return x > padding[3] + 40 && x < width - padding[1] - 40 && y > padding[0] + 40 && y < height - padding[2] - 40
}

/** Grows with the number of vessels, and a little with the zoom */
const RADIUS = [
  '*',
  ['interpolate', ['linear'], ['get', 'vesselCount'], 1, 4.5, 5, 7, 20, 11, 50, 16, 100, 22],
  ['interpolate', ['linear'], ['zoom'], 4, 0.85, 8, 1, 12, 1.35]
]

const IS_SELECTED = ['==', ['get', 'locode'], ['var', 'selected']]
const IS_HOVERED = ['==', ['get', 'locode'], ['var', 'hovered']]
const HAS_AUCTION = ['==', ['get', 'hasAuction'], 1]
/** The ring of the ports with a fish auction */
export const AUCTION_COLOR = '#D99A1E'

const PORT_STYLE: FlatStyle = {
  'circle-fill-color': [
    'case',
    IS_SELECTED,
    COLORS.accent,
    IS_HOVERED,
    '#2A86C0',
    ['interpolate', ['linear'], ['get', 'vesselCount'], 1, '#5E93B5', 10, '#2D6A92', 40, '#0E4A6E', 80, '#0B2233']
  ],
  'circle-radius': ['case', IS_SELECTED, ['*', RADIUS, 1.15], IS_HOVERED, ['*', RADIUS, 1.12], RADIUS],
  'circle-stroke-color': ['case', IS_SELECTED, '#FFFFFF', HAS_AUCTION, AUCTION_COLOR, '#FFFFFF'],
  'circle-stroke-width': ['case', IS_SELECTED, 3, HAS_AUCTION, 3, 1.5]
} as FlatStyle

const labelStyles = new Map<string, Style[]>()

/** The vessel count inside the biggest ports, and the names of the ports when zoomed in */
function getLabelStyle(port: Port, count: number, resolution: number): Style[] {
  const showsName = resolution < RESOLUTION_OF_LABEL_ZOOM
  const showsCount = count >= 30 || (count >= 10 && resolution < RESOLUTION_OF_COUNT_ZOOM)
  const key = `${port.locode}-${count}-${showsName}-${showsCount}`
  const cached = labelStyles.get(key)
  if (cached) {
    return cached
  }
  const styles: Style[] = []
  if (showsCount) {
    styles.push(
      new Style({
        text: new Text({
          fill: new Fill({ color: COLORS.white }),
          font: `600 11px ${FONTS.sans}`,
          text: String(count)
        })
      })
    )
  }
  if (showsName) {
    styles.push(
      new Style({
        text: new Text({
          fill: new Fill({ color: COLORS.ink }),
          font: `600 13px ${FONTS.sans}`,
          offsetY: 10 + Math.sqrt(count) * 2,
          stroke: new Stroke({ color: 'rgba(255, 255, 255, 0.95)', width: 4 }),
          text: port.name,
          textBaseline: 'top'
        })
      })
    )
  }
  labelStyles.set(key, styles)

  return styles
}

/** Web Mercator resolution at the zoom level from which the port names are shown */
const RESOLUTION_OF_LABEL_ZOOM = 156543.03392804097 / 2 ** LABEL_ZOOM
const RESOLUTION_OF_COUNT_ZOOM = 156543.03392804097 / 2 ** 7

const MapElement = styled.div`
  position: absolute;
  inset: 0;
  background: #e8edf0;

  .ol-scale-line {
    right: 16px;
    bottom: 16px;
    left: auto;
    padding: 4px 8px;
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(8px);
  }

  .ol-scale-line-inner {
    border-color: ${COLORS.muted};
    color: ${COLORS.muted};
    font: 500 11px ${FONTS.sans};
  }

  .ol-attribution {
    right: 16px;
    bottom: 48px;
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(8px);
    font: 11px ${FONTS.sans};
  }

  .ol-attribution button {
    border-radius: 6px;
    background: transparent;
    color: ${COLORS.muted};
  }

  @media (max-width: 760px) {
    .ol-scale-line {
      display: none;
    }

    .ol-attribution {
      top: 76px;
      right: 12px;
      bottom: auto;
    }
  }
`

const Controls = styled.div`
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;

  @media (max-width: 760px) {
    top: 12px;
    right: 12px;
  }
`

const ControlGroup = styled.div`
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 12px;
  background: rgba(250, 249, 246, 0.9);
  box-shadow: 0 6px 20px rgba(11, 34, 51, 0.16);
  backdrop-filter: blur(16px);
`

const ControlButton = styled.button`
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border: none;
  background: transparent;
  color: ${COLORS.ink};
  cursor: pointer;
  transition: background 0.15s;

  & + & {
    border-top: 1px solid ${COLORS.divider};
  }

  &:hover {
    background: ${COLORS.white};
  }
`

const Legend = styled.div`
  position: absolute;
  bottom: 16px;
  left: ${PANEL_WIDTH + PANEL_MARGIN * 2}px;
  z-index: 1;
  display: flex;
  gap: 14px;
  padding: 7px 12px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 12px;
  background: rgba(250, 249, 246, 0.9);
  box-shadow: 0 6px 20px rgba(11, 34, 51, 0.12);
  color: ${COLORS.ink};
  font: 500 12px ${FONTS.sans};
  backdrop-filter: blur(16px);

  span {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  i {
    width: 12px;
    height: 12px;
    border: 1.5px solid ${COLORS.white};
    border-radius: 50%;
    background: #2d6a92;
    box-shadow: 0 0 0 1px rgba(11, 34, 51, 0.15);
  }

  i[data-auction] {
    border: 3px solid ${AUCTION_COLOR};
  }

  @media ${MOBILE_QUERY} {
    display: none;
  }
`

const AuctionLine = styled.span`
  color: #f3c46b !important;
`

const Tooltip = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 7px 12px;
  border-radius: 10px;
  background: ${COLORS.ink};
  box-shadow: 0 8px 24px rgba(11, 34, 51, 0.28);
  color: ${COLORS.white};
  font: 12px ${FONTS.sans};
  white-space: nowrap;
  pointer-events: none;

  &:empty {
    display: none;
  }

  strong {
    font-size: 13px;
    font-weight: 600;
  }

  span {
    color: ${COLORS.inkSoft};
  }

  &::after {
    content: '';
    position: absolute;
    bottom: -5px;
    left: 50%;
    width: 10px;
    height: 10px;
    background: ${COLORS.ink};
    transform: translateX(-50%) rotate(45deg);
  }
`

const pulse = keyframes`
  from { transform: scale(0.6); opacity: 0.7; }
  to { transform: scale(2.6); opacity: 0; }
`

const Pulse = styled.div`
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 2px solid ${COLORS.accent};
  pointer-events: none;
  animation: ${pulse} 1.8s ease-out infinite;

  @media (prefers-reduced-motion: reduce) {
    animation: none;
    opacity: 0.6;
  }
`

/** e.g. "15 656 t" */
function formatTonnes(tonnes: number): string {
  return `${formatNumber(Math.round(tonnes))} t`
}
