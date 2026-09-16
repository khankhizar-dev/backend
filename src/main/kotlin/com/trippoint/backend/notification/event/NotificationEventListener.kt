package com.trippoint.backend.notification.event

import com.trippoint.backend.notification.service.NotificationPreferenceService
import com.trippoint.backend.notification.service.NotificationQuietHoursService
import com.trippoint.backend.notification.service.NotificationService
import com.trippoint.backend.notification.subscription.NotificationSubscriptionService
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.DateTimeException
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
class NotificationEventListener(
    private val notificationService: NotificationService,
    private val notificationPreferenceService: NotificationPreferenceService,
    private val notificationSubscriptionService: NotificationSubscriptionService,
    private val notificationQuietHoursService: NotificationQuietHoursService
) {

    @EventListener
    fun handle(event: NotificationEvent) {

        val preference =
            notificationPreferenceService.getOrCreate(
                event.recipientUserId
            )

        if (!preference.inAppEnabled) {
            return
        }

        val zoneId = try {
            ZoneId.of(preference.timezone)
        } catch (ex: DateTimeException) {
            ZoneId.of("Asia/Kolkata")
        }

        val currentTime = ZonedDateTime
            .now(zoneId)
            .toLocalTime()

        if (
            notificationQuietHoursService.isQuietHours(
                preference,
                currentTime
            )
        ) {
            return
        }

        val notification = notificationService.create(
            recipientUserId = event.recipientUserId,
            actorUserId = event.actorUserId,
            tripId = event.tripId,
            category = event.category,
            type = event.type,
            title = event.title,
            message = event.message,
            targetType = event.targetType,
            targetId = event.targetId,
            targetName = event.targetName
        )

        notificationSubscriptionService.publish(notification)
    }
}