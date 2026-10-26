package org.openvesselphotos.domain.wikidata

import org.openvesselphotos.domain.entities.WikidataData

interface WikidataClient {
    /** What Wikidata knows of the vessels with these MMSIs, by MMSI; vessels it does not know are left out. */
    fun findByMmsi(mmsis: Collection<String>): Map<String, WikidataData>
}
