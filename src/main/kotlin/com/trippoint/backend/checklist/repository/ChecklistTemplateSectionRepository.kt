package com.trippoint.backend.checklist.repository

import com.trippoint.backend.checklist.entity.ChecklistTemplateSection
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ChecklistTemplateSectionRepository :
    JpaRepository<ChecklistTemplateSection, UUID> {

    fun findAllByTemplateIdOrderByPositionAsc(
        templateId: UUID
    ): List<ChecklistTemplateSection>

    fun findByIdAndTemplateId(
        id: UUID,
        templateId: UUID
    ): ChecklistTemplateSection?

    fun findFirstByTemplateIdOrderByPositionDesc(
        templateId: UUID
    ): ChecklistTemplateSection?

    fun deleteAllByTemplateId(
        templateId: UUID
    )
}