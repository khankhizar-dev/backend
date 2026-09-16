package com.trippoint.backend.notification.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(
    name = "notification_preferences",
    indexes = [
        Index(
            name = "idx_notification_preferences_user",
            columnList = "user_id"
        )
    ]
)
class NotificationPreference(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false, unique = true)
    var userId: UUID,

    @Column(name = "push_enabled", nullable = false)
    var pushEnabled: Boolean = true,

    @Column(name = "email_enabled", nullable = false)
    var emailEnabled: Boolean = true,

    @Column(name = "in_app_enabled", nullable = false)
    var inAppEnabled: Boolean = true,

    @Column(name = "sms_enabled", nullable = false)
    var smsEnabled: Boolean = false,

    @Column(name = "digest_enabled", nullable = false)
    var digestEnabled: Boolean = false,

    @Column(name = "quiet_hours_enabled", nullable = false)
    var quietHoursEnabled: Boolean = false,

    @Column(name = "quiet_hours_start")
    var quietHoursStart: LocalTime? = null,

    @Column(name = "quiet_hours_end")
    var quietHoursEnd: LocalTime? = null,

    @Column(nullable = false, length = 50)
    var timezone: String = "Asia/Kolkata",

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @jakarta.persistence.PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}