package com.trippoint.backend.notification.service

import com.trippoint.backend.notification.entity.NotificationPreference
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalTime
import java.util.UUID

class NotificationQuietHoursServiceTest {

    private val service = NotificationQuietHoursService()

    private fun preference(
        enabled: Boolean = true,
        start: LocalTime? = LocalTime.of(22, 0),
        end: LocalTime? = LocalTime.of(7, 0)
    ): NotificationPreference {
        return NotificationPreference(
            userId = UUID.randomUUID(),
            quietHoursEnabled = enabled,
            quietHoursStart = start,
            quietHoursEnd = end
        )
    }

    @Test
    fun `should return false when quiet hours are disabled`() {
        val preference = preference(
            enabled = false
        )

        assertFalse(
            service.isQuietHours(
                preference,
                LocalTime.of(23, 0)
            )
        )
    }

    @Test
    fun `should return true during overnight quiet hours`() {
        val preference = preference()

        assertTrue(
            service.isQuietHours(
                preference,
                LocalTime.of(23, 0)
            )
        )

        assertTrue(
            service.isQuietHours(
                preference,
                LocalTime.of(5, 0)
            )
        )
    }

    @Test
    fun `should return false outside overnight quiet hours`() {
        val preference = preference()

        assertFalse(
            service.isQuietHours(
                preference,
                LocalTime.of(12, 0)
            )
        )
    }

    @Test
    fun `should return true at quiet hours start`() {
        val preference = preference()

        assertTrue(
            service.isQuietHours(
                preference,
                LocalTime.of(22, 0)
            )
        )
    }

    @Test
    fun `should return false at quiet hours end`() {
        val preference = preference()

        assertFalse(
            service.isQuietHours(
                preference,
                LocalTime.of(7, 0)
            )
        )
    }

    @Test
    fun `should support daytime quiet hours`() {
        val preference = preference(
            start = LocalTime.of(10, 0),
            end = LocalTime.of(14, 0)
        )

        assertTrue(
            service.isQuietHours(
                preference,
                LocalTime.of(12, 0)
            )
        )

        assertFalse(
            service.isQuietHours(
                preference,
                LocalTime.of(15, 0)
            )
        )
    }

    @Test
    fun `should return false when quiet hours start is missing`() {
        val preference = preference(
            start = null
        )

        assertFalse(
            service.isQuietHours(
                preference,
                LocalTime.of(23, 0)
            )
        )
    }

    @Test
    fun `should return false when quiet hours end is missing`() {
        val preference = preference(
            end = null
        )

        assertFalse(
            service.isQuietHours(
                preference,
                LocalTime.of(23, 0)
            )
        )
    }

    @Test
    fun `should treat equal start and end as full day quiet hours`() {
        val preference = preference(
            start = LocalTime.of(22, 0),
            end = LocalTime.of(22, 0)
        )

        assertTrue(
            service.isQuietHours(
                preference,
                LocalTime.of(10, 0)
            )
        )
    }
}