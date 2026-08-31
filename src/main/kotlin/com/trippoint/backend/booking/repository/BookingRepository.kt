package com.trippoint.backend.booking.repository

import com.trippoint.backend.booking.entity.Booking
import com.trippoint.backend.booking.model.BookingStatus
import com.trippoint.backend.booking.model.BookingType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BookingRepository : JpaRepository<Booking, UUID> {

    fun findAllByTripIdOrderByStartAtAsc(
        tripId: UUID
    ): List<Booking>

    fun findAllByTripIdAndStatusOrderByStartAtAsc(
        tripId: UUID,
        status: BookingStatus
    ): List<Booking>

    fun findAllByTripIdAndTypeOrderByStartAtAsc(
        tripId: UUID,
        type: BookingType
    ): List<Booking>

    fun findByIdAndTripId(
        id: UUID,
        tripId: UUID
    ): Booking?

    fun findByIdAndTripIdAndCreatedBy(
        id: UUID,
        tripId: UUID,
        createdBy: UUID
    ): Booking?

    fun existsByTripIdAndBookingReference(
        tripId: UUID,
        bookingReference: String
    ): Boolean
}