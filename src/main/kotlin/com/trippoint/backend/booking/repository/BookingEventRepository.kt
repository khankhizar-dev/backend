package com.trippoint.backend.booking.repository

import com.trippoint.backend.booking.entity.BookingEvent
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BookingEventRepository : JpaRepository<BookingEvent, UUID> {

    fun findAllByBookingIdOrderByCreatedAtAsc(
        bookingId: UUID
    ): List<BookingEvent>
}