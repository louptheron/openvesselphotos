package org.openvesselphotos.domain.use_cases.public_api

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.openvesselphotos.domain.exceptions.BackendUsageException
import org.openvesselphotos.domain.fishing_activity.FishingActivityClient
import org.openvesselphotos.domain.fishing_activity.FishingActivityRepository

class GetFishingEffortUTests {
    private val client = mock(FishingActivityClient::class.java)
    private val repository = mock(FishingActivityRepository::class.java)

    @Test
    fun `tile Should ask for the French vessels, with the gear types of the gear family`() {
        `when`(client.isEnabled).thenReturn(true)
        `when`(client.fishingEffortTile(5, 15, 11, "FRA", listOf("trawlers"))).thenReturn(byteArrayOf(1))

        assertThat(GetFishingEffort(client, repository).tile(5, 15, 11, "chalut-de-fond")).containsExactly(1)
        verify(client).fishingEffortTile(5, 15, 11, "FRA", listOf("trawlers"))
    }

    @Test
    fun `tile Should refuse tiles out of the map, gears other than trawls, or without token`() {
        `when`(client.isEnabled).thenReturn(true)
        val getFishingEffort = GetFishingEffort(client, repository)

        assertThatThrownBy { getFishingEffort.tile(10, 0, 0, null) }.isInstanceOf(BackendUsageException::class.java)
        assertThatThrownBy { getFishingEffort.tile(2, 4, 0, null) }.isInstanceOf(BackendUsageException::class.java)
        // Too few French dredgers carry AIS for a meaningful map
        assertThatThrownBy { getFishingEffort.tile(2, 0, 0, "dragues") }.isInstanceOf(BackendUsageException::class.java)

        `when`(client.isEnabled).thenReturn(false)
        assertThatThrownBy { getFishingEffort.tile(2, 0, 0, null) }.isInstanceOf(BackendUsageException::class.java)
    }

    @Test
    fun `port Should give the cells of the vessels of the port with the register gears of the family`() {
        GetFishingEffort(client, repository).port("frlrt", "chalut-pelagique")

        verify(repository).findPortCells("FRLRT", listOf("OTM", "PTM", "TM", "TMS"))
    }
}
