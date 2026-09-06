package com.trippoint.backend.budget.graphql

import com.trippoint.backend.budget.entity.Expense
import com.trippoint.backend.budget.model.ExpenseCategory
import com.trippoint.backend.budget.model.PaymentMethod
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class ExpenseResponse(
    val id: UUID,
    val tripId: UUID,
    val budgetId: UUID,
    val bookingId: UUID?,
    val category: ExpenseCategory,
    val title: String,
    val description: String?,
    val amount: BigDecimal,
    val currency: String,
    val exchangeRate: BigDecimal?,
    val convertedAmount: BigDecimal?,
    val expenseDate: LocalDateTime,
    val paymentMethod: PaymentMethod?,
    val paidBy: UUID,
    val createdBy: UUID,
    val recurring: Boolean,
    val recurrenceRule: String?,
    val archived: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(expense: Expense): ExpenseResponse {
            return ExpenseResponse(
                id = expense.id,
                tripId = expense.tripId,
                budgetId = expense.budgetId,
                bookingId = expense.bookingId,
                category = expense.category,
                title = expense.title,
                description = expense.description,
                amount = expense.amount,
                currency = expense.currency,
                exchangeRate = expense.exchangeRate,
                convertedAmount = expense.convertedAmount,
                expenseDate = expense.expenseDate,
                paymentMethod = expense.paymentMethod,
                paidBy = expense.paidBy,
                createdBy = expense.createdBy,
                recurring = expense.recurring,
                recurrenceRule = expense.recurrenceRule,
                archived = expense.archived,
                createdAt = expense.createdAt,
                updatedAt = expense.updatedAt
            )
        }
    }
}