package org.openvesselphotos.infrastructure.api.admin

import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.use_cases.admin.HomePortImportRow
import org.openvesselphotos.domain.use_cases.admin.PortPositionImportRow
import org.openvesselphotos.domain.use_cases.admin.VesselOwnerImportRow

/**
 * Reads comma-separated files of the ports site: the home ports of the vessels (columns cfr, vessel_name, port_locode,
 * port_name, port_country, observations_total, observations_at_port, mean_distance_km) and the positions of the ports
 * (locode, latitude, longitude). Unknown columns are ignored.
 */
object PortsCsvParser {
    fun parseHomePorts(content: String): List<HomePortImportRow> =
        parse(
            content,
            mapOf(
                "cfr" to listOf("cfr"),
                "vesselName" to listOf("vesselname", "name"),
                "portLocode" to listOf("portlocode", "locode"),
                "portName" to listOf("portname"),
                "portCountry" to listOf("portcountry", "country"),
                "observationsTotal" to listOf("observationstotal"),
                "observationsAtPort" to listOf("observationsatport"),
                "meanDistanceKm" to listOf("meandistancekm"),
            ),
            required = setOf("cfr", "portLocode", "observationsTotal", "observationsAtPort"),
        ) { line, cell ->
            HomePortImportRow(
                line = line,
                cfr = cell("cfr"),
                vesselName = cell("vesselName"),
                portLocode = cell("portLocode"),
                portName = cell("portName"),
                portCountry = cell("portCountry"),
                observationsTotal = cell("observationsTotal"),
                observationsAtPort = cell("observationsAtPort"),
                meanDistanceKm = cell("meanDistanceKm"),
            )
        }

    fun parsePositions(content: String): List<PortPositionImportRow> =
        parse(
            content,
            mapOf(
                "locode" to listOf("locode", "portlocode"),
                "latitude" to listOf("latitude", "lat"),
                "longitude" to listOf("longitude", "lon", "lng"),
            ),
        ) { line, cell ->
            PortPositionImportRow(
                line = line,
                locode = cell("locode"),
                latitude = cell("latitude"),
                longitude = cell("longitude"),
            )
        }

    /** Columns cfr, armateur (or owner), groupe (or group, optional) and source (a link) */
    fun parseOwners(content: String): List<VesselOwnerImportRow> =
        parse(
            content,
            mapOf(
                "cfr" to listOf("cfr"),
                "owner" to listOf("armateur", "armement", "owner"),
                "group" to listOf("groupe", "group"),
                "sourceUrl" to listOf("source", "sourceurl"),
            ),
            required = setOf("cfr", "owner", "sourceUrl"),
        ) { line, cell ->
            VesselOwnerImportRow(
                line = line,
                cfr = cell("cfr"),
                owner = cell("owner"),
                group = cell("group"),
                sourceUrl = cell("sourceUrl"),
            )
        }

    private fun <T> parse(
        content: String,
        columns: Map<String, List<String>>,
        required: Set<String> = columns.keys,
        toRow: (line: Int, cell: (String) -> String?) -> T,
    ): List<T> {
        val records = PhotosCsvParser.readRecords(content.removePrefix("﻿"))
        val (headerLine, header) = records.firstOrNull() ?: throw invalid("The file is empty.")
        val headerNames = header.map { PhotosCsvParser.normalize(it) }
        val indexes =
            columns.mapValues { (column, aliases) ->
                val index = aliases.map { headerNames.indexOf(it) }.firstOrNull { it >= 0 }
                if (index == null && column in required) {
                    throw invalid("No ${aliases.first()} column found in the header (line $headerLine).")
                }
                index
            }

        return records.drop(1).map { (line, cells) ->
            toRow(line) { column -> indexes[column]?.let { cells.getOrNull(it) }?.takeIf { it.isNotBlank() } }
        }
    }

    private fun invalid(message: String) = BackendUsageException(BackendUsageErrorCode.INVALID_INPUT, message)
}
