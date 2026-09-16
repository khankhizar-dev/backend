package com.trippoint.backend.notification.entity

import com.trippoint.backend.notification.model.NotificationCategory
import com.trippoint.backend.notification.model.NotificationType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "notifications",
    indexes = [
        Index(
            name = "idx_notifications_recipient_created",
            columnList = "recipient_user_id,created_at"
        ),
        Index(
            name = "idx_notifications_recipient_unread",
            columnList = "recipient_user_id,is_read,created_at"
        ),
        Index(
            name = "idx_notifications_recipient_archived",
            columnList = "recipient_user_id,is_archived,created_at"
        ),
        Index(
            name = "idx_notifications_trip",
            columnList = "trip_id,created_at"
        ),
        Index(
            name = "idx_notifications_target",
            columnList = "target_type,target_id"
        )
    ]
)
class Notification(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "recipient_user_id", nullable = false)
    var recipientUserId: UUID,

    @Column(name = "actor_user_id")
    var actorUserId: UUID? = null,

    @Column(name = "trip_id")
    var tripId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var category: NotificationCategory,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var type: NotificationType,

    @Column(nullable = false, length = 255)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var message: String,

    @Column(name = "target_type", length = 30)
    var targetType: String? = null,

    @Column(name = "target_id")
    var targetId: UUID? = null,

    @Column(name = "target_name", length = 500)
    var targetName: String? = null,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "is_archived", nullable = false)
    var isArchived: Boolean = false,

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

    @Column(name = "snoozed_until")
    var snoozedUntil: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "read_at")
    var readAt: LocalDateTime? = null,

    @Column(name = "archived_at")
    var archivedAt: LocalDateTime? = null,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
)