package org.openvesselphotos.domain.use_cases.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openvesselphotos.domain.fishing_activity.FishingCell
import org.openvesselphotos.domain.fishing_activity.FishingEvent

class SyncFishingActivityUTests {
    @Test
    fun `toCells Should sum the hours of fishing by cell of 0,05 degree`() {
        val cells =
            SyncFishingActivity.toCells(
                listOf(
                    FishingEvent(47.671, -3.415, 2.0),
                    FishingEvent(47.699, -3.401, 1.5),
                    FishingEvent(47.71, -3.40, 4.0),
                    // Not fishing
                    FishingEvent(47.67, -3.41, 0.0),
                ),
            )

        assertThat(cells).containsExactlyInAnyOrder(
            FishingCell(47.675, -3.425, 3.5),
            FishingCell(47.725, -3.375, 4.0),
        )
    }
}
