package com.trippoint.backend.trip.model

import java.time.LocalDateTime
import java.util.UUID

data class TripEvent(
    val id: UUID,
    val tripId: UUID,
    val actorId: UUID,
    val eventType: TripEventType,
    val message: String,
    val metadata: Map<String, Any?> = emptyMap(),
    val createdAt: LocalDateTime
)

enum class TripEventType {
    TRIP_CREATED,
    MEMBER_INVITED,
    MEMBER_JOINED,
    MEMBER_REMOVED,

    ITINERARY_CREATED,
    ITINERARY_UPDATED,

    BOOKING_CREATED,
    BOOKING_UPDATED,
    BOOKING_CANCELLED,

    EXPENSE_CREATED,
    EXPENSE_UPDATED,
    EXPENSE_ARCHIVED,

    BUDGET_CREATED,
    BUDGET_UPDATED
}
