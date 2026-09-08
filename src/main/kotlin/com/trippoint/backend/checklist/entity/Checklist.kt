package com.trippoint.backend.checklist.entity

import com.trippoint.backend.checklist.model.ChecklistStatus
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "checklists",
    indexes = [
        Index(
            name = "idx_checklists_trip_id",
            columnList = "trip_id"
        ),
        Index(
            name = "idx_checklists_created_by",
            columnList = "created_by"
        ),
        Index(
            name = "idx_checklists_trip_status",
            columnList = "trip_id,status"
        )
    ]
)
class Checklist(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "trip_id", nullable = false)
    var tripId: UUID,

    @Column(name = "created_by", nullable = false)
    var createdBy: UUID,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(length = 1000)
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ChecklistStatus = ChecklistStatus.ACTIVE,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}