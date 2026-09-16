package com.trippoint.backend.notification.service

import com.trippoint.backend.notification.entity.Reminder
import com.trippoint.backend.notification.event.NotificationEvent
import com.trippoint.backend.notification.event.NotificationEventPublisher
import com.trippoint.backend.notification.model.NotificationCategory
import com.trippoint.backend.notification.model.NotificationType
import com.trippoint.backend.notification.model.ReminderStatus
import com.trippoint.backend.notification.repository.ReminderRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class ReminderProcessingServiceTest {

    private lateinit var reminderRepository: ReminderRepository
    private lateinit var notificationEventPublisher: NotificationEventPublisher

    private lateinit var service: ReminderProcessingService

    @BeforeEach
    fun setUp() {
        reminderRepository = mockk()
        notificationEventPublisher = mockk()

        service = ReminderProcessingService(
            reminderRepository = reminderRepository,
            notificationEventPublisher = notificationEventPublisher
        )
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun createReminder(
        userId: UUID = UUID.randomUUID(),
        tripId: UUID? = UUID.randomUUID(),
        title: String = "Book hotel",
        dueAt: LocalDateTime = LocalDateTime.now().minusMinutes(10),
        status: ReminderStatus = ReminderStatus.UPCOMING,
        snoozedUntil: LocalDateTime? = null
    ): Reminder {
        return Reminder(
            id = UUID.randomUUID(),
            userId = userId,
            tripId = tripId,
            title = title,
            description = "Hotel booking reminder",
            dueAt = dueAt,
            status = status,
            snoozedUntil = snoozedUntil,
            recurring = false,
            recurrenceRule = null,
            completedAt = null,
            createdAt = LocalDateTime.now().minusHours(1),
            updatedAt = LocalDateTime.now().minusHours(1)
        )
    }

    // -------------------------------------------------------------------------
    // Successful processing
    // -------------------------------------------------------------------------

    @Test
    fun `processOverdueReminders should mark due reminder as overdue`() {
        val now = LocalDateTime.of(2026, 9, 16, 12, 0)

        val reminder = createReminder(
            dueAt = now.minusMinutes(10)
        )

        every {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        } returns listOf(reminder)

        every {
            notificationEventPublisher.publish(any())
        } just Runs

        every {
            reminderRepository.saveAll(any<Iterable<Reminder>>())
        } answers {
            firstArg()
        }

        val result = service.processOverdueReminders(now)

        assertEquals(1, result)
        assertEquals(ReminderStatus.OVERDUE, reminder.status)

        verify(exactly = 1) {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        }

        verify(exactly = 1) {
            reminderRepository.saveAll(any<Iterable<Reminder>>())
        }
    }

    @Test
    fun `processOverdueReminders should publish overdue notification`() {
        val now = LocalDateTime.of(2026, 9, 16, 12, 0)

        val userId = UUID.randomUUID()
        val tripId = UUID.randomUUID()

        val reminder = createReminder(
            userId = userId,
            tripId = tripId,
            title = "Check passport",
            dueAt = now.minusMinutes(30)
        )

        every {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        } returns listOf(reminder)

        every {
            reminderRepository.saveAll(any<Iterable<Reminder>>())
        } answers {
            firstArg()
        }

        val eventSlot = slot<NotificationEvent>()

        every {
            notificationEventPublisher.publish(capture(eventSlot))
        } just Runs

        service.processOverdueReminders(now)

        val event = eventSlot.captured

        assertEquals(userId, event.recipientUserId)
        assertNull(event.actorUserId)
        assertEquals(tripId, event.tripId)

        assertEquals(
            NotificationCategory.REMINDER,
            event.category
        )

        assertEquals(
            NotificationType.REMINDER_OVERDUE,
            event.type
        )

        assertEquals(
            "Reminder overdue",
            event.title
        )

        assertEquals(
            "Reminder \"Check passport\" is overdue",
            event.message
        )

        assertEquals(
            "REMINDER",
            event.targetType
        )

        assertEquals(
            reminder.id,
            event.targetId
        )

        assertEquals(
            reminder.title,
            event.targetName
        )
    }

    @Test
    fun `processOverdueReminders should publish notification to reminder owner`() {
        val now = LocalDateTime.of(2026, 9, 16, 12, 0)

        val userId = UUID.randomUUID()

        val reminder = createReminder(
            userId = userId,
            dueAt = now.minusMinutes(5)
        )

        every {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        } returns listOf(reminder)

        every {
            reminderRepository.saveAll(any<Iterable<Reminder>>())
        } answers {
            firstArg()
        }

        every {
            notificationEventPublisher.publish(
                match {
                    it.recipientUserId == userId
                }
            )
        } just Runs

        service.processOverdueReminders(now)

        verify(exactly = 1) {
            notificationEventPublisher.publish(
                match {
                    it.recipientUserId == userId
                }
            )
        }
    }

    // -------------------------------------------------------------------------
    // Future reminders
    // -------------------------------------------------------------------------

    @Test
    fun `processOverdueReminders should not mark future reminder as overdue`() {
        val now = LocalDateTime.of(2026, 9, 16, 12, 0)

        val reminder = createReminder(
            dueAt = now.plusMinutes(30)
        )

        every {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        } returns emptyList()

        val result = service.processOverdueReminders(now)

        assertEquals(0, result)
        assertEquals(ReminderStatus.UPCOMING, reminder.status)

        verify(exactly = 1) {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        }

        verify(exactly = 0) {
            reminderRepository.saveAll(any<Iterable<Reminder>>())
        }

        verify(exactly = 0) {
            notificationEventPublisher.publish(any())
        }
    }

    // -------------------------------------------------------------------------
    // Empty repository result
    // -------------------------------------------------------------------------

    @Test
    fun `processOverdueReminders should return zero when there are no due reminders`() {
        val now = LocalDateTime.of(2026, 9, 16, 12, 0)

        every {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        } returns emptyList()

        val result = service.processOverdueReminders(now)

        assertEquals(0, result)

        verify(exactly = 1) {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        }

        verify(exactly = 0) {
            reminderRepository.saveAll(any<Iterable<Reminder>>())
        }

        verify(exactly = 0) {
            notificationEventPublisher.publish(any())
        }
    }

    // -------------------------------------------------------------------------
    // Snoozed reminders
    // -------------------------------------------------------------------------

    @Test
    fun `processOverdueReminders should not process reminder while snooze is active`() {
        val now = LocalDateTime.of(2026, 9, 16, 12, 0)

        val reminder = createReminder(
            dueAt = now.minusHours(2),
            snoozedUntil = now.plusHours(1)
        )

        every {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        } returns listOf(reminder)

        val result = service.processOverdueReminders(now)

        assertEquals(0, result)

        assertEquals(
            ReminderStatus.UPCOMING,
            reminder.status
        )

        assertEquals(
            now.plusHours(1),
            reminder.snoozedUntil
        )

        verify(exactly = 1) {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        }

        verify(exactly = 0) {
            reminderRepository.saveAll(any<Iterable<Reminder>>())
        }

        verify(exactly = 0) {
            notificationEventPublisher.publish(any())
        }
    }

    @Test
    fun `processOverdueReminders should process reminder when snooze has expired`() {
        val now = LocalDateTime.of(2026, 9, 16, 12, 0)

        val reminder = createReminder(
            dueAt = now.minusHours(2),
            snoozedUntil = now.minusMinutes(1)
        )

        every {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        } returns listOf(reminder)

        every {
            notificationEventPublisher.publish(any())
        } just Runs

        every {
            reminderRepository.saveAll(any<Iterable<Reminder>>())
        } answers {
            firstArg()
        }

        val result = service.processOverdueReminders(now)

        assertEquals(1, result)

        assertEquals(
            ReminderStatus.OVERDUE,
            reminder.status
        )

        verify(exactly = 1) {
            notificationEventPublisher.publish(
                match {
                    it.targetId == reminder.id &&
                            it.type == NotificationType.REMINDER_OVERDUE
                }
            )
        }
    }

    @Test
    fun `processOverdueReminders should process reminder when snooze is exactly now`() {
        val now = LocalDateTime.of(2026, 9, 16, 12, 0)

        val reminder = createReminder(
            dueAt = now.minusHours(1),
            snoozedUntil = now
        )

        every {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        } returns listOf(reminder)

        every {
            notificationEventPublisher.publish(any())
        } just Runs

        every {
            reminderRepository.saveAll(any<Iterable<Reminder>>())
        } answers {
            firstArg()
        }

        val result = service.processOverdueReminders(now)

        assertEquals(1, result)
        assertEquals(ReminderStatus.OVERDUE, reminder.status)
    }

    // -------------------------------------------------------------------------
    // Multiple reminders
    // -------------------------------------------------------------------------

    @Test
    fun `processOverdueReminders should process multiple due reminders`() {
        val now = LocalDateTime.of(2026, 9, 16, 12, 0)

        val reminder1 = createReminder(
            title = "Book hotel",
            dueAt = now.minusHours(2)
        )

        val reminder2 = createReminder(
            title = "Buy tickets",
            dueAt = now.minusHours(1)
        )

        val reminder3 = createReminder(
            title = "Check passport",
            dueAt = now.minusMinutes(10)
        )

        every {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        } returns listOf(
            reminder1,
            reminder2,
            reminder3
        )

        every {
            notificationEventPublisher.publish(any())
        } just Runs

        every {
            reminderRepository.saveAll(any<Iterable<Reminder>>())
        } answers {
            firstArg()
        }

        val result = service.processOverdueReminders(now)

        assertEquals(3, result)

        assertEquals(
            ReminderStatus.OVERDUE,
            reminder1.status
        )

        assertEquals(
            ReminderStatus.OVERDUE,
            reminder2.status
        )

        assertEquals(
            ReminderStatus.OVERDUE,
            reminder3.status
        )

        verify(exactly = 3) {
            notificationEventPublisher.publish(any())
        }

        verify(exactly = 1) {
            reminderRepository.saveAll(
                match<Iterable<Reminder>> {
                    it.toList().size == 3
                }
            )
        }
    }

    // -------------------------------------------------------------------------
    // Mixed reminders: active snooze + processable
    // -------------------------------------------------------------------------

    @Test
    fun `processOverdueReminders should process non-snoozed reminders and skip snoozed reminders`() {
        val now = LocalDateTime.of(2026, 9, 16, 12, 0)

        val processableReminder = createReminder(
            title = "Book hotel",
            dueAt = now.minusHours(2)
        )

        val snoozedReminder = createReminder(
            title = "Buy tickets",
            dueAt = now.minusHours(2),
            snoozedUntil = now.plusHours(2)
        )

        every {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        } returns listOf(
            processableReminder,
            snoozedReminder
        )

        every {
            notificationEventPublisher.publish(any())
        } just Runs

        every {
            reminderRepository.saveAll<Reminder>(
                any<Iterable<Reminder>>()
            )
        } answers {
            firstArg()
        }

        val result = service.processOverdueReminders(now)

        assertEquals(1, result)

        assertEquals(
            ReminderStatus.OVERDUE,
            processableReminder.status
        )

        assertEquals(
            ReminderStatus.UPCOMING,
            snoozedReminder.status
        )

        verify(exactly = 1) {
            notificationEventPublisher.publish(
                match {
                    it.targetId == processableReminder.id
                }
            )
        }

        verify(exactly = 0) {
            notificationEventPublisher.publish(
                match {
                    it.targetId == snoozedReminder.id
                }
            )
        }

        verify(exactly = 1) {
            reminderRepository.saveAll<Reminder>(
                match<Iterable<Reminder>> {
                    val reminders = it.toList()

                    reminders.size == 1 &&
                            reminders.first().id == processableReminder.id &&
                            reminders.first().status == ReminderStatus.OVERDUE
                }
            )
        }
    }

    // -------------------------------------------------------------------------
    // Repository query
    // -------------------------------------------------------------------------

    @Test
    fun `processOverdueReminders should query only upcoming reminders`() {
        val now = LocalDateTime.of(2026, 9, 16, 12, 0)

        every {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        } returns emptyList()

        service.processOverdueReminders(now)

        verify(exactly = 1) {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        }
    }

    // -------------------------------------------------------------------------
    // Notification payload
    // -------------------------------------------------------------------------

    @Test
    fun `processOverdueReminders should use REMINDER as notification target type`() {
        val now = LocalDateTime.of(2026, 9, 16, 12, 0)

        val reminder = createReminder(
            title = "Submit visa documents",
            dueAt = now.minusMinutes(15)
        )

        every {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        } returns listOf(reminder)

        every {
            reminderRepository.saveAll(any<Iterable<Reminder>>())
        } answers {
            firstArg()
        }

        val eventSlot = slot<NotificationEvent>()

        every {
            notificationEventPublisher.publish(capture(eventSlot))
        } just Runs

        service.processOverdueReminders(now)

        assertEquals(
            "REMINDER",
            eventSlot.captured.targetType
        )

        assertEquals(
            reminder.id,
            eventSlot.captured.targetId
        )

        assertEquals(
            reminder.title,
            eventSlot.captured.targetName
        )
    }

    @Test
    fun `processOverdueReminders should not set actor user`() {
        val now = LocalDateTime.of(2026, 9, 16, 12, 0)

        val reminder = createReminder(
            dueAt = now.minusMinutes(15)
        )

        every {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        } returns listOf(reminder)

        every {
            reminderRepository.saveAll(any<Iterable<Reminder>>())
        } answers {
            firstArg()
        }

        val eventSlot = slot<NotificationEvent>()

        every {
            notificationEventPublisher.publish(capture(eventSlot))
        } just Runs

        service.processOverdueReminders(now)

        assertNull(eventSlot.captured.actorUserId)
    }

    // -------------------------------------------------------------------------
    // Recurring reminder
    // -------------------------------------------------------------------------

    @Test
    fun `processOverdueReminders should process recurring reminder`() {
        val now = LocalDateTime.of(2026, 9, 16, 12, 0)

        val reminder = createReminder(
            title = "Weekly trip planning",
            dueAt = now.minusMinutes(20)
        ).apply {
            recurring = true
            recurrenceRule = "FREQ=WEEKLY"
        }

        every {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        } returns listOf(reminder)

        every {
            notificationEventPublisher.publish(any())
        } just Runs

        every {
            reminderRepository.saveAll(any<Iterable<Reminder>>())
        } answers {
            firstArg()
        }

        val result = service.processOverdueReminders(now)

        assertEquals(1, result)

        assertEquals(
            ReminderStatus.OVERDUE,
            reminder.status
        )

        assertTrue(reminder.recurring)
        assertEquals("FREQ=WEEKLY", reminder.recurrenceRule)

        verify(exactly = 1) {
            notificationEventPublisher.publish(any())
        }
    }

    // -------------------------------------------------------------------------
    // Repository persistence
    // -------------------------------------------------------------------------

    @Test
    fun `processOverdueReminders should save processed reminders`() {
        val now = LocalDateTime.of(2026, 9, 16, 12, 0)

        val reminder = createReminder(
            dueAt = now.minusMinutes(10)
        )

        every {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        } returns listOf(reminder)

        every {
            notificationEventPublisher.publish(any())
        } just Runs

        every {
            reminderRepository.saveAll(any<Iterable<Reminder>>())
        } answers {
            firstArg()
        }

        service.processOverdueReminders(now)

        verify(exactly = 1) {
            reminderRepository.saveAll(
                match<Iterable<Reminder>> {
                    val reminders = it.toList()

                    reminders.size == 1 &&
                            reminders.first().id == reminder.id &&
                            reminders.first().status == ReminderStatus.OVERDUE
                }
            )
        }
    }

    // -------------------------------------------------------------------------
    // Verification
    // -------------------------------------------------------------------------

    @Test
    fun `processOverdueReminders should not publish notification when all reminders are snoozed`() {
        val now = LocalDateTime.of(2026, 9, 16, 12, 0)

        val reminder1 = createReminder(
            dueAt = now.minusHours(1),
            snoozedUntil = now.plusHours(1)
        )

        val reminder2 = createReminder(
            dueAt = now.minusHours(2),
            snoozedUntil = now.plusHours(2)
        )

        every {
            reminderRepository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )
        } returns listOf(reminder1, reminder2)

        val result = service.processOverdueReminders(now)

        assertEquals(0, result)

        assertEquals(
            ReminderStatus.UPCOMING,
            reminder1.status
        )

        assertEquals(
            ReminderStatus.UPCOMING,
            reminder2.status
        )

        verify(exactly = 0) {
            notificationEventPublisher.publish(any())
        }

        verify(exactly = 0) {
            reminderRepository.saveAll(any<Iterable<Reminder>>())
        }
    }
}