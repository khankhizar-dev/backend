package com.trippoint.backend.budget.graphql

import java.math.BigDecimal
import java.util.UUID

data class BudgetSummaryResponse(
    val budget: BudgetResponse,
    val spentAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val expenseCount: Long,
    val categoryBreakdown: List<CategorySpendingResponse>
)