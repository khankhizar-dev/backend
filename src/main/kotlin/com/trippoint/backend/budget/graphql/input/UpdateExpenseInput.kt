package com.trippoint.backend.budget.graphql.input

import com.trippoint.backend.budget.model.ExpenseCategory
import com.trippoint.backend.budget.model.PaymentMethod
import java.math.BigDecimal

data class UpdateExpenseInput(
    val category: ExpenseCategory? = null,
    val title: String? = null,
    val description: String? = null,
    val amount: BigDecimal? = null,
    val currency: String? = null,
    val expenseDate: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val paidBy: String? = null,
    val bookingId: String? = null,
    val recurring: Boolean? = null,
    val recurrenceRule: String? = null
)
