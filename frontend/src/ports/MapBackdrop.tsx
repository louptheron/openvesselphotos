import 'ol/ol.css'

import { Map, View } from 'ol'
import { fromLonLat } from 'ol/proj'
import { MapboxVectorLayer } from 'ol-mapbox-style'
import { useEffect, useRef } from 'react'
import styled from 'styled-components'

import { COLORS } from '../theme'
import { BASEMAP_STYLE_URL } from './PortsMap'

/**
 * The basemap of the map, still and faded, behind the pages beyond it (see ContentPage): the same place, the same
 * site. Not interactive, and hidden from assistive technologies.
 */
export function MapBackdrop() {
  const elementRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!elementRef.current) {
      return undefined
    }
    const map = new Map({
      controls: [],
      interactions: [],
      layers: [new MapboxVectorLayer({ styleUrl: BASEMAP_STYLE_URL })],
      target: elementRef.current,
      // France, close: its Atlantic coast on the left of the page, the Mediterranean on the right
      view: new View({ center: fromLonLat([2.2, 46.4]), zoom: 6.8 })
    })

    return () => map.setTarget(undefined)
  }, [])

  return (
    <Backdrop aria-hidden>
      <MapElement ref={elementRef} />
      <Wash />
    </Backdrop>
  )
}

const Backdrop = styled.div`
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: ${COLORS.ground};
`

const MapElement = styled.div`
  position: absolute;
  inset: 0;
`

/** The map stays in the background: the text in front of it must read as on paper */
const Wash = styled.div`
  position: absolute;
  inset: 0;
  background: rgba(245, 243, 238, 0.45);
`
