package com.trippoint.backend.notification.graphql

import com.trippoint.backend.notification.model.NotificationCategory
import java.time.LocalDateTime
import java.util.UUID

data class NotificationFilterInput(
    val category: NotificationCategory? = null
)

data class SnoozeNotificationInput(
    val notificationId: UUID,
    val snoozedUntil: LocalDateTime
)