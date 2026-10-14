package org.openvesselphotos.infrastructure.ports

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CsvPortLocationRepositoryUTests {
    private val repository = CsvPortLocationRepository()

    @Test
    fun `findByCode Should find ports from UN LOCODE and the register's extra codes`() {
        val brest = repository.findByCode("FRBES")!!
        assertThat(brest.latitude).isCloseTo(
            48.4,
            org.assertj.core.data.Offset
                .offset(0.01),
        )
        assertThat(brest.longitude).isCloseTo(
            -4.48,
            org.assertj.core.data.Offset
                .offset(0.01),
        )
        // Missing from UN/LOCODE, added by scripts/ports.py
        assertThat(repository.findByCode("REREU")).isNotNull()
        assertThat(repository.findByCode("FRLC5")).isNotNull()
        assertThat(repository.findByCode("ZZZZZ")).isNull()
    }
}
