package com.trippoint.backend.checklist.repository

import com.trippoint.backend.checklist.entity.ChecklistTemplate
import com.trippoint.backend.checklist.model.ChecklistStatus
import com.trippoint.backend.checklist.model.ChecklistTemplateType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ChecklistTemplateRepository : JpaRepository<ChecklistTemplate, UUID> {

    fun findAllByStatusOrderByCreatedAtDesc(
        status: ChecklistStatus
    ): List<ChecklistTemplate>

    fun findAllByTypeAndStatusOrderByCreatedAtDesc(
        type: ChecklistTemplateType,
        status: ChecklistStatus
    ): List<ChecklistTemplate>

    fun findAllByCreatedByAndStatusOrderByCreatedAtDesc(
        createdBy: UUID,
        status: ChecklistStatus
    ): List<ChecklistTemplate>

    fun findByIdAndStatus(
        id: UUID,
        status: ChecklistStatus
    ): ChecklistTemplate?

    fun findByIdAndCreatedBy(
        id: UUID,
        createdBy: UUID
    ): ChecklistTemplate?
}