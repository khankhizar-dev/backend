package com.trippoint.backend.booking.graphql

import com.trippoint.backend.booking.entity.Booking
import com.trippoint.backend.booking.model.BookingSource
import com.trippoint.backend.booking.model.BookingStatus
import com.trippoint.backend.booking.model.BookingType
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class BookingResponse(
    val id: UUID,
    val tripId: UUID,
    val itineraryDayId: UUID?,
    val createdBy: UUID,
    val type: BookingType,
    val status: BookingStatus,
    val title: String,
    val provider: String?,
    val bookingReference: String?,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val location: String?,
    val amount: BigDecimal?,
    val currency: String?,
    val source: BookingSource,
    val notes: String?,
    val details: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(booking: Booking): BookingResponse =
            BookingResponse(
                id = booking.id,
                tripId = booking.tripId,
                itineraryDayId = booking.itineraryDayId,
                createdBy = booking.createdBy,
                type = booking.type,
                status = booking.status,
                title = booking.title,
                provider = booking.provider,
                bookingReference = booking.bookingReference,
                startAt = booking.startAt,
                endAt = booking.endAt,
                location = booking.location,
                amount = booking.amount,
                currency = booking.currency,
                source = booking.source,
                notes = booking.notes,
                details = booking.details,
                createdAt = booking.createdAt,
                updatedAt = booking.updatedAt
            )
    }
}