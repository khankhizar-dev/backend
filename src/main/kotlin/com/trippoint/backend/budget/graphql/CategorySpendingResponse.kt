package com.trippoint.backend.budget.graphql

import com.trippoint.backend.budget.model.ExpenseCategory
import java.math.BigDecimal

data class CategorySpendingResponse(
    val category: ExpenseCategory,
    val amount: BigDecimal
)