package com.trippoint.backend.notification.service

import com.trippoint.backend.notification.entity.Reminder
import com.trippoint.backend.notification.model.ReminderStatus
import com.trippoint.backend.notification.repository.ReminderRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ReminderServiceTest {

    private lateinit var repository: ReminderRepository
    private lateinit var service: ReminderService

    private val userId = UUID.randomUUID()
    private val otherUserId = UUID.randomUUID()
    private val tripId = UUID.randomUUID()
    private val reminderId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        repository = mockk()
        service = ReminderService(repository)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // ---------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------

    @Test
    fun `create should create upcoming reminder`() {
        every {
            repository.save(any())
        } answers {
            firstArg()
        }

        val dueAt = LocalDateTime.now().plusHours(2)

        val result = service.create(
            userId = userId,
            tripId = tripId,
            title = "Book airport taxi",
            description = "Book taxi one day before departure",
            dueAt = dueAt
        )

        assertEquals(userId, result.userId)
        assertEquals(tripId, result.tripId)
        assertEquals("Book airport taxi", result.title)
        assertEquals(
            "Book taxi one day before departure",
            result.description
        )
        assertEquals(dueAt, result.dueAt)
        assertEquals(ReminderStatus.UPCOMING, result.status)
        assertFalse(result.recurring)
        assertNull(result.recurrenceRule)
        assertNull(result.completedAt)
        assertNull(result.snoozedUntil)

        verify(exactly = 1) {
            repository.save(any())
        }
    }

    @Test
    fun `create should reject blank title`() {
        val dueAt = LocalDateTime.now().plusHours(2)

        assertFailsWith<IllegalArgumentException> {
            service.create(
                userId = userId,
                title = "   ",
                dueAt = dueAt
            )
        }

        verify(exactly = 0) {
            repository.save(any())
        }
    }

    @Test
    fun `create should reject past due time`() {
        val dueAt = LocalDateTime.now().minusMinutes(1)

        assertFailsWith<IllegalArgumentException> {
            service.create(
                userId = userId,
                title = "Past reminder",
                dueAt = dueAt
            )
        }

        verify(exactly = 0) {
            repository.save(any())
        }
    }

    @Test
    fun `create should create recurring reminder with recurrence rule`() {
        every {
            repository.save(any())
        } answers {
            firstArg()
        }

        val dueAt = LocalDateTime.now().plusDays(1)

        val result = service.create(
            userId = userId,
            title = "Weekly trip planning",
            dueAt = dueAt,
            recurring = true,
            recurrenceRule = "WEEKLY"
        )

        assertTrue(result.recurring)
        assertEquals("WEEKLY", result.recurrenceRule)
        assertEquals(ReminderStatus.UPCOMING, result.status)

        verify(exactly = 1) {
            repository.save(any())
        }
    }

    @Test
    fun `create should reject recurring reminder without recurrence rule`() {
        val dueAt = LocalDateTime.now().plusDays(1)

        assertFailsWith<IllegalArgumentException> {
            service.create(
                userId = userId,
                title = "Recurring reminder",
                dueAt = dueAt,
                recurring = true,
                recurrenceRule = null
            )
        }

        verify(exactly = 0) {
            repository.save(any())
        }
    }

    // ---------------------------------------------------------
    // GET
    // ---------------------------------------------------------

    @Test
    fun `getReminder should return reminder belonging to user`() {
        val reminder = reminder(userId)

        every {
            repository.findByIdAndUserId(
                reminder.id,
                userId
            )
        } returns reminder

        val result = service.getReminder(
            reminder.id,
            userId
        )

        assertEquals(reminder.id, result.id)
        assertEquals(userId, result.userId)

        verify(exactly = 1) {
            repository.findByIdAndUserId(
                reminder.id,
                userId
            )
        }
    }

    @Test
    fun `getReminder should reject missing reminder`() {
        every {
            repository.findByIdAndUserId(
                reminderId,
                userId
            )
        } returns null

        assertFailsWith<IllegalArgumentException> {
            service.getReminder(
                reminderId,
                userId
            )
        }
    }

    @Test
    fun `getReminder should reject reminder belonging to another user`() {
        val reminder = reminder(otherUserId)

        every {
            repository.findByIdAndUserId(
                reminder.id,
                userId
            )
        } returns null

        assertFailsWith<IllegalArgumentException> {
            service.getReminder(
                reminder.id,
                userId
            )
        }

        verify(exactly = 1) {
            repository.findByIdAndUserId(
                reminder.id,
                userId
            )
        }
    }

    // ---------------------------------------------------------
    // LIST
    // ---------------------------------------------------------

    @Test
    fun `getReminders should return all reminders for user`() {
        val reminders = listOf(
            reminder(userId),
            reminder(userId)
        )

        val pageable = PageRequest.of(0, 20)

        every {
            repository.findAllByUserIdOrderByDueAtAscIdAsc(
                userId,
                pageable
            )
        } returns reminders

        val result = service.getReminders(
            userId = userId
        )

        assertEquals(2, result.size)
        assertEquals(reminders, result)

        verify(exactly = 1) {
            repository.findAllByUserIdOrderByDueAtAscIdAsc(
                userId,
                pageable
            )
        }
    }

    @Test
    fun `getReminders should filter by status`() {
        val reminders = listOf(
            reminder(userId).apply {
                status = ReminderStatus.UPCOMING
            }
        )

        val pageable = PageRequest.of(0, 20)

        every {
            repository.findAllByUserIdAndStatusOrderByDueAtAscIdAsc(
                userId,
                ReminderStatus.UPCOMING,
                pageable
            )
        } returns reminders

        val result = service.getReminders(
            userId = userId,
            status = ReminderStatus.UPCOMING
        )

        assertEquals(1, result.size)
        assertEquals(
            ReminderStatus.UPCOMING,
            result.first().status
        )

        verify(exactly = 1) {
            repository.findAllByUserIdAndStatusOrderByDueAtAscIdAsc(
                userId,
                ReminderStatus.UPCOMING,
                pageable
            )
        }
    }

    @Test
    fun `getReminders should filter by trip`() {
        val reminders = listOf(
            reminder(userId).apply {
                this.tripId = tripId
            }
        )

        val pageable = PageRequest.of(0, 20)

        every {
            repository.findAllByUserIdAndTripIdOrderByDueAtAscIdAsc(
                userId,
                tripId,
                pageable
            )
        } returns reminders

        val result = service.getReminders(
            userId = userId,
            tripId = tripId
        )

        assertEquals(1, result.size)
        assertEquals(tripId, result.first().tripId)

        verify(exactly = 1) {
            repository.findAllByUserIdAndTripIdOrderByDueAtAscIdAsc(
                userId,
                tripId,
                pageable
            )
        }
    }

    @Test
    fun `getReminders should cap limit at 100`() {
        val pageable = PageRequest.of(0, 100)

        every {
            repository.findAllByUserIdOrderByDueAtAscIdAsc(
                userId,
                pageable
            )
        } returns emptyList()

        val result = service.getReminders(
            userId = userId,
            limit = 500
        )

        assertTrue(result.isEmpty())

        verify(exactly = 1) {
            repository.findAllByUserIdOrderByDueAtAscIdAsc(
                userId,
                pageable
            )
        }
    }

    @Test
    fun `getReminders should enforce minimum limit of one`() {
        val pageable = PageRequest.of(0, 1)

        every {
            repository.findAllByUserIdOrderByDueAtAscIdAsc(
                userId,
                pageable
            )
        } returns emptyList()

        val result = service.getReminders(
            userId = userId,
            limit = 0
        )

        assertTrue(result.isEmpty())

        verify(exactly = 1) {
            repository.findAllByUserIdOrderByDueAtAscIdAsc(
                userId,
                pageable
            )
        }
    }

    // ---------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------

    @Test
    fun `update should update reminder title`() {
        val reminder = reminder(userId)

        every {
            repository.findByIdAndUserId(
                reminder.id,
                userId
            )
        } returns reminder

        every {
            repository.save(reminder)
        } returns reminder

        val result = service.update(
            reminderId = reminder.id,
            userId = userId,
            title = "Updated title"
        )

        assertEquals("Updated title", result.title)

        verify(exactly = 1) {
            repository.save(reminder)
        }
    }

    @Test
    fun `update should update reminder due time`() {
        val reminder = reminder(userId)

        val newDueAt = LocalDateTime.now().plusDays(2)

        every {
            repository.findByIdAndUserId(
                reminder.id,
                userId
            )
        } returns reminder

        every {
            repository.save(reminder)
        } returns reminder

        val result = service.update(
            reminderId = reminder.id,
            userId = userId,
            dueAt = newDueAt
        )

        assertEquals(newDueAt, result.dueAt)

        verify(exactly = 1) {
            repository.save(reminder)
        }
    }

    @Test
    fun `update should support partial update`() {
        val reminder = reminder(userId).apply {
            title = "Original title"
            description = "Original description"
            recurring = false
        }

        every {
            repository.findByIdAndUserId(
                reminder.id,
                userId
            )
        } returns reminder

        every {
            repository.save(reminder)
        } returns reminder

        val result = service.update(
            reminderId = reminder.id,
            userId = userId,
            title = "New title"
        )

        assertEquals("New title", result.title)
        assertEquals(
            "Original description",
            result.description
        )
        assertFalse(result.recurring)

        verify(exactly = 1) {
            repository.save(reminder)
        }
    }

    @Test
    fun `update should reject blank title`() {
        val reminder = reminder(userId)

        every {
            repository.findByIdAndUserId(
                reminder.id,
                userId
            )
        } returns reminder

        assertFailsWith<IllegalArgumentException> {
            service.update(
                reminderId = reminder.id,
                userId = userId,
                title = "   "
            )
        }

        verify(exactly = 0) {
            repository.save(any())
        }
    }

    @Test
    fun `update should reject past due time`() {
        val reminder = reminder(userId)

        every {
            repository.findByIdAndUserId(
                reminder.id,
                userId
            )
        } returns reminder

        val pastDueAt = LocalDateTime.now().minusMinutes(1)

        assertFailsWith<IllegalArgumentException> {
            service.update(
                reminderId = reminder.id,
                userId = userId,
                dueAt = pastDueAt
            )
        }

        verify(exactly = 0) {
            repository.save(any())
        }
    }

    @Test
    fun `update should reject recurring reminder without recurrence rule`() {
        val reminder = reminder(userId)

        every {
            repository.findByIdAndUserId(
                reminder.id,
                userId
            )
        } returns reminder

        assertFailsWith<IllegalArgumentException> {
            service.update(
                reminderId = reminder.id,
                userId = userId,
                recurring = true
            )
        }

        verify(exactly = 0) {
            repository.save(any())
        }
    }

    // ---------------------------------------------------------
    // COMPLETE
    // ---------------------------------------------------------

    @Test
    fun `complete should mark reminder completed`() {
        val reminder = reminder(userId)

        every {
            repository.findByIdAndUserId(
                reminder.id,
                userId
            )
        } returns reminder

        every {
            repository.save(reminder)
        } returns reminder

        val result = service.complete(
            reminder.id,
            userId
        )

        assertEquals(
            ReminderStatus.COMPLETED,
            result.status
        )
        assertNotNull(result.completedAt)

        verify(exactly = 1) {
            repository.save(reminder)
        }
    }

    // ---------------------------------------------------------
    // SNOOZE
    // ---------------------------------------------------------

    @Test
    fun `snooze should set future snooze time`() {
        val reminder = reminder(userId)
        val snoozedUntil = LocalDateTime.now().plusHours(2)

        every {
            repository.findByIdAndUserId(
                reminder.id,
                userId
            )
        } returns reminder

        every {
            repository.save(reminder)
        } returns reminder

        val result = service.snooze(
            reminder.id,
            userId,
            snoozedUntil
        )

        assertEquals(
            snoozedUntil,
            result.snoozedUntil
        )

        verify(exactly = 1) {
            repository.save(reminder)
        }
    }

    @Test
    fun `snooze should reject past time`() {
        val reminder = reminder(userId)
        val snoozedUntil = LocalDateTime.now().minusMinutes(1)

        assertFailsWith<IllegalArgumentException> {
            service.snooze(
                reminder.id,
                userId,
                snoozedUntil
            )
        }

        verify(exactly = 0) {
            repository.findByIdAndUserId(any(), any())
        }

        verify(exactly = 0) {
            repository.save(any())
        }
    }

    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------

    @Test
    fun `delete should delete user's reminder`() {
        val reminder = reminder(userId)

        every {
            repository.findByIdAndUserId(
                reminder.id,
                userId
            )
        } returns reminder

        every {
            repository.delete(reminder)
        } returns Unit

        service.delete(
            reminder.id,
            userId
        )

        verify(exactly = 1) {
            repository.delete(reminder)
        }
    }

    @Test
    fun `delete should reject another user's reminder`() {
        val reminder = reminder(otherUserId)

        every {
            repository.findByIdAndUserId(
                reminder.id,
                userId
            )
        } returns null

        assertFailsWith<IllegalArgumentException> {
            service.delete(
                reminder.id,
                userId
            )
        }

        verify(exactly = 0) {
            repository.delete(any())
        }
    }

    // ---------------------------------------------------------
    // OVERDUE
    // ---------------------------------------------------------

    @Test
    fun `markOverdue should mark due upcoming reminders as overdue`() {
        val reminder1 = reminder(userId).apply {
            status = ReminderStatus.UPCOMING
            dueAt = LocalDateTime.now().minusHours(1)
        }

        val reminder2 = reminder(userId).apply {
            status = ReminderStatus.UPCOMING
            dueAt = LocalDateTime.now().minusMinutes(10)
        }

        every {
            repository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                any()
            )
        } returns listOf(reminder1, reminder2)

        every {
            repository.saveAll(listOf(reminder1, reminder2))
        } returns listOf(reminder1, reminder2)

        val result = service.markOverdue(
            LocalDateTime.now()
        )

        assertEquals(2, result)

        assertEquals(
            ReminderStatus.OVERDUE,
            reminder1.status
        )

        assertEquals(
            ReminderStatus.OVERDUE,
            reminder2.status
        )

        verify(exactly = 1) {
            repository.saveAll(
                listOf(reminder1, reminder2)
            )
        }
    }

    @Test
    fun `markOverdue should return zero when no reminders are due`() {
        every {
            repository.findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                any()
            )
        } returns emptyList()

        val result = service.markOverdue(
            LocalDateTime.now()
        )

        assertEquals(0, result)

        verify(exactly = 0) {
            repository.saveAll(any<Iterable<Reminder>>())
        }
    }

    // ---------------------------------------------------------
    // HELPER
    // ---------------------------------------------------------

    private fun reminder(
        userId: UUID
    ): Reminder {
        return Reminder(
            id = reminderId,
            userId = userId,
            tripId = tripId,
            title = "Test reminder",
            description = "Test description",
            dueAt = LocalDateTime.now().plusDays(1)
        )
    }
}