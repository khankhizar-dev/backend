package com.trippoint.backend.checklist.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "checklist_sections",
    indexes = [
        Index(
            name = "idx_checklist_sections_checklist_id",
            columnList = "checklist_id"
        ),
        Index(
            name = "idx_checklist_sections_checklist_position",
            columnList = "checklist_id,position"
        )
    ]
)
class ChecklistSection(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "checklist_id", nullable = false)
    var checklistId: UUID,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(nullable = false)
    var position: Int,

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