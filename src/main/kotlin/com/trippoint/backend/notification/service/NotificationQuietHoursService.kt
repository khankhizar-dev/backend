package com.trippoint.backend.notification.service

import com.trippoint.backend.notification.entity.NotificationPreference
import org.springframework.stereotype.Service
import java.time.LocalTime

@Service
class NotificationQuietHoursService {

    fun isQuietHours(
        preference: NotificationPreference,
        currentTime: LocalTime
    ): Boolean {

        if (!preference.quietHoursEnabled) {
            return false
        }

        val start = preference.quietHoursStart
            ?: return false

        val end = preference.quietHoursEnd
            ?: return false

        return if (start == end) {
            true
        } else if (start.isBefore(end)) {
            currentTime >= start && currentTime < end
        } else {
            // Overnight window, e.g. 22:00 -> 07:00
            currentTime >= start || currentTime < end
        }
    }
}