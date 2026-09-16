package com.trippoint.backend.notification.service

import com.trippoint.backend.notification.entity.NotificationPreference
import com.trippoint.backend.notification.repository.NotificationPreferenceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DateTimeException
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

@Service
class NotificationPreferenceService(
    private val repository: NotificationPreferenceRepository
) {

    @Transactional
    fun getOrCreate(userId: UUID): NotificationPreference {
        return repository.findByUserId(userId)
            ?: repository.save(
                NotificationPreference(
                    userId = userId
                )
            )
    }

    @Transactional
    fun update(
        userId: UUID,
        pushEnabled: Boolean? = null,
        emailEnabled: Boolean? = null,
        inAppEnabled: Boolean? = null,
        smsEnabled: Boolean? = null,
        digestEnabled: Boolean? = null,
        quietHoursEnabled: Boolean? = null,
        quietHoursStart: LocalTime? = null,
        quietHoursEnd: LocalTime? = null,
        timezone: String? = null
    ): NotificationPreference {

        val preference = getOrCreate(userId)

        pushEnabled?.let { preference.pushEnabled = it }
        emailEnabled?.let { preference.emailEnabled = it }
        inAppEnabled?.let { preference.inAppEnabled = it }
        smsEnabled?.let { preference.smsEnabled = it }
        digestEnabled?.let { preference.digestEnabled = it }
        quietHoursEnabled?.let { preference.quietHoursEnabled = it }
        quietHoursStart?.let { preference.quietHoursStart = it }
        quietHoursEnd?.let { preference.quietHoursEnd = it }
        timezone?.let {
            try {
                ZoneId.of(it)
            } catch (ex: DateTimeException) {
                throw IllegalArgumentException("Invalid timezone: $it")
            }

            preference.timezone = it
        }

        return repository.save(preference)
    }
}