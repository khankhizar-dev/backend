package com.trippoint.backend.trip.service

import com.trippoint.backend.trip.model.TripMemberStatus
import com.trippoint.backend.trip.repository.TripMemberRepository
import com.trippoint.backend.trip.repository.TripRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TripAccessService(
    private val tripRepository: TripRepository,
    private val tripMemberRepository: TripMemberRepository
) {

    fun requireMemberAccess(tripId: UUID, userId: UUID) {
        val trip = tripRepository.findById(tripId)
            .orElseThrow {
                IllegalArgumentException("Trip not found")
            }

        // Trip owner automatically has access.
        if (trip.ownerId == userId) {
            return
        }

        val member = tripMemberRepository.findByTripIdAndUserId(
            tripId,
            userId
        )

        if (member == null || member.status != TripMemberStatus.ACCEPTED) {
            throw IllegalAccessException(
                "You do not have access to this trip"
            )
        }
    }

    fun requireOwnerAccess(tripId: UUID, userId: UUID) {
        val trip = tripRepository.findById(tripId)
            .orElseThrow {
                IllegalArgumentException("Trip not found")
            }

        if (trip.ownerId != userId) {
            throw IllegalAccessException(
                "Only the trip owner can perform this operation"
            )
        }
    }
}