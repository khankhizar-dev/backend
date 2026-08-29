package com.trippoint.backend.itinerary.graphql

import com.trippoint.backend.itinerary.entity.ItineraryActivity
import com.trippoint.backend.itinerary.model.ActivityType
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

data class ItineraryActivityResponse(
    val id: UUID,
    val itineraryDayId: UUID,
    val title: String,
    val description: String?,
    val type: ActivityType,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val sortOrder: Int,
    val completed: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {

    companion object {

        fun from(
            activity: ItineraryActivity
        ): ItineraryActivityResponse =
            ItineraryActivityResponse(
                id = activity.id,
                itineraryDayId = activity.itineraryDayId,
                title = activity.title,
                description = activity.description,
                type = activity.type,
                startTime = activity.startTime,
                endTime = activity.endTime,
                location = activity.location,
                latitude = activity.latitude,
                longitude = activity.longitude,
                sortOrder = activity.sortOrder,
                completed = activity.completed,
                createdAt = activity.createdAt,
                updatedAt = activity.updatedAt
            )
    }
}