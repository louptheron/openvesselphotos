package org.openvesselphotos.infrastructure.api.admin

import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.use_cases.admin.PhotoImportRow

/**
 * Reads a list of photos to import, one per line, with the columns mmsi (or cfr), vessel_name, author, licence (SPDX
 * identifier or label, e.g. "CC-BY-SA-3.0" or "CC BY-SA 3.0"), file_page (the page of the original photo),
 * image_1600_url (or image_url) and, optionally, date. Unknown columns are ignored; fields may span several lines
 * when quoted.
 */
object PhotosCsvParser {
    private val COLUMNS =
        mapOf(
            "cfr" to listOf("cfr"),
            "mmsi" to listOf("mmsi"),
            "vesselName" to listOf("vesselname", "name"),
            "credit" to listOf("author", "credit"),
            // ovp_licence first: the licence as Open Vessel Photos names it, when the export provides it
            "licence" to listOf("ovplicence", "licence", "license"),
            "takenOn" to listOf("date", "takenon"),
            "sourceUrl" to listOf("filepage", "sourceurl", "source"),
            "imageUrl" to listOf("image1600url", "imageurl"),
        )

    fun parse(content: String): List<PhotoImportRow> {
        val records = readRecords(content.removePrefix("﻿"))
        val (headerLine, header) = records.firstOrNull() ?: throw invalid("The file is empty.")
        val headerNames = header.map { normalize(it) }

        fun indexesOf(column: String) =
            requireNotNull(COLUMNS[column]).map { headerNames.indexOf(it) }.filter { it >= 0 }

        if (indexesOf("cfr").isEmpty() && indexesOf("mmsi").isEmpty()) {
            throw invalid("No CFR or MMSI column found in the header (line $headerLine).")
        }
        listOf("credit", "licence", "sourceUrl", "imageUrl").forEach { column ->
            if (indexesOf(column).isEmpty()) {
                throw invalid("No ${COLUMNS[column]?.first()} column found in the header (line $headerLine).")
            }
        }

        return records.drop(1).map { (line, cells) ->
            // The first non-empty cell among the column's aliases
            fun cell(column: String) =
                indexesOf(column).firstNotNullOfOrNull { index -> cells.getOrNull(index)?.takeIf { it.isNotBlank() } }

            PhotoImportRow(
                line = line,
                cfr = cell("cfr"),
                mmsi = cell("mmsi"),
                vesselName = cell("vesselName"),
                credit = cell("credit"),
                licence = cell("licence"),
                takenOn = cell("takenOn"),
                sourceUrl = cell("sourceUrl"),
                imageUrl = cell("imageUrl"),
            )
        }
    }

    /** The records of a comma-separated file with their line number, quoted fields included ("a,b", "say ""hi"""). */
    private fun readRecords(content: String): List<Pair<Int, List<String>>> {
        val records = mutableListOf<Pair<Int, List<String>>>()
        var cells = mutableListOf<String>()
        val current = StringBuilder()
        var isQuoted = false
        var line = 1
        var recordLine = 1
        var index = 0

        fun endRecord() {
            cells += current.toString().trim()
            current.clear()
            if (cells.any { it.isNotEmpty() }) {
                records += recordLine to cells
            }
            cells = mutableListOf()
        }

        while (index < content.length) {
            val char = content[index]
            when {
                char == '"' && isQuoted && content.getOrNull(index + 1) == '"' -> {
                    current.append('"')
                    index++
                }
                char == '"' -> isQuoted = !isQuoted
                char == ',' && !isQuoted -> {
                    cells += current.toString().trim()
                    current.clear()
                }
                (char == '\n' || char == '\r') && !isQuoted -> {
                    if (char == '\r' && content.getOrNull(index + 1) == '\n') {
                        index++
                    }
                    endRecord()
                    line++
                    recordLine = line
                }
                else -> {
                    if (char == '\n') {
                        line++
                    }
                    current.append(char)
                }
            }
            index++
        }
        endRecord()

        return records
    }

    private fun normalize(value: String) = value.lowercase().replace(Regex("[^a-z0-9]"), "")

    private fun invalid(message: String) = BackendUsageException(BackendUsageErrorCode.INVALID_INPUT, message)
}
