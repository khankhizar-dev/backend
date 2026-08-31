package com.trippoint.backend.booking.repository

import com.trippoint.backend.booking.entity.BookingTraveller
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BookingTravellerRepository : JpaRepository<BookingTraveller, UUID> {

    fun findAllByBookingId(
        bookingId: UUID
    ): List<BookingTraveller>

    fun findByIdAndBookingId(
        id: UUID,
        bookingId: UUID
    ): BookingTraveller?

    fun deleteAllByBookingId(
        bookingId: UUID
    )
}