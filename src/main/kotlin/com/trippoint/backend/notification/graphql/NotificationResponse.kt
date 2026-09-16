package com.trippoint.backend.notification.graphql

import com.trippoint.backend.notification.entity.Notification
import java.time.LocalDateTime
import java.util.UUID

data class NotificationResponse(
    val id: UUID,
    val recipientUserId: UUID,
    val actorUserId: UUID?,
    val tripId: UUID?,
    val category: String,
    val type: String,
    val title: String,
    val message: String,
    val targetType: String?,
    val targetId: UUID?,
    val targetName: String?,
    val isRead: Boolean,
    val isArchived: Boolean,
    val isDeleted: Boolean,
    val snoozedUntil: LocalDateTime?,
    val createdAt: LocalDateTime,
    val readAt: LocalDateTime?,
    val archivedAt: LocalDateTime?,
    val deletedAt: LocalDateTime?
) {
    companion object {
        fun from(notification: Notification): NotificationResponse {
            return NotificationResponse(
                id = notification.id,
                recipientUserId = notification.recipientUserId,
                actorUserId = notification.actorUserId,
                tripId = notification.tripId,
                category = notification.category.name,
                type = notification.type.name,
                title = notification.title,
                message = notification.message,
                targetType = notification.targetType,
                targetId = notification.targetId,
                targetName = notification.targetName,
                isRead = notification.isRead,
                isArchived = notification.isArchived,
                isDeleted = notification.isDeleted,
                snoozedUntil = notification.snoozedUntil,
                createdAt = notification.createdAt,
                readAt = notification.readAt,
                archivedAt = notification.archivedAt,
                deletedAt = notification.deletedAt
            )
        }
    }
}