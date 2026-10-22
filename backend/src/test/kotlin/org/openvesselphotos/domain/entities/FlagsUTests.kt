package org.openvesselphotos.domain.entities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlagsUTests {
    @Test
    fun `The flag of an MMSI Should be its MID's country`() {
        assertThat(Flags.ofMmsi("257123000")).isEqualTo("NOR")
        assertThat(Flags.ofMmsi("227000101")).isEqualTo("FRA")
        assertThat(Flags.ofMmsi("371829000")).isEqualTo("PAN")
        assertThat(Flags.ofMmsi("538001234")).isEqualTo("MHL")
        assertThat(Flags.ofMmsi("366904950")).isEqualTo("USA")
        // Not a ship's MMSI (a SART), or no MMSI
        assertThat(Flags.ofMmsi("970123456")).isNull()
        assertThat(Flags.ofMmsi(null)).isNull()
    }

    @Test
    fun `A flag Should list its MIDs, and be named from its alpha-2 code`() {
        assertThat(Flags.midsOf("NOR")).containsExactly("257", "258", "259")
        assertThat(Flags.midsOf("FRA")).containsExactly("226", "227", "228")
        assertThat(Flags.alpha2("NOR")).isEqualTo("NO")
        assertThat(Flags.englishName("NLD")).isEqualTo("Netherlands")
    }
}
