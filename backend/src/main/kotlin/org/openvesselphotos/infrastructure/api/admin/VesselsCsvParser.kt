package org.openvesselphotos.infrastructure.api.admin

import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.use_cases.admin.VesselImportRow

/**
 * Reads a list of vessels: an export of the EU fleet register (columns "CFR", "MMSI", "Name of Vessel",
 * "External Marking", "Place of Registration Name"…, or "Vessel Name", "Ext. Marking", "Port Name" in older exports) or a simple file with the columns cfr, mmsi, name, registration, port, port_code. The register's event tells vessels that
 * left the fleet. The separator (";" or ",") and
 * the column order come from the header line; unknown columns are ignored.
 */
object VesselsCsvParser {
    private val COLUMNS =
        mapOf(
            "cfr" to listOf("cfr"),
            "mmsi" to listOf("mmsi"),
            "name" to listOf("nameofvessel", "vesselname", "name", "nom", "nomdunavire"),
            "registration" to listOf("extmarking", "externalmarking", "marking", "registration", "immatriculation"),
            "event" to listOf("eventtype", "eventcode"),
            "portCode" to listOf("placeofregistrationcode", "portcode"),
            "vesselType" to listOf("vesseltype"),
            "yearBuilt" to listOf("yearofconstruction"),
            "length" to listOf("loam", "loa"),
            "enginePower" to listOf("powerofmainenginekw"),
            "tonnage" to listOf("tonnagegt"),
            "mainGear" to listOf("mainfishinggear"),
            "otherGear1" to listOf("subsidiaryfishinggear1"),
            "otherGear2" to listOf("subsidiaryfishinggear2"),
            "otherGear3" to listOf("subsidiaryfishinggear3"),
            "otherGear4" to listOf("subsidiaryfishinggear4"),
            "otherGear5" to listOf("subsidiaryfishinggear5"),
            "callSign" to listOf("ircs"),
            "port" to listOf("placeofregistrationname", "portname", "port", "portdimmatriculation"),
        )

    fun parse(content: String): List<VesselImportRow> {
        val lines = content.removePrefix("").lines().filter { it.isNotBlank() }
        val header = lines.firstOrNull() ?: throw invalid("The file is empty.")
        val separator = if (header.count { it == ';' } >= header.count { it == ',' }) ';' else ','
        val headerNames = splitLine(header, separator).map { normalize(it) }
        val indexes =
            COLUMNS.mapValues { (_, aliases) ->
                aliases.firstNotNullOfOrNull { alias ->
                    headerNames.indexOf(alias).takeIf {
                        it >=
                            0
                    }
                }
            }
        if (indexes["cfr"] == null && indexes["mmsi"] == null) {
            throw invalid("No CFR or MMSI column found in the header: ${header.take(200)}")
        }

        return lines.drop(1).mapIndexed { index, line ->
            val cells = splitLine(line, separator)

            fun cell(column: String) = indexes[column]?.let { cells.getOrNull(it) }

            VesselImportRow(
                line = index + 2,
                cfr = cell("cfr"),
                mmsi = cell("mmsi"),
                name = cell("name"),
                registration = cell("registration"),
                port = cell("port"),
                portCode = cell("portCode"),
                vesselType = cell("vesselType"),
                yearBuilt = cell("yearBuilt"),
                length = cell("length"),
                enginePower = cell("enginePower"),
                tonnage = cell("tonnage"),
                mainGear = cell("mainGear"),
                otherGears = (1..5).mapNotNull { cell("otherGear$it")?.takeIf { gear -> gear.isNotBlank() } },
                callSign = cell("callSign"),
                event = cell("event"),
            )
        }
    }

    /** Splits a CSV line, with fields optionally quoted ("a;b" or "say ""hi"""). */
    private fun splitLine(
        line: String,
        separator: Char,
    ): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var isQuoted = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && isQuoted && line.getOrNull(index + 1) == '"' -> {
                    current.append('"')
                    index++
                }
                char == '"' -> isQuoted = !isQuoted
                char == separator && !isQuoted -> {
                    cells += current.toString().trim()
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }
        cells += current.toString().trim()

        return cells
    }

    private fun normalize(value: String) = value.lowercase().replace(Regex("[^a-z0-9]"), "")

    private fun invalid(message: String) = BackendUsageException(BackendUsageErrorCode.INVALID_INPUT, message)
}
