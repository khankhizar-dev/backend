package com.trippoint.backend.budget.service

import com.trippoint.backend.booking.repository.BookingRepository
import com.trippoint.backend.budget.entity.Budget
import com.trippoint.backend.budget.entity.Expense
import com.trippoint.backend.budget.graphql.input.CreateExpenseInput
import com.trippoint.backend.budget.graphql.input.ExpenseFilterInput
import com.trippoint.backend.budget.graphql.input.UpdateExpenseInput
import com.trippoint.backend.budget.model.ExpenseCategory
import com.trippoint.backend.budget.model.PaymentMethod
import com.trippoint.backend.budget.repository.BudgetRepository
import com.trippoint.backend.budget.repository.ExpenseRepository
import com.trippoint.backend.trip.entity.Trip
import com.trippoint.backend.trip.entity.TripMember
import com.trippoint.backend.trip.model.TripMemberRole
import com.trippoint.backend.trip.model.TripMemberStatus
import com.trippoint.backend.trip.repository.TripMemberRepository
import com.trippoint.backend.trip.repository.TripRepository
import com.trippoint.backend.trip.service.TripAccessService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class ExpenseServiceTest {

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var tripRepository: TripRepository
    private lateinit var tripMemberRepository: TripMemberRepository
    private lateinit var bookingRepository: BookingRepository
    private lateinit var tripAccessService: TripAccessService

    private lateinit var service: ExpenseService

    private val ownerId = UUID.randomUUID()
    private val memberId = UUID.randomUUID()
    private val otherUserId = UUID.randomUUID()

    private val tripId = UUID.randomUUID()
    private val budgetId = UUID.randomUUID()
    private val expenseId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        expenseRepository = mock()
        budgetRepository = mock()
        tripRepository = mock()
        tripMemberRepository = mock()
        bookingRepository = mock()
        tripAccessService = mock()

        service = ExpenseService(
            expenseRepository = expenseRepository,
            budgetRepository = budgetRepository,
            tripRepository = tripRepository,
            tripMemberRepository = tripMemberRepository,
            bookingRepository = bookingRepository,
            tripAccessService = tripAccessService
        )
    }

    private fun trip(): Trip =
        Trip(
            id = tripId,
            ownerId = ownerId,
            name = "Dubai Trip",
            destination = "Dubai",
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2026, 10, 5)
        )

    private fun budget(
        locked: Boolean = false
    ): Budget =
        Budget(
            id = budgetId,
            tripId = tripId,
            totalAmount = BigDecimal("100000.00"),
            currency = "INR",
            locked = locked,
            createdBy = ownerId
        )

    private fun createExpenseInput(
        amount: BigDecimal = BigDecimal("2500.00"),
        currency: String = "INR",
        paidBy: UUID = memberId
    ): CreateExpenseInput =
        CreateExpenseInput(
            category = ExpenseCategory.FOOD,
            title = "Dinner",
            description = "Dinner at restaurant",
            amount = amount,
            currency = currency,
            expenseDate = "2026-10-01T20:00:00",
            paymentMethod = PaymentMethod.UPI,
            paidBy = paidBy.toString()
        )

    private fun expense(
        createdBy: UUID = memberId
    ): Expense =
        Expense(
            id = expenseId,
            tripId = tripId,
            budgetId = budgetId,
            bookingId = null,
            category = ExpenseCategory.FOOD,
            title = "Dinner",
            description = "Dinner at restaurant",
            amount = BigDecimal("2500.00"),
            currency = "INR",
            expenseDate = LocalDateTime.of(2026, 10, 1, 20, 0),
            paymentMethod = PaymentMethod.UPI,
            paidBy = memberId,
            createdBy = createdBy
        )

    // ---------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------

    @Test
    fun `create expense successfully as accepted member`() {

        val input = createExpenseInput()

        whenever(budgetRepository.findByTripId(tripId))
            .thenReturn(budget())

        whenever(
            tripRepository.findById(tripId)
        ).thenReturn(Optional.of(trip()))

        whenever(
            tripMemberRepository.findByTripIdAndUserId(
                tripId,
                memberId
            )
        ).thenReturn(
            TripMember(
                tripId = tripId,
                userId = memberId,
                role = TripMemberRole.MEMBER,
                status = TripMemberStatus.ACCEPTED
            )
        )

        whenever(expenseRepository.save(any<Expense>()))
            .thenAnswer { it.arguments[0] as Expense }

        val result = service.createExpense(
            userId = memberId,
            tripId = tripId,
            input = input
        )

        assertNotNull(result)
        assertEquals(tripId, result.tripId)
        assertEquals(budgetId, result.budgetId)
        assertEquals(BigDecimal("2500.00"), result.amount)
        assertEquals("INR", result.currency)
        assertEquals(BigDecimal.ONE, result.exchangeRate)
        assertEquals(BigDecimal("2500.00"), result.convertedAmount)
        assertEquals(memberId, result.createdBy)
        assertEquals(memberId, result.paidBy)
        assertEquals(false, result.archived)

        verify(expenseRepository).save(any())
    }

    @Test
    fun `create expense successfully as trip owner`() {

        val input = createExpenseInput(
            paidBy = ownerId
        )

        whenever(budgetRepository.findByTripId(tripId))
            .thenReturn(budget())

        whenever(tripRepository.findById(tripId))
            .thenReturn(Optional.of(trip()))

        whenever(expenseRepository.save(any<Expense>()))
            .thenAnswer { it.arguments[0] as Expense }

        val result = service.createExpense(
            userId = ownerId,
            tripId = tripId,
            input = input
        )

        assertEquals(ownerId, result.createdBy)
        assertEquals(ownerId, result.paidBy)
        assertEquals(BigDecimal("2500.00"), result.amount)

        verify(expenseRepository).save(any())
    }

    @Test
    fun `create expense fails when budget does not exist`() {

        whenever(budgetRepository.findByTripId(tripId))
            .thenReturn(null)

        val exception = assertThrows<IllegalArgumentException> {
            service.createExpense(
                userId = memberId,
                tripId = tripId,
                input = createExpenseInput()
            )
        }

        assertEquals(
            "Budget not found for this trip",
            exception.message
        )

        verify(expenseRepository, never()).save(any())
    }

    @Test
    fun `create expense fails when budget is locked`() {

        whenever(budgetRepository.findByTripId(tripId))
            .thenReturn(budget(locked = true))

        val exception = assertThrows<IllegalArgumentException> {
            service.createExpense(
                userId = memberId,
                tripId = tripId,
                input = createExpenseInput()
            )
        }

        assertEquals(
            "Budget is locked",
            exception.message
        )

        verify(expenseRepository, never()).save(any())
    }

    @Test
    fun `create expense fails when amount is zero`() {

        whenever(budgetRepository.findByTripId(tripId))
            .thenReturn(budget())

        val exception = assertThrows<IllegalArgumentException> {
            service.createExpense(
                userId = memberId,
                tripId = tripId,
                input = createExpenseInput(
                    amount = BigDecimal.ZERO
                )
            )
        }

        assertNotNull(exception.message)

        verify(expenseRepository, never()).save(any())
    }

    @Test
    fun `create expense fails when amount is negative`() {

        whenever(budgetRepository.findByTripId(tripId))
            .thenReturn(budget())

        val exception = assertThrows<IllegalArgumentException> {
            service.createExpense(
                userId = memberId,
                tripId = tripId,
                input = createExpenseInput(
                    amount = BigDecimal("-100.00")
                )
            )
    }

    assertNotNull(exception.message)

    verify(expenseRepository, never()).save(any())
}

@Test
fun `create expense fails when paidBy is not accepted member or owner`() {

    whenever(budgetRepository.findByTripId(tripId))
        .thenReturn(budget())

    whenever(tripRepository.findById(tripId))
        .thenReturn(Optional.of(trip()))

    whenever(
        tripMemberRepository.findByTripIdAndUserId(
            tripId,
            otherUserId
        )
    ).thenReturn(null)

    val exception = assertThrows<IllegalArgumentException> {
        service.createExpense(
            userId = memberId,
            tripId = tripId,
            input = createExpenseInput(
                paidBy = otherUserId
            )
        )
    }

    assertEquals(
        "User is not an accepted member of this trip",
        exception.message
    )

    verify(expenseRepository, never()).save(any())
}

    @Test
    fun `creator can update own expense`() {

        val existingExpense = expense(createdBy = memberId)

        whenever(budgetRepository.findByTripId(tripId))
            .thenReturn(budget())

        whenever(expenseRepository.findByIdAndTripId(expenseId, tripId))
            .thenReturn(existingExpense)

        whenever(expenseRepository.save(any<Expense>()))
            .thenAnswer { it.arguments[0] as Expense }

        val input = UpdateExpenseInput(
            title = "Updated Dinner",
            description = "Updated description",
            amount = BigDecimal("3000.00"),
            currency = "INR",
            paymentMethod = PaymentMethod.CREDIT_CARD
        )

        val result = service.updateExpense(
            userId = memberId,
            tripId = tripId,
            expenseId = expenseId,
            input = input
        )

        assertEquals("Updated Dinner", result.title)
        assertEquals("Updated description", result.description)
        assertEquals(BigDecimal("3000.00"), result.amount)
        assertEquals(PaymentMethod.CREDIT_CARD, result.paymentMethod)

        verify(expenseRepository).save(existingExpense)
    }

    @Test
    fun `owner can update another member expense`() {

        val existingExpense = expense(createdBy = memberId)

        whenever(budgetRepository.findByTripId(tripId))
            .thenReturn(budget())

        whenever(expenseRepository.findByIdAndTripId(expenseId, tripId))
            .thenReturn(existingExpense)

        whenever(expenseRepository.save(any<Expense>()))
            .thenAnswer { it.arguments[0] as Expense }

        val input = UpdateExpenseInput(
            title = "Owner Updated Expense",
            amount = BigDecimal("3500.00"),
            currency = "INR"
        )

        val result = service.updateExpense(
            userId = ownerId,
            tripId = tripId,
            expenseId = expenseId,
            input = input
        )

        assertEquals("Owner Updated Expense", result.title)
        assertEquals(BigDecimal("3500.00"), result.amount)

        verify(expenseRepository).save(existingExpense)
    }

    @Test
    fun `creator can archive own expense`() {

        val existingExpense = expense(createdBy = memberId)

        whenever(budgetRepository.findByTripId(tripId))
            .thenReturn(budget())

        whenever(expenseRepository.findByIdAndTripId(expenseId, tripId))
            .thenReturn(existingExpense)

        whenever(expenseRepository.save(any<Expense>()))
            .thenAnswer { it.arguments[0] as Expense }

        val result = service.archiveExpense(
            userId = memberId,
            tripId = tripId,
            expenseId = expenseId
        )

        assertEquals(true, result.archived)

        verify(expenseRepository).save(existingExpense)
    }

    @Test
    fun `owner can archive another member expense`() {

        val existingExpense = expense(createdBy = memberId)

        whenever(budgetRepository.findByTripId(tripId))
            .thenReturn(budget())

        whenever(expenseRepository.findByIdAndTripId(expenseId, tripId))
            .thenReturn(existingExpense)

        whenever(expenseRepository.save(any<Expense>()))
            .thenAnswer { it.arguments[0] as Expense }

        val result = service.archiveExpense(
            userId = ownerId,
            tripId = tripId,
            expenseId = expenseId
        )

        assertEquals(true, result.archived)

        verify(expenseRepository).save(existingExpense)
    }

    @Test
    fun `already archived expense cannot be archived again`() {

        val existingExpense = expense(createdBy = memberId).apply {
            archived = true
        }

        whenever(budgetRepository.findByTripId(tripId))
            .thenReturn(budget())

        whenever(expenseRepository.findByIdAndTripId(expenseId, tripId))
            .thenReturn(existingExpense)

        assertThrows<IllegalArgumentException> {
            service.archiveExpense(
                userId = memberId,
                tripId = tripId,
                expenseId = expenseId
            )
        }

        verify(expenseRepository, never()).save(any())
    }

    @Test
    fun `locked budget rejects archive`() {

        whenever(budgetRepository.findByTripId(tripId))
            .thenReturn(budget(locked = true))

        assertThrows<IllegalArgumentException> {
            service.archiveExpense(
                userId = memberId,
                tripId = tripId,
                expenseId = expenseId
            )
        }

        verify(expenseRepository, never()).save(any())
    }

    @Test
    fun `get expenses returns active expenses by default`() {

        val expense1 = expense().apply {
            title = "Dinner"
            archived = false
        }

        val expense2 = expense().apply {
            id = UUID.randomUUID()
            title = "Taxi"
            archived = false
        }

        whenever(
            expenseRepository
                .findAllByTripIdAndArchivedFalseOrderByExpenseDateDesc(tripId)
        ).thenReturn(listOf(expense1, expense2))

        val result = service.getExpenses(
            userId = memberId,
            tripId = tripId
        )

        assertEquals(2, result.size)
        assertEquals("Dinner", result[0].title)
        assertEquals("Taxi", result[1].title)

        verify(
            expenseRepository
        ).findAllByTripIdAndArchivedFalseOrderByExpenseDateDesc(tripId)

        verify(
            expenseRepository,
            never()
        ).findAllByTripIdAndArchivedTrueOrderByExpenseDateDesc(tripId)
    }

    @Test
    fun `get expenses includes archived expenses when requested`() {

        val activeExpense = expense().apply {
            title = "Dinner"
            archived = false
        }

        val archivedExpense = expense().apply {
            id = UUID.randomUUID()
            title = "Old Hotel"
            archived = true
        }

        whenever(
            expenseRepository
                .findAllByTripIdAndArchivedFalseOrderByExpenseDateDesc(tripId)
        ).thenReturn(listOf(activeExpense))

        whenever(
            expenseRepository
                .findAllByTripIdAndArchivedTrueOrderByExpenseDateDesc(tripId)
        ).thenReturn(listOf(archivedExpense))

        val result = service.getExpenses(
            userId = memberId,
            tripId = tripId,
            filter = ExpenseFilterInput(
                includeArchived = true
            )
        )

        assertEquals(2, result.size)
        assertEquals("Dinner", result[0].title)
        assertEquals("Old Hotel", result[1].title)
    }

    @Test
    fun `get expenses filters by search text`() {

        val dinner = expense().apply {
            title = "Dinner at Marina"
            description = "Restaurant dinner"
        }

        val taxi = expense().apply {
            id = UUID.randomUUID()
            title = "Airport Taxi"
            description = "Transfer to hotel"
        }

        whenever(
            expenseRepository
                .findAllByTripIdAndArchivedFalseOrderByExpenseDateDesc(tripId)
        ).thenReturn(listOf(dinner, taxi))

        val result = service.getExpenses(
            userId = memberId,
            tripId = tripId,
            filter = ExpenseFilterInput(
                search = "marina"
            )
        )

        assertEquals(1, result.size)
        assertEquals("Dinner at Marina", result[0].title)
    }

    @Test
    fun `get expenses filters by description`() {

        val expense1 = expense().apply {
            title = "Restaurant"
            description = "Dinner at Marina"
        }

        val expense2 = expense().apply {
            id = UUID.randomUUID()
            title = "Taxi"
            description = "Airport transfer"
        }

        whenever(
            expenseRepository
                .findAllByTripIdAndArchivedFalseOrderByExpenseDateDesc(tripId)
        ).thenReturn(listOf(expense1, expense2))

        val result = service.getExpenses(
            userId = memberId,
            tripId = tripId,
            filter = ExpenseFilterInput(
                search = "airport"
            )
        )

        assertEquals(1, result.size)
        assertEquals("Taxi", result[0].title)
    }

    @Test
    fun `get expenses filters by category`() {

        val food = expense().apply {
            category = ExpenseCategory.FOOD
            title = "Dinner"
        }

        val transport = expense().apply {
            id = UUID.randomUUID()
            category = ExpenseCategory.TRANSPORT
            title = "Taxi"
        }

        whenever(
            expenseRepository
                .findAllByTripIdAndArchivedFalseOrderByExpenseDateDesc(tripId)
        ).thenReturn(listOf(food, transport))

        val result = service.getExpenses(
            userId = memberId,
            tripId = tripId,
            filter = ExpenseFilterInput(
                category = ExpenseCategory.TRANSPORT
            )
        )

        assertEquals(1, result.size)
        assertEquals(ExpenseCategory.TRANSPORT, result[0].category)
    }

    @Test
    fun `get expenses filters by amount range`() {

        val cheap = expense().apply {
            amount = BigDecimal("500.00")
            title = "Coffee"
        }

        val middle = expense().apply {
            id = UUID.randomUUID()
            amount = BigDecimal("2500.00")
            title = "Dinner"
        }

        val expensive = expense().apply {
            id = UUID.randomUUID()
            amount = BigDecimal("8000.00")
            title = "Hotel"
        }

        whenever(
            expenseRepository
                .findAllByTripIdAndArchivedFalseOrderByExpenseDateDesc(tripId)
        ).thenReturn(
            listOf(cheap, middle, expensive)
        )

        val result = service.getExpenses(
            userId = memberId,
            tripId = tripId,
            filter = ExpenseFilterInput(
                minAmount = BigDecimal("1000.00"),
                maxAmount = BigDecimal("5000.00")
            )
        )

        assertEquals(1, result.size)
        assertEquals("Dinner", result[0].title)
        assertEquals(
            BigDecimal("2500.00"),
            result[0].amount
        )
    }

    @Test
    fun `get expenses filters by date range`() {
        val expense1 = expense().apply {
            title = "Breakfast"
            expenseDate = LocalDateTime.of(2026, 10, 2, 9, 0)
        }

        val expense2 = expense().apply {
            id = UUID.randomUUID()
            title = "Dinner"
            expenseDate = LocalDateTime.of(2026, 10, 3, 20, 0)
        }

        val expense3 = expense().apply {
            id = UUID.randomUUID()
            title = "Hotel"
            expenseDate = LocalDateTime.of(2026, 10, 5, 12, 0)
        }

        whenever(
            expenseRepository.findAllByTripIdAndArchivedFalseOrderByExpenseDateDesc(tripId)
        ).thenReturn(
            listOf(expense3, expense2, expense1)
        )

        stubMemberAccess()

        val result = service.getExpenses(
            userId = memberId,
            tripId = tripId,
            filter = ExpenseFilterInput(
                fromDate = "2026-10-02T00:00:00",
                toDate = "2026-10-04T23:59:59"
            )
        )

        assertEquals(2, result.size)
        assertEquals("Dinner", result[0].title)
        assertEquals("Breakfast", result[1].title)
    }

    private fun stubMemberAccess(userId: UUID = memberId) {
        whenever(tripRepository.findById(tripId))
            .thenReturn(Optional.of(trip()))

        if (userId != ownerId) {
            whenever(
                tripMemberRepository.findByTripIdAndUserId(tripId, userId)
            ).thenReturn(
                TripMember(
                    tripId = tripId,
                    userId = userId,
                    role = TripMemberRole.MEMBER,
                    status = TripMemberStatus.ACCEPTED
                )
            )
        }
    }

    @Test
    fun `member can get expense`() {
        val existingExpense = expense().apply {
            title = "Dinner"
            createdBy = memberId
        }

        whenever(expenseRepository.findByIdAndTripId(existingExpense.id, tripId))
            .thenReturn(existingExpense)

        stubMemberAccess()

        val result = service.getExpense(
            userId = memberId,
            tripId = tripId,
            expenseId = existingExpense.id
        )

        assertEquals(existingExpense.id, result.id)
        assertEquals("Dinner", result.title)

        verify(expenseRepository).findByIdAndTripId(existingExpense.id, tripId)
    }

    @Test
    fun `owner can get another member expense`() {
        val existingExpense = expense().apply {
            title = "Hotel"
            createdBy = memberId
        }

        whenever(expenseRepository.findByIdAndTripId(existingExpense.id, tripId))
            .thenReturn(existingExpense)

        stubMemberAccess(ownerId)

        val result = service.getExpense(
            userId = ownerId,
            tripId = tripId,
            expenseId = existingExpense.id
        )

        assertEquals(existingExpense.id, result.id)
        assertEquals("Hotel", result.title)
    }

    @Test
    fun `get expense fails when expense does not exist`() {
        val expenseId = UUID.randomUUID()

        stubMemberAccess()

        whenever(expenseRepository.findByIdAndTripId(expenseId, tripId))
            .thenReturn(null)

        val exception = assertThrows<IllegalArgumentException> {
            service.getExpense(
                userId = memberId,
                tripId = tripId,
                expenseId = expenseId
            )
        }

        assertEquals("Expense not found", exception.message)
    }
}