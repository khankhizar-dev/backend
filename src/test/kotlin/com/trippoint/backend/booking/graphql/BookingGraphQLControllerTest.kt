package com.trippoint.backend.booking.graphql

import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.booking.graphql.input.BookingFilterInput
import com.trippoint.backend.booking.graphql.input.CreateBookingInput
import com.trippoint.backend.booking.graphql.input.CreateBookingTravellerInput
import com.trippoint.backend.booking.graphql.input.UpdateBookingInput
import com.trippoint.backend.booking.graphql.input.UpdateBookingTravellerInput
import com.trippoint.backend.booking.model.BookingStatus
import com.trippoint.backend.booking.model.BookingType
import com.trippoint.backend.booking.service.BookingService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.Authentication
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals

class BookingGraphQLControllerTest {

    private lateinit var bookingService: BookingService
    private lateinit var controller: BookingGraphQLController

    private lateinit var authentication: Authentication
    private lateinit var principal: UserPrincipal

    private val userId = UUID.randomUUID()
    private val tripId = UUID.randomUUID()
    private val bookingId = UUID.randomUUID()
    private val travellerId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        bookingService = mockk()
        authentication = mockk()
        principal = mockk()

        controller = BookingGraphQLController(
            bookingService
        )

        every {
            authentication.principal
        } returns principal

        every {
            principal.userId
        } returns userId
    }

    @Test
    fun `bookings returns bookings`() {

        val response = listOf(
            bookingResponse()
        )

        every {
            bookingService.getBookings(
                userId,
                tripId,
                null
            )
        } returns response

        val result = controller.bookings(
            tripId,
            null,
            authentication
        )

        assertEquals(response, result)

        verify(exactly = 1) {
            bookingService.getBookings(
                userId,
                tripId,
                null
            )
        }
    }

    @Test
    fun `bookingTravellers returns travellers`() {

        val response = emptyList<BookingTravellerResponse>()

        every {
            bookingService.getBookingTravellers(
                userId,
                tripId,
                bookingId
            )
        } returns response

        val result = controller.bookingTravellers(
            tripId,
            bookingId,
            authentication
        )

        assertEquals(response, result)

        verify(exactly = 1) {
            bookingService.getBookingTravellers(
                userId,
                tripId,
                bookingId
            )
        }
    }

    @Test
    fun `bookingEvents returns events`() {

        val response = emptyList<BookingEventResponse>()

        every {
            bookingService.getBookingEvents(
                userId,
                tripId,
                bookingId
            )
        } returns response

        val result = controller.bookingEvents(
            tripId,
            bookingId,
            authentication
        )

        assertEquals(response, result)

        verify(exactly = 1) {
            bookingService.getBookingEvents(
                userId,
                tripId,
                bookingId
            )
        }
    }

    @Test
    fun `createBooking creates booking`() {

        val input = CreateBookingInput(
            itineraryDayId = null,
            type = BookingType.FLIGHT,
            title = "Flight to Dubai",
            provider = "Air India",
            bookingReference = "AI294",
            startAt = "2026-10-15T10:00:00",
            endAt = "2026-10-15T13:00:00",
            location = "Delhi Airport",
            amount = BigDecimal("12500"),
            currency = "INR",
            notes = "Window seat",
            details = null
        )

        val response = bookingResponse()

        every {
            bookingService.createBooking(
                userId,
                tripId,
                input
            )
        } returns response

        val result = controller.createBooking(
            tripId,
            input,
            authentication
        )

        assertEquals(response, result)

        verify(exactly = 1) {
            bookingService.createBooking(
                userId,
                tripId,
                input
            )
        }
    }

    @Test
    fun `updateBooking updates booking`() {

        val input = UpdateBookingInput(
            type = BookingType.FLIGHT,
            title = "Updated Flight",
            provider = "Emirates",
            bookingReference = "EK123",
            startAt = "2026-10-15T12:00:00",
            endAt = "2026-10-15T16:00:00",
            location = "Dubai Airport",
            amount = BigDecimal("15000"),
            currency = "AED",
            status = BookingStatus.CONFIRMED,
            notes = "Updated",
            details = null
        )

        val response = bookingResponse()

        every {
            bookingService.updateBooking(
                userId,
                tripId,
                bookingId,
                input
            )
        } returns response

        val result = controller.updateBooking(
            tripId,
            bookingId,
            input,
            authentication
        )

        assertEquals(response, result)

        verify(exactly = 1) {
            bookingService.updateBooking(
                userId,
                tripId,
                bookingId,
                input
            )
        }
    }

    @Test
    fun `deleteBooking deletes booking`() {

        every {
            bookingService.deleteBooking(
                userId,
                tripId,
                bookingId
            )
        } returns true

        val result = controller.deleteBooking(
            tripId,
            bookingId,
            authentication
        )

        assertEquals(true, result)

        verify(exactly = 1) {
            bookingService.deleteBooking(
                userId,
                tripId,
                bookingId
            )
        }
    }

    @Test
    fun `bookings rejects invalid authentication`() {

        every {
            authentication.principal
        } returns "invalid-principal"

        assertThrows<IllegalArgumentException> {
            controller.bookings(
                tripId,
                null,
                authentication
            )
        }

        verify(exactly = 0) {
            bookingService.getBookings(
                any(),
                any(),
                any()
            )
        }
    }

    private fun bookingResponse(): BookingResponse =
        BookingResponse(
            id = bookingId,
            tripId = tripId,
            itineraryDayId = null,
            createdBy = userId,
            type = BookingType.FLIGHT,
            status = BookingStatus.CONFIRMED,
            title = "Flight to Dubai",
            provider = "Air India",
            bookingReference = "AI294",
            startAt = null,
            endAt = null,
            location = "Delhi Airport",
            amount = BigDecimal("12500"),
            currency = "INR",
            source = com.trippoint.backend.booking.model.BookingSource.MANUAL,
            notes = null,
            details = null,
            createdAt = java.time.LocalDateTime.now(),
            updatedAt = java.time.LocalDateTime.now()
        )

    @Test
    fun `addBookingTraveller adds traveller`() {

        val input = CreateBookingTravellerInput(
            firstName = "Khizar",
            lastName = "Khan",
            email = "khizar@example.com",
            phoneNumber = "+919999999999",
            dateOfBirth = "1989-01-12",
            ticketNumber = "TICKET123",
            seatNumber = "12A"
        )

        val response = BookingTravellerResponse(
            id = travellerId,
            bookingId = bookingId,
            firstName = "Khizar",
            lastName = "Khan",
            email = "khizar@example.com",
            phoneNumber = "+919999999999",
            dateOfBirth = java.time.LocalDate.of(1989, 1, 12),
            ticketNumber = "TICKET123",
            seatNumber = "12A",
            createdAt = java.time.LocalDateTime.now(),
            updatedAt = java.time.LocalDateTime.now()
        )

        every {
            bookingService.addBookingTraveller(
                userId,
                tripId,
                bookingId,
                input
            )
        } returns response

        val result = controller.addBookingTraveller(
            tripId,
            bookingId,
            input,
            authentication
        )

        assertEquals(response, result)

        verify(exactly = 1) {
            bookingService.addBookingTraveller(
                userId,
                tripId,
                bookingId,
                input
            )
        }
    }

    @Test
    fun `updateBookingTraveller updates traveller`() {

        val input = UpdateBookingTravellerInput(
            seatNumber = "14A"
        )

        val response = BookingTravellerResponse(
            id = travellerId,
            bookingId = bookingId,
            firstName = "Khizar",
            lastName = "Khan",
            email = "khizar@example.com",
            phoneNumber = null,
            dateOfBirth = null,
            ticketNumber = null,
            seatNumber = "14A",
            createdAt = java.time.LocalDateTime.now(),
            updatedAt = java.time.LocalDateTime.now()
        )

        every {
            bookingService.updateBookingTraveller(
                userId,
                tripId,
                bookingId,
                travellerId,
                input
            )
        } returns response

        val result = controller.updateBookingTraveller(
            tripId,
            bookingId,
            travellerId,
            input,
            authentication
        )

        assertEquals(response, result)

        verify(exactly = 1) {
            bookingService.updateBookingTraveller(
                userId,
                tripId,
                bookingId,
                travellerId,
                input
            )
        }
    }

    @Test
    fun `deleteBookingTraveller deletes traveller`() {

        every {
            bookingService.deleteBookingTraveller(
                userId,
                tripId,
                bookingId,
                travellerId
            )
        } returns true

        val result = controller.deleteBookingTraveller(
            tripId,
            bookingId,
            travellerId,
            authentication
        )

        assertEquals(true, result)

        verify(exactly = 1) {
            bookingService.deleteBookingTraveller(
                userId,
                tripId,
                bookingId,
                travellerId
            )
        }
    }

    @Test
    fun `addBookingTraveller rejects invalid authentication`() {

        every {
            authentication.principal
        } returns "invalid-principal"

        val input = CreateBookingTravellerInput(
            firstName = "Khizar"
        )

        assertThrows<IllegalArgumentException> {
            controller.addBookingTraveller(
                tripId,
                bookingId,
                input,
                authentication
            )
        }

        verify(exactly = 0) {
            bookingService.addBookingTraveller(
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `bookings passes filter to service`() {

        val filter = BookingFilterInput(
            type = BookingType.FLIGHT,
            status = BookingStatus.CONFIRMED,
            search = "Emirates"
        )

        val response = listOf(
            bookingResponse()
        )

        every {
            bookingService.getBookings(
                userId,
                tripId,
                filter
            )
        } returns response

        val result = controller.bookings(
            tripId,
            filter,
            authentication
        )

        assertEquals(response, result)

        verify(exactly = 1) {
            bookingService.getBookings(
                userId,
                tripId,
                filter
            )
        }
    }
}