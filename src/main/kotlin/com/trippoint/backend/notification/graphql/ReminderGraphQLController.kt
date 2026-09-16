package com.trippoint.backend.notification.graphql

import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.notification.entity.Reminder
import com.trippoint.backend.notification.model.ReminderStatus
import com.trippoint.backend.notification.service.ReminderService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDateTime
import java.util.UUID

@Controller
class ReminderGraphQLController(
    private val reminderService: ReminderService
) {

    @QueryMapping
    fun reminders(
        @Argument limit: Int?,
        @Argument status: ReminderStatus?,
        @Argument tripId: UUID?
    ): List<Reminder> {

        return reminderService.getReminders(
            userId = currentUserId(),
            limit = limit ?: 20,
            status = status,
            tripId = tripId
        )
    }

    @QueryMapping
    fun reminder(
        @Argument id: UUID
    ): Reminder {
        return reminderService.getReminder(
            reminderId = id,
            userId = currentUserId()
        )
    }

    @MutationMapping
    fun createReminder(
        @Argument input: CreateReminderInput
    ): Reminder {

        return reminderService.create(
            userId = currentUserId(),
            tripId = input.tripId,
            title = input.title,
            description = input.description,
            dueAt = LocalDateTime.parse(input.dueAt),
            recurring = input.recurring ?: false,
            recurrenceRule = input.recurrenceRule
        )
    }

    @MutationMapping
    fun updateReminder(
        @Argument id: UUID,
        @Argument input: UpdateReminderInput
    ): Reminder {

        return reminderService.update(
            reminderId = id,
            userId = currentUserId(),
            title = input.title,
            description = input.description,
            dueAt = input.dueAt?.let(LocalDateTime::parse),
            recurring = input.recurring,
            recurrenceRule = input.recurrenceRule
        )
    }

    @MutationMapping
    fun completeReminder(
        @Argument id: UUID
    ): Reminder {
        return reminderService.complete(
            reminderId = id,
            userId = currentUserId()
        )
    }

    @MutationMapping
    fun snoozeReminder(
        @Argument id: UUID,
        @Argument snoozedUntil: String
    ): Reminder {
        return reminderService.snooze(
            reminderId = id,
            userId = currentUserId(),
            snoozedUntil = LocalDateTime.parse(snoozedUntil)
        )
    }

    @MutationMapping
    fun deleteReminder(
        @Argument id: UUID
    ): Boolean {

        reminderService.delete(
            reminderId = id,
            userId = currentUserId()
        )

        return true
    }

    private fun currentUserId(): UUID {
        val authentication =
            SecurityContextHolder
                .getContext()
                .authentication
                ?: throw IllegalStateException("Unauthenticated")

        return UUID.fromString(authentication.name)
    }
}

data class CreateReminderInput(
    val tripId: UUID? = null,
    val title: String,
    val description: String? = null,
    val dueAt: String,
    val recurring: Boolean? = null,
    val recurrenceRule: String? = null
)

data class UpdateReminderInput(
    val title: String? = null,
    val description: String? = null,
    val dueAt: String? = null,
    val recurring: Boolean? = null,
    val recurrenceRule: String? = null
)