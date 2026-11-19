package org.openvesselphotos.domain.search_engines

import org.openvesselphotos.domain.entities.Vessel

/** Tells search engines that pages changed, so that they crawl them again soon rather than at their next visit. */
interface SearchEngineNotifier {
    /** The vessel's page changed: a photo was published or removed. Never fails, never waits. */
    fun vesselChanged(vessel: Vessel)
}
