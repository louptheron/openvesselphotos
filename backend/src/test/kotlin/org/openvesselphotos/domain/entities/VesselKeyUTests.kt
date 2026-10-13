package org.openvesselphotos.domain.entities

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.openvesselphotos.domain.exceptions.BackendUsageException

class VesselKeyUTests {
    @Test
    fun `of Should normalize a CFR`() {
        assertThat(VesselKey.of(KeyType.CFR, " fra 000-123456 ").value).isEqualTo("FRA000123456")
    }

    @Test
    fun `of Should reject an invalid MMSI`() {
        assertThatThrownBy { VesselKey.of(KeyType.MMSI, "22700010") }.isInstanceOf(BackendUsageException::class.java)
        assertThatThrownBy {
            VesselKey.of(
                KeyType.MMSI,
                "FRA000123456",
            )
        }.isInstanceOf(BackendUsageException::class.java)
    }

    @Test
    fun `detect Should recognize each key type`() {
        assertThat(VesselKey.detect("227000101")).isEqualTo(VesselKey(KeyType.MMSI, "227000101"))
        assertThat(VesselKey.detect("fra000123456")).isEqualTo(VesselKey(KeyType.CFR, "FRA000123456"))
        assertThat(VesselKey.detect("Ker Avel")).isNull()
    }
}
