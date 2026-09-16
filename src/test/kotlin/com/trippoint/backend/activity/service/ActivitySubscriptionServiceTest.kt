package com.trippoint.backend.activity.service

import com.trippoint.backend.trip.service.TripAccessService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class ActivitySubscriptionServiceTest {

    private val tripAccessService = mockk<TripAccessService>()
    private val service = ActivitySubscriptionService(tripAccessService)
    private val tripId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @Test
    fun `subscription requires trip member access`() {
        every {
            tripAccessService.requireMemberAccess(tripId, userId)
        } throws IllegalAccessException("not a member")

        assertThrows<IllegalAccessException> {
            service.subscribe(userId, tripId)
        }
    }

    @Test
    fun `subscription checks member access before creating stream`() {
        every {
            tripAccessService.requireMemberAccess(tripId, userId)
        } just runs

        service.subscribe(userId, tripId)

        verify(exactly = 1) {
            tripAccessService.requireMemberAccess(tripId, userId)
        }
    }
}
