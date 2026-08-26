package com.trippoint.backend.trip.repository

import com.trippoint.backend.trip.entity.TripMember
import com.trippoint.backend.trip.model.TripMemberStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TripMemberRepository : JpaRepository<TripMember, UUID> {

    fun findAllByTripId(
        tripId: UUID
    ): List<TripMember>

    fun findAllByTripIdAndStatus(
        tripId: UUID,
        status: TripMemberStatus
    ): List<TripMember>

    fun findByTripIdAndUserId(
        tripId: UUID,
        userId: UUID
    ): TripMember?

    fun countByTripIdAndStatus(
        tripId: UUID,
        status: TripMemberStatus
    ): Long

    fun findAllByUserIdAndStatus(
        userId: UUID,
        status: TripMemberStatus
    ): List<TripMember>

    fun deleteAllByTripId(
        tripId: UUID
    )
}