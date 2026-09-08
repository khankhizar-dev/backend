package com.trippoint.backend.checklist.repository

import com.trippoint.backend.checklist.entity.ChecklistTemplateItem
import com.trippoint.backend.checklist.model.ChecklistItemCategory
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ChecklistTemplateItemRepository :
    JpaRepository<ChecklistTemplateItem, UUID> {

    fun findAllBySectionIdOrderByPositionAsc(
        sectionId: UUID
    ): List<ChecklistTemplateItem>

    fun findByIdAndSectionId(
        id: UUID,
        sectionId: UUID
    ): ChecklistTemplateItem?

    fun findFirstBySectionIdOrderByPositionDesc(
        sectionId: UUID
    ): ChecklistTemplateItem?

    fun countBySectionId(
        sectionId: UUID
    ): Long

    fun countBySectionIdAndCategory(
        sectionId: UUID,
        category: ChecklistItemCategory
    ): Long

    fun deleteAllBySectionId(
        sectionId: UUID
    )
}