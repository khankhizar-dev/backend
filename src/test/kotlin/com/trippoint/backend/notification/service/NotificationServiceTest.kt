package com.trippoint.backend.notification.service

import com.trippoint.backend.notification.entity.Notification
import com.trippoint.backend.notification.model.NotificationCategory
import com.trippoint.backend.notification.model.NotificationType
import com.trippoint.backend.notification.repository.NotificationRepository
import com.trippoint.backend.notification.subscription.NotificationSubscriptionService
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class NotificationServiceTest {

    private lateinit var repository: NotificationRepository
    private lateinit var service: NotificationService
    private lateinit var notificationSubscriptionService: NotificationSubscriptionService

    private val userId = UUID.randomUUID()
    private val otherUserId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        repository = mockk()
        notificationSubscriptionService = mockk(relaxed = true)
        service = NotificationService(repository, notificationSubscriptionService)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `create should create notification for recipient`() {
        every { repository.save(any()) } answers { firstArg() }

        val notification = service.create(
            recipientUserId = userId,
            actorUserId = otherUserId,
            tripId = UUID.randomUUID(),
            category = NotificationCategory.TRIP,
            type = NotificationType.TRIP_UPDATED,
            title = "Trip updated",
            message = "Your trip has been updated",
            targetType = "TRIP",
            targetId = UUID.randomUUID(),
            targetName = "Goa Trip"
        )

        assertEquals(userId, notification.recipientUserId)
        assertEquals(otherUserId, notification.actorUserId)
        assertEquals(NotificationCategory.TRIP, notification.category)
        assertEquals(NotificationType.TRIP_UPDATED, notification.type)
        assertFalse(notification.isRead)
        assertFalse(notification.isArchived)
        assertFalse(notification.isDeleted)

        verify(exactly = 1) { repository.save(any()) }
        verify(exactly = 1) {
            notificationSubscriptionService.publish(notification)
        }
    }

    @Test
    fun `getNotification should return notification belonging to user`() {
        val notification = notification(userId)

        every {
            repository.findByIdAndRecipientUserId(notification.id, userId)
        } returns notification

        val result = service.getNotification(
            notification.id,
            userId
        )

        assertEquals(notification.id, result.id)
        assertEquals(userId, result.recipientUserId)
    }

    @Test
    fun `getNotification should reject deleted notification`() {
        val notification = notification(userId).apply {
            isDeleted = true
        }

        every {
            repository.findByIdAndRecipientUserId(notification.id, userId)
        } returns notification

        assertThrows(IllegalArgumentException::class.java) {
            service.getNotification(notification.id, userId)
        }
    }

    @Test
    fun `getNotification should reject notification belonging to another user`() {
        val notification = notification(userId)

        every {
            repository.findByIdAndRecipientUserId(
                notification.id,
                otherUserId
            )
        } returns null

        assertThrows(IllegalArgumentException::class.java) {
            service.getNotification(
                notification.id,
                otherUserId
            )
        }
    }

    @Test
    fun `markAsRead should mark unread notification`() {
        val notification = notification(userId)

        every {
            repository.findByIdAndRecipientUserId(notification.id, userId)
        } returns notification

        every { repository.save(notification) } returns notification

        val result = service.markAsRead(
            notification.id,
            userId
        )

        assertTrue(result.isRead)
        assertNotNull(result.readAt)

        verify(exactly = 1) {
            repository.save(notification)
        }
    }

    @Test
    fun `markAsRead should not change readAt when already read`() {
        val originalReadAt = LocalDateTime.now().minusMinutes(10)

        val notification = notification(userId).apply {
            isRead = true
            readAt = originalReadAt
        }

        every {
            repository.findByIdAndRecipientUserId(notification.id, userId)
        } returns notification

        every { repository.save(notification) } returns notification

        val result = service.markAsRead(
            notification.id,
            userId
        )

        assertTrue(result.isRead)
        assertEquals(originalReadAt, result.readAt)
    }

    @Test
    fun `getUnreadCount should return repository count`() {
        every {
            repository.countByRecipientUserIdAndIsReadFalseAndIsDeletedFalse(userId)
        } returns 7L

        val result = service.getUnreadCount(userId)

        assertEquals(7L, result)
    }

    @Test
    fun `archive should archive notification`() {
        val notification = notification(userId)

        every {
            repository.findByIdAndRecipientUserId(notification.id, userId)
        } returns notification

        every { repository.save(notification) } returns notification

        val result = service.archive(
            notification.id,
            userId
        )

        assertTrue(result.isArchived)
        assertNotNull(result.archivedAt)
    }

    @Test
    fun `snooze should set future snooze time`() {
        val notification = notification(userId)
        val snoozeUntil = LocalDateTime.now().plusHours(2)

        every {
            repository.findByIdAndRecipientUserId(notification.id, userId)
        } returns notification

        every { repository.save(notification) } returns notification

        val result = service.snooze(
            notification.id,
            userId,
            snoozeUntil
        )

        assertEquals(snoozeUntil, result.snoozedUntil)
    }

    @Test
    fun `snooze should reject past time`() {
        val notification = notification(userId)
        val snoozeUntil = LocalDateTime.now().minusMinutes(1)

        assertThrows(IllegalArgumentException::class.java) {
            service.snooze(
                notification.id,
                userId,
                snoozeUntil
            )
        }

        verify(exactly = 0) {
            repository.save(any())
        }
    }

    @Test
    fun `clearSnooze should remove snooze time`() {
        val notification = notification(userId).apply {
            snoozedUntil = LocalDateTime.now().plusHours(1)
        }

        every {
            repository.findByIdAndRecipientUserId(notification.id, userId)
        } returns notification

        every { repository.save(notification) } returns notification

        val result = service.clearSnooze(
            notification.id,
            userId
        )

        assertNull(result.snoozedUntil)
    }

    @Test
    fun `delete should soft delete notification`() {
        val notification = notification(userId)

        every {
            repository.findByIdAndRecipientUserId(notification.id, userId)
        } returns notification

        every { repository.save(notification) } returns notification

        val result = service.delete(
            notification.id,
            userId
        )

        assertTrue(result.isDeleted)
        assertNotNull(result.deletedAt)
    }

    private fun notification(
        recipientUserId: UUID
    ): Notification {
        return Notification(
            recipientUserId = recipientUserId,
            category = NotificationCategory.TRIP,
            type = NotificationType.TRIP_UPDATED,
            title = "Trip updated",
            message = "Trip was updated"
        )
    }
}