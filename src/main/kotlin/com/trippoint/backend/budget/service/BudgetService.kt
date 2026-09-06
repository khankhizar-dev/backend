package com.trippoint.backend.budget.service

import com.trippoint.backend.booking.repository.BookingRepository
import com.trippoint.backend.budget.entity.Budget
import com.trippoint.backend.budget.entity.BudgetOverview
import com.trippoint.backend.budget.entity.CategoryBreakdown
import com.trippoint.backend.budget.entity.Expense
import com.trippoint.backend.budget.graphql.input.CreateBudgetInput
import com.trippoint.backend.budget.graphql.input.UpdateBudgetInput
import com.trippoint.backend.budget.model.CategoryExpenseReport
import com.trippoint.backend.budget.model.DailyExpenseReport
import com.trippoint.backend.budget.model.ExpenseCategory
import com.trippoint.backend.budget.model.MemberSettlement
import com.trippoint.backend.budget.model.SettlementSummary
import com.trippoint.backend.budget.model.SettlementTransfer
import com.trippoint.backend.budget.repository.BudgetRepository
import com.trippoint.backend.budget.repository.ExpenseRepository
import com.trippoint.backend.trip.entity.Trip
import com.trippoint.backend.trip.model.TripMemberStatus
import com.trippoint.backend.trip.repository.TripMemberRepository
import com.trippoint.backend.trip.repository.TripRepository
import com.trippoint.backend.trip.service.TripAccessService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

@Service
class BudgetService(
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val tripRepository: TripRepository,
    private val tripMemberRepository: TripMemberRepository,
    private val bookingRepository: BookingRepository,
    private val tripAccessService: TripAccessService
) {

    // ---------------------------------------------------------
    // Budget
    // ---------------------------------------------------------

    @Transactional
    fun createBudget(
        userId: UUID,
        tripId: UUID,
        input: CreateBudgetInput
    ): Budget {

        val trip = getTrip(tripId)

        require(trip.ownerId == userId) {
            "Only the trip owner can create a budget"
        }

        require(!budgetRepository.existsByTripId(tripId)) {
            "Budget already exists for this trip"
        }

        validateAmount(input.totalAmount)

        val currency = normalizeCurrency(input.currency)

        val budget = Budget(
            tripId = tripId,
            totalAmount = input.totalAmount,
            currency = currency,
            createdBy = userId
        )
        tripAccessService.requireOwnerAccess(tripId, userId)

        return budgetRepository.save(budget)
    }

    @Transactional(readOnly = true)
    fun getBudget(
        userId: UUID,
        tripId: UUID
    ): Budget {

        requireTripAccess(
            userId = userId,
            tripId = tripId
        )

        tripAccessService.requireOwnerAccess(tripId, userId)

        return budgetRepository.findByTripId(tripId)
            ?: throw IllegalArgumentException("Budget not found")
    }

    @Transactional
    fun updateBudget(
        userId: UUID,
        tripId: UUID,
        input: UpdateBudgetInput
    ): Budget {

        val trip = getTrip(tripId)

        require(trip.ownerId == userId) {
            "Only the trip owner can update the budget"
        }

        val budget = budgetRepository.findByTripId(tripId)
            ?: throw IllegalArgumentException("Budget not found")

        require(!budget.locked) {
            "Budget is locked"
        }

        input.totalAmount?.let {
            validateAmount(it)
            budget.totalAmount = it
        }

        input.currency?.let {
            budget.currency = normalizeCurrency(it)
        }

        tripAccessService.requireOwnerAccess(tripId, userId)

        return budgetRepository.save(budget)
    }

    // ---------------------------------------------------------
    // Summary
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    fun getSpentAmount(
        userId: UUID,
        tripId: UUID
    ): BigDecimal {

        val budget = getBudget(
            userId = userId,
            tripId = tripId
        )

        val expenses = expenseRepository
            .findAllByTripIdAndArchivedFalseOrderByExpenseDateDesc(
                tripId
            )

        return expenses.fold(BigDecimal.ZERO) { total, expense ->
            total + amountForBudget(
                expense,
                budget
            )
        }
    }

    @Transactional(readOnly = true)
    fun getRemainingAmount(
        userId: UUID,
        tripId: UUID
    ): BigDecimal {

        val budget = getBudget(
            userId = userId,
            tripId = tripId
        )

        return budget.totalAmount.subtract(
            getSpentAmount(
                userId = userId,
                tripId = tripId
            )
        )
    }

    @Transactional(readOnly = true)
    fun getCategoryBreakdown(
        userId: UUID,
        tripId: UUID
    ): Map<ExpenseCategory, BigDecimal> {

        val budget = getBudget(
            userId = userId,
            tripId = tripId
        )

        return expenseRepository
            .findAllByTripIdAndArchivedFalseOrderByExpenseDateDesc(
                tripId
            )
            .groupBy { it.category }
            .mapValues { (_, expenses) ->
                expenses.fold(BigDecimal.ZERO) { total, expense ->
                    total + amountForBudget(
                        expense,
                        budget
                    )
                }
            }
    }

    // ---------------------------------------------------------
    // Validation / authorization
    // ---------------------------------------------------------

    private fun getTrip(tripId: UUID): Trip =
        tripRepository.findById(tripId)
            .orElseThrow {
                IllegalArgumentException("Trip not found")
            }

    private fun requireTripAccess(
        userId: UUID,
        tripId: UUID
    ) {

        val trip = getTrip(tripId)

        if (trip.ownerId == userId) {
            return
        }

        val member = tripMemberRepository
            .findByTripIdAndUserId(
                tripId,
                userId
            )

        require(
            member?.status == TripMemberStatus.ACCEPTED
        ) {
            "You do not have access to this trip"
        }
    }

    private fun requireAcceptedMember(
        tripId: UUID,
        userId: UUID
    ) {

        val trip = getTrip(tripId)

        if (trip.ownerId == userId) {
            return
        }

        val member = tripMemberRepository
            .findByTripIdAndUserId(
                tripId,
                userId
            )

        require(
            member?.status == TripMemberStatus.ACCEPTED
        ) {
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
            "Booking not found in this trip"
        )
    }

    private fun validateAmount(
        amount: BigDecimal
    ) {

        require(amount >= BigDecimal.ZERO) {
            "Amount cannot be negative"
        }
    }

    private fun normalizeCurrency(
        currency: String
    ): String {

        val normalized = currency.trim().uppercase()

        require(normalized.length == 3) {
            "Currency must contain 3 characters"
        }

        return normalized
    }

    // ---------------------------------------------------------
    // Currency
    // ---------------------------------------------------------

    private fun applyConversionIfPossible(
        expense: Expense,
        budget: Budget
    ) {

        if (expense.currency == budget.currency) {
            expense.exchangeRate = BigDecimal.ONE
            expense.convertedAmount = expense.amount
        } else {
            /*
             * External exchange-rate integration will be added later.
             *
             * We deliberately don't guess an exchange rate here.
             */
            expense.exchangeRate = null
            expense.convertedAmount = null
        }
    }

    private fun amountForBudget(
        expense: Expense,
        budget: Budget
    ): BigDecimal {

        if (expense.currency == budget.currency) {
            return expense.amount
        }

        return expense.convertedAmount
            ?: BigDecimal.ZERO
    }

    @Transactional(readOnly = true)
    fun getExpenseCount(
        userId: UUID,
        tripId: UUID
    ): Long {

        requireTripAccess(userId, tripId)

        return expenseRepository.countActiveExpenses(tripId)
    }

    @Transactional(readOnly = true)
    fun getOverview(
        userId: UUID,
        tripId: UUID
    ): BudgetOverview {

        tripAccessService.requireMemberAccess(tripId, userId)

        val budget = budgetRepository.findByTripId(tripId)
            ?: throw IllegalArgumentException("Budget not found")

        val spent = expenseRepository.sumAmountByTripId(tripId)
            .let { it ?: BigDecimal.ZERO }

        val remaining = budget.totalAmount.subtract(spent)

        val percentageUsed =
            if (budget.totalAmount.compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal.ZERO
            } else {
                spent
                    .multiply(BigDecimal("100"))
                    .divide(
                        budget.totalAmount,
                        2,
                        RoundingMode.HALF_UP
                    )
            }

        val expenseCount = expenseRepository.countActiveExpenses(tripId)

        val categoryBreakdown = expenseRepository
            .sumAmountByCategory(tripId)
            .map { row ->

                val category = row[0] as ExpenseCategory
                val amount = row[1] as BigDecimal

                val percentage =
                    if (spent.compareTo(BigDecimal.ZERO) == 0) {
                        BigDecimal.ZERO
                    } else {
                        amount
                            .multiply(BigDecimal("100"))
                            .divide(
                                spent,
                                2,
                                RoundingMode.HALF_UP
                            )
                    }

                CategoryBreakdown(
                    category = category,
                    amount = amount,
                    percentage = percentage
                )
            }

        return BudgetOverview(
            budget = budget.totalAmount,
            spent = spent,
            remaining = remaining,
            percentageUsed = percentageUsed,
            expenseCount = expenseCount,
            categoryBreakdown = categoryBreakdown
        )
    }

    @Transactional(readOnly = true)
    fun getDailyExpenseReport(
        userId: UUID,
        tripId: UUID,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<DailyExpenseReport> {

        tripAccessService.requireMemberAccess(
            tripId = tripId,
            userId = userId
        )

        require(!fromDate.isAfter(toDate)) {
            "From date cannot be after to date"
        }

        val expenses = expenseRepository
            .findAllByTripIdAndExpenseDateBetweenAndArchivedFalseOrderByExpenseDateDesc(
                tripId = tripId,
                startDate = fromDate.atStartOfDay(),
                endDate = toDate.atTime(23, 59, 59)
            )

        return expenses
            .groupBy { it.expenseDate.toLocalDate() }
            .map { (date, dailyExpenses) ->

                val amount = dailyExpenses.fold(BigDecimal.ZERO) { total, expense ->
                    total + expense.amount
                }

                DailyExpenseReport(
                    date = date,
                    amount = amount,
                    expenseCount = dailyExpenses.size.toLong()
                )
            }
            .sortedBy { it.date }
    }

    @Transactional(readOnly = true)
    fun getCategoryExpenseReport(
        userId: UUID,
        tripId: UUID,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<CategoryExpenseReport> {

        tripAccessService.requireMemberAccess(
            tripId = tripId,
            userId = userId
        )

        require(!fromDate.isAfter(toDate)) {
            "From date cannot be after to date"
        }

        val expenses = expenseRepository
            .findAllByTripIdAndExpenseDateBetweenAndArchivedFalseOrderByExpenseDateDesc(
                tripId = tripId,
                startDate = fromDate.atStartOfDay(),
                endDate = toDate.atTime(23, 59, 59)
            )

        val totalAmount = expenses.fold(BigDecimal.ZERO) { total, expense ->
            total + expense.amount
        }

        return expenses
            .groupBy { it.category }
            .map { (category, categoryExpenses) ->

                val amount = categoryExpenses.fold(BigDecimal.ZERO) { total, expense ->
                    total + expense.amount
                }

                val percentage =
                    if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
                        BigDecimal.ZERO
                    } else {
                        amount
                            .multiply(BigDecimal("100"))
                            .divide(
                                totalAmount,
                                2,
                                RoundingMode.HALF_UP
                            )
                    }

                CategoryExpenseReport(
                    category = category,
                    amount = amount,
                    percentage = percentage,
                    expenseCount = categoryExpenses.size.toLong()
                )
            }
            .sortedByDescending { it.amount }
    }

    @Transactional(readOnly = true)
    fun getSettlementSummary(
        userId: UUID,
        tripId: UUID
    ): SettlementSummary {

        tripAccessService.requireMemberAccess(
            tripId = tripId,
            userId = userId
        )

        val trip = getTrip(tripId)

        val acceptedMembers = tripMemberRepository
            .findAllByTripIdAndStatus(
                tripId = tripId,
                status = TripMemberStatus.ACCEPTED
            )

        val participantIds = buildSet {
            add(trip.ownerId)

            acceptedMembers.forEach {
                add(it.userId)
            }
        }

        val expenses = expenseRepository
            .findAllByTripIdAndArchivedFalseOrderByExpenseDateDesc(tripId)

        /*
         * Only expenses whose paidBy is a valid trip participant
         * should contribute to settlement.
         */
        expenses.forEach { expense ->
            require(expense.paidBy in participantIds) {
                "Expense paidBy is not a valid trip participant"
            }
        }

        val totalExpense = expenses.fold(BigDecimal.ZERO) { total, expense ->
            total + expense.amount
        }

        val memberCount = participantIds.size

        require(memberCount > 0) {
            "Trip has no participants"
        }

        val equalShare = totalExpense.divide(
            BigDecimal(memberCount),
            2,
            RoundingMode.DOWN
        )

        val distributedAmount = equalShare
            .multiply(BigDecimal(memberCount))

        val roundingRemainder = totalExpense
            .subtract(distributedAmount)

        val paidAmounts = participantIds.associateWith { participantId ->
            expenses
                .filter { it.paidBy == participantId }
                .fold(BigDecimal.ZERO) { total, expense ->
                    total + expense.amount
                }
        }

        val sortedParticipants = participantIds.sorted()

        val memberSettlements = sortedParticipants
            .mapIndexed { index, participantId ->

                val paidAmount = paidAmounts[participantId]
                    ?: BigDecimal.ZERO

                val shareAmount =
                    if (index == 0) {
                        equalShare.add(roundingRemainder)
                    } else {
                        equalShare
                    }

                MemberSettlement(
                    userId = participantId,
                    paidAmount = paidAmount,
                    shareAmount = shareAmount,
                    balance = paidAmount.subtract(shareAmount)
                )
            }
            .sortedBy { it.userId }

        val settlements = calculateTransfers(
            memberSettlements
        )

        return SettlementSummary(
            totalExpense = totalExpense,
            memberCount = memberCount,
            equalShare = equalShare,
            members = memberSettlements,
            settlements = settlements
        )
    }

    private fun calculateTransfers(
        members: List<MemberSettlement>
    ): List<SettlementTransfer> {

        val creditors = members
            .filter { it.balance > BigDecimal.ZERO }
            .map {
                it.userId to it.balance
            }
            .toMutableList()

        val debtors = members
            .filter { it.balance < BigDecimal.ZERO }
            .map {
                it.userId to it.balance.abs()
            }
            .toMutableList()

        val transfers = mutableListOf<SettlementTransfer>()

        var creditorIndex = 0
        var debtorIndex = 0

        while (
            creditorIndex < creditors.size &&
            debtorIndex < debtors.size
        ) {

            val (creditorId, creditorAmount) =
                creditors[creditorIndex]

            val (debtorId, debtorAmount) =
                debtors[debtorIndex]

            val transferAmount =
                creditorAmount.min(debtorAmount)

            if (transferAmount > BigDecimal.ZERO) {
                transfers += SettlementTransfer(
                    fromUserId = debtorId,
                    toUserId = creditorId,
                    amount = transferAmount
                )
            }

            val remainingCreditor =
                creditorAmount.subtract(transferAmount)

            val remainingDebtor =
                debtorAmount.subtract(transferAmount)

            creditors[creditorIndex] =
                creditorId to remainingCreditor

            debtors[debtorIndex] =
                debtorId to remainingDebtor

            if (remainingCreditor.compareTo(BigDecimal.ZERO) == 0) {
                creditorIndex++
            }

            if (remainingDebtor.compareTo(BigDecimal.ZERO) == 0) {
                debtorIndex++
            }
        }

        return transfers
    }
}