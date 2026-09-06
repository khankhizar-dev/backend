package com.trippoint.backend.budget.model

import java.math.BigDecimal
import java.time.LocalDate

data class DailyExpenseReport(
    val date: LocalDate,
    val amount: BigDecimal,
    val expenseCount: Long
)
