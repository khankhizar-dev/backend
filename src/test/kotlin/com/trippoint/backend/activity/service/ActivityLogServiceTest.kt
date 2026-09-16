package com.trippoint.backend.activity.service

import com.trippoint.backend.activity.event.ActivityLogEventPublisher
import com.trippoint.backend.activity.model.ActivityTarget
import com.trippoint.backend.activity.repository.ActivityLogRepository
import com.trippoint.backend.trip.repository.TripMemberRepository
import com.trippoint.backend.trip.repository.TripRepository
import com.trippoint.backend.trip.service.TripAccessService
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class ActivityLogServiceTest {

    private lateinit var activityLogRepository: ActivityLogRepository
    private lateinit var activityLogService: ActivityLogService
    private lateinit var tripRepository: TripRepository
    private lateinit var tripMemberRepository: TripMemberRepository
    private lateinit var tripAccessService: TripAccessService
    private lateinit var activityLogEventPublisher: ActivityLogEventPublisher

    private val tripId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        activityLogRepository = mockk()
        tripRepository = mockk()
        tripMemberRepository = mockk()
        activityLogEventPublisher = mockk()

        tripAccessService = TripAccessService(
            tripRepository = tripRepository,
            tripMemberRepository = tripMemberRepository
        )

        activityLogService = ActivityLogService(
            activityLogRepository = activityLogRepository,
            tripAccessService = tripAccessService,
            activityLogEventPublisher = activityLogEventPublisher
        )

        every {
            activityLogEventPublisher.publish(any())
        } just Runs

        every {
            activityLogEventPublisher.publish(any())
        } just Runs
    }

    @Test
    fun `log creates activity entry`() {
        every {
            activityLogRepository.save(any())
        } answers {
            firstArg()
        }

        val result = activityLogService.log(
            tripId = tripId,
            userId = userId,
            action = " sent a message ",
            targetType = ActivityTarget.CHAT,
            targetName = " Chat "
        )

        assertEquals(tripId, result.tripId)
        assertEquals(userId, result.userId)
        assertEquals("sent a message", result.action)
        assertEquals(ActivityTarget.CHAT, result.targetType)
        assertEquals("Chat", result.targetName)
    }

    @Test
    fun `blank action is rejected`() {
        assertThrows<IllegalArgumentException> {
            activityLogService.log(
                tripId = tripId,
                userId = userId,
                action = " ",
                targetType = ActivityTarget.CHAT,
                targetName = "Chat"
            )
        }
    }

    @Test
    fun `blank target name is rejected`() {
        assertThrows<IllegalArgumentException> {
            activityLogService.log(
                tripId = tripId,
                userId = userId,
                action = "sent a message",
                targetType = ActivityTarget.CHAT,
                targetName = " "
            )
        }
    }
}