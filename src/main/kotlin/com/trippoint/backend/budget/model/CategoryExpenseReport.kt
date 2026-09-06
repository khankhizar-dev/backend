package com.trippoint.backend.budget.model

import java.math.BigDecimal

data class CategoryExpenseReport(
    val category: ExpenseCategory,
    val amount: BigDecimal,
    val percentage: BigDecimal,
    val expenseCount: Long
)
