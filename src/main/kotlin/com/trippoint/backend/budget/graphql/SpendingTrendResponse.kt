package com.trippoint.backend.budget.graphql

import java.math.BigDecimal
import java.time.LocalDate

data class SpendingTrendResponse(
    val date: LocalDate,
    val amount: BigDecimal
)