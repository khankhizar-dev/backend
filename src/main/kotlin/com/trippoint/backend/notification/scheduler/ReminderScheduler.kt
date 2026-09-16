package com.trippoint.backend.notification.scheduler

import com.trippoint.backend.notification.service.ReminderProcessingService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ReminderScheduler(
    private val reminderProcessingService: ReminderProcessingService
) {

    @Scheduled(fixedDelay = 60_000)
    fun processReminders() {
        reminderProcessingService.processOverdueReminders(
            LocalDateTime.now()
        )
    }
}