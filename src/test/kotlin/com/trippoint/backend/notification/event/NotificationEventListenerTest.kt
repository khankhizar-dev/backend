package com.trippoint.backend.notification.event

import com.trippoint.backend.notification.entity.Notification
import com.trippoint.backend.notification.entity.NotificationPreference
import com.trippoint.backend.notification.model.NotificationCategory
import com.trippoint.backend.notification.model.NotificationType
import com.trippoint.backend.notification.service.NotificationService
import com.trippoint.backend.notification.service.NotificationPreferenceService
import com.trippoint.backend.notification.service.NotificationQuietHoursService
import com.trippoint.backend.notification.subscription.NotificationSubscriptionService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalTime
import java.util.UUID

class NotificationEventListenerTest {

    private lateinit var notificationService: NotificationService
    private lateinit var notificationPreferenceService: NotificationPreferenceService
    private lateinit var notificationSubscriptionService: NotificationSubscriptionService
    private lateinit var notificationQuietHoursService: NotificationQuietHoursService

    private lateinit var listener: NotificationEventListener

    @BeforeEach
    fun setUp() {
        notificationService = mockk()
        notificationPreferenceService = mockk()
        notificationSubscriptionService = mockk()
        notificationQuietHoursService = mockk()

        listener = NotificationEventListener(
            notificationService = notificationService,
            notificationPreferenceService = notificationPreferenceService,
            notificationSubscriptionService = notificationSubscriptionService,
            notificationQuietHoursService = notificationQuietHoursService
        )
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun event(
        recipientUserId: UUID = UUID.randomUUID()
    ): NotificationEvent {
        return NotificationEvent(
            recipientUserId = recipientUserId,
            actorUserId = UUID.randomUUID(),
            tripId = UUID.randomUUID(),
            category = NotificationCategory.REMINDER,
            type = NotificationType.REMINDER_OVERDUE,
            title = "Reminder overdue",
            message = "Your reminder is overdue",
            targetType = "REMINDER",
            targetId = UUID.randomUUID(),
            targetName = "Book hotel"
        )
    }

    private fun preference(
        userId: UUID,
        inAppEnabled: Boolean
    ): NotificationPreference {
        return NotificationPreference(
            userId = userId,
            inAppEnabled = inAppEnabled
        )
    }

    // -------------------------------------------------------------------------
    // In-app enabled
    // -------------------------------------------------------------------------

    @Test
    fun `handle should create and publish notification when in-app notifications are enabled`() {
        val event = event()

        val preference = preference(
            userId = event.recipientUserId,
            inAppEnabled = true
        )

        val notification = mockk<Notification>()

        every {
            notificationPreferenceService.getOrCreate(
                event.recipientUserId
            )
        } returns preference

        every {
            notificationQuietHoursService.isQuietHours(
                preference,
                any()
            )
        } returns false

        every {
            notificationService.create(
                recipientUserId = event.recipientUserId,
                actorUserId = event.actorUserId,
                tripId = event.tripId,
                category = event.category,
                type = event.type,
                title = event.title,
                message = event.message,
                targetType = event.targetType,
                targetId = event.targetId,
                targetName = event.targetName
            )
        } returns notification

        every {
            notificationSubscriptionService.publish(notification)
        } just Runs

        listener.handle(event)

        verify(exactly = 1) {
            notificationPreferenceService.getOrCreate(
                event.recipientUserId
            )
        }

        verify(exactly = 1) {
            notificationQuietHoursService.isQuietHours(
                preference,
                any()
            )
        }

        verify(exactly = 1) {
            notificationService.create(
                recipientUserId = event.recipientUserId,
                actorUserId = event.actorUserId,
                tripId = event.tripId,
                category = event.category,
                type = event.type,
                title = event.title,
                message = event.message,
                targetType = event.targetType,
                targetId = event.targetId,
                targetName = event.targetName
            )
        }

        verify(exactly = 1) {
            notificationSubscriptionService.publish(notification)
        }
    }

    // -------------------------------------------------------------------------
    // In-app disabled
    // -------------------------------------------------------------------------

    @Test
    fun `handle should not create notification when in-app notifications are disabled`() {
        val event = event()

        val preference = preference(
            userId = event.recipientUserId,
            inAppEnabled = false
        )

        every {
            notificationPreferenceService.getOrCreate(
                event.recipientUserId
            )
        } returns preference

        listener.handle(event)

        verify(exactly = 1) {
            notificationPreferenceService.getOrCreate(
                event.recipientUserId
            )
        }

        verify(exactly = 0) {
            notificationQuietHoursService.isQuietHours(
                any(),
                any()
            )
        }

        verify(exactly = 0) {
            notificationService.create(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }

        verify(exactly = 0) {
            notificationSubscriptionService.publish(any())
        }
    }

    // -------------------------------------------------------------------------
    // Recipient preference
    // -------------------------------------------------------------------------

    @Test
    fun `handle should use recipient user preference`() {
        val recipientUserId = UUID.randomUUID()

        val event = event(
            recipientUserId = recipientUserId
        )

        val preference = preference(
            userId = recipientUserId,
            inAppEnabled = true
        )

        val notification = mockk<Notification>()

        every {
            notificationPreferenceService.getOrCreate(
                recipientUserId
            )
        } returns preference

        every {
            notificationQuietHoursService.isQuietHours(
                preference,
                any()
            )
        } returns false

        every {
            notificationService.create(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns notification

        every {
            notificationSubscriptionService.publish(notification)
        } just Runs

        listener.handle(event)

        verify(exactly = 1) {
            notificationPreferenceService.getOrCreate(
                recipientUserId
            )
        }

        verify(exactly = 1) {
            notificationQuietHoursService.isQuietHours(
                preference,
                any()
            )
        }

        verify(exactly = 1) {
            notificationService.create(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }

        verify(exactly = 1) {
            notificationSubscriptionService.publish(notification)
        }
    }

    // -------------------------------------------------------------------------
    // Outside quiet hours
    // -------------------------------------------------------------------------

    @Test
    fun `handle should create notification outside quiet hours`() {
        val event = event()

        val preference = preference(
            userId = event.recipientUserId,
            inAppEnabled = true
        ).apply {
            timezone = "Asia/Kolkata"
            quietHoursEnabled = true
            quietHoursStart = LocalTime.of(22, 0)
            quietHoursEnd = LocalTime.of(7, 0)
        }

        val notification = mockk<Notification>()

        every {
            notificationPreferenceService.getOrCreate(
                event.recipientUserId
            )
        } returns preference

        every {
            notificationQuietHoursService.isQuietHours(
                preference,
                any()
            )
        } returns false

        every {
            notificationService.create(
                recipientUserId = event.recipientUserId,
                actorUserId = event.actorUserId,
                tripId = event.tripId,
                category = event.category,
                type = event.type,
                title = event.title,
                message = event.message,
                targetType = event.targetType,
                targetId = event.targetId,
                targetName = event.targetName
            )
        } returns notification

        every {
            notificationSubscriptionService.publish(notification)
        } just Runs

        listener.handle(event)

        verify(exactly = 1) {
            notificationPreferenceService.getOrCreate(
                event.recipientUserId
            )
        }

        verify(exactly = 1) {
            notificationQuietHoursService.isQuietHours(
                preference,
                any()
            )
        }

        verify(exactly = 1) {
            notificationService.create(
                recipientUserId = event.recipientUserId,
                actorUserId = event.actorUserId,
                tripId = event.tripId,
                category = event.category,
                type = event.type,
                title = event.title,
                message = event.message,
                targetType = event.targetType,
                targetId = event.targetId,
                targetName = event.targetName
            )
        }

        verify(exactly = 1) {
            notificationSubscriptionService.publish(notification)
        }
    }

    // -------------------------------------------------------------------------
    // During quiet hours
    // -------------------------------------------------------------------------

    @Test
    fun `handle should not create notification during quiet hours`() {
        val event = event()

        val preference = preference(
            userId = event.recipientUserId,
            inAppEnabled = true
        ).apply {
            timezone = "Asia/Kolkata"
            quietHoursEnabled = true
            quietHoursStart = LocalTime.of(22, 0)
            quietHoursEnd = LocalTime.of(7, 0)
        }

        every {
            notificationPreferenceService.getOrCreate(
                event.recipientUserId
            )
        } returns preference

        every {
            notificationQuietHoursService.isQuietHours(
                preference,
                any()
            )
        } returns true

        listener.handle(event)

        verify(exactly = 1) {
            notificationPreferenceService.getOrCreate(
                event.recipientUserId
            )
        }

        verify(exactly = 1) {
            notificationQuietHoursService.isQuietHours(
                preference,
                any()
            )
        }

        verify(exactly = 0) {
            notificationService.create(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }

        verify(exactly = 0) {
            notificationSubscriptionService.publish(any())
        }
    }

    // -------------------------------------------------------------------------
    // Invalid timezone
    // -------------------------------------------------------------------------

    @Test
    fun `handle should use India timezone when stored timezone is invalid`() {
        val event = event()

        val preference = preference(
            userId = event.recipientUserId,
            inAppEnabled = true
        ).apply {
            timezone = "INVALID/TIMEZONE"
        }

        val notification = mockk<Notification>()

        every {
            notificationPreferenceService.getOrCreate(
                event.recipientUserId
            )
        } returns preference

        every {
            notificationQuietHoursService.isQuietHours(
                preference,
                any()
            )
        } returns false

        every {
            notificationService.create(
                recipientUserId = event.recipientUserId,
                actorUserId = event.actorUserId,
                tripId = event.tripId,
                category = event.category,
                type = event.type,
                title = event.title,
                message = event.message,
                targetType = event.targetType,
                targetId = event.targetId,
                targetName = event.targetName
            )
        } returns notification

        every {
            notificationSubscriptionService.publish(notification)
        } just Runs

        listener.handle(event)

        verify(exactly = 1) {
            notificationPreferenceService.getOrCreate(
                event.recipientUserId
            )
        }

        verify(exactly = 1) {
            notificationQuietHoursService.isQuietHours(
                preference,
                any()
            )
        }

        verify(exactly = 1) {
            notificationService.create(
                recipientUserId = event.recipientUserId,
                actorUserId = event.actorUserId,
                tripId = event.tripId,
                category = event.category,
                type = event.type,
                title = event.title,
                message = event.message,
                targetType = event.targetType,
                targetId = event.targetId,
                targetName = event.targetName
            )
        }

        verify(exactly = 1) {
            notificationSubscriptionService.publish(notification)
        }
    }
}