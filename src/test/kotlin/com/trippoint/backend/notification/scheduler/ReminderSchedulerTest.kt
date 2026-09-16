package com.trippoint.backend.notification.scheduler

import com.trippoint.backend.notification.service.ReminderProcessingService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ReminderSchedulerTest {

    private lateinit var reminderProcessingService: ReminderProcessingService
    private lateinit var scheduler: ReminderScheduler

    @BeforeEach
    fun setUp() {
        reminderProcessingService = mockk()

        scheduler = ReminderScheduler(
            reminderProcessingService
        )
    }

    @Test
    fun `processReminders should invoke reminder processing service`() {
        every {
            reminderProcessingService.processOverdueReminders(any())
        } returns 1

        scheduler.processReminders()

        verify(exactly = 1) {
            reminderProcessingService.processOverdueReminders(any())
        }
    }

    @Test
    fun `processReminders should not fail when there are no overdue reminders`() {
        every {
            reminderProcessingService.processOverdueReminders(any())
        } returns 0

        scheduler.processReminders()

        verify(exactly = 1) {
            reminderProcessingService.processOverdueReminders(any())
        }
    }

    @Test
    fun `processReminders should invoke processing service when reminders are processed`() {
        every {
            reminderProcessingService.processOverdueReminders(any())
        } returns 5

        scheduler.processReminders()

        verify(exactly = 1) {
            reminderProcessingService.processOverdueReminders(any())
        }
    }
}