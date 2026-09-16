package com.trippoint.backend.activity.entity

import com.trippoint.backend.activity.model.ActivityTarget
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
    name = "activity_logs",
    indexes = [
        Index(
            name = "idx_activity_logs_trip_created",
            columnList = "trip_id,created_at"
        ),
        Index(
            name = "idx_activity_logs_trip_target",
            columnList = "trip_id,target_type"
        )
    ]
)
class ActivityLog(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "trip_id", nullable = false)
    var tripId: UUID,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(nullable = false, length = 255)
    var action: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    var targetType: ActivityTarget,

    @Column(name = "target_name", nullable = false, length = 500)
    var targetName: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)