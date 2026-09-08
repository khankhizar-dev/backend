package com.trippoint.backend.checklist.entity

import com.trippoint.backend.checklist.model.ChecklistStatus
import com.trippoint.backend.checklist.model.ChecklistTemplateType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "checklist_templates",
    indexes = [
        Index(
            name = "idx_checklist_templates_created_by",
            columnList = "created_by"
        ),
        Index(
            name = "idx_checklist_templates_type",
            columnList = "type"
        ),
        Index(
            name = "idx_checklist_templates_status",
            columnList = "status"
        ),
        Index(
            name = "idx_checklist_templates_type_status",
            columnList = "type,status"
        )
    ]
)
class ChecklistTemplate(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "created_by")
    var createdBy: UUID? = null,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(length = 1000)
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: ChecklistTemplateType,

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