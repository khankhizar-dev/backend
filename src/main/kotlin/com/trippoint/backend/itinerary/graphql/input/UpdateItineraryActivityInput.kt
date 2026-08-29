package com.trippoint.backend.itinerary.graphql.input

import com.trippoint.backend.itinerary.model.ActivityType

data class UpdateItineraryActivityInput(
    val title: String?,
    val description: String?,
    val type: ActivityType?,
    val startTime: String?,
    val endTime: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val sortOrder: Int?,
    val completed: Boolean?
)
