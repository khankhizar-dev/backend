package com.trippoint.backend.booking.service

import com.trippoint.backend.booking.entity.Booking
import com.trippoint.backend.booking.entity.BookingEvent
import com.trippoint.backend.booking.entity.BookingTraveller
import com.trippoint.backend.booking.graphql.input.BookingFilterInput
import com.trippoint.backend.booking.graphql.input.CreateBookingInput
import com.trippoint.backend.booking.graphql.input.CreateBookingTravellerInput
import com.trippoint.backend.booking.graphql.input.UpdateBookingInput
import com.trippoint.backend.booking.graphql.input.UpdateBookingTravellerInput
import com.trippoint.backend.booking.model.BookingSource
import com.trippoint.backend.booking.model.BookingStatus
import com.trippoint.backend.booking.model.BookingType
import com.trippoint.backend.booking.repository.BookingEventRepository
import com.trippoint.backend.booking.repository.BookingRepository
import com.trippoint.backend.booking.repository.BookingTravellerRepository
import com.trippoint.backend.trip.entity.Trip
import com.trippoint.backend.trip.repository.TripRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class BookingServiceTest {

    private lateinit var bookingRepository: BookingRepository
    private lateinit var bookingTravellerRepository: BookingTravellerRepository
    private lateinit var bookingEventRepository: BookingEventRepository
    private lateinit var tripRepository: TripRepository

    private lateinit var service: BookingService

    private val userId = UUID.randomUUID()
    private val otherUserId = UUID.randomUUID()
    private val tripId = UUID.randomUUID()
    private val itineraryDayId = UUID.randomUUID()
    private val bookingId = UUID.randomUUID()
    private val travellerId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        bookingRepository = mockk()
        bookingTravellerRepository = mockk()
        bookingEventRepository = mockk()
        tripRepository = mockk()

        service = BookingService(
            bookingRepository,
            bookingTravellerRepository,
            bookingEventRepository,
            tripRepository
        )
    }

    @Test
    fun `create booking successfully`() {

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.existsByTripIdAndBookingReference(
                tripId,
                "AI294"
            )
        } returns false

        every {
            bookingRepository.save(any())
        } answers { firstArg() }

        every {
            bookingEventRepository.save(any())
        } answers { firstArg() }

        val input = CreateBookingInput(
            itineraryDayId = itineraryDayId.toString(),
            type = BookingType.FLIGHT,
            title = "Flight to Dubai",
            provider = "Air India",
            bookingReference = "AI294",
            startAt = "2026-10-15T10:00:00",
            endAt = "2026-10-15T13:00:00",
            location = "Delhi Airport",
            amount = BigDecimal("12500.00"),
            currency = "inr",
            notes = "Window seat",
            details = null
        )

        val result = service.createBooking(
            userId,
            tripId,
            input
        )

        assertEquals(tripId, result.tripId)
        assertEquals(BookingType.FLIGHT, result.type)
        assertEquals("Flight to Dubai", result.title)
        assertEquals("Air India", result.provider)
        assertEquals("AI294", result.bookingReference)
        assertEquals(BookingSource.MANUAL, result.source)
        assertEquals("INR", result.currency)

        verify(exactly = 1) {
            bookingRepository.save(any())
        }

        verify(exactly = 1) {
            bookingEventRepository.save(any())
        }
    }

    @Test
    fun `create booking rejects non owner`() {

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                otherUserId
            )
        } returns null

        val input = CreateBookingInput(
            itineraryDayId = null,
            type = BookingType.FLIGHT,
            title = "Flight",
            provider = null,
            bookingReference = null,
            startAt = null,
            endAt = null,
            location = null,
            amount = null,
            currency = null,
            notes = null,
            details = null
        )

        assertThrows<IllegalArgumentException> {
            service.createBooking(
                otherUserId,
                tripId,
                input
            )
        }

        verify(exactly = 0) {
            bookingRepository.save(any())
        }
    }

    @Test
    fun `create booking rejects blank title`() {

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        val input = CreateBookingInput(
            itineraryDayId = null,
            type = BookingType.HOTEL,
            title = "   ",
            provider = "Marriott",
            bookingReference = null,
            startAt = null,
            endAt = null,
            location = null,
            amount = null,
            currency = null,
            notes = null,
            details = null
        )

        assertThrows<IllegalArgumentException> {
            service.createBooking(
                userId,
                tripId,
                input
            )
        }

        verify(exactly = 0) {
            bookingRepository.save(any())
        }
    }

    @Test
    fun `create booking rejects duplicate booking reference`() {

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.existsByTripIdAndBookingReference(
                tripId,
                "AI294"
            )
        } returns true

        val input = CreateBookingInput(
            itineraryDayId = null,
            type = BookingType.FLIGHT,
            title = "Flight to Dubai",
            provider = "Air India",
            bookingReference = "AI294",
            startAt = null,
            endAt = null,
            location = null,
            amount = null,
            currency = null,
            notes = null,
            details = null
        )

        assertThrows<IllegalArgumentException> {
            service.createBooking(
                userId,
                tripId,
                input
            )
        }

        verify(exactly = 0) {
            bookingRepository.save(any())
        }
    }

    @Test
    fun `create booking rejects end time before start time`() {

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        val input = CreateBookingInput(
            itineraryDayId = null,
            type = BookingType.FLIGHT,
            title = "Flight",
            provider = null,
            bookingReference = null,
            startAt = "2026-10-15T20:00:00",
            endAt = "2026-10-15T18:00:00",
            location = null,
            amount = null,
            currency = null,
            notes = null,
            details = null
        )

        assertThrows<IllegalArgumentException> {
            service.createBooking(
                userId,
                tripId,
                input
            )
        }

        verify(exactly = 0) {
            bookingRepository.save(any())
        }
    }

    @Test
    fun `create booking rejects negative amount`() {

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        val input = CreateBookingInput(
            itineraryDayId = null,
            type = BookingType.HOTEL,
            title = "Hotel",
            provider = "Marriott",
            bookingReference = null,
            startAt = null,
            endAt = null,
            location = null,
            amount = BigDecimal("-100"),
            currency = "INR",
            notes = null,
            details = null
        )

        assertThrows<IllegalArgumentException> {
            service.createBooking(
                userId,
                tripId,
                input
            )
        }

        verify(exactly = 0) {
            bookingRepository.save(any())
        }
    }

    @Test
    fun `get bookings returns trip bookings`() {

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findAllByTripIdOrderByStartAtAsc(tripId)
        } returns listOf(
            booking(
                title = "Flight to Dubai"
            ),
            booking(
                title = "Dubai Hotel"
            )
        )

        val result = service.getBookings(
            userId,
            tripId,
            null
        )

        assertEquals(2, result.size)
        assertEquals("Flight to Dubai", result[0].title)
        assertEquals("Dubai Hotel", result[1].title)
    }

    @Test
    fun `get booking rejects missing booking`() {

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(
                bookingId,
                tripId
            )
        } returns null

        assertThrows<IllegalArgumentException> {
            service.getBooking(
                userId,
                tripId,
                bookingId
            )
        }
    }

    @Test
    fun `update booking successfully`() {

        val existingBooking = booking()

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(
                bookingId,
                tripId
            )
        } returns existingBooking

        every {
            bookingRepository.save(any())
        } answers { firstArg() }

        every {
            bookingEventRepository.save(any())
        } answers { firstArg() }

        val input = UpdateBookingInput(
            title = "Updated Flight",
            provider = "Emirates",
            bookingReference = null,
            startAt = "2026-10-15T12:00:00",
            endAt = "2026-10-15T16:00:00",
            location = "Dubai Airport",
            amount = BigDecimal("15000"),
            currency = "AED",
            status = BookingStatus.CONFIRMED,
            notes = "Updated booking",
            details = null
        )

        val result = service.updateBooking(
            userId,
            tripId,
            bookingId,
            input
        )

        assertEquals("Updated Flight", result.title)
        assertEquals("Emirates", result.provider)
        assertEquals(BookingStatus.CONFIRMED, result.status)
        assertEquals("AED", result.currency)
        assertEquals(BigDecimal("15000"), result.amount)

        verify(exactly = 1) {
            bookingRepository.save(existingBooking)
        }

        verify(exactly = 1) {
            bookingEventRepository.save(any())
        }
    }

    @Test
    fun `update booking rejects non owner`() {

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                otherUserId
            )
        } returns null

        val input = UpdateBookingInput(
            title = "Hacked Booking"
        )

        assertThrows<IllegalArgumentException> {
            service.updateBooking(
                otherUserId,
                tripId,
                bookingId,
                input
            )
        }

        verify(exactly = 0) {
            bookingRepository.save(any())
        }
    }

    @Test
    fun `update booking rejects missing booking`() {

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(
                bookingId,
                tripId
            )
        } returns null

        val input = UpdateBookingInput(
            title = "Updated Booking"
        )

        assertThrows<IllegalArgumentException> {
            service.updateBooking(
                userId,
                tripId,
                bookingId,
                input
            )
        }

        verify(exactly = 0) {
            bookingRepository.save(any())
        }
    }

    @Test
    fun `update booking rejects end time before start time`() {

        val existingBooking = booking()

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(
                bookingId,
                tripId
            )
        } returns existingBooking

        val input = UpdateBookingInput(
            startAt = "2026-10-15T20:00:00",
            endAt = "2026-10-15T18:00:00"
        )

        assertThrows<IllegalArgumentException> {
            service.updateBooking(
                userId,
                tripId,
                bookingId,
                input
            )
        }

        verify(exactly = 0) {
            bookingRepository.save(any())
        }
    }

    @Test
    fun `update booking rejects duplicate booking reference`() {

        val existingBooking = booking().apply {
            bookingReference = "OLD123"
        }

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(
                bookingId,
                tripId
            )
        } returns existingBooking

        every {
            bookingRepository.existsByTripIdAndBookingReference(
                tripId,
                "NEW123"
            )
        } returns true

        val input = UpdateBookingInput(
            bookingReference = "NEW123"
        )

        assertThrows<IllegalArgumentException> {
            service.updateBooking(
                userId,
                tripId,
                bookingId,
                input
            )
        }

        verify(exactly = 0) {
            bookingRepository.save(any())
        }
    }

    @Test
    fun `delete booking successfully`() {

        val existingBooking = booking()

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(
                bookingId,
                tripId
            )
        } returns existingBooking

        every {
            bookingEventRepository
                .findAllByBookingIdOrderByCreatedAtAsc(bookingId)
        } returns emptyList()

        every {
            bookingEventRepository.deleteAll(emptyList())
        } just Runs

        every {
            bookingTravellerRepository.deleteAllByBookingId(bookingId)
        } just Runs

        every {
            bookingRepository.delete(existingBooking)
        } just Runs

        val result = service.deleteBooking(
            userId,
            tripId,
            bookingId
        )

        assertEquals(true, result)

        verify(exactly = 1) {
            bookingTravellerRepository.deleteAllByBookingId(bookingId)
        }

        verify(exactly = 1) {
            bookingRepository.delete(existingBooking)
        }
    }

    @Test
    fun `delete booking rejects missing booking`() {

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(
                bookingId,
                tripId
            )
        } returns null

        assertThrows<IllegalArgumentException> {
            service.deleteBooking(
                userId,
                tripId,
                bookingId
            )
        }

        verify(exactly = 0) {
            bookingRepository.delete(any())
        }
    }

    @Test
    fun `get booking events returns events`() {

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(
                bookingId,
                tripId
            )
        } returns booking()

        val event = BookingEvent(
            id = UUID.randomUUID(),
            bookingId = bookingId,
            eventType = "BOOKING_CREATED",
            description = "Booking created",
            createdBy = userId
        )

        every {
            bookingEventRepository
                .findAllByBookingIdOrderByCreatedAtAsc(bookingId)
        } returns listOf(event)

        val result = service.getBookingEvents(
            userId,
            tripId,
            bookingId
        )

        assertEquals(1, result.size)
        assertEquals("BOOKING_CREATED", result[0].eventType)
    }

    @Test
    fun `get booking travellers returns travellers`() {

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(
                bookingId,
                tripId
            )
        } returns booking()

        val traveller = BookingTraveller(
            id = UUID.randomUUID(),
            bookingId = bookingId,
            firstName = "Khizar",
            lastName = "Khan"
        )

        every {
            bookingTravellerRepository.findAllByBookingId(bookingId)
        } returns listOf(traveller)

        val result = service.getBookingTravellers(
            userId,
            tripId,
            bookingId
        )

        assertEquals(1, result.size)
        assertEquals("Khizar", result[0].firstName)
    }

    @Test
    fun `add booking traveller successfully`() {

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(bookingId, tripId)
        } returns booking()

        every {
            bookingTravellerRepository.save(any())
        } answers { firstArg() }

        val input = CreateBookingTravellerInput(
            firstName = "Khizar",
            lastName = "Khan",
            email = "khizar@example.com",
            phoneNumber = "+919999999999",
            dateOfBirth = "1989-01-12",
            ticketNumber = "TICKET123",
            seatNumber = "12A"
        )

        val result = service.addBookingTraveller(
            userId,
            tripId,
            bookingId,
            input
        )

        assertEquals("Khizar", result.firstName)
        assertEquals("Khan", result.lastName)
        assertEquals("khizar@example.com", result.email)
        assertEquals("12A", result.seatNumber)

        verify(exactly = 1) {
            bookingTravellerRepository.save(any())
        }
    }

    @Test
    fun `add booking traveller rejects blank first name`() {

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(bookingId, tripId)
        } returns booking()

        val input = CreateBookingTravellerInput(
            firstName = "   "
        )

        assertThrows<IllegalArgumentException> {
            service.addBookingTraveller(
                userId,
                tripId,
                bookingId,
                input
            )
        }

        verify(exactly = 0) {
            bookingTravellerRepository.save(any())
        }
    }

    @Test
    fun `update booking traveller successfully`() {

        val traveller = BookingTraveller(
            id = travellerId,
            bookingId = bookingId,
            firstName = "Khizar",
            lastName = "Khan",
            seatNumber = "12A"
        )

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(bookingId, tripId)
        } returns booking()

        every {
            bookingTravellerRepository.findByIdAndBookingId(
                travellerId,
                bookingId
            )
        } returns traveller

        every {
            bookingTravellerRepository.save(any())
        } answers { firstArg() }

        val input = UpdateBookingTravellerInput(
            seatNumber = "14A"
        )

        val result = service.updateBookingTraveller(
            userId,
            tripId,
            bookingId,
            travellerId,
            input
        )

        assertEquals("Khizar", result.firstName)
        assertEquals("14A", result.seatNumber)

        verify(exactly = 1) {
            bookingTravellerRepository.save(traveller)
        }
    }

    @Test
    fun `update booking traveller rejects missing traveller`() {

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(bookingId, tripId)
        } returns booking()

        every {
            bookingTravellerRepository.findByIdAndBookingId(
                travellerId,
                bookingId
            )
        } returns null

        assertThrows<IllegalArgumentException> {
            service.updateBookingTraveller(
                userId,
                tripId,
                bookingId,
                travellerId,
                UpdateBookingTravellerInput(
                    seatNumber = "14A"
                )
            )
        }

        verify(exactly = 0) {
            bookingTravellerRepository.save(any())
        }
    }

    @Test
    fun `delete booking traveller successfully`() {

        val traveller = BookingTraveller(
            id = travellerId,
            bookingId = bookingId,
            firstName = "Khizar"
        )

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(bookingId, tripId)
        } returns booking()

        every {
            bookingTravellerRepository.findByIdAndBookingId(
                travellerId,
                bookingId
            )
        } returns traveller

        every {
            bookingTravellerRepository.delete(traveller)
        } just Runs

        val result = service.deleteBookingTraveller(
            userId,
            tripId,
            bookingId,
            travellerId
        )

        assertEquals(true, result)

        verify(exactly = 1) {
            bookingTravellerRepository.delete(traveller)
        }
    }

    @Test
    fun `delete booking traveller rejects missing traveller`() {

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(bookingId, tripId)
        } returns booking()

        every {
            bookingTravellerRepository.findByIdAndBookingId(
                travellerId,
                bookingId
            )
        } returns null

        assertThrows<IllegalArgumentException> {
            service.deleteBookingTraveller(
                userId,
                tripId,
                bookingId,
                travellerId
            )
        }

        verify(exactly = 0) {
            bookingTravellerRepository.delete(any())
        }
    }

    private fun trip(): Trip =
        Trip(
            id = tripId,
            ownerId = userId,
            name = "Dubai Trip",
            destination = "Dubai",
            startDate = LocalDate.of(2026, 10, 15),
            endDate = LocalDate.of(2026, 10, 20)
        )

    private fun booking(
        title: String = "Test Booking"
    ): Booking =
        Booking(
            id = bookingId,
            tripId = tripId,
            createdBy = userId,
            type = BookingType.FLIGHT,
            title = title
        )

    @Test
    fun `get bookings filters by type`() {

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findAllByTripIdOrderByStartAtAsc(tripId)
        } returns listOf(
            booking(
                title = "Flight",
            ).apply {
                type = BookingType.FLIGHT
            },
            booking(
                title = "Hotel"
            ).apply {
                type = BookingType.HOTEL
            }
        )

        val result = service.getBookings(
            userId,
            tripId,
            BookingFilterInput(
                type = BookingType.FLIGHT
            )
        )

        assertEquals(1, result.size)
        assertEquals("Flight", result[0].title)
    }

    @Test
    fun `get bookings filters by status`() {

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findAllByTripIdOrderByStartAtAsc(tripId)
        } returns listOf(
            booking("Confirmed").apply {
                status = BookingStatus.CONFIRMED
            },
            booking("Pending").apply {
                status = BookingStatus.PENDING
            }
        )

        val result = service.getBookings(
            userId,
            tripId,
            BookingFilterInput(
                status = BookingStatus.CONFIRMED
            )
        )

        assertEquals(1, result.size)
        assertEquals("Confirmed", result[0].title)
    }

    @Test
    fun `get bookings searches title provider and reference`() {

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findAllByTripIdOrderByStartAtAsc(tripId)
        } returns listOf(
            booking("Dubai Flight").apply {
                provider = "Emirates"
                bookingReference = "EK521"
            },
            booking("Dubai Hotel").apply {
                provider = "Marriott"
                bookingReference = "HT123"
            }
        )

        val result = service.getBookings(
            userId,
            tripId,
            BookingFilterInput(
                search = "EK521"
            )
        )

        assertEquals(1, result.size)
        assertEquals("Dubai Flight", result[0].title)
    }

    @Test
    fun `booking can transition from pending to confirmed`() {

        val existingBooking = booking().apply {
            status = BookingStatus.PENDING
        }

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(
                bookingId,
                tripId
            )
        } returns existingBooking

        every {
            bookingRepository.save(any())
        } answers { firstArg() }

        every {
            bookingEventRepository.save(any())
        } answers { firstArg() }

        val result = service.updateBooking(
            userId,
            tripId,
            bookingId,
            UpdateBookingInput(
                status = BookingStatus.CONFIRMED
            )
        )

        assertEquals(
            BookingStatus.CONFIRMED,
            result.status
        )
    }

    @Test
    fun `booking can transition from confirmed to completed`() {

        val existingBooking = booking().apply {
            status = BookingStatus.CONFIRMED
        }

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(
                bookingId,
                tripId
            )
        } returns existingBooking

        every {
            bookingRepository.save(any())
        } answers { firstArg() }

        every {
            bookingEventRepository.save(any())
        } answers { firstArg() }

        val result = service.updateBooking(
            userId,
            tripId,
            bookingId,
            UpdateBookingInput(
                status = BookingStatus.COMPLETED
            )
        )

        assertEquals(
            BookingStatus.COMPLETED,
            result.status
        )
    }

    @Test
    fun `booking rejects invalid status transition`() {

        val existingBooking = booking().apply {
            status = BookingStatus.COMPLETED
        }

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(
                bookingId,
                tripId
            )
        } returns existingBooking

        assertThrows<IllegalArgumentException> {

            service.updateBooking(
                userId,
                tripId,
                bookingId,
                UpdateBookingInput(
                    status = BookingStatus.PENDING
                )
            )
        }

        verify(exactly = 0) {
            bookingRepository.save(any())
        }
    }

    @Test
    fun `cancelled booking cannot be confirmed`() {

        val existingBooking = booking().apply {
            status = BookingStatus.CANCELLED
        }

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(
                bookingId,
                tripId
            )
        } returns existingBooking

        assertThrows<IllegalArgumentException> {

            service.updateBooking(
                userId,
                tripId,
                bookingId,
                UpdateBookingInput(
                    status = BookingStatus.CONFIRMED
                )
            )
        }
    }

    @Test
    fun `pending booking can be confirmed`() {
        val existingBooking = booking().apply {
            status = BookingStatus.PENDING
        }

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(bookingId, tripId)
        } returns existingBooking

        every {
            bookingRepository.save(any())
        } answers { firstArg() }

        every {
            bookingEventRepository.save(any())
        } answers { firstArg() }

        val result = service.updateBooking(
            userId,
            tripId,
            bookingId,
            UpdateBookingInput(
                status = BookingStatus.CONFIRMED
            )
        )

        assertEquals(BookingStatus.CONFIRMED, result.status)

        verify {
            bookingEventRepository.save(
                match {
                    it.eventType == "STATUS_CHANGED"
                }
            )
        }
    }

    @Test
    fun `pending booking can be cancelled`() {
        val existingBooking = booking().apply {
            status = BookingStatus.PENDING
        }

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(bookingId, tripId)
        } returns existingBooking

        every {
            bookingRepository.save(any())
        } answers { firstArg() }

        every {
            bookingEventRepository.save(any())
        } answers { firstArg() }

        val result = service.updateBooking(
            userId,
            tripId,
            bookingId,
            UpdateBookingInput(
                status = BookingStatus.CANCELLED
            )
        )

        assertEquals(BookingStatus.CANCELLED, result.status)
    }

    @Test
    fun `confirmed booking can be completed`() {
        val existingBooking = booking().apply {
            status = BookingStatus.CONFIRMED
        }

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(bookingId, tripId)
        } returns existingBooking

        every {
            bookingRepository.save(any())
        } answers { firstArg() }

        every {
            bookingEventRepository.save(any())
        } answers { firstArg() }

        val result = service.updateBooking(
            userId,
            tripId,
            bookingId,
            UpdateBookingInput(
                status = BookingStatus.COMPLETED
            )
        )

        assertEquals(BookingStatus.COMPLETED, result.status)
    }

    @Test
    fun `confirmed booking can be refunded`() {
        val existingBooking = booking().apply {
            status = BookingStatus.CONFIRMED
        }

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(bookingId, tripId)
        } returns existingBooking

        every {
            bookingRepository.save(any())
        } answers { firstArg() }

        every {
            bookingEventRepository.save(any())
        } answers { firstArg() }

        val result = service.updateBooking(
            userId,
            tripId,
            bookingId,
            UpdateBookingInput(
                status = BookingStatus.REFUNDED
            )
        )

        assertEquals(BookingStatus.REFUNDED, result.status)
    }

    @Test
    fun `completed booking cannot return to pending`() {
        val existingBooking = booking().apply {
            status = BookingStatus.COMPLETED
        }

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(bookingId, tripId)
        } returns existingBooking

        assertThrows<IllegalArgumentException> {
            service.updateBooking(
                userId,
                tripId,
                bookingId,
                UpdateBookingInput(
                    status = BookingStatus.PENDING
                )
            )
        }

        verify(exactly = 0) {
            bookingRepository.save(any())
        }
    }

    @Test
    fun `status only update creates status event but not booking updated event`() {
        val existingBooking = booking().apply {
            status = BookingStatus.PENDING
        }

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(bookingId, tripId)
        } returns existingBooking

        every {
            bookingRepository.save(any())
        } answers { firstArg() }

        every {
            bookingEventRepository.save(any())
        } answers { firstArg() }

        service.updateBooking(
            userId,
            tripId,
            bookingId,
            UpdateBookingInput(
                status = BookingStatus.CONFIRMED
            )
        )

        verify(exactly = 1) {
            bookingEventRepository.save(
                match {
                    it.eventType == "STATUS_CHANGED"
                }
            )
        }

        verify(exactly = 0) {
            bookingEventRepository.save(
                match {
                    it.eventType == "BOOKING_UPDATED"
                }
            )
        }
    }

    @Test
    fun `title update creates booking updated event`() {
        val existingBooking = booking().apply {
            status = BookingStatus.PENDING
        }

        every {
            tripRepository.findByIdAndOwnerId(tripId, userId)
        } returns trip()

        every {
            bookingRepository.findByIdAndTripId(bookingId, tripId)
        } returns existingBooking

        every {
            bookingRepository.save(any())
        } answers { firstArg() }

        every {
            bookingEventRepository.save(any())
        } answers { firstArg() }

        service.updateBooking(
            userId,
            tripId,
            bookingId,
            UpdateBookingInput(
                title = "Updated Flight"
            )
        )

        verify(exactly = 1) {
            bookingEventRepository.save(
                match {
                    it.eventType == "BOOKING_UPDATED"
                }
            )
        }

        verify(exactly = 0) {
            bookingEventRepository.save(
                match {
                    it.eventType == "STATUS_CHANGED"
                }
            )
        }
    }

    @Test
    fun `get booking rejects trip owned by another user`() {

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns null

        assertThrows<IllegalArgumentException> {
            service.getBooking(
                userId,
                tripId,
                bookingId
            )
        }

        verify(exactly = 0) {
            bookingRepository.findByIdAndTripId(
                any(),
                any()
            )
        }
    }

    @Test
    fun `update booking rejects trip owned by another user`() {

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns null

        assertThrows<IllegalArgumentException> {
            service.updateBooking(
                userId,
                tripId,
                bookingId,
                UpdateBookingInput(
                    title = "Hacked Booking"
                )
            )
        }

        verify(exactly = 0) {
            bookingRepository.findByIdAndTripId(
                any(),
                any()
            )
        }

        verify(exactly = 0) {
            bookingRepository.save(any())
        }
    }

    @Test
    fun `delete booking rejects trip owned by another user`() {

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns null

        assertThrows<IllegalArgumentException> {
            service.deleteBooking(
                userId,
                tripId,
                bookingId
            )
        }

        verify(exactly = 0) {
            bookingRepository.findByIdAndTripId(
                any(),
                any()
            )
        }

        verify(exactly = 0) {
            bookingRepository.delete(any())
        }
    }

    @Test
    fun `add booking traveller rejects unauthorized trip`() {

        every {
            tripRepository.findByIdAndOwnerId(
                tripId,
                userId
            )
        } returns null

        val input = CreateBookingTravellerInput(
            firstName = "John"
        )

        assertThrows<IllegalArgumentException> {
            service.addBookingTraveller(
                userId,
                tripId,
                bookingId,
                input
            )
        }

        verify(exactly = 0) {
            bookingTravellerRepository.save(any())
        }
    }
}