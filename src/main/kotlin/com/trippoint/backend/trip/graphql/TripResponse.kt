package com.trippoint.backend.trip.graphql

import com.trippoint.backend.trip.entity.Trip
import com.trippoint.backend.trip.model.TripStatus
import java.time.format.DateTimeFormatter

data class TripResponse(
    val id: String,
    val ownerId: String,
    val name: String,
    val destination: String,
    val startDate: String,
    val endDate: String,
    val status: TripStatus,
    val progress: Int,
    val travelers: Int,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {

        private val DATE_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE

        fun from(trip: Trip): TripResponse =
            TripResponse(
                id = trip.id.toString(),
                ownerId = trip.ownerId.toString(),
                name = trip.name,
                destination = trip.destination,
                startDate = trip.startDate.format(DATE_FORMATTER),
                endDate = trip.endDate.format(DATE_FORMATTER),
                status = trip.status,
                progress = 0,
                travelers = 1,
                createdAt = trip.createdAt.toString(),
                updatedAt = trip.updatedAt.toString()
            )
    }
}
