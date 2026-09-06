package com.trippoint.backend.budget.entity

import com.trippoint.backend.budget.model.ExpenseCategory
import java.math.BigDecimal

data class BudgetOverview(
    val budget: BigDecimal,
    val spent: BigDecimal,
    val remaining: BigDecimal,
    val percentageUsed: BigDecimal,
    val expenseCount: Long,
    val categoryBreakdown: List<CategoryBreakdown>
)

data class CategoryBreakdown(
    val category: ExpenseCategory,
    val amount: BigDecimal,
    val percentage: BigDecimal
)
