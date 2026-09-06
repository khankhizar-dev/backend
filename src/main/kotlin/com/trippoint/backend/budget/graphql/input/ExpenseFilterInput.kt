package com.trippoint.backend.budget.graphql.input

import com.trippoint.backend.budget.model.ExpenseCategory
import java.math.BigDecimal

data class ExpenseFilterInput(
    val search: String? = null,
    val category: ExpenseCategory? = null,
    val minAmount: BigDecimal? = null,
    val maxAmount: BigDecimal? = null,
    val fromDate: String? = null,
    val toDate: String? = null,
    val includeArchived: Boolean = false
)
