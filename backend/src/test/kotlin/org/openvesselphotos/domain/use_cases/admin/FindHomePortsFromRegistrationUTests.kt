package org.openvesselphotos.domain.use_cases.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openvesselphotos.domain.entities.Port

class FindHomePortsFromRegistrationUTests {
    private val ports =
        listOf(
            Port("FRGVC", "Le Guilvinec", "FR", 47.79, -4.28),
            Port("FRPEB", "Port-en-Bessin", "FR", 49.35, -0.75),
            Port("FRLC5", "La Cotinière", "FR", 45.91, -1.33),
            Port("FRSPA", "Saint-Pierre", "FR", -21.34, 55.48),
            Port("PMFSP", "Saint-Pierre", "PM", 46.78, -56.17),
        )

    @Test
    fun `portOf Should match a quartier to the port of its name, as written in the register`() {
        assertThat(FindHomePortsFromRegistration.portOf("LE GUILVINEC", ports, emptyMap())?.locode).isEqualTo("FRGVC")
        assertThat(FindHomePortsFromRegistration.portOf("GUILVINEC", ports, emptyMap())?.locode).isEqualTo("FRGVC")
    }

    @Test
    fun `portOf Should match a quartier named after its island to its fishing port`() {
        assertThat(FindHomePortsFromRegistration.portOf("ILE D OLERON", ports, emptyMap())?.locode).isEqualTo("FRLC5")
    }

    @Test
    fun `portOf Should take the port most of the quartier's located vessels are based at, without port of its name`() {
        assertThat(
            FindHomePortsFromRegistration.portOf("CAEN", ports, mapOf("FRPEB" to 12, "FRGVC" to 1))?.locode,
        ).isEqualTo("FRPEB")
    }

    @Test
    fun `portOf Should take, among the ports of its name, the one the quartier's located vessels are based at`() {
        assertThat(
            FindHomePortsFromRegistration.portOf("SAINT PIERRE", ports, mapOf("PMFSP" to 1, "FRGVC" to 5))?.locode,
        ).isEqualTo("PMFSP")
        assertThat(FindHomePortsFromRegistration.portOf("SAINT PIERRE", ports, mapOf("FRGVC" to 5))).isNull()
    }

    @Test
    fun `portOf Should match nothing without port of its name nor enough located vessels of the quartier`() {
        assertThat(FindHomePortsFromRegistration.portOf("CAEN", ports, mapOf("FRPEB" to 2))).isNull()
    }

    @Test
    fun `portOf Should match nothing when the located vessels are based at a port not on the map`() {
        assertThat(FindHomePortsFromRegistration.portOf("CAEN", ports, mapOf("FRCFR" to 10))).isNull()
    }
}
