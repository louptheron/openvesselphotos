package org.openvesselphotos.domain.use_cases.public_api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openvesselphotos.domain.entities.Port
import org.openvesselphotos.domain.entities.PortWithVesselCount
import org.openvesselphotos.domain.entities.slugOf

class GetPortsUTests {
    @Test
    fun `slugOf Should keep the words of a name, without accents nor case`() {
        assertThat(slugOf("Le Guilvinec")).isEqualTo("le-guilvinec")
        assertThat(slugOf("Saint-Quay-Portrieux")).isEqualTo("saint-quay-portrieux")
        assertThat(slugOf("CHEF-DE-BAIE")).isEqualTo("chef-de-baie")
        assertThat(slugOf("Sète")).isEqualTo("sete")
        assertThat(slugOf("L’Herbaudière")).isEqualTo("l-herbaudiere")
        assertThat(slugOf("Bœuf")).isEqualTo("boeuf")
    }

    @Test
    fun `withUniqueSlugs Should give the LOCODE to the ports of the same name, but the one with the most vessels`() {
        val ports =
            GetPorts.withUniqueSlugs(
                listOf(
                    PortWithVesselCount(Port("FRSPA", "Saint-Pierre", "FR"), 3),
                    PortWithVesselCount(Port("PMFSP", "Saint-Pierre", "PM"), 12),
                    PortWithVesselCount(Port("FRQUY", "Erquy", "FR"), 2),
                ),
            )

        assertThat(ports.map { it.slug }).containsExactly("saint-pierre-frspa", "saint-pierre", "erquy")
    }
}
