package com.trippoint.backend.budget.graphql

import com.trippoint.backend.budget.entity.Budget
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class BudgetResponse(
    val id: UUID,
    val tripId: UUID,
    val totalAmount: BigDecimal,
    val currency: String,
    val locked: Boolean,
    val createdBy: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(budget: Budget): BudgetResponse {
            return BudgetResponse(
                id = budget.id,
                tripId = budget.tripId,
                totalAmount = budget.totalAmount,
                currency = budget.currency,
                locked = budget.locked,
                createdBy = budget.createdBy,
                createdAt = budget.createdAt,
                updatedAt = budget.updatedAt
            )
        }
    }
}