package org.openvesselphotos.domain.entities

/**
 * A TAC (total allowable catch) the Union fixes for a stock, with the quota of France: the share its producer
 * organisations then divide between their vessels (not public).
 */
data class FishingOpportunity(
    /** As the Council regulation codes it, e.g. "SOL/07D." */
    val tacCode: String,
    /** e.g. "Manche Est (7d)" */
    val area: String,
    val region: FishingRegion,
    /** null when the Union does not set it, e.g. in Norwegian waters */
    val tacTonnes: Double?,
    /** The initial quota of France, before the exchanges between Member States */
    val franceTonnes: Double,
)

/** The TACs and quotas a species is fished under */
data class SpeciesQuotas(
    val quotas: List<FishingOpportunity>,
) {
    /** The quota of France for the species: each TAC once, though it can be in two regions */
    val franceTonnes: Double get() = quotas.distinctBy { it.tacCode }.sumOf { it.franceTonnes }

    /** The TACs of the region, France's largest quota first; null when none is */
    fun inRegion(region: FishingRegion): SpeciesQuotas? =
        quotas
            .filter { it.region == region }
            .takeIf { it.isNotEmpty() }
            ?.let { SpeciesQuotas(it.sortedByDescending { quota -> quota.franceTonnes }) }
}

/** The fishing opportunities of a year (Council regulation) */
data class FishingOpportunities(
    val year: Int,
    val url: String,
    /** By species name, normalized as the auction sales are merged (see ImportAuctionSales.normalize) */
    val bySpecies: Map<String, SpeciesQuotas>,
) {
    companion object {
        val NONE = FishingOpportunities(0, "", emptyMap())
    }
}
