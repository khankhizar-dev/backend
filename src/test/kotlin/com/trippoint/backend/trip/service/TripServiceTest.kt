package com.trippoint.backend.trip.service

import com.trippoint.backend.auth.entity.User
import com.trippoint.backend.auth.repository.UserRepository
import com.trippoint.backend.trip.entity.Trip
import com.trippoint.backend.trip.entity.TripMember
import com.trippoint.backend.trip.graphql.input.CreateTripInput
import com.trippoint.backend.trip.graphql.input.TripFilterInput
import com.trippoint.backend.trip.graphql.input.UpdateTripInput
import com.trippoint.backend.trip.model.TripMemberRole
import com.trippoint.backend.trip.model.TripMemberStatus
import com.trippoint.backend.trip.model.TripStatus
import com.trippoint.backend.trip.repository.TripMemberRepository
import com.trippoint.backend.trip.repository.TripRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TripServiceTest {

    private lateinit var tripRepository: TripRepository
    private lateinit var tripMemberRepository: TripMemberRepository
    private lateinit var userRepository: UserRepository
    private lateinit var tripService: TripService

    private val userId = UUID.randomUUID()
    private val otherUserId = UUID.randomUUID()
    private val tripId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        tripRepository = mockk()
        tripMemberRepository = mockk()
        userRepository = mockk()

        tripService = TripService(
            tripRepository = tripRepository,
            tripMemberRepository = tripMemberRepository,
            userRepository = userRepository
        )
    }

    @Test
    fun `createTrip creates trip and owner membership`() {

        val savedTrip = Trip(
            id = tripId,
            ownerId = userId,
            name = "Bali Trip",
            destination = "Bali, Indonesia",
            startDate = LocalDate.of(2026, 9, 10),
            endDate = LocalDate.of(2026, 9, 18),
            status = TripStatus.DRAFT
        )

        every {
            tripRepository.save(any())
        } returns savedTrip

        every {
            tripMemberRepository.save(any())
        } answers {
            firstArg()
        }

        val input = CreateTripInput(
            name = " Bali Trip ",
            destination = " Bali, Indonesia ",
            startDate = "2026-09-10",
            endDate = "2026-09-18"
        )

        val result = tripService.createTrip(
            userId = userId,
            input = input
        )

        assertEquals(tripId.toString(), result.id)
        assertEquals("Bali Trip", result.name)
        assertEquals("Bali, Indonesia", result.destination)

        verify(exactly = 1) {
            tripRepository.save(any())
        }

        verify(exactly = 1) {
            tripMemberRepository.save(
                match {
                    it.tripId == tripId &&
                            it.userId == userId &&
                            it.role == TripMemberRole.OWNER &&
                            it.status == TripMemberStatus.ACCEPTED
                }
            )
        }
    }

    @Test
    fun `createTrip rejects blank name`() {

        val input = CreateTripInput(
            name = "   ",
            destination = "Bali",
            startDate = "2026-09-10",
            endDate = "2026-09-18"
        )

        assertThrows<IllegalArgumentException> {
            tripService.createTrip(userId, input)
        }

        verify(exactly = 0) {
            tripRepository.save(any())
        }

        verify(exactly = 0) {
            tripMemberRepository.save(any())
        }
    }

    @Test
    fun `createTrip rejects blank destination`() {

        val input = CreateTripInput(
            name = "Bali Trip",
            destination = "   ",
            startDate = "2026-09-10",
            endDate = "2026-09-18"
        )

        assertThrows<IllegalArgumentException> {
            tripService.createTrip(userId, input)
        }

        verify(exactly = 0) {
            tripRepository.save(any())
        }
    }

    @Test
    fun `createTrip rejects invalid start date`() {

        val input = CreateTripInput(
            name = "Bali Trip",
            destination = "Bali",
            startDate = "10-09-2026",
            endDate = "2026-09-18"
        )

        assertThrows<Exception> {
            tripService.createTrip(userId, input)
        }

        verify(exactly = 0) {
            tripRepository.save(any())
        }
    }

    @Test
    fun `createTrip rejects end date before start date`() {

        val input = CreateTripInput(
            name = "Bali Trip",
            destination = "Bali",
            startDate = "2026-09-20",
            endDate = "2026-09-10"
        )

        assertThrows<IllegalArgumentException> {
            tripService.createTrip(userId, input)
        }

        verify(exactly = 0) {
            tripRepository.save(any())
        }
    }

    @Test
    fun `createTrip allows same start and end date`() {

        val savedTrip = Trip(
            id = tripId,
            ownerId = userId,
            name = "Day Trip",
            destination = "Delhi",
            startDate = LocalDate.of(2026, 9, 10),
            endDate = LocalDate.of(2026, 9, 10),
            status = TripStatus.DRAFT
        )

        every {
            tripRepository.save(any())
        } returns savedTrip

        every {
            tripMemberRepository.save(any())
        } answers {
            firstArg()
        }

        val input = CreateTripInput(
            name = "Day Trip",
            destination = "Delhi",
            startDate = "2026-09-10",
            endDate = "2026-09-10"
        )

        val result = tripService.createTrip(userId, input)

        assertEquals(tripId.toString(), result.id)
    }

    @Test
    fun `getTrip returns trip for owner`() {

        val trip = Trip(
            id = tripId,
            ownerId = userId,
            name = "Bali Trip",
            destination = "Bali",
            startDate = LocalDate.of(2026, 9, 10),
            endDate = LocalDate.of(2026, 9, 18),
            status = TripStatus.DRAFT
        )

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip

        val result = tripService.getTrip(
            userId = userId,
            tripId = tripId
        )

        assertEquals(tripId.toString(), result.id)
        assertEquals("Bali Trip", result.name)

        verify {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        }
    }

    @Test
    fun `getTrip rejects non owner`() {

        every {
            tripRepository.findByIdAndOwnerId(tripId, otherUserId)
        } returns null

        assertThrows<IllegalArgumentException> {
            tripService.getTrip(
                userId = otherUserId,
                tripId = tripId
            )
        }
    }

    @Test
    fun `getTrips returns owner trips ordered by repository`() {

        val trip1 = Trip(
            id = UUID.randomUUID(),
            ownerId = userId,
            name = "Bali Trip",
            destination = "Bali",
            startDate = LocalDate.of(2026, 9, 10),
            endDate = LocalDate.of(2026, 9, 18),
            status = TripStatus.DRAFT
        )

        val trip2 = Trip(
            id = UUID.randomUUID(),
            ownerId = userId,
            name = "Dubai Trip",
            destination = "Dubai",
            startDate = LocalDate.of(2026, 10, 10),
            endDate = LocalDate.of(2026, 10, 15),
            status = TripStatus.DRAFT
        )

        every {
            tripRepository.findAllByOwnerIdOrderByStartDateAsc(userId)
        } returns listOf(trip1, trip2)

        val result = tripService.getTrips(
            userId = userId,
            filter = null
        )

        assertEquals(2, result.size)
        assertEquals("Bali Trip", result[0].name)
        assertEquals("Dubai Trip", result[1].name)
    }

    @Test
    fun `getTrips filters by search`() {

        val bali = Trip(
            id = UUID.randomUUID(),
            ownerId = userId,
            name = "Summer Vacation",
            destination = "Bali, Indonesia",
            startDate = LocalDate.of(2026, 9, 10),
            endDate = LocalDate.of(2026, 9, 18),
            status = TripStatus.DRAFT
        )

        val dubai = Trip(
            id = UUID.randomUUID(),
            ownerId = userId,
            name = "Business Trip",
            destination = "Dubai",
            startDate = LocalDate.of(2026, 10, 10),
            endDate = LocalDate.of(2026, 10, 15),
            status = TripStatus.DRAFT
        )

        every {
            tripRepository.findAllByOwnerIdOrderByStartDateAsc(userId)
        } returns listOf(bali, dubai)

        val filter = TripFilterInput(
            search = "bali",
            status = null
        )

        val result = tripService.getTrips(
            userId = userId,
            filter = filter
        )

        assertEquals(1, result.size)
        assertEquals("Summer Vacation", result.first().name)
    }

    @Test
    fun `inviteMember creates pending member`() {

        val invitedUserId = UUID.randomUUID()

        val trip = Trip(
            id = tripId,
            ownerId = userId,
            name = "Bali Trip",
            destination = "Bali",
            startDate = LocalDate.of(2026, 9, 10),
            endDate = LocalDate.of(2026, 9, 18),
            status = TripStatus.DRAFT
        )

        val invitedUser = mockk<User>()

        every { invitedUser.id } returns invitedUserId

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip

        every {
            userRepository.findByEmail("friend@example.com")
        } returns invitedUser

        every {
            tripMemberRepository.findByTripIdAndUserId(
                tripId,
                invitedUserId
            )
        } returns null

        every {
            tripMemberRepository.save(any())
        } answers {
            firstArg()
        }

        val result = tripService.inviteMember(
            ownerId = userId,
            tripId = tripId,
            email = " FRIEND@EXAMPLE.COM "
        )

        assertEquals(tripId.toString(), result.tripId)
        assertEquals(invitedUserId.toString(), result.userId)
        assertEquals(TripMemberRole.MEMBER, result.role)
        assertEquals(TripMemberStatus.PENDING, result.status)

        verify {
            userRepository.findByEmail("friend@example.com")
        }

        verify {
            tripMemberRepository.save(
                match {
                    it.tripId == tripId &&
                            it.userId == invitedUserId &&
                            it.role == TripMemberRole.MEMBER &&
                            it.status == TripMemberStatus.PENDING
                }
            )
        }
    }

    @Test
    fun `inviteMember rejects unknown user`() {

        val trip = Trip(
            id = tripId,
            ownerId = userId,
            name = "Bali Trip",
            destination = "Bali",
            startDate = LocalDate.of(2026, 9, 10),
            endDate = LocalDate.of(2026, 9, 18),
            status = TripStatus.DRAFT
        )

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip

        every {
            userRepository.findByEmail("unknown@example.com")
        } returns null

        assertThrows<IllegalArgumentException> {
            tripService.inviteMember(
                userId,
                tripId,
                "unknown@example.com"
            )
        }

        verify(exactly = 0) {
            tripMemberRepository.save(any())
        }
    }

    @Test
    fun `inviteMember rejects owner`() {

        val trip = Trip(
            id = tripId,
            ownerId = userId,
            name = "Bali Trip",
            destination = "Bali",
            startDate = LocalDate.of(2026, 9, 10),
            endDate = LocalDate.of(2026, 9, 18),
            status = TripStatus.DRAFT
        )

        val owner = mockk<User>()
        every { owner.id } returns userId

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip

        every {
            userRepository.findByEmail("owner@example.com")
        } returns owner

        assertThrows<IllegalArgumentException> {
            tripService.inviteMember(
                userId,
                tripId,
                "owner@example.com"
            )
        }

        verify(exactly = 0) {
            tripMemberRepository.save(any())
        }
    }

    @Test
    fun `acceptTripInvitation accepts pending invitation`() {

        val member = TripMember(
            id = UUID.randomUUID(),
            tripId = tripId,
            userId = userId,
            role = TripMemberRole.MEMBER,
            status = TripMemberStatus.PENDING
        )

        every {
            tripMemberRepository.findByTripIdAndUserId(
                tripId,
                userId
            )
        } returns member

        every {
            tripMemberRepository.save(member)
        } returns member

        val result = tripService.acceptTripInvitation(
            userId,
            tripId
        )

        assertEquals(TripMemberStatus.ACCEPTED, result.status)
        assertNotNull(result.joinedAt)

        verify {
            tripMemberRepository.save(member)
        }
    }

    @Test
    fun `acceptTripInvitation rejects accepted invitation`() {

        val member = TripMember(
            id = UUID.randomUUID(),
            tripId = tripId,
            userId = userId,
            role = TripMemberRole.MEMBER,
            status = TripMemberStatus.ACCEPTED
        )

        every {
            tripMemberRepository.findByTripIdAndUserId(
                tripId,
                userId
            )
        } returns member

        assertThrows<IllegalArgumentException> {
            tripService.acceptTripInvitation(
                userId,
                tripId
            )
        }

        verify(exactly = 0) {
            tripMemberRepository.save(any())
        }
    }

    @Test
    fun `acceptTripInvitation rejects missing invitation`() {

        every {
            tripMemberRepository.findByTripIdAndUserId(
                tripId,
                userId
            )
        } returns null

        assertThrows<IllegalArgumentException> {
            tripService.acceptTripInvitation(
                userId,
                tripId
            )
        }

        verify(exactly = 0) {
            tripMemberRepository.save(any())
        }
    }

    @Test
    fun `getMyTripInvitations returns pending invitations`() {

        val member = TripMember(
            id = UUID.randomUUID(),
            tripId = tripId,
            userId = userId,
            role = TripMemberRole.MEMBER,
            status = TripMemberStatus.PENDING
        )

        every {
            tripMemberRepository.findAllByUserIdAndStatus(
                userId,
                TripMemberStatus.PENDING
            )
        } returns listOf(member)

        val result = tripService.getMyTripInvitations(userId)

        assertEquals(1, result.size)
        assertEquals(
            TripMemberStatus.PENDING,
            result.first().status
        )
    }

    @Test
    fun `updateTrip updates trip successfully`() {

        val trip = Trip(
            id = tripId,
            ownerId = userId,
            name = "Old Trip",
            destination = "Dubai",
            startDate = LocalDate.of(2026, 9, 10),
            endDate = LocalDate.of(2026, 9, 15),
            status = TripStatus.DRAFT
        )

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns trip

        every {
            tripRepository.save(trip)
        } returns trip

        val input = UpdateTripInput(
            name = "Updated Trip",
            destination = "Bali",
            startDate = "2026-09-12",
            endDate = "2026-09-20"
        )

        val result = tripService.updateTrip(
            userId,
            tripId,
            input
        )

        assertEquals("Updated Trip", result.name)
        assertEquals("Bali", result.destination)

        verify {
            tripRepository.save(trip)
        }
    }

    @Test
    fun `updateTrip rejects non owner`() {

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                otherUserId
            )
        } returns null

        val input = UpdateTripInput(
            name = "Hacked Trip"
        )

        assertThrows<IllegalArgumentException> {
            tripService.updateTrip(
                otherUserId,
                tripId,
                input
            )
        }

        verify(exactly = 0) {
            tripRepository.save(any())
        }
    }

    @Test
    fun `updateTrip rejects end date before start date`() {

        val trip = Trip(
            id = tripId,
            ownerId = userId,
            name = "Bali Trip",
            destination = "Bali",
            startDate = LocalDate.of(2026, 9, 10),
            endDate = LocalDate.of(2026, 9, 20),
            status = TripStatus.DRAFT
        )

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns trip

        val input = UpdateTripInput(
            startDate = "2026-09-20",
            endDate = "2026-09-10"
        )

        assertThrows<IllegalArgumentException> {
            tripService.updateTrip(
                userId,
                tripId,
                input
            )
        }

        verify(exactly = 0) {
            tripRepository.save(any())
        }
    }

    @Test
    fun `archiveTrip archives trip successfully`() {

        val trip = Trip(
            id = tripId,
            ownerId = userId,
            name = "Bali Trip",
            destination = "Bali",
            startDate = LocalDate.of(2026, 9, 10),
            endDate = LocalDate.of(2026, 9, 18),
            status = TripStatus.UPCOMING
        )

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns trip

        every {
            tripRepository.save(trip)
        } returns trip

        val result = tripService.archiveTrip(
            userId = userId,
            tripId = tripId
        )

        assertEquals(true, result)
        assertEquals(TripStatus.ARCHIVED, trip.status)

        verify(exactly = 1) {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        }

        verify(exactly = 1) {
            tripRepository.save(trip)
        }
    }

    @Test
    fun `archiveTrip rejects nonexistent trip`() {

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns null

        assertThrows<IllegalArgumentException> {
            tripService.archiveTrip(
                userId = userId,
                tripId = tripId
            )
        }

        verify(exactly = 0) {
            tripRepository.save(any())
        }
    }

    @Test
    fun `archiveTrip rejects non owner`() {

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                otherUserId
            )
        } returns null

        assertThrows<IllegalArgumentException> {
            tripService.archiveTrip(
                userId = otherUserId,
                tripId = tripId
            )
        }

        verify(exactly = 0) {
            tripRepository.save(any())
        }
    }

    @Test
    fun `archiveTrip rejects already archived trip`() {

        val trip = Trip(
            id = tripId,
            ownerId = userId,
            name = "Bali Trip",
            destination = "Bali",
            startDate = LocalDate.of(2026, 9, 10),
            endDate = LocalDate.of(2026, 9, 18),
            status = TripStatus.ARCHIVED
        )

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns trip

        assertThrows<IllegalArgumentException> {
            tripService.archiveTrip(
                userId = userId,
                tripId = tripId
            )
        }

        verify(exactly = 0) {
            tripRepository.save(any())
        }

        assertEquals(
            TripStatus.ARCHIVED,
            trip.status
        )
    }

    @Test
    fun `restoreTrip restores archived trip to draft`() {

        val trip = Trip(
            id = tripId,
            ownerId = userId,
            name = "Bali Trip",
            destination = "Bali",
            startDate = LocalDate.of(2026, 9, 10),
            endDate = LocalDate.of(2026, 9, 18),
            status = TripStatus.ARCHIVED
        )

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns trip

        every {
            tripRepository.save(trip)
        } returns trip

        val result = tripService.restoreTrip(
            userId = userId,
            tripId = tripId
        )

        assertEquals(tripId.toString(), result.id)
        assertEquals(TripStatus.DRAFT, trip.status)

        verify(exactly = 1) {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        }

        verify(exactly = 1) {
            tripRepository.save(trip)
        }
    }

    @Test
    fun `restoreTrip rejects nonexistent trip`() {

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns null

        assertThrows<IllegalArgumentException> {
            tripService.restoreTrip(
                userId = userId,
                tripId = tripId
            )
        }

        verify(exactly = 0) {
            tripRepository.save(any())
        }
    }

    @Test
    fun `restoreTrip rejects non owner`() {

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                otherUserId
            )
        } returns null

        assertThrows<IllegalArgumentException> {
            tripService.restoreTrip(
                userId = otherUserId,
                tripId = tripId
            )
        }

        verify(exactly = 0) {
            tripRepository.save(any())
        }
    }

    @Test
    fun `restoreTrip rejects non archived trip`() {

        val trip = Trip(
            id = tripId,
            ownerId = userId,
            name = "Bali Trip",
            destination = "Bali",
            startDate = LocalDate.of(2026, 9, 10),
            endDate = LocalDate.of(2026, 9, 18),
            status = TripStatus.UPCOMING
        )

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns trip

        assertThrows<IllegalArgumentException> {
            tripService.restoreTrip(
                userId = userId,
                tripId = tripId
            )
        }

        verify(exactly = 0) {
            tripRepository.save(any())
        }

        assertEquals(
            TripStatus.UPCOMING,
            trip.status
        )
    }

    @Test
    fun `restoreTrip rejects completed trip`() {

        val trip = Trip(
            id = tripId,
            ownerId = userId,
            name = "Bali Trip",
            destination = "Bali",
            startDate = LocalDate.of(2026, 9, 10),
            endDate = LocalDate.of(2026, 9, 18),
            status = TripStatus.COMPLETED
        )

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns trip

        assertThrows<IllegalArgumentException> {
            tripService.restoreTrip(
                userId = userId,
                tripId = tripId
            )
        }

        verify(exactly = 0) {
            tripRepository.save(any())
        }
    }

    @Test
    fun `deleteTrip deletes trip and its members successfully`() {

        val trip = Trip(
            id = tripId,
            ownerId = userId,
            name = "Bali Trip",
            destination = "Bali",
            startDate = LocalDate.of(2026, 9, 10),
            endDate = LocalDate.of(2026, 9, 18),
            status = TripStatus.DRAFT
        )

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns trip

        every {
            tripMemberRepository.deleteAllByTripId(tripId)
        } just Runs

        every {
            tripRepository.delete(trip)
        } just Runs

        val result = tripService.deleteTrip(
            userId = userId,
            tripId = tripId
        )

        assertEquals(true, result)

        verify(exactly = 1) {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        }

        verify(exactly = 1) {
            tripMemberRepository.deleteAllByTripId(tripId)
        }

        verify(exactly = 1) {
            tripRepository.delete(trip)
        }
    }

    @Test
    fun `deleteTrip rejects nonexistent trip`() {

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns null

        assertThrows<IllegalArgumentException> {
            tripService.deleteTrip(
                userId = userId,
                tripId = tripId
            )
        }

        verify(exactly = 0) {
            tripMemberRepository.deleteAllByTripId(any())
        }

        verify(exactly = 0) {
            tripRepository.delete(any())
        }
    }

    @Test
    fun `deleteTrip rejects non owner`() {

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                otherUserId
            )
        } returns null

        assertThrows<IllegalArgumentException> {
            tripService.deleteTrip(
                userId = otherUserId,
                tripId = tripId
            )
        }

        verify(exactly = 0) {
            tripMemberRepository.deleteAllByTripId(any())
        }

        verify(exactly = 0) {
            tripRepository.delete(any())
        }
    }

    @Test
    fun `deleteTrip deletes members before trip`() {

        val trip = Trip(
            id = tripId,
            ownerId = userId,
            name = "Bali Trip",
            destination = "Bali",
            startDate = LocalDate.of(2026, 9, 10),
            endDate = LocalDate.of(2026, 9, 18),
            status = TripStatus.DRAFT
        )

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns trip

        every {
            tripMemberRepository.deleteAllByTripId(tripId)
        } just Runs

        every {
            tripRepository.delete(trip)
        } just Runs

        tripService.deleteTrip(
            userId = userId,
            tripId = tripId
        )

        verifySequence {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )

            tripMemberRepository.deleteAllByTripId(
                tripId
            )

            tripRepository.delete(
                trip
            )
        }
    }
}