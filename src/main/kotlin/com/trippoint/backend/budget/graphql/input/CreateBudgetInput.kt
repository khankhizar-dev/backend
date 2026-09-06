package com.trippoint.backend.budget.graphql.input

import java.math.BigDecimal


data class CreateBudgetInput(
    val totalAmount: BigDecimal,
    val currency: String
)
