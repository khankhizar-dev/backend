package com.trippoint.backend.itinerary.graphql

import com.trippoint.backend.itinerary.entity.ItineraryDay
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class ItineraryDayResponse(
    val id: UUID,
    val tripId: UUID,
    val dayNumber: Int,
    val date: LocalDate,
    val title: String?,
    val notes: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(day: ItineraryDay): ItineraryDayResponse =
            ItineraryDayResponse(
                id = day.id,
                tripId = day.tripId,
                dayNumber = day.dayNumber,
                date = day.date,
                title = day.title,
                notes = day.notes,
                createdAt = day.createdAt,
                updatedAt = day.updatedAt
            )
    }
}
