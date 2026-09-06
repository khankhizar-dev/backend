package com.trippoint.backend.budget.service

import com.trippoint.backend.booking.repository.BookingRepository
import com.trippoint.backend.budget.entity.Expense
import com.trippoint.backend.budget.graphql.input.CreateExpenseInput
import com.trippoint.backend.budget.graphql.input.ExpenseFilterInput
import com.trippoint.backend.budget.graphql.input.UpdateExpenseInput
import com.trippoint.backend.budget.repository.BudgetRepository
import com.trippoint.backend.budget.repository.ExpenseRepository
import com.trippoint.backend.trip.model.TripMemberStatus
import com.trippoint.backend.trip.repository.TripMemberRepository
import com.trippoint.backend.trip.repository.TripRepository
import com.trippoint.backend.trip.service.TripAccessService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val tripRepository: TripRepository,
    private val tripMemberRepository: TripMemberRepository,
    private val bookingRepository: BookingRepository,
    private val tripAccessService: TripAccessService
) {

    @Transactional
    fun createExpense(
        userId: UUID,
        tripId: UUID,
        input: CreateExpenseInput
    ): Expense {

        tripAccessService.requireMemberAccess(tripId, userId)

        val budget = budgetRepository.findByTripId(tripId)
            ?: throw IllegalArgumentException("Budget not found for this trip")

        require(!budget.locked) {
            "Budget is locked"
        }

        validateAmount(input.amount)

        val paidBy = UUID.fromString(input.paidBy)

        requireAcceptedMemberOrOwner(
            tripId = tripId,
            userId = paidBy
        )

        val bookingId = input.bookingId?.let {
            UUID.fromString(it)
        }

        bookingId?.let {
            validateBookingBelongsToTrip(it, tripId)
        }

        val currency = normalizeCurrency(input.currency)

        val expense = Expense(
            tripId = tripId,
            budgetId = budget.id,
            bookingId = bookingId,
            category = input.category,
            title = input.title.trim(),
            description = input.description?.trim(),
            amount = input.amount,
            currency = currency,
            expenseDate = LocalDateTime.parse(input.expenseDate),
            paymentMethod = input.paymentMethod,
            paidBy = paidBy,
            createdBy = userId,
            recurring = input.recurring,
            recurrenceRule = input.recurrenceRule?.trim(),
            archived = false
        )

        applyConversionIfPossible(expense, budget)

        return expenseRepository.save(expense)
    }

    @Transactional(readOnly = true)
    fun getExpenses(
        userId: UUID,
        tripId: UUID,
        filter: ExpenseFilterInput? = null
    ): List<Expense> {

        tripAccessService.requireMemberAccess(tripId, userId)

        val expenses = if (filter?.includeArchived == true) {
            expenseRepository
                .findAllByTripIdAndArchivedFalseOrderByExpenseDateDesc(tripId) +
                    expenseRepository
                        .findAllByTripIdAndArchivedTrueOrderByExpenseDateDesc(tripId)
        } else {
            expenseRepository
                .findAllByTripIdAndArchivedFalseOrderByExpenseDateDesc(tripId)
        }

        return expenses.filter { expense ->

            val matchesSearch =
                filter?.search.isNullOrBlank() ||
                        expense.title.contains(
                            filter!!.search!!,
                            ignoreCase = true
                        ) ||
                        expense.description?.contains(
                            filter.search!!,
                            ignoreCase = true
                        ) == true

            val matchesCategory =
                filter?.category == null ||
                        expense.category == filter.category

            val matchesMinAmount =
                filter?.minAmount == null ||
                        expense.amount >= filter.minAmount

            val matchesMaxAmount =
                filter?.maxAmount == null ||
                        expense.amount <= filter.maxAmount

            val matchesFromDate =
                filter?.fromDate == null ||
                        !expense.expenseDate.isBefore(
                            LocalDateTime.parse(filter.fromDate)
                        )

            val matchesToDate =
                filter?.toDate == null ||
                        !expense.expenseDate.isAfter(
                            LocalDateTime.parse(filter.toDate)
                        )

            matchesSearch &&
                    matchesCategory &&
                    matchesMinAmount &&
                    matchesMaxAmount &&
                    matchesFromDate &&
                    matchesToDate
        }
    }

    @Transactional(readOnly = true)
    fun getExpense(
        userId: UUID,
        tripId: UUID,
        expenseId: UUID
    ): Expense {

        tripAccessService.requireMemberAccess(tripId, userId)

        return expenseRepository.findByIdAndTripId(
            expenseId,
            tripId
        ) ?: throw IllegalArgumentException("Expense not found")
    }

    @Transactional
    fun updateExpense(
        userId: UUID,
        tripId: UUID,
        expenseId: UUID,
        input: UpdateExpenseInput
    ): Expense {

        tripAccessService.requireMemberAccess(tripId, userId)

        val budget = budgetRepository.findByTripId(tripId)
            ?: throw IllegalArgumentException("Budget not found for this trip")

        require(!budget.locked) {
            "Budget is locked"
        }

        val expense = expenseRepository.findByIdAndTripId(
            expenseId,
            tripId
        ) ?: throw IllegalArgumentException("Expense not found")

        if (expense.createdBy != userId) {
            tripAccessService.requireOwnerAccess(tripId, userId)
        }

        input.category?.let {
            expense.category = it
        }

        input.title?.let {
            expense.title = it.trim()
        }

        input.description?.let {
            expense.description = it.trim()
        }

        input.amount?.let {
            validateAmount(it)
            expense.amount = it
        }

        input.currency?.let {
            expense.currency = normalizeCurrency(it)
        }

        input.expenseDate?.let {
            expense.expenseDate = LocalDateTime.parse(it)
        }

        input.paymentMethod?.let {
            expense.paymentMethod = it
        }

        input.paidBy?.let {
            val paidBy = UUID.fromString(it)

            requireAcceptedMemberOrOwner(
                tripId = tripId,
                userId = paidBy
            )

            expense.paidBy = paidBy
        }

        input.bookingId?.let {
            val bookingId = UUID.fromString(it)

            validateBookingBelongsToTrip(
                bookingId,
                tripId
            )

            expense.bookingId = bookingId
        }

        input.recurring?.let {
            expense.recurring = it
        }

        input.recurrenceRule?.let {
            expense.recurrenceRule = it.trim()
        }

        applyConversionIfPossible(expense, budget)

        return expenseRepository.save(expense)
    }

    @Transactional
    fun archiveExpense(
        userId: UUID,
        tripId: UUID,
        expenseId: UUID
    ): Expense {

        tripAccessService.requireMemberAccess(tripId, userId)

        val budget = budgetRepository.findByTripId(tripId)
            ?: throw IllegalArgumentException("Budget not found for this trip")

        require(!budget.locked) {
            "Budget is locked"
        }

        val expense = expenseRepository.findByIdAndTripId(
            expenseId,
            tripId
        ) ?: throw IllegalArgumentException("Expense not found")

        if (expense.createdBy != userId) {
            tripAccessService.requireOwnerAccess(tripId, userId)
        }

        require(!expense.archived) {
            "Expense is already archived"
        }

        expense.archived = true

        return expenseRepository.save(expense)
    }

    private fun getTrip(tripId: UUID) =
        tripRepository.findById(tripId)
            .orElseThrow {
                IllegalArgumentException("Trip not found")
            }

    private fun requireAcceptedMemberOrOwner(
        tripId: UUID,
        userId: UUID
    ) {
        val trip = getTrip(tripId)

        if (trip.ownerId == userId) {
            return
        }

        val member = tripMemberRepository
            .findByTripIdAndUserId(tripId, userId)

        require(member?.status == TripMemberStatus.ACCEPTED) {
            "User is not an accepted member of this trip"
        }
    }

    private fun validateBookingBelongsToTrip(
        bookingId: UUID,
        tripId: UUID
    ) {
        bookingRepository.findByIdAndTripId(
            bookingId,
            tripId
        ) ?: throw IllegalArgumentException(
            "Booking does not belong to this trip"
        )
    }

    private fun validateAmount(amount: BigDecimal) {
        require(amount >= BigDecimal.ZERO) {
            "Amount cannot be negative"
        }
    }

    private fun normalizeCurrency(currency: String): String {
        val normalized = currency.trim().uppercase()

        require(normalized.length == 3) {
            "Currency must be a 3-letter ISO code"
        }

        return normalized
    }

    private fun applyConversionIfPossible(
        expense: Expense,
        budget: com.trippoint.backend.budget.entity.Budget
    ) {
        if (expense.currency == budget.currency) {
            expense.exchangeRate = BigDecimal.ONE
            expense.convertedAmount = expense.amount
        } else {
            expense.exchangeRate = null
            expense.convertedAmount = null
        }
    }
}