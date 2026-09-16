package com.trippoint.backend.notification.graphql

import com.trippoint.backend.notification.entity.NotificationPreference
import com.trippoint.backend.notification.service.NotificationPreferenceService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import java.time.LocalTime
import java.util.UUID

@Controller
class NotificationPreferenceGraphQLController(
    private val notificationPreferenceService: NotificationPreferenceService
) {

    @QueryMapping
    fun notificationPreferences(): NotificationPreference {
        return notificationPreferenceService.getOrCreate(currentUserId())
    }

    @MutationMapping
    fun updateNotificationPreferences(
        @Argument input: UpdateNotificationPreferencesInput
    ): NotificationPreference {

        return notificationPreferenceService.update(
            userId = currentUserId(),
            pushEnabled = input.pushEnabled,
            emailEnabled = input.emailEnabled,
            inAppEnabled = input.inAppEnabled,
            smsEnabled = input.smsEnabled,
            digestEnabled = input.digestEnabled,
            quietHoursEnabled = input.quietHoursEnabled,
            quietHoursStart = input.quietHoursStart?.let(LocalTime::parse),
            quietHoursEnd = input.quietHoursEnd?.let(LocalTime::parse)
        )
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

data class UpdateNotificationPreferencesInput(
    val pushEnabled: Boolean? = null,
    val emailEnabled: Boolean? = null,
    val inAppEnabled: Boolean? = null,
    val smsEnabled: Boolean? = null,
    val digestEnabled: Boolean? = null,
    val quietHoursEnabled: Boolean? = null,
    val quietHoursStart: String? = null,
    val quietHoursEnd: String? = null
)