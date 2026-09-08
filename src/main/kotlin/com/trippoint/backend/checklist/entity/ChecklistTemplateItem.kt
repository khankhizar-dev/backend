package com.trippoint.backend.checklist.entity

import com.trippoint.backend.checklist.model.ChecklistItemCategory
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
    name = "checklist_template_items",
    indexes = [
        Index(
            name = "idx_checklist_template_items_section_id",
            columnList = "section_id"
        ),
        Index(
            name = "idx_checklist_template_items_section_position",
            columnList = "section_id,position"
        ),
        Index(
            name = "idx_checklist_template_items_section_category",
            columnList = "section_id,category"
        )
    ]
)
class ChecklistTemplateItem(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "section_id", nullable = false)
    var sectionId: UUID,

    @Column(nullable = false, length = 300)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var category: ChecklistItemCategory,

    @Column(nullable = false)
    var essential: Boolean = false,

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