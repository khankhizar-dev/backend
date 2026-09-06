package com.trippoint.backend.trip.service

import com.trippoint.backend.trip.entity.Trip
import com.trippoint.backend.trip.entity.TripMember
import com.trippoint.backend.trip.model.TripMemberRole
import com.trippoint.backend.trip.model.TripMemberStatus
import com.trippoint.backend.trip.model.TripStatus
import com.trippoint.backend.trip.repository.TripMemberRepository
import com.trippoint.backend.trip.repository.TripRepository
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class TripAccessServiceTest {

    private lateinit var tripRepository: TripRepository
    private lateinit var tripMemberRepository: TripMemberRepository
    private lateinit var service: TripAccessService

    private val ownerId = UUID.randomUUID()
    private val memberId = UUID.randomUUID()
    private val pendingMemberId = UUID.randomUUID()
    private val nonMemberId = UUID.randomUUID()
    private val tripId = UUID.randomUUID()

    private lateinit var trip: Trip

    @BeforeEach
    fun setUp() {
        tripRepository = mock()
        tripMemberRepository = mock()

        service = TripAccessService(
            tripRepository,
            tripMemberRepository
        )

        trip = Trip(
            id = tripId,
            ownerId = ownerId,
            name = "Bangalore Trip",
            destination = "Goa",
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2026, 10, 5),
            status = TripStatus.DRAFT
        )

        whenever(tripRepository.findById(tripId))
            .thenReturn(Optional.of(trip))
    }

    @Test
    fun `owner can access trip`() {

        assertDoesNotThrow {
            service.requireMemberAccess(
                tripId = tripId,
                userId = ownerId
            )
        }

        verify(tripRepository).findById(tripId)
        verifyNoInteractions(tripMemberRepository)
    }

    @Test
    fun `accepted member can access trip`() {

        whenever(
            tripMemberRepository.findByTripIdAndUserId(
                tripId,
                memberId
            )
        ).thenReturn(
            TripMember(
                id = UUID.randomUUID(),
                tripId = tripId,
                userId = memberId,
                role = TripMemberRole.MEMBER,
                status = TripMemberStatus.ACCEPTED
            )
        )

        assertDoesNotThrow {
            service.requireMemberAccess(
                tripId = tripId,
                userId = memberId
            )
        }
    }

    @Test
    fun `pending member cannot access trip`() {

        whenever(
            tripMemberRepository.findByTripIdAndUserId(
                tripId,
                pendingMemberId
            )
        ).thenReturn(
            TripMember(
                id = UUID.randomUUID(),
                tripId = tripId,
                userId = pendingMemberId,
                role = TripMemberRole.MEMBER,
                status = TripMemberStatus.PENDING
            )
        )

        assertThrows(IllegalAccessException::class.java) {
            service.requireMemberAccess(
                tripId = tripId,
                userId = pendingMemberId
            )
        }
    }

    @Test
    fun `non member cannot access trip`() {

        whenever(
            tripMemberRepository.findByTripIdAndUserId(
                tripId,
                nonMemberId
            )
        ).thenReturn(null)

        assertThrows(IllegalAccessException::class.java) {
            service.requireMemberAccess(
                tripId = tripId,
                userId = nonMemberId
            )
        }
    }

    @Test
    fun `declined member cannot access trip`() {

        whenever(
            tripMemberRepository.findByTripIdAndUserId(
                tripId,
                pendingMemberId
            )
        ).thenReturn(
            TripMember(
                id = UUID.randomUUID(),
                tripId = tripId,
                userId = pendingMemberId,
                role = TripMemberRole.MEMBER,
                status = TripMemberStatus.DECLINED
            )
        )

        assertThrows(IllegalAccessException::class.java) {
            service.requireMemberAccess(
                tripId = tripId,
                userId = pendingMemberId
            )
        }
    }

    @Test
    fun `non owner cannot use owner access`() {

        assertThrows(IllegalAccessException::class.java) {
            service.requireOwnerAccess(
                tripId = tripId,
                userId = memberId
            )
        }

        verify(tripRepository).findById(tripId)
    }

    @Test
    fun `owner passes owner access`() {

        assertDoesNotThrow {
            service.requireOwnerAccess(
                tripId = tripId,
                userId = ownerId
            )
        }
    }

    @Test
    fun `unknown trip throws trip not found`() {

        val unknownTripId = UUID.randomUUID()

        whenever(tripRepository.findById(unknownTripId))
            .thenReturn(Optional.empty())

        val exception = assertThrows(
            IllegalArgumentException::class.java
        ) {
            service.requireMemberAccess(
                tripId = unknownTripId,
                userId = ownerId
            )
        }

        assert(exception.message == "Trip not found")
    }
}