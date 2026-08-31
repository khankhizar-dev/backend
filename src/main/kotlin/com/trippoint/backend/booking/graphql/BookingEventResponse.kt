package com.trippoint.backend.booking.graphql

import com.trippoint.backend.booking.entity.BookingEvent
import java.time.LocalDateTime
import java.util.UUID

data class BookingEventResponse(
    val id: UUID,
    val bookingId: UUID,
    val eventType: String,
    val description: String?,
    val metadata: String?,
    val createdBy: UUID?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(event: BookingEvent): BookingEventResponse =
            BookingEventResponse(
                id = event.id,
                bookingId = event.bookingId,
                eventType = event.eventType,
                description = event.description,
                metadata = event.metadata,
                createdBy = event.createdBy,
                createdAt = event.createdAt
            )
    }
}