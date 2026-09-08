package com.trippoint.backend.checklist.repository

import com.trippoint.backend.checklist.entity.Checklist
import com.trippoint.backend.checklist.model.ChecklistStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ChecklistRepository : JpaRepository<Checklist, UUID> {

    fun findAllByTripIdOrderByCreatedAtDesc(
        tripId: UUID
    ): List<Checklist>

    fun findAllByTripIdAndStatusOrderByCreatedAtDesc(
        tripId: UUID,
        status: ChecklistStatus
    ): List<Checklist>

    fun findByIdAndTripId(
        id: UUID,
        tripId: UUID
    ): Checklist?

    fun findByIdAndTripIdAndCreatedBy(
        id: UUID,
        tripId: UUID,
        createdBy: UUID
    ): Checklist?
}