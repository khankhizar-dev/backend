package com.trippoint.backend.checklist.repository

import com.trippoint.backend.checklist.entity.ChecklistSection
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ChecklistSectionRepository : JpaRepository<ChecklistSection, UUID> {

    fun findAllByChecklistIdOrderByPositionAsc(
        checklistId: UUID
    ): List<ChecklistSection>

    fun findByIdAndChecklistId(
        id: UUID,
        checklistId: UUID
    ): ChecklistSection?

    fun findFirstByChecklistIdOrderByPositionDesc(
        checklistId: UUID
    ): ChecklistSection?

    fun existsByIdAndChecklistId(
        id: UUID,
        checklistId: UUID
    ): Boolean

    fun deleteAllByChecklistId(
        checklistId: UUID
    )
}