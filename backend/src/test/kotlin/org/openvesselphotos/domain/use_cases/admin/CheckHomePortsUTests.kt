package org.openvesselphotos.domain.use_cases.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openvesselphotos.domain.entities.HomePortCandidate
import org.openvesselphotos.domain.entities.Port
import org.openvesselphotos.domain.fishing_activity.HomePortCheckResult
import org.openvesselphotos.domain.fishing_activity.PortVisit
import java.time.LocalDate

class CheckHomePortsUTests {
    private val vessel = HomePortCandidate("FRA000123456", "KER ARMOR", null, null)
    private val ports =
        listOf(
            Port("FRLRT", "Lorient", "FR", 47.73, -3.36),
            Port("FRLOC", "Locmiquélic", "FR", 47.72, -3.34),
            Port("FRCOC", "Concarneau", "FR", 47.87, -3.92),
        )
    private val today = LocalDate.of(2026, 11, 20)

    private fun visits(
        count: Int,
        id: String,
        latitude: Double,
        longitude: Double,
        flag: String = "FRA",
    ) = List(count) { PortVisit(id, id.substringAfter("-").uppercase(), flag, latitude, longitude) }

    @Test
    fun `check Should agree when the port visited most is the imported one`() {
        val check =
            CheckHomePorts.check(
                vessel,
                "FRLRT",
                visits(9, "fra-lorient", 47.74, -3.37) + visits(2, "fra-concarneau", 47.87, -3.92),
                ports,
                today,
            )

        assertThat(check.result).isEqualTo(HomePortCheckResult.AGREES)
        assertThat(check.visitsAtImported).isEqualTo(9)
        assertThat(check.visitsTotal).isEqualTo(11)
    }

    @Test
    fun `check Should disagree when the vessel visited another port most, far away`() {
        val check =
            CheckHomePorts.check(
                vessel,
                "FRLRT",
                visits(10, "fra-concarneau", 47.87, -3.92) + visits(1, "fra-lorient", 47.74, -3.37),
                ports,
                today,
            )

        assertThat(check.result).isEqualTo(HomePortCheckResult.DISAGREES)
        assertThat(check.gfwLocode).isEqualTo("FRCOC")
        assertThat(check.gfwPortName).isEqualTo("Concarneau")
        assertThat(check.visitsAtGfwPort).isEqualTo(10)
        assertThat(check.visitsAtImported).isEqualTo(1)
        assertThat(check.distanceKm).isBetween(40.0, 50.0)
    }

    @Test
    fun `check Should tell a port nearby apart`() {
        val check = CheckHomePorts.check(vessel, "FRLRT", visits(6, "fra-locmiquelic", 47.72, -3.34), ports, today)

        assertThat(check.result).isEqualTo(HomePortCheckResult.NEARBY)
        assertThat(check.gfwLocode).isEqualTo("FRLOC")
    }

    @Test
    fun `check Should tell a vessel not found, with too few visits or in a port not on the map`() {
        assertThat(CheckHomePorts.check(vessel, "FRLRT", null, ports, today).result)
            .isEqualTo(HomePortCheckResult.NOT_FOUND)
        assertThat(CheckHomePorts.check(vessel, "FRLRT", visits(1, "fra-lorient", 47.74, -3.37), ports, today).result)
            .isEqualTo(HomePortCheckResult.TOO_FEW_VISITS)

        val abroad =
            CheckHomePorts.check(
                vessel,
                "FRLRT",
                visits(8, "irl-killybegs", 54.63, -8.44, "IRL"),
                ports,
                today,
            )
        assertThat(abroad.result).isEqualTo(HomePortCheckResult.NOT_ON_THE_MAP)
        assertThat(abroad.gfwPortName).isEqualTo("KILLYBEGS")
    }
}
