package com.trippoint.backend.checklist.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "checklist_template_sections",
    indexes = [
        Index(
            name = "idx_checklist_template_sections_template_id",
            columnList = "template_id"
        ),
        Index(
            name = "idx_checklist_template_sections_template_position",
            columnList = "template_id,position"
        )
    ]
)
class ChecklistTemplateSection(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "template_id", nullable = false)
    var templateId: UUID,

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