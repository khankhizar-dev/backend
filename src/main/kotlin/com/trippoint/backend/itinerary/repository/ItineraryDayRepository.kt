package com.trippoint.backend.itinerary.repository

import com.trippoint.backend.itinerary.entity.ItineraryDay
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface ItineraryDayRepository : JpaRepository<ItineraryDay, UUID> {

    fun findAllByTripIdOrderByDayNumberAsc(
        tripId: UUID
    ): List<ItineraryDay>

    fun findByTripIdAndDayNumber(
        tripId: UUID,
        dayNumber: Int
    ): ItineraryDay?

    fun findByTripIdAndDate(
        tripId: UUID,
        date: LocalDate
    ): ItineraryDay?

    fun existsByTripIdAndDayNumber(
        tripId: UUID,
        dayNumber: Int
    ): Boolean

    fun existsByTripIdAndDate(
        tripId: UUID,
        date: LocalDate
    ): Boolean
}