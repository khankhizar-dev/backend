package com.trippoint.backend.notification.event

import com.trippoint.backend.notification.model.NotificationCategory
import com.trippoint.backend.notification.model.NotificationType
import java.util.UUID

data class NotificationEvent(
    val recipientUserId: UUID,
    val actorUserId: UUID? = null,
    val tripId: UUID? = null,
    val category: NotificationCategory,
    val type: NotificationType,
    val title: String,
    val message: String,
    val targetType: String? = null,
    val targetId: UUID? = null,
    val targetName: String? = null
)