package com.trippoint.backend.checklist.repository

import com.trippoint.backend.checklist.entity.ChecklistItem
import com.trippoint.backend.checklist.model.ChecklistItemCategory
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ChecklistItemRepository : JpaRepository<ChecklistItem, UUID> {

    fun findAllBySectionIdOrderByPositionAsc(
        sectionId: UUID
    ): List<ChecklistItem>

    fun findByIdAndSectionId(
        id: UUID,
        sectionId: UUID
    ): ChecklistItem?

    fun findFirstBySectionIdOrderByPositionDesc(
        sectionId: UUID
    ): ChecklistItem?

    fun countBySectionId(
        sectionId: UUID
    ): Long

    fun countBySectionIdAndCompleted(
        sectionId: UUID,
        completed: Boolean
    ): Long

    fun countBySectionIdAndCategory(
        sectionId: UUID,
        category: ChecklistItemCategory
    ): Long

    fun deleteAllBySectionId(
        sectionId: UUID
    )
}