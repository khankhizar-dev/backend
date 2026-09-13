package com.trippoint.backend.budget.service

import com.trippoint.backend.booking.repository.BookingRepository
import com.trippoint.backend.budget.entity.Budget
import com.trippoint.backend.budget.graphql.input.CreateBudgetInput
import com.trippoint.backend.budget.graphql.input.UpdateBudgetInput
import com.trippoint.backend.budget.repository.BudgetRepository
import com.trippoint.backend.budget.repository.ExpenseRepository
import com.trippoint.backend.trip.entity.Trip
import com.trippoint.backend.trip.entity.TripMember
import com.trippoint.backend.trip.model.TripMemberRole
import com.trippoint.backend.trip.model.TripMemberStatus
import com.trippoint.backend.trip.model.TripStatus
import com.trippoint.backend.trip.repository.TripMemberRepository
import com.trippoint.backend.trip.repository.TripRepository
import com.trippoint.backend.trip.service.TripAccessService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BudgetServiceTest {

    private lateinit var budgetRepository: BudgetRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var tripRepository: TripRepository
    private lateinit var tripMemberRepository: TripMemberRepository
    private lateinit var bookingRepository: BookingRepository

    private lateinit var tripAccessService: TripAccessService
    private lateinit var service: BudgetService

    private val tripId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val ownerId = UUID.randomUUID()
    private val otherUserId = UUID.randomUUID()

    private lateinit var budget: Budget

    @BeforeEach
    fun setup() {
        budgetRepository = mockk()
        expenseRepository = mockk()
        tripRepository = mockk()
        tripMemberRepository = mockk()
        bookingRepository = mockk()

        tripAccessService = TripAccessService(
            tripRepository = tripRepository,
            tripMemberRepository = tripMemberRepository
        )

        service = BudgetService(
            budgetRepository = budgetRepository,
            expenseRepository = expenseRepository,
            tripRepository = tripRepository,
            tripMemberRepository = tripMemberRepository,
            bookingRepository = bookingRepository,
            tripAccessService = tripAccessService
        )

        budget = budget(
            tripId = tripId,
            createdBy = ownerId
        )
    }

    // ----------------------------------------------------------------
    // CREATE BUDGET
    // ----------------------------------------------------------------

    @Test
    fun `owner can create budget`() {
        every {
            tripRepository.findById(tripId)
        } returns Optional.of(
            trip(
                id = tripId,
                ownerId = ownerId
            )
        )

        every {
            budgetRepository.existsByTripId(tripId)
        } returns false

        every {
            budgetRepository.save(any())
        } answers { firstArg() }

        val result = service.createBudget(
            userId = ownerId,
            tripId = tripId,
            input = CreateBudgetInput(
                totalAmount = BigDecimal("5000"),
                currency = "USD"
            )
        )

        assertEquals(tripId, result.tripId)
        assertEquals(BigDecimal("5000"), result.totalAmount)
        assertEquals("USD", result.currency)

        verify(exactly = 1) {
            budgetRepository.save(any())
        }
    }

    @Test
    fun `non owner cannot create budget`() {
        every {
            tripRepository.findById(tripId)
        } returns Optional.of(
            trip(
                id = tripId,
                ownerId = ownerId
            )
        )

        assertFailsWith<IllegalArgumentException> {
            service.createBudget(
                userId = userId,
                tripId = tripId,
                input = CreateBudgetInput(
                    totalAmount = BigDecimal("5000"),
                    currency = "USD"
                )
            )
        }

        verify(exactly = 0) {
            budgetRepository.save(any())
        }
    }

    @Test
    fun `cannot create duplicate budget`() {
        every {
            tripRepository.findById(tripId)
        } returns Optional.of(
            trip(
                id = tripId,
                ownerId = ownerId
            )
        )

        every {
            budgetRepository.existsByTripId(tripId)
        } returns true

        assertFailsWith<IllegalArgumentException> {
            service.createBudget(
                userId = ownerId,
                tripId = tripId,
                input = CreateBudgetInput(
                    totalAmount = BigDecimal("5000"),
                    currency = "USD"
                )
            )
        }

        verify(exactly = 0) {
            budgetRepository.save(any())
        }
    }

    // ----------------------------------------------------------------
    // GET BUDGET
    // ----------------------------------------------------------------

    @Test
    fun `owner can view budget`() {
        mockOwnerAccess()

        every {
            budgetRepository.findByTripId(tripId)
        } returns budget

        val result = service.getBudget(
            userId = ownerId,
            tripId = tripId
        )

        assertEquals(budget, result)
    }

    @Test
    fun `accepted member can view budget`() {
        mockAcceptedMemberAccess()

        every {
            budgetRepository.findByTripId(tripId)
        } returns budget

        val result = service.getBudget(
            userId = userId,
            tripId = tripId
        )

        assertEquals(budget, result)
    }

    @Test
    fun `pending member cannot view budget`() {
        mockPendingMemberAccess()

        assertFailsWith<IllegalAccessException> {
            service.getBudget(
                userId = userId,
                tripId = tripId
            )
        }

        verify(exactly = 0) {
            budgetRepository.findByTripId(tripId)
        }
    }

    @Test
    fun `declined member cannot view budget`() {
        mockDeclinedMemberAccess()

        assertFailsWith<IllegalAccessException> {
            service.getBudget(
                userId = userId,
                tripId = tripId
            )
        }

        verify(exactly = 0) {
            budgetRepository.findByTripId(tripId)
        }
    }

    @Test
    fun `non member cannot view budget`() {
        every {
            tripRepository.findById(tripId)
        } returns Optional.of(
            trip(
                id = tripId,
                ownerId = ownerId
            )
        )

        every {
            tripMemberRepository.findByTripIdAndUserId(
                tripId,
                userId
            )
        } returns null

        assertFailsWith<IllegalAccessException> {
            service.getBudget(
                userId = userId,
                tripId = tripId
            )
        }

        verify(exactly = 0) {
            budgetRepository.findByTripId(tripId)
        }
    }

    @Test
    fun `get budget throws when budget does not exist`() {
        mockOwnerAccess()

        every {
            budgetRepository.findByTripId(tripId)
        } returns null

        assertFailsWith<IllegalArgumentException> {
            service.getBudget(
                userId = ownerId,
                tripId = tripId
            )
        }
    }

    // ----------------------------------------------------------------
    // UPDATE BUDGET
    // ----------------------------------------------------------------

    @Test
    fun `owner can update budget`() {
        every {
            tripRepository.findById(tripId)
        } returns Optional.of(
            trip(
                id = tripId,
                ownerId = ownerId
            )
        )

        every {
            budgetRepository.findByTripId(tripId)
        } returns budget

        every {
            budgetRepository.save(any())
        } answers { firstArg() }

        val result = service.updateBudget(
            userId = ownerId,
            tripId = tripId,
            input = UpdateBudgetInput(
                totalAmount = BigDecimal("7500"),
                currency = "EUR"
            )
        )

        assertEquals(BigDecimal("7500"), result.totalAmount)
        assertEquals("EUR", result.currency)

        verify(exactly = 1) {
            budgetRepository.save(budget)
        }
    }

    @Test
    fun `accepted member cannot update budget`() {
        every {
            tripRepository.findById(tripId)
        } returns Optional.of(
            trip(
                id = tripId,
                ownerId = ownerId
            )
        )

        assertFailsWith<IllegalArgumentException> {
            service.updateBudget(
                userId = userId,
                tripId = tripId,
                input = UpdateBudgetInput(
                    totalAmount = BigDecimal("7500"),
                    currency = "EUR"
                )
            )
        }

    verify(exactly = 0) {
        budgetRepository.save(any())
    }
}

@Test
fun `cannot update locked budget`() {
    every {
        tripRepository.findById(tripId)
    } returns Optional.of(
        trip(
            id = tripId,
            ownerId = ownerId
        )
    )

    val lockedBudget = budget(
        tripId = tripId,
        createdBy = ownerId,
        locked = true
    )

    every {
        budgetRepository.findByTripId(tripId)
    } returns lockedBudget

    assertFailsWith<IllegalArgumentException> {
        service.updateBudget(
            userId = ownerId,
            tripId = tripId,
            input = UpdateBudgetInput(
                totalAmount = BigDecimal("7500"),
                currency = "EUR"
            )
        )
    }

    verify(exactly = 0) {
        budgetRepository.save(any())
    }
}

// ----------------------------------------------------------------
// EXPENSE COUNT
// ----------------------------------------------------------------

@Test
fun `owner can get expense count`() {
    mockOwnerAccess()

    every {
        expenseRepository.countActiveExpenses(tripId)
    } returns 5L

    val result = service.getExpenseCount(
        userId = ownerId,
        tripId = tripId
    )

    assertEquals(5L, result)
}

@Test
fun `accepted member can get expense count`() {
    mockAcceptedMemberAccess()

    every {
        expenseRepository.countActiveExpenses(tripId)
    } returns 5L

    val result = service.getExpenseCount(
        userId = userId,
        tripId = tripId
    )

    assertEquals(5L, result)
}

@Test
fun `pending member cannot get expense count`() {
    mockPendingMemberAccess()

    assertFailsWith<IllegalAccessException> {
        service.getExpenseCount(
            userId = userId,
            tripId = tripId
        )
    }

    verify(exactly = 0) {
        expenseRepository.countActiveExpenses(tripId)
    }
}

// ----------------------------------------------------------------
// HELPERS
// ----------------------------------------------------------------

private fun mockOwnerAccess() {
    every {
        tripRepository.findById(tripId)
    } returns Optional.of(
        trip(
            id = tripId,
            ownerId = ownerId
        )
    )
}

private fun mockAcceptedMemberAccess() {
    every {
        tripRepository.findById(tripId)
    } returns Optional.of(
        trip(
            id = tripId,
            ownerId = ownerId
        )
    )

    every {
        tripMemberRepository.findByTripIdAndUserId(
            tripId,
            userId
        )
    } returns TripMember(
        id = UUID.randomUUID(),
        tripId = tripId,
        userId = userId,
        role = TripMemberRole.MEMBER,
        status = TripMemberStatus.ACCEPTED
    )
}

private fun mockPendingMemberAccess() {
    every {
        tripRepository.findById(tripId)
    } returns Optional.of(
        trip(
            id = tripId,
            ownerId = ownerId
        )
    )

    every {
        tripMemberRepository.findByTripIdAndUserId(
            tripId,
            userId
        )
    } returns TripMember(
        id = UUID.randomUUID(),
        tripId = tripId,
        userId = userId,
        role = TripMemberRole.MEMBER,
        status = TripMemberStatus.PENDING
    )
}

private fun mockDeclinedMemberAccess() {
    every {
        tripRepository.findById(tripId)
    } returns Optional.of(
        trip(
            id = tripId,
            ownerId = ownerId
        )
    )

    every {
        tripMemberRepository.findByTripIdAndUserId(
            tripId,
            userId
        )
    } returns TripMember(
        id = UUID.randomUUID(),
        tripId = tripId,
        userId = userId,
        role = TripMemberRole.MEMBER,
        status = TripMemberStatus.DECLINED
    )
}

private fun trip(
    id: UUID = tripId,
    ownerId: UUID = this.ownerId
): Trip {
    return Trip(
        id = id,
        ownerId = ownerId,
        name = "Dubai Trip",
        destination = "Dubai",
        startDate = LocalDate.of(2026, 10, 15),
        endDate = LocalDate.of(2026, 10, 20),
        status = TripStatus.DRAFT
    )
}

private fun budget(
    tripId: UUID,
    createdBy: UUID,
    locked: Boolean = false
): Budget {
    return Budget(
        id = UUID.randomUUID(),
        tripId = tripId,
        totalAmount = BigDecimal("5000"),
        currency = "USD",
        createdBy = createdBy,
        locked = locked
    )
}
}