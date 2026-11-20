package org.openvesselphotos.infrastructure.web

import java.text.NumberFormat
import java.util.Locale

/**
 * Charts of the content pages of the ports site, rendered by the server as HTML and CSS (see the classes in
 * frontend/src/ports/ContentPage.tsx): they read the same on phones, search engines see their figures, and each comes
 * with its source and its table of data. Values are escaped here.
 */
object ContentCharts {
    /** The sizes of vessels, light to dark as they grow, as in frontend/src/ports/FishingFacts.tsx */
    val SIZE_COLORS = listOf("#8FB4C9", "#3F7BA6", "#0B2233")
    private const val HIGHLIGHT = "#1C5A80"
    private const val MUTED = "#8FA3B3"

    data class Tile(
        val value: String,
        val label: String,
    )

    /** Key figures, in a row of tiles */
    fun tiles(
        tiles: List<Tile>,
        source: String,
    ): String =
        """<div class="stat-tiles">""" +
            tiles.joinToString("") {
                """<div class="stat-tile"><strong>${escapeHtml(it.value)}</strong>""" +
                    "<span>${escapeHtml(it.label)}</span></div>"
            } + "</div>" + """<p class="chart-source">Source : ${escapeHtml(source)}</p>"""

    /** Wholes split into parts, e.g. the vessels, the jobs and the landings by size of vessel, in % */
    fun shares(
        title: String,
        partNames: List<String>,
        rows: List<Pair<String, List<Double>>>,
        source: String,
    ): String {
        val legend =
            partNames.mapIndexed { index, name ->
                """<li><i style="background:${SIZE_COLORS[index]}"></i>${escapeHtml(name)}</li>"""
            }
        val bars =
            rows.joinToString("") { (label, values) ->
                val description =
                    "$label : " +
                        partNames.zip(values).joinToString(", ") { (name, value) -> "$name ${percent(value)}" }
                """<div class="chart-row"><span class="chart-label">${escapeHtml(label)}</span>""" +
                    """<div class="chart-stack" role="img" aria-label="${escapeHtml(description)}">""" +
                    values
                        .mapIndexed { index, value ->
                            // Near-black on the light part, white on the dark ones
                            val ink = if (index == 0) "#0B2233" else "#FFFFFF"
                            val text = if (value >= MIN_LABELLED_SHARE) percent(value) else ""
                            """<span class="chart-part" style="width:${css(
                                value,
                            )}%;background:${SIZE_COLORS[index]};""" +
                                """color:$ink" title="${escapeHtml("${partNames[index]} : ${percent(value)}")}">""" +
                                "$text</span>"
                        }.joinToString("") + "</div></div>"
            }

        return figure(
            title,
            """<ul class="chart-legend">${legend.joinToString("")}</ul>$bars""",
            source,
            listOf("") + partNames,
            rows.map { (label, values) -> listOf(label) + values.map { percent(it) } },
        )
    }

    data class Range(
        val label: String,
        val min: Double,
        val max: Double,
        /** The fleets the chart is about, drawn darker */
        val isHighlighted: Boolean,
        /** e.g. "20 à 40" */
        val value: String,
    )

    /** A range of values for each row, on a common axis from 0 */
    fun ranges(
        title: String,
        rows: List<Range>,
        axisMax: Double,
        unit: String,
        source: String,
        note: String? = null,
    ): String {
        val bars =
            rows.joinToString("") {
                val color = if (it.isHighlighted) HIGHLIGHT else MUTED
                val description = "${it.label} : ${it.value} $unit"
                """<div class="chart-row"><span class="chart-label">${escapeHtml(it.label)}</span>""" +
                    """<div class="chart-track" role="img" aria-label="${escapeHtml(description)}">""" +
                    """<span class="chart-range" style="left:${css(it.min / axisMax * 100)}%;""" +
                    """width:${css((it.max - it.min) / axisMax * 100)}%;background:$color" """ +
                    """title="${escapeHtml(description)}"></span></div>""" +
                    """<span class="chart-value">${escapeHtml(it.value)}</span></div>"""
            }

        return figure(
            title,
            bars + (note?.let { """<p class="chart-note">${escapeHtml(it)}</p>""" } ?: ""),
            source,
            listOf("", unit.replaceFirstChar { it.uppercase() }),
            rows.map { listOf(it.label, it.value) },
        )
    }

    /** One value per row, as bars from 0, e.g. shares in % */
    fun bars(
        title: String,
        rows: List<Pair<String, Double>>,
        source: String,
    ): String {
        val bars =
            rows.joinToString("") { (label, value) ->
                """<div class="chart-row"><span class="chart-label">${escapeHtml(label)}</span>""" +
                    """<div class="chart-track" role="img" aria-label="${escapeHtml(
                        "$label : ${percent(value)}",
                    )}">""" +
                    """<span class="chart-bar" style="width:${css(value)}%;background:$HIGHLIGHT"></span></div>""" +
                    """<span class="chart-value">${percent(value)}</span></div>"""
            }

        return figure(
            title,
            bars,
            source,
            listOf("", "Part"),
            rows.map { (label, value) -> listOf(label, percent(value)) },
        )
    }

    private fun figure(
        title: String,
        chart: String,
        source: String,
        header: List<String>,
        rows: List<List<String>>,
    ): String =
        """<figure class="chart"><figcaption class="chart-title">${escapeHtml(title)}</figcaption>$chart""" +
            """<p class="chart-source">Source : ${escapeHtml(source)}</p>""" +
            "<details><summary>Voir les données</summary><table><thead><tr>" +
            header.joinToString("") { """<th scope="col">${escapeHtml(it)}</th>""" } + "</tr></thead><tbody>" +
            rows.joinToString("") { row ->
                "<tr>" + """<th scope="row">${escapeHtml(row.first())}</th>""" +
                    row.drop(1).joinToString("") { "<td>${escapeHtml(it)}</td>" } + "</tr>"
            } + "</tbody></table></details></figure>"

    /** e.g. "42,5 %", or "84 %" for a whole number */
    fun percent(value: Double): String =
        NumberFormat.getNumberInstance(Locale.FRANCE).apply { maximumFractionDigits = 1 }.format(value) + "\u00a0%"

    /** A CSS number, with a dot and at most 2 decimals */
    private fun css(value: Double) = "%.2f".format(Locale.ROOT, value.coerceIn(0.0, 100.0))

    private const val MIN_LABELLED_SHARE = 8.0
}
