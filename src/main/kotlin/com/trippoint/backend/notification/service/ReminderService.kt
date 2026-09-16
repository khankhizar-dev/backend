package com.trippoint.backend.notification.service

import com.trippoint.backend.notification.entity.Reminder
import com.trippoint.backend.notification.model.ReminderStatus
import com.trippoint.backend.notification.repository.ReminderRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class ReminderService(
    private val reminderRepository: ReminderRepository
) {

    @Transactional
    fun create(
        userId: UUID,
        tripId: UUID? = null,
        title: String,
        description: String? = null,
        dueAt: LocalDateTime,
        recurring: Boolean = false,
        recurrenceRule: String? = null
    ): Reminder {

        require(title.isNotBlank()) {
            "Reminder title cannot be blank"
        }

        require(dueAt.isAfter(LocalDateTime.now())) {
            "Reminder due time must be in the future"
        }

        if (recurring) {
            require(!recurrenceRule.isNullOrBlank()) {
                "Recurrence rule is required for recurring reminders"
            }
        }

        return reminderRepository.save(
            Reminder(
                userId = userId,
                tripId = tripId,
                title = title,
                description = description,
                dueAt = dueAt,
                status = ReminderStatus.UPCOMING,
                recurring = recurring,
                recurrenceRule = recurrenceRule
            )
        )
    }

    @Transactional(readOnly = true)
    fun getReminder(
        reminderId: UUID,
        userId: UUID
    ): Reminder {
        return reminderRepository.findByIdAndUserId(
            reminderId,
            userId
        ) ?: throw IllegalArgumentException("Reminder not found")
    }

    @Transactional(readOnly = true)
    fun getReminders(
        userId: UUID,
        limit: Int = 20,
        status: ReminderStatus? = null,
        tripId: UUID? = null
    ): List<Reminder> {

        val safeLimit = limit.coerceIn(1, 100)
        val pageable = PageRequest.of(0, safeLimit)

        return when {
            tripId != null -> {
                reminderRepository
                    .findAllByUserIdAndTripIdOrderByDueAtAscIdAsc(
                        userId,
                        tripId,
                        pageable
                    )
            }

            status != null -> {
                reminderRepository
                    .findAllByUserIdAndStatusOrderByDueAtAscIdAsc(
                        userId,
                        status,
                        pageable
                    )
            }

            else -> {
                reminderRepository
                    .findAllByUserIdOrderByDueAtAscIdAsc(
                        userId,
                        pageable
                    )
            }
        }
    }

    @Transactional
    fun update(
        reminderId: UUID,
        userId: UUID,
        title: String? = null,
        description: String? = null,
        dueAt: LocalDateTime? = null,
        recurring: Boolean? = null,
        recurrenceRule: String? = null
    ): Reminder {

        val reminder = getReminder(reminderId, userId)

        title?.let {
            require(it.isNotBlank()) {
                "Reminder title cannot be blank"
            }
            reminder.title = it
        }

        description?.let {
            reminder.description = it
        }

        dueAt?.let {
            require(it.isAfter(LocalDateTime.now())) {
                "Reminder due time must be in the future"
            }
            reminder.dueAt = it
        }

        recurring?.let {
            reminder.recurring = it
        }

        recurrenceRule?.let {
            reminder.recurrenceRule = it
        }

        if (reminder.recurring) {
            require(!reminder.recurrenceRule.isNullOrBlank()) {
                "Recurrence rule is required for recurring reminders"
            }
        }

        return reminderRepository.save(reminder)
    }

    @Transactional
    fun complete(
        reminderId: UUID,
        userId: UUID
    ): Reminder {

        val reminder = getReminder(reminderId, userId)

        reminder.status = ReminderStatus.COMPLETED
        reminder.completedAt = LocalDateTime.now()

        return reminderRepository.save(reminder)
    }

    @Transactional
    fun snooze(
        reminderId: UUID,
        userId: UUID,
        snoozedUntil: LocalDateTime
    ): Reminder {

        require(snoozedUntil.isAfter(LocalDateTime.now())) {
            "Snooze time must be in the future"
        }

        val reminder = getReminder(reminderId, userId)

        reminder.snoozedUntil = snoozedUntil

        return reminderRepository.save(reminder)
    }

    @Transactional
    fun delete(
        reminderId: UUID,
        userId: UUID
    ) {
        val reminder = getReminder(reminderId, userId)
        reminderRepository.delete(reminder)
    }

    @Transactional
    fun markOverdue(now: LocalDateTime): Int {

        val reminders = reminderRepository
            .findAllByStatusAndDueAtBefore(
                ReminderStatus.UPCOMING,
                now
            )

        if (reminders.isEmpty()) {
            return 0
        }

        reminders.forEach {
            it.status = ReminderStatus.OVERDUE
        }

        reminderRepository.saveAll(reminders)

        return reminders.size
    }
}