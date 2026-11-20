package org.openvesselphotos.domain.use_cases.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openvesselphotos.domain.entities.HomePortCandidate
import org.openvesselphotos.domain.entities.HomePortSource
import org.openvesselphotos.domain.entities.Port
import org.openvesselphotos.domain.fishing_activity.PortVisit
import org.openvesselphotos.domain.use_cases.admin.FindHomePortsFromPortVisits.Result

class FindHomePortsFromPortVisitsUTests {
    private val candidate = HomePortCandidate("FRA000123456", "KER ARMOR", null, null)
    private val ports =
        listOf(
            Port("FRLRT", "Lorient", "FR", 47.73, -3.36),
            Port("FRSET", "Sète", "FR", 43.40, 3.70),
            Port("FRPT3", "Port Tudy", "FR", 47.6431, -3.4475),
        )

    private fun visits(
        count: Int,
        id: String,
        flag: String,
        latitude: Double,
        longitude: Double,
    ) = List(count) { PortVisit(id, id.substringAfter("-").uppercase(), flag, latitude, longitude) }

    @Test
    fun `homePortOf Should take the port visited most, on the map`() {
        val result =
            FindHomePortsFromPortVisits.homePortOf(
                candidate,
                visits(5, "fra-lorient", "FRA", 47.74, -3.37) + visits(2, "fra-sete", "FRA", 43.40, 3.70),
                ports,
            )

        val homePort = (result as Result.Placed).homePort
        assertThat(homePort.portLocode).isEqualTo("FRLRT")
        assertThat(homePort.observationsAtPort).isEqualTo(5)
        assertThat(homePort.observationsTotal).isEqualTo(7)
        assertThat(homePort.source).isEqualTo(HomePortSource.GFW)
    }

    @Test
    fun `homePortOf Should prefer a port of the vessel's own country, as offshore vessels also land abroad`() {
        val result =
            FindHomePortsFromPortVisits.homePortOf(
                candidate,
                visits(12, "irl-castletownbere", "IRL", 51.65, -9.91) + visits(3, "fra-lorient", "FRA", 47.73, -3.36),
                ports,
            )

        assertThat((result as Result.Placed).homePort.portLocode).isEqualTo("FRLRT")
    }

    @Test
    fun `homePortOf Should take the port of the same name, before a nearer one`() {
        // The anchorage of Lorient is in its roadstead, nearer Port Tudy (Groix) than the position of Lorient
        val visits = List(7) { PortVisit("fra-lorient", "fra-lorient", "FRA", 47.671, -3.415) }

        val result = FindHomePortsFromPortVisits.homePortOf(candidate, visits, ports)

        assertThat((result as Result.Placed).homePort.portLocode).isEqualTo("FRLRT")
    }

    @Test
    fun `homePortOf Should not guess from too few visits, nor from a port far from the map's`() {
        assertThat(FindHomePortsFromPortVisits.homePortOf(candidate, emptyList(), ports)).isEqualTo(Result.TooFewVisits)
        assertThat(
            FindHomePortsFromPortVisits.homePortOf(candidate, visits(1, "fra-lorient", "FRA", 47.73, -3.36), ports),
        ).isEqualTo(Result.TooFewVisits)
        assertThat(
            FindHomePortsFromPortVisits.homePortOf(candidate, visits(4, "fra-brest", "FRA", 48.38, -4.49), ports),
        ).isEqualTo(Result.NotOnTheMap("BREST"))
    }

    @Test
    fun `distanceKm Should be the great-circle distance`() {
        // Lorient to Sète: about 730 km as the crow flies
        assertThat(FindHomePortsFromPortVisits.distanceKm(47.73, -3.36, 43.40, 3.70)).isBetween(720.0, 740.0)
    }
}
