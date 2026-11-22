package org.openvesselphotos.domain.entities

/**
 * The status of a fish stock (a species in a sea area), as Ifremer classifies it from the fishing pressure (F against
 * FRMD) and the spawning biomass (B against Bref and Blim).
 */
enum class StockStatus(
    val label: String,
) {
    /** F ≤ FRMD and B ≥ Bref */
    BON_ETAT("en bon état"),

    /** F ≤ FRMD and B < Bref: rebuilding */
    RECONSTITUABLE("reconstituable"),

    /** F > FRMD and B ≥ Bref */
    SURPECHE("surpêché"),

    /** F > FRMD and B < Bref */
    SURPECHE_DEGRADE("surpêché et dégradé"),

    /** B < Blim, or a scientific advice of no catch */
    EFFONDRE("effondré"),

    /** Assessed, but without reference points */
    NON_CLASSIFIE("non classifié"),
}

/** The regions of Ifremer's yearly assessment of the stocks landed in the Hexagone */
enum class FishingRegion(
    val label: String,
) {
    MNMEST("Mer du Nord – Manche Est"),
    MOMCOE("Manche Ouest – mer Celtique – Ouest Écosse"),
    GASCOGNE("Golfe de Gascogne"),
    MED("Méditerranée"),
    BARENTS("Mer de Barents – mer de Norvège"),
    ;

    companion object {
        /**
         * The region of the waters off a French port, from the limits of the ICES divisions: the Bay of Biscay (VIII)
         * south of 48° N, the Eastern Channel (VIId) east of 2° W.
         */
        fun ofPort(
            latitude: Double?,
            longitude: Double?,
        ): FishingRegion? {
            if (latitude == null || longitude == null) return null

            return when {
                latitude < 43.8 && longitude > 2.9 -> MED
                latitude < 48.0 -> GASCOGNE
                longitude > -2.0 -> MNMEST
                else -> MOMCOE
            }
        }
    }
}

data class FishStock(
    /** As Ifremer names it, e.g. "Sole commune (Solea solea), Division VIId" */
    val name: String,
    val region: FishingRegion,
    val status: StockStatus,
    /** French landings from the stock in the region, in the year assessed */
    val tonnes: Double,
)

/** The stocks a species sold in the auctions is landed from */
data class SpeciesStocks(
    val stocks: List<FishStock>,
) {
    /**
     * The status shared by the stocks the species is mostly landed from (each at least [MAIN_SHARE] of the
     * landings); null when they differ, e.g. sole: effondré in the Eastern Channel, reconstituable in the Bay of Biscay.
     */
    val status: StockStatus?
        get() {
            val total = stocks.sumOf { it.tonnes }
            val main = stocks.filter { total <= 0 || it.tonnes >= total * MAIN_SHARE }

            return main.map { it.status }.distinct().singleOrNull()
        }

    /** The stocks landed in the region, the largest first; null when none is */
    fun inRegion(region: FishingRegion): SpeciesStocks? =
        stocks
            .filter { it.region == region }
            .takeIf { it.isNotEmpty() }
            ?.let { SpeciesStocks(it.sortedByDescending { stock -> stock.tonnes }) }

    companion object {
        const val MAIN_SHARE = 0.05
    }
}

/** Ifremer's yearly assessment of the stocks landed by French vessels */
data class StockAssessment(
    /** e.g. 2025 for the "Bilan 2025", of the landings of 2024 */
    val edition: Int,
    val url: String,
    /** By species name, normalized as the auction sales are merged (see ImportAuctionSales.normalize) */
    val bySpecies: Map<String, SpeciesStocks>,
) {
    companion object {
        val NONE = StockAssessment(0, "", emptyMap())
    }
}
