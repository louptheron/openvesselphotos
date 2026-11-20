package org.openvesselphotos.infrastructure.api.admin

import org.openvesselphotos.domain.entities.AuctionSalesData
import org.openvesselphotos.domain.entities.AuctionSpeciesSales
import org.openvesselphotos.domain.entities.SpeciesMonthlySales
import org.openvesselphotos.domain.exceptions.BackendUsageErrorCode
import org.openvesselphotos.domain.exceptions.BackendUsageException
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader

/**
 * Reads FranceAgriMer's yearly workbook "Ventes déclarées en halles à marée" (BIL-MER-VENTES_HAM-Axx.xlsm), as
 * published: an Excel file is a zip of XML documents, read here without running its macros. Tables are found by their
 * labels, not their positions, so that the workbook of the next year can be read too:
 * - the sheet "FR_détail_criées x espèces": each auction, then a row "Nom de l'espèce" and its species, with the
 *   quantity (t) and value (k€) of the previous year in columns B and C and of the year in F and G;
 * - the sheets of the main species (A4 "Données par espèce", the species in A9): the months ("jan" to "déc") with the
 *   quantity of the previous year in column B and of the year in D.
 */
object AuctionSalesWorkbookParser {
    private const val AUCTIONS_SHEET_PREFIX = "FR_détail_criées"
    private const val SPECIES_HEADER = "Nom de l'espèce"
    private const val SPECIES_SHEET_TITLE = "Données par espèce"
    private const val TOTAL = "Total"
    private val MONTHS =
        listOf("jan", "fév", "mars", "avril", "mai", "juin", "juil", "août", "sept", "oct", "nov", "déc")
    private val YEAR = Regex("(19|20)\\d{2}")

    /** Cells of a sheet by row number, then by column letters, e.g. rows[15]["B"] */
    private typealias Sheet = Map<Int, Map<String, String>>

    fun parse(content: ByteArray): AuctionSalesData {
        val parts = readParts(content)
        val strings = parts["xl/sharedStrings.xml"]?.let { readSharedStrings(it) }.orEmpty()
        val sheets = readSheetPaths(parts).mapValues { (_, path) -> parts[path] }
        val auctionsSheet =
            sheets.entries.firstOrNull { it.key.startsWith(AUCTIONS_SHEET_PREFIX) }?.value
                ?: throw invalid(
                    "No sheet \"$AUCTIONS_SHEET_PREFIX…\": is it FranceAgriMer's workbook \"Ventes déclarées en halles à marée\"?",
                )
        val (sales, years) = readAuctions(readSheet(auctionsSheet, strings))
        val monthlySales =
            sheets
                .filterKeys { it.startsWith("FR_") }
                .values
                .filterNotNull()
                .flatMap { readMonthlySales(readSheet(it, strings), years) }

        return AuctionSalesData(sales, monthlySales)
    }

    /** The species sales of each auction, and the two years of the workbook (previous, current) */
    private fun readAuctions(sheet: Sheet): Pair<List<AuctionSpeciesSales>, Pair<Int, Int>> {
        val sales = mutableListOf<AuctionSpeciesSales>()
        var years: Pair<Int, Int>? = null
        var auction: String? = null
        var inTable = false
        for (rowNumber in sheet.keys.sorted()) {
            val row = sheet.getValue(rowNumber)
            val label = row["A"]?.trim().orEmpty()
            val previousYear = row["B"]?.trim()?.takeIf { YEAR.matches(it) }
            val year = row["F"]?.trim()?.takeIf { YEAR.matches(it) }
            when {
                previousYear != null && year != null -> years = previousYear.toInt() to year.toInt()
                label == SPECIES_HEADER -> inTable = true
                // A row with only a name: the next auction (or a title)
                row.keys == setOf("A") -> {
                    auction = label
                    inTable = false
                }
                label.isEmpty() -> inTable = false
                inTable && auction != null && years != null -> {
                    val species = label.takeUnless { it == TOTAL }
                    sale(auction, years.first, species, row["B"], row["C"])?.let { sales.add(it) }
                    sale(auction, years.second, species, row["F"], row["G"])?.let { sales.add(it) }
                }
            }
        }
        if (years == null || sales.isEmpty()) {
            throw invalid("No auction sales found in the sheet \"$AUCTIONS_SHEET_PREFIX…\".")
        }

        return sales to years
    }

    private fun sale(
        auction: String,
        year: Int,
        species: String?,
        quantity: String?,
        value: String?,
    ): AuctionSpeciesSales? {
        val quantityTonnes = quantity?.toDoubleOrNull() ?: return null
        val valueThousandEuros = value?.toDoubleOrNull() ?: return null

        return AuctionSpeciesSales(auction, year, species?.take(100), quantityTonnes, valueThousandEuros)
    }

    private fun readMonthlySales(
        sheet: Sheet,
        years: Pair<Int, Int>,
    ): List<SpeciesMonthlySales> {
        if (sheet[4]?.get("A")?.trim() != SPECIES_SHEET_TITLE) {
            return emptyList()
        }
        val species = sheet[9]?.get("A")?.trim()?.take(100) ?: return emptyList()
        val firstMonthRow =
            sheet.keys.sorted().firstOrNull { sheet.getValue(it)["A"]?.trim() == MONTHS.first() }
                ?: return emptyList()

        return MONTHS
            .mapIndexedNotNull { index, month ->
                val row =
                    sheet[firstMonthRow + index]?.takeIf { it["A"]?.trim() == month } ?: return@mapIndexedNotNull null
                // An empty month: nothing sold, e.g. in the closed season of the scallops
                listOf(
                    SpeciesMonthlySales(species, years.first, index + 1, row["B"]?.toDoubleOrNull() ?: 0.0),
                    SpeciesMonthlySales(species, years.second, index + 1, row["D"]?.toDoubleOrNull() ?: 0.0),
                )
            }.flatten()
    }

    /** The XML parts of the workbook, by path; images, charts and macros are skipped */
    private fun readParts(content: ByteArray): Map<String, ByteArray> {
        val parts = mutableMapOf<String, ByteArray>()
        try {
            ZipInputStream(ByteArrayInputStream(content)).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    val isNeeded =
                        entry.name == "xl/workbook.xml" ||
                            entry.name == "xl/_rels/workbook.xml.rels" ||
                            entry.name == "xl/sharedStrings.xml" ||
                            (entry.name.startsWith("xl/worksheets/") && entry.name.endsWith(".xml"))
                    if (isNeeded) {
                        parts[entry.name] = zip.readNBytes(MAX_PART_SIZE + 1)
                        if (parts.getValue(entry.name).size > MAX_PART_SIZE) {
                            throw invalid("The part ${entry.name} of the file is too large.")
                        }
                    }
                }
            }
        } catch (e: java.util.zip.ZipException) {
            throw invalid("The file is not an Excel workbook (.xlsx, .xlsm).")
        }
        if ("xl/workbook.xml" !in parts) {
            throw invalid("The file is not an Excel workbook (.xlsx, .xlsm).")
        }

        return parts
    }

    /** Sheet name to the path of its XML part */
    private fun readSheetPaths(parts: Map<String, ByteArray>): Map<String, String> {
        val targets = mutableMapOf<String, String>()
        parts["xl/_rels/workbook.xml.rels"]?.let { rels ->
            read(rels) { reader ->
                if (reader.localName == "Relationship") {
                    targets[reader.getAttributeValue(null, "Id")] = reader.getAttributeValue(null, "Target")
                }
            }
        }
        val paths = linkedMapOf<String, String>()
        read(parts.getValue("xl/workbook.xml")) { reader ->
            if (reader.localName == "sheet") {
                val id =
                    (0 until reader.attributeCount).firstNotNullOfOrNull { index ->
                        reader.getAttributeValue(index).takeIf { reader.getAttributeLocalName(index) == "id" }
                    }
                val target = targets[id] ?: return@read
                paths[reader.getAttributeValue(null, "name")] =
                    target.removePrefix("/").let { if (it.startsWith("xl/")) it else "xl/$it" }
            }
        }

        return paths
    }

    private fun readSharedStrings(xml: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val current = StringBuilder()
        var isInText = false
        readEvents(xml) { reader, event ->
            when {
                event == XMLStreamConstants.START_ELEMENT && reader.localName == "si" -> current.clear()
                event == XMLStreamConstants.START_ELEMENT && reader.localName == "t" -> isInText = true
                event == XMLStreamConstants.END_ELEMENT && reader.localName == "t" -> isInText = false
                event == XMLStreamConstants.END_ELEMENT && reader.localName == "si" -> strings += current.toString()
                event == XMLStreamConstants.CHARACTERS && isInText -> current.append(reader.text)
            }
        }

        return strings
    }

    /** The values of the cells (shared strings resolved, formulas by their last computed value) */
    private fun readSheet(
        xml: ByteArray,
        strings: List<String>,
    ): Sheet {
        val rows = mutableMapOf<Int, MutableMap<String, String>>()
        var reference: String? = null
        var type: String? = null
        val value = StringBuilder()
        var isInValue = false
        readEvents(xml) { reader, event ->
            when {
                event == XMLStreamConstants.START_ELEMENT && reader.localName == "c" -> {
                    reference = reader.getAttributeValue(null, "r")
                    type = reader.getAttributeValue(null, "t")
                    value.clear()
                }
                event == XMLStreamConstants.START_ELEMENT && (reader.localName == "v" || reader.localName == "t") ->
                    isInValue = true
                event == XMLStreamConstants.END_ELEMENT && (reader.localName == "v" || reader.localName == "t") ->
                    isInValue = false
                event == XMLStreamConstants.CHARACTERS && isInValue -> value.append(reader.text)
                event == XMLStreamConstants.END_ELEMENT && reader.localName == "c" -> {
                    val cell = reference?.let { CELL_REFERENCE.matchEntire(it) }
                    if (cell != null && value.isNotEmpty()) {
                        val text =
                            if (type == "s") strings.getOrNull(value.toString().toInt()) else value.toString()
                        if (text != null) {
                            rows.getOrPut(cell.groupValues[2].toInt()) { mutableMapOf() }[cell.groupValues[1]] = text
                        }
                    }
                }
            }
        }

        return rows
    }

    private val CELL_REFERENCE = Regex("([A-Z]+)(\\d+)")

    private fun read(
        xml: ByteArray,
        onStartElement: (XMLStreamReader) -> Unit,
    ) = readEvents(xml) { reader, event ->
        if (event == XMLStreamConstants.START_ELEMENT) {
            onStartElement(reader)
        }
    }

    private fun readEvents(
        xml: ByteArray,
        onEvent: (XMLStreamReader, Int) -> Unit,
    ) {
        val reader = XML_INPUT_FACTORY.createXMLStreamReader(ByteArrayInputStream(xml))
        try {
            while (reader.hasNext()) {
                onEvent(reader, reader.next())
            }
        } finally {
            reader.close()
        }
    }

    /** No DTD nor external entities: the file comes from an upload */
    private val XML_INPUT_FACTORY: XMLInputFactory =
        XMLInputFactory.newFactory().apply {
            setProperty(XMLInputFactory.SUPPORT_DTD, false)
            setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
        }

    private const val MAX_PART_SIZE = 20 * 1024 * 1024

    private fun invalid(message: String) = BackendUsageException(BackendUsageErrorCode.INVALID_INPUT, message)
}
