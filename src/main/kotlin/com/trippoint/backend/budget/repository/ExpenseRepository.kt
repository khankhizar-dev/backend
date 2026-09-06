package com.trippoint.backend.budget.repository

import com.trippoint.backend.budget.entity.Expense
import com.trippoint.backend.budget.model.ExpenseCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

interface ExpenseRepository : JpaRepository<Expense, UUID> {

    fun findAllByTripIdAndArchivedFalseOrderByExpenseDateDesc(
        tripId: UUID
    ): List<Expense>

    fun findAllByTripIdAndArchivedTrueOrderByExpenseDateDesc(
        tripId: UUID
    ): List<Expense>

    fun findByIdAndTripId(
        id: UUID,
        tripId: UUID
    ): Expense?

    fun findAllByTripIdAndCategoryAndArchivedFalseOrderByExpenseDateDesc(
        tripId: UUID,
        category: ExpenseCategory
    ): List<Expense>

    fun findAllByTripIdAndExpenseDateBetweenAndArchivedFalseOrderByExpenseDateDesc(
        tripId: UUID,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<Expense>

    fun findAllByTripIdAndBookingIdAndArchivedFalse(
        tripId: UUID,
        bookingId: UUID
    ): List<Expense>

    fun findAllByBudgetIdAndArchivedFalseOrderByExpenseDateDesc(
        budgetId: UUID
    ): List<Expense>

    @Query(
        """
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.tripId = :tripId
          AND e.archived = false
    """
    )
    fun sumAmountByTripId(
        @Param("tripId") tripId: UUID
    ): BigDecimal

    @Query(
        """
        SELECT COALESCE(SUM(e.convertedAmount), 0)
        FROM Expense e
        WHERE e.tripId = :tripId
          AND e.archived = false
          AND e.convertedAmount IS NOT NULL
    """
    )
    fun sumConvertedAmountByTripId(
        @Param("tripId") tripId: UUID
    ): BigDecimal

    @Query(
        """
        SELECT e.category, COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.tripId = :tripId
          AND e.archived = false
        GROUP BY e.category
        ORDER BY e.category
    """
    )
    fun sumAmountByCategory(
        @Param("tripId") tripId: UUID
    ): List<Array<Any>>

    @Query(
        """
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.tripId = :tripId
          AND e.category = :category
          AND e.archived = false
    """
    )
    fun sumAmountByTripIdAndCategory(
        @Param("tripId") tripId: UUID,
        @Param("category") category: ExpenseCategory
    ): BigDecimal

    @Query(
        """
        SELECT COUNT(e)
        FROM Expense e
        WHERE e.tripId = :tripId
          AND e.archived = false
    """
    )
    fun countActiveExpenses(
        @Param("tripId") tripId: UUID
    ): Long
}