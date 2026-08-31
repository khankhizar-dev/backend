package com.trippoint.backend.booking.graphql

import com.trippoint.backend.booking.entity.BookingTraveller
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class BookingTravellerResponse(
    val id: UUID,
    val bookingId: UUID,
    val firstName: String,
    val lastName: String?,
    val email: String?,
    val phoneNumber: String?,
    val dateOfBirth: LocalDate?,
    val ticketNumber: String?,
    val seatNumber: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(traveller: BookingTraveller): BookingTravellerResponse =
            BookingTravellerResponse(
                id = traveller.id,
                bookingId = traveller.bookingId,
                firstName = traveller.firstName,
                lastName = traveller.lastName,
                email = traveller.email,
                phoneNumber = traveller.phoneNumber,
                dateOfBirth = traveller.dateOfBirth,
                ticketNumber = traveller.ticketNumber,
                seatNumber = traveller.seatNumber,
                createdAt = traveller.createdAt,
                updatedAt = traveller.updatedAt
            )
    }
}