package com.trippoint.backend.notification.service

import com.trippoint.backend.notification.entity.Reminder
import com.trippoint.backend.notification.event.NotificationEvent
import com.trippoint.backend.notification.event.NotificationEventPublisher
import com.trippoint.backend.notification.model.NotificationCategory
import com.trippoint.backend.notification.model.NotificationType
import com.trippoint.backend.notification.model.ReminderStatus
import com.trippoint.backend.notification.repository.ReminderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ReminderProcessingService(
    private val reminderRepository: ReminderRepository,
    private val notificationEventPublisher: NotificationEventPublisher
) {

    @Transactional
    fun processOverdueReminders(
        now: LocalDateTime = LocalDateTime.now()
    ): Int {

        val reminders = reminderRepository
            .findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )

        if (reminders.isEmpty()) {
            return 0
        }

        val processedReminders = mutableListOf<Reminder>()

        reminders.forEach { reminder ->

            // Do not process reminders that are still snoozed.
            if (
                reminder.snoozedUntil != null &&
                reminder.snoozedUntil!!.isAfter(now)
            ) {
                return@forEach
            }

            reminder.status = ReminderStatus.OVERDUE

            processedReminders.add(reminder)

            notificationEventPublisher.publish(
                NotificationEvent(
                    recipientUserId = reminder.userId,
                    actorUserId = null,
                    tripId = reminder.tripId,
                    category = NotificationCategory.REMINDER,
                    type = NotificationType.REMINDER_OVERDUE,
                    title = "Reminder overdue",
                    message = "Reminder \"${reminder.title}\" is overdue",
                    targetType = "REMINDER",
                    targetId = reminder.id,
                    targetName = reminder.title
                )
            )
        }

        if (processedReminders.isEmpty()) {
            return 0
        }

        reminderRepository.saveAll<Reminder>(processedReminders)

        return processedReminders.size
    }
}