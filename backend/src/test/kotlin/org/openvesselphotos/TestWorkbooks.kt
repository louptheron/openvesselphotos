package org.openvesselphotos

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Small Excel workbooks laid out like the ones of FranceAgriMer, for the tests */
object TestWorkbooks {
    private val MONTHS =
        listOf("jan", "fév", "mars", "avril", "mai", "juin", "juil", "août", "sept", "oct", "nov", "déc")

    /**
     * "Ventes déclarées en halles à marée": the auctions Erquy and La Rochelle, and the monthly sales of Bar (nothing
     * sold in March)
     */
    fun auctionSales(): ByteArray {
        val auctions =
            mapOf(
                1 to mapOf("A" to "Ventes déclarées en halles à marée par la flotte française"),
                12 to mapOf("A" to "Erquy"),
                14 to mapOf("B" to "2024", "F" to "2025"),
                15 to mapOf("A" to "Nom de l'espèce", "B" to "Quantité (t)"),
                16 to mapOf("A" to "Total", "B" to "100", "C" to "300", "F" to "120", "G" to "400"),
                17 to mapOf("A" to "Bar", "B" to "10", "C" to "150", "F" to "12", "G" to "180"),
                18 to mapOf("A" to "Autres espèces", "B" to "90", "C" to "150", "F" to "108", "G" to "220"),
                20 to mapOf("A" to "La Rochelle"),
                22 to mapOf("B" to "2024", "F" to "2025"),
                23 to mapOf("A" to "Nom de l'espèce"),
                24 to mapOf("A" to "Total", "B" to "50", "C" to "100", "F" to "60", "G" to "130"),
                25 to mapOf("A" to "Rouget-Barbet", "B" to "5", "C" to "40", "F" to "6", "G" to "50"),
                26 to mapOf("A" to "Sole", "B" to "2", "C" to "30", "F" to "3", "G" to "45"),
            )
        val bar =
            mapOf(
                4 to mapOf("A" to "Données par espèce"),
                9 to mapOf("A" to "Bar"),
                35 to mapOf("B" to "2024", "D" to "2025"),
            ) +
                MONTHS
                    .mapIndexed { index, month ->
                        (36 + index) to
                            if (month == "mars") {
                                mapOf("A" to month)
                            } else {
                                mapOf("A" to month, "B" to "${index + 1}", "D" to "${(index + 1) * 2}")
                            }
                    }.toMap()

        return workbook(
            listOf(
                "Sommaire" to mapOf(1 to mapOf("A" to "Sommaire")),
                "FR_détail_criées x espèces" to auctions,
                "FR_Bar" to bar,
            ),
        )
    }

    /** A workbook of the given sheets, with inline strings, as an .xlsx file */
    fun workbook(sheets: List<Pair<String, Map<Int, Map<String, String>>>>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            fun add(
                path: String,
                content: String,
            ) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }

            val namespace = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
            val relationships = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
            add(
                "xl/workbook.xml",
                """<workbook xmlns="$namespace" xmlns:r="$relationships"><sheets>""" +
                    sheets
                        .mapIndexed { index, (name, _) ->
                            """<sheet name="$name" sheetId="${index + 1}" r:id="rId${index + 1}"/>"""
                        }.joinToString("") +
                    "</sheets></workbook>",
            )
            add(
                "xl/_rels/workbook.xml.rels",
                """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
                    sheets.indices.joinToString("") {
                        """<Relationship Id="rId${it + 1}" Target="worksheets/sheet${it + 1}.xml"/>"""
                    } +
                    "</Relationships>",
            )
            sheets.forEachIndexed { index, (_, rows) ->
                add(
                    "xl/worksheets/sheet${index + 1}.xml",
                    """<worksheet xmlns="$namespace"><sheetData>""" +
                        rows.entries.sortedBy { it.key }.joinToString("") { (row, cells) ->
                            """<row r="$row">""" +
                                cells.entries.joinToString("") { (column, value) ->
                                    if (value.toDoubleOrNull() != null) {
                                        """<c r="$column$row"><v>$value</v></c>"""
                                    } else {
                                        """<c r="$column$row" t="inlineStr"><is><t>$value</t></is></c>"""
                                    }
                                } + "</row>"
                        } +
                        "</sheetData></worksheet>",
                )
            }
        }

        return output.toByteArray()
    }
}
