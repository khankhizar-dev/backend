package com.trippoint.backend.budget.graphql.input

import java.math.BigDecimal

data class UpdateBudgetInput(
    val totalAmount: BigDecimal? = null,
    val currency: String? = null
)
