package com.trippoint.backend.budget.repository

import com.trippoint.backend.budget.entity.Budget
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BudgetRepository : JpaRepository<Budget, UUID> {

    fun findByTripId(tripId: UUID): Budget?

    fun existsByTripId(tripId: UUID): Boolean

    fun findByTripIdAndCreatedBy(
        tripId: UUID,
        createdBy: UUID
    ): Budget?
}