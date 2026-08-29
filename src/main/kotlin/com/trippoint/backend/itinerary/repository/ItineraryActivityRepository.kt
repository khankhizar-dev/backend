package com.trippoint.backend.itinerary.repository

import com.trippoint.backend.itinerary.entity.ItineraryActivity
import com.trippoint.backend.itinerary.model.ActivityType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ItineraryActivityRepository : JpaRepository<ItineraryActivity, UUID> {

    fun findAllByItineraryDayIdOrderBySortOrderAsc(
        itineraryDayId: UUID
    ): List<ItineraryActivity>

    fun findByIdAndItineraryDayId(
        id: UUID,
        itineraryDayId: UUID
    ): ItineraryActivity?

    fun existsByItineraryDayIdAndSortOrder(
        itineraryDayId: UUID,
        sortOrder: Int
    ): Boolean

    fun countByItineraryDayId(
        itineraryDayId: UUID
    ): Long

    fun deleteAllByItineraryDayId(
        itineraryDayId: UUID
    )

    fun findAllByItineraryDayIdAndTypeOrderBySortOrderAsc(
        itineraryDayId: UUID,
        type: ActivityType
    ): List<ItineraryActivity>
}