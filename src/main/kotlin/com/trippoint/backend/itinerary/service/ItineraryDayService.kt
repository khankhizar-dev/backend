package com.trippoint.backend.itinerary.service

import com.trippoint.backend.itinerary.entity.ItineraryDay
import com.trippoint.backend.itinerary.graphql.ItineraryDayResponse
import com.trippoint.backend.itinerary.graphql.input.CreateItineraryDayInput
import com.trippoint.backend.itinerary.graphql.input.UpdateItineraryDayInput
import com.trippoint.backend.itinerary.repository.ItineraryDayRepository
import com.trippoint.backend.trip.repository.TripRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class ItineraryDayService(
    private val itineraryDayRepository: ItineraryDayRepository,
    private val tripRepository: TripRepository
) {

    @Transactional
    fun createDay(
        userId: UUID,
        tripId: UUID,
        input: CreateItineraryDayInput
    ): ItineraryDayResponse {

        val trip = tripRepository.findByIdAndOwnerId(
            tripId,
            userId
        ) ?: throw IllegalArgumentException("Trip not found")

        require(input.dayNumber > 0) {
            "Day number must be greater than zero"
        }

        val date = LocalDate.parse(input.date)

        require(
            !date.isBefore(trip.startDate) &&
                    !date.isAfter(trip.endDate)
        ) {
            "Itinerary day must be within the trip dates"
        }

        require(
            !itineraryDayRepository.existsByTripIdAndDayNumber(
                tripId,
                input.dayNumber
            )
        ) {
            "Itinerary day number already exists"
        }

        require(
            !itineraryDayRepository.existsByTripIdAndDate(
                tripId,
                date
            )
        ) {
            "An itinerary already exists for this date"
        }

        val title = input.title
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        val notes = input.notes
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        return ItineraryDayResponse.from(
            itineraryDayRepository.save(
                ItineraryDay(
                    tripId = tripId,
                    dayNumber = input.dayNumber,
                    date = date,
                    title = title,
                    notes = notes
                )
            )
        )
    }

    @Transactional(readOnly = true)
    fun getDays(
        userId: UUID,
        tripId: UUID
    ): List<ItineraryDayResponse> {

        tripRepository.findByIdAndOwnerId(
            tripId,
            userId
        ) ?: throw IllegalArgumentException("Trip not found")

        return itineraryDayRepository
            .findAllByTripIdOrderByDayNumberAsc(tripId)
            .map(ItineraryDayResponse::from)
    }

    @Transactional(readOnly = true)
    fun getDay(
        userId: UUID,
        tripId: UUID,
        dayNumber: Int
    ): ItineraryDayResponse {

        tripRepository.findByIdAndOwnerId(
            tripId,
            userId
        ) ?: throw IllegalArgumentException("Trip not found")

        val day = itineraryDayRepository.findByTripIdAndDayNumber(
            tripId,
            dayNumber
        ) ?: throw IllegalArgumentException("Itinerary day not found")

        return ItineraryDayResponse.from(day)
    }

    @Transactional
    fun updateDay(
        userId: UUID,
        tripId: UUID,
        dayNumber: Int,
        input: UpdateItineraryDayInput
    ): ItineraryDayResponse {

        val trip = tripRepository.findByIdAndOwnerId(
            tripId,
            userId
        ) ?: throw IllegalArgumentException("Trip not found")

        val day = itineraryDayRepository
            .findByTripIdAndDayNumber(
                tripId,
                dayNumber
            )
            ?: throw IllegalArgumentException("Itinerary day not found")

        input.dayNumber?.let { newDayNumber ->

            require(newDayNumber > 0) {
                "Day number must be greater than zero"
            }

            if (newDayNumber != day.dayNumber) {
                require(
                    !itineraryDayRepository.existsByTripIdAndDayNumber(
                        tripId,
                        newDayNumber
                    )
                ) {
                    "Itinerary day number already exists"
                }

                day.dayNumber = newDayNumber
            }
        }

        input.date?.let { dateString ->

            val newDate = LocalDate.parse(dateString)

            require(
                !newDate.isBefore(trip.startDate) &&
                        !newDate.isAfter(trip.endDate)
            ) {
                "Itinerary day must be within the trip dates"
            }

            if (!newDate.isEqual(day.date)) {
                require(
                    !itineraryDayRepository.existsByTripIdAndDate(
                        tripId,
                        newDate
                    )
                ) {
                    "An itinerary already exists for this date"
                }

                day.date = newDate
            }
        }

        input.title?.let {
            day.title = it.trim().takeIf { value -> value.isNotBlank() }
        }

        input.notes?.let {
            day.notes = it.trim().takeIf { value -> value.isNotBlank() }
        }

        day.updatedAt = LocalDateTime.now()

        return ItineraryDayResponse.from(
            itineraryDayRepository.save(day)
        )
    }

    @Transactional
    fun deleteDay(
        userId: UUID,
        tripId: UUID,
        dayNumber: Int
    ): Boolean {

        tripRepository.findByIdAndOwnerId(
            tripId,
            userId
        ) ?: throw IllegalArgumentException("Trip not found")

        val day = itineraryDayRepository
            .findByTripIdAndDayNumber(
                tripId,
                dayNumber
            )
            ?: throw IllegalArgumentException("Itinerary day not found")

        itineraryDayRepository.delete(day)

        return true
    }
}