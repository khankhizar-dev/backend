package com.trippoint.backend.budget.entity

import com.trippoint.backend.budget.model.ExpenseCategory
import com.trippoint.backend.budget.model.PaymentMethod
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "expenses",
    indexes = [
        Index(
            name = "idx_expenses_trip_id",
            columnList = "trip_id"
        ),
        Index(
            name = "idx_expenses_budget_id",
            columnList = "budget_id"
        ),
        Index(
            name = "idx_expenses_booking_id",
            columnList = "booking_id"
        ),
        Index(
            name = "idx_expenses_category",
            columnList = "category"
        ),
        Index(
            name = "idx_expenses_paid_by",
            columnList = "paid_by"
        ),
        Index(
            name = "idx_expenses_expense_date",
            columnList = "expense_date"
        ),
        Index(
            name = "idx_expenses_trip_category",
            columnList = "trip_id,category"
        ),
        Index(
            name = "idx_expenses_trip_date",
            columnList = "trip_id,expense_date"
        ),
        Index(
            name = "idx_expenses_trip_archived",
            columnList = "trip_id,archived"
        )
    ]
)
class Expense(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "trip_id", nullable = false)
    var tripId: UUID,

    @Column(name = "budget_id", nullable = false)
    var budgetId: UUID,

    @Column(name = "booking_id")
    var bookingId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var category: ExpenseCategory,

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(length = 500)
    var description: String? = null,

    @Column(
        nullable = false,
        precision = 14,
        scale = 2
    )
    var amount: BigDecimal,

    @Column(nullable = false, length = 3)
    var currency: String,

    @Column(
        name = "exchange_rate",
        precision = 18,
        scale = 8
    )
    var exchangeRate: BigDecimal? = null,

    @Column(
        name = "converted_amount",
        precision = 14,
        scale = 2
    )
    var convertedAmount: BigDecimal? = null,

    @Column(name = "expense_date", nullable = false)
    var expenseDate: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    var paymentMethod: PaymentMethod? = null,

    @Column(name = "paid_by", nullable = false)
    var paidBy: UUID,

    @Column(name = "created_by", nullable = false)
    var createdBy: UUID,

    @Column(nullable = false)
    var recurring: Boolean = false,

    @Column(name = "recurrence_rule", length = 100)
    var recurrenceRule: String? = null,

    @Column(nullable = false)
    var archived: Boolean = false,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}