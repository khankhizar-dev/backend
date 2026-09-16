package com.trippoint.backend.notification.entity

import com.trippoint.backend.notification.model.ReminderStatus
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
    name = "reminders",
    indexes = [
        Index(
            name = "idx_reminders_user_due",
            columnList = "user_id,due_at"
        ),
        Index(
            name = "idx_reminders_user_status",
            columnList = "user_id,status,due_at"
        ),
        Index(
            name = "idx_reminders_trip",
            columnList = "trip_id,due_at"
        )
    ]
)
class Reminder(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "trip_id")
    var tripId: UUID? = null,

    @Column(nullable = false, length = 255)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "due_at", nullable = false)
    var dueAt: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ReminderStatus = ReminderStatus.UPCOMING,

    @Column(name = "snoozed_until")
    var snoozedUntil: LocalDateTime? = null,

    @Column(nullable = false)
    var recurring: Boolean = false,

    @Column(name = "recurrence_rule", length = 255)
    var recurrenceRule: String? = null,

    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null,

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