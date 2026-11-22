package org.openvesselphotos.domain.use_cases.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openvesselphotos.domain.entities.HomePortSource
import org.openvesselphotos.domain.use_cases.admin.ImportHomePortsFromPortReturns.PortReturn

class ImportHomePortsFromPortReturnsUTests {
    @Test
    fun `topPorts Should give each vessel the port of most of its returns to port, among all its returns to port`() {
        val (homePorts, ties) =
            ImportHomePortsFromPortReturns.topPorts(
                listOf(
                    PortReturn("FRA000110854", "FRQUY", 86),
                    PortReturn("FRA000110854", "FRSBK", 15),
                    PortReturn("FRA000110855", "FRSQ2", 4),
                ),
            )

        assertThat(ties).isEmpty()
        assertThat(homePorts.map { Triple(it.cfr, it.portLocode, it.observationsAtPort to it.observationsTotal) })
            .containsExactly(
                Triple("FRA000110854", "FRQUY", 86 to 101),
                Triple("FRA000110855", "FRSQ2", 4 to 4),
            )
        assertThat(homePorts.map { it.source }).containsOnly(HomePortSource.RETURNS)
    }

    @Test
    fun `topPorts Should leave out a vessel returning as often at two ports`() {
        val (homePorts, ties) =
            ImportHomePortsFromPortReturns.topPorts(
                listOf(
                    PortReturn("FRA000110854", "FRQUY", 5),
                    PortReturn("FRA000110854", "FRSBK", 5),
                    PortReturn("FRA000110854", "FRCBT", 1),
                ),
            )

        assertThat(homePorts).isEmpty()
        assertThat(ties).containsExactly("FRA000110854")
    }

    @Test
    fun `topPorts Should add up the returns to port of a port listed twice`() {
        val (homePorts, _) =
            ImportHomePortsFromPortReturns.topPorts(
                listOf(
                    PortReturn("FRA000110854", "FRQUY", 3),
                    PortReturn("FRA000110854", "FRSBK", 5),
                    PortReturn("FRA000110854", "FRQUY", 4),
                ),
            )

        assertThat(homePorts.single().portLocode).isEqualTo("FRQUY")
        assertThat(homePorts.single().observationsAtPort).isEqualTo(7)
    }
}
