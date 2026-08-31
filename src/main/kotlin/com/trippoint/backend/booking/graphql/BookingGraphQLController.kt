package com.trippoint.backend.booking.graphql

import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.booking.graphql.input.BookingFilterInput
import com.trippoint.backend.booking.graphql.input.CreateBookingInput
import com.trippoint.backend.booking.graphql.input.CreateBookingTravellerInput
import com.trippoint.backend.booking.graphql.input.UpdateBookingInput
import com.trippoint.backend.booking.graphql.input.UpdateBookingTravellerInput
import com.trippoint.backend.booking.service.BookingService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import java.util.UUID

@Controller
class BookingGraphQLController(
    private val bookingService: BookingService
) {

    @QueryMapping
    fun bookings(
        @Argument tripId: UUID,
        @Argument filter: BookingFilterInput?,
        authentication: Authentication?
    ): List<BookingResponse> {

        return bookingService.getBookings(
            authenticatedUserId(authentication),
            tripId,
            filter
        )
    }

    @QueryMapping
    fun bookingTravellers(
        @Argument tripId: UUID,
        @Argument bookingId: UUID,
        authentication: Authentication?
    ): List<BookingTravellerResponse> {

        return bookingService.getBookingTravellers(
            authenticatedUserId(authentication),
            tripId,
            bookingId
        )
    }

    @QueryMapping
    fun bookingEvents(
        @Argument tripId: UUID,
        @Argument bookingId: UUID,
        authentication: Authentication?
    ): List<BookingEventResponse> {

        return bookingService.getBookingEvents(
            authenticatedUserId(authentication),
            tripId,
            bookingId
        )
    }

    @MutationMapping
    fun createBooking(
        @Argument tripId: UUID,
        @Argument input: CreateBookingInput,
        authentication: Authentication?
    ): BookingResponse {

        return bookingService.createBooking(
            authenticatedUserId(authentication),
            tripId,
            input
        )
    }

    @MutationMapping
    fun updateBooking(
        @Argument tripId: UUID,
        @Argument bookingId: UUID,
        @Argument input: UpdateBookingInput,
        authentication: Authentication?
    ): BookingResponse {

        return bookingService.updateBooking(
            authenticatedUserId(authentication),
            tripId,
            bookingId,
            input
        )
    }

    @MutationMapping
    fun deleteBooking(
        @Argument tripId: UUID,
        @Argument bookingId: UUID,
        authentication: Authentication?
    ): Boolean {

        return bookingService.deleteBooking(
            authenticatedUserId(authentication),
            tripId,
            bookingId
        )
    }

    private fun authenticatedUserId(
        authentication: Authentication?
    ): UUID {

        val auth = authentication
            ?: SecurityContextHolder
                .getContext()
                .authentication
            ?: throw IllegalArgumentException("User not authenticated")

        val principal = auth.principal as? UserPrincipal
            ?: throw IllegalArgumentException(
                "Invalid authentication principal"
            )

        return principal.userId
    }

    @MutationMapping
    fun addBookingTraveller(
        @Argument tripId: UUID,
        @Argument bookingId: UUID,
        @Argument input: CreateBookingTravellerInput,
        authentication: Authentication?
    ): BookingTravellerResponse {

        return bookingService.addBookingTraveller(
            authenticatedUserId(authentication),
            tripId,
            bookingId,
            input
        )
    }

    @MutationMapping
    fun updateBookingTraveller(
        @Argument tripId: UUID,
        @Argument bookingId: UUID,
        @Argument travellerId: UUID,
        @Argument input: UpdateBookingTravellerInput,
        authentication: Authentication?
    ): BookingTravellerResponse {

        return bookingService.updateBookingTraveller(
            authenticatedUserId(authentication),
            tripId,
            bookingId,
            travellerId,
            input
        )
    }

    @MutationMapping
    fun deleteBookingTraveller(
        @Argument tripId: UUID,
        @Argument bookingId: UUID,
        @Argument travellerId: UUID,
        authentication: Authentication?
    ): Boolean {

        return bookingService.deleteBookingTraveller(
            authenticatedUserId(authentication),
            tripId,
            bookingId,
            travellerId
        )
    }
}