package com.trippoint.backend.budget.graphql.input

import com.trippoint.backend.budget.model.ExpenseCategory
import com.trippoint.backend.budget.model.PaymentMethod
import java.math.BigDecimal

data class CreateExpenseInput(
    val bookingId: String? = null,
    val category: ExpenseCategory,
    val title: String,
    val description: String? = null,
    val amount: BigDecimal,
    val currency: String,
    val expenseDate: String,
    val paymentMethod: PaymentMethod? = null,
    val paidBy: String,
    val recurring: Boolean = false,
    val recurrenceRule: String? = null
)
