package org.openvesselphotos.domain.entities

import java.time.LocalDate

/** Where the data of the ports site comes from, see its methodology page */
enum class DataSource(
    /** In French, as shown on the changelog */
    val label: String,
) {
    HOME_PORTS("Ports d’attache, positions des navires"),
    HOME_PORTS_GFW("Ports d’attache, Global Fishing Watch"),
    HOME_PORTS_REGISTER("Ports d’attache, quartiers d’immatriculation"),
    HOME_PORTS_CORRECTIONS("Ports d’attache, corrections des visiteurs"),
    AUCTION_SALES("Ventes des criées, FranceAgriMer"),
    FLEET_REGISTER("Registre de la flotte de l’UE"),
    PORT_POSITIONS("Positions des ports"),
    VESSEL_OWNERS("Armateurs"),
}

/** An update of the data, for the public changelog */
data class DataUpdate(
    val source: DataSource,
    val updatedOn: LocalDate,
    /** In French, e.g. "Ventes 2025 de 38 criées" */
    val summary: String,
)

/** How much of the French fleet has a known home port, and how it was found */
data class HomePortCoverage(
    /** French vessels of the EU fleet register (CFR starting with FRA) */
    val registerVessels: Int,
    /** Among them, those with a home port */
    val withHomePort: Int,
    /** Home ports from the imported observations at quay */
    val fromObservations: Int,
    /** Home ports from the port visits on Global Fishing Watch */
    val fromPortVisits: Int,
    /** Home ports from the place of registration (quartier maritime) in the EU fleet register */
    val fromRegistration: Int = 0,
) {
    /** From 0 to 1; null without register */
    val share: Double? get() = if (registerVessels > 0) withHomePort.toDouble() / registerVessels else null
}
