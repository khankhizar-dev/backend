package com.trippoint.backend.itinerary.service

import com.trippoint.backend.itinerary.entity.ItineraryActivity
import com.trippoint.backend.itinerary.graphql.ItineraryActivityResponse
import com.trippoint.backend.itinerary.graphql.input.CreateItineraryActivityInput
import com.trippoint.backend.itinerary.graphql.input.UpdateItineraryActivityInput
import com.trippoint.backend.itinerary.repository.ItineraryActivityRepository
import com.trippoint.backend.itinerary.repository.ItineraryDayRepository
import com.trippoint.backend.trip.repository.TripRepository
import com.trippoint.backend.trip.service.TripAccessService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
import java.util.UUID

@Service
class ItineraryActivityService(
    private val itineraryActivityRepository: ItineraryActivityRepository,
    private val itineraryDayRepository: ItineraryDayRepository,
    private val tripAccessService: TripAccessService
) {

    @Transactional
    fun createActivity(
        userId: UUID,
        tripId: UUID,
        itineraryDayId: UUID,
        input: CreateItineraryActivityInput
    ): ItineraryActivityResponse {

        // 1. Verify trip membership
        tripAccessService.requireMemberAccess(
            tripId = tripId,
            userId = userId
        )

        // 2. Verify itinerary day belongs to the trip
        val day = itineraryDayRepository.findById(itineraryDayId)
            .orElseThrow {
                IllegalArgumentException("Itinerary day not found")
            }

        require(day.tripId == tripId) {
            "Itinerary day does not belong to this trip"
        }

        // 3. Validate title
        val title = input.title.trim()

        require(title.isNotBlank()) {
            "Activity title cannot be blank"
        }

        // 4. Parse times
        val startTime = input.startTime
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let(LocalTime::parse)

        val endTime = input.endTime
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let(LocalTime::parse)

        require(
            startTime == null ||
                    endTime == null ||
                    !endTime.isBefore(startTime)
        ) {
            "End time cannot be before start time"
        }

        // 5. Validate sort order
        val sortOrder = input.sortOrder ?: 0

        require(sortOrder >= 0) {
            "Sort order cannot be negative"
        }

        // 6. Validate coordinates
        validateCoordinates(
            input.latitude,
            input.longitude
        )

        val activity = ItineraryActivity(
            itineraryDayId = itineraryDayId,
            title = title,
            description = input.description
                ?.trim()
                ?.takeIf { it.isNotBlank() },
            type = input.type,
            startTime = startTime,
            endTime = endTime,
            location = input.location
                ?.trim()
                ?.takeIf { it.isNotBlank() },
            latitude = input.latitude,
            longitude = input.longitude,
            sortOrder = sortOrder
        )

        return ItineraryActivityResponse.from(
            itineraryActivityRepository.save(activity)
        )
    }

    @Transactional(readOnly = true)
    fun getActivities(
        userId: UUID,
        tripId: UUID,
        itineraryDayId: UUID
    ): List<ItineraryActivityResponse> {

        verifyDayAccess(
            userId,
            tripId,
            itineraryDayId
        )

        return itineraryActivityRepository
            .findAllByItineraryDayIdOrderBySortOrderAsc(
                itineraryDayId
            )
            .map(ItineraryActivityResponse::from)
    }

    @Transactional(readOnly = true)
    fun getActivity(
        userId: UUID,
        tripId: UUID,
        itineraryDayId: UUID,
        activityId: UUID
    ): ItineraryActivityResponse {

        verifyDayAccess(
            userId,
            tripId,
            itineraryDayId
        )

        val activity = itineraryActivityRepository
            .findByIdAndItineraryDayId(
                activityId,
                itineraryDayId
            )
            ?: throw IllegalArgumentException(
                "Itinerary activity not found"
            )

        return ItineraryActivityResponse.from(activity)
    }

    @Transactional
    fun updateActivity(
        userId: UUID,
        tripId: UUID,
        itineraryDayId: UUID,
        activityId: UUID,
        input: UpdateItineraryActivityInput
    ): ItineraryActivityResponse {

        verifyDayAccess(
            userId,
            tripId,
            itineraryDayId
        )

        val activity = itineraryActivityRepository
            .findByIdAndItineraryDayId(
                activityId,
                itineraryDayId
            )
            ?: throw IllegalArgumentException(
                "Itinerary activity not found"
            )

        input.title?.let {
            val title = it.trim()

            require(title.isNotBlank()) {
                "Activity title cannot be blank"
            }

            activity.title = title
        }

        input.description?.let {
            activity.description =
                it.trim().takeIf { value -> value.isNotBlank() }
        }

        input.type?.let {
            activity.type = it
        }

        input.startTime?.let {
            activity.startTime =
                it.trim()
                    .takeIf { value -> value.isNotBlank() }
                    ?.let(LocalTime::parse)
        }

        input.endTime?.let {
            activity.endTime =
                it.trim()
                    .takeIf { value -> value.isNotBlank() }
                    ?.let(LocalTime::parse)
        }

        require(
            activity.startTime == null ||
                    activity.endTime == null ||
                    !activity.endTime!!.isBefore(
                        activity.startTime
                    )
        ) {
            "End time cannot be before start time"
        }

        input.location?.let {
            activity.location =
                it.trim().takeIf { value -> value.isNotBlank() }
        }

        input.latitude?.let {
            activity.latitude = it
        }

        input.longitude?.let {
            activity.longitude = it
        }

        validateCoordinates(
            activity.latitude,
            activity.longitude
        )

        input.sortOrder?.let {
            require(it >= 0) {
                "Sort order cannot be negative"
            }

            activity.sortOrder = it
        }

        input.completed?.let {
            activity.completed = it
        }

        return ItineraryActivityResponse.from(
            itineraryActivityRepository.save(activity)
        )
    }

    @Transactional
    fun deleteActivity(
        userId: UUID,
        tripId: UUID,
        itineraryDayId: UUID,
        activityId: UUID
    ): Boolean {

        verifyDayAccess(
            userId,
            tripId,
            itineraryDayId
        )

        val activity = itineraryActivityRepository
            .findByIdAndItineraryDayId(
                activityId,
                itineraryDayId
            )
            ?: throw IllegalArgumentException(
                "Itinerary activity not found"
            )

        itineraryActivityRepository.delete(activity)

        return true
    }

    @Transactional
    fun markActivityCompleted(
        userId: UUID,
        tripId: UUID,
        itineraryDayId: UUID,
        activityId: UUID,
        completed: Boolean
    ): ItineraryActivityResponse {

        verifyDayAccess(
            userId,
            tripId,
            itineraryDayId
        )

        val activity = itineraryActivityRepository
            .findByIdAndItineraryDayId(
                activityId,
                itineraryDayId
            )
            ?: throw IllegalArgumentException(
                "Itinerary activity not found"
            )

        activity.completed = completed

        return ItineraryActivityResponse.from(
            itineraryActivityRepository.save(activity)
        )
    }

    private fun verifyDayAccess(
        userId: UUID,
        tripId: UUID,
        itineraryDayId: UUID
    ) {

        tripAccessService.requireMemberAccess(
            tripId = tripId,
            userId = userId
        )

        val day = itineraryDayRepository.findById(itineraryDayId)
            .orElseThrow {
                IllegalArgumentException("Itinerary day not found")
            }

        require(day.tripId == tripId) {
            "Itinerary day does not belong to this trip"
        }
    }

    private fun validateCoordinates(
        latitude: Double?,
        longitude: Double?
    ) {

        if (latitude != null) {
            require(latitude in -90.0..90.0) {
                "Latitude must be between -90 and 90"
            }
        }

        if (longitude != null) {
            require(longitude in -180.0..180.0) {
                "Longitude must be between -180 and 180"
            }
        }
    }
}