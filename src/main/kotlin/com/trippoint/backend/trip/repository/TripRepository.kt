package com.trippoint.backend.trip.repository

import com.trippoint.backend.trip.entity.Trip
import com.trippoint.backend.trip.model.TripStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TripRepository : JpaRepository<Trip, UUID> {

    fun findAllByOwnerIdOrderByStartDateAsc(
        ownerId: UUID
    ): List<Trip>

    fun findAllByOwnerIdAndStatusOrderByStartDateAsc(
        ownerId: UUID,
        status: TripStatus
    ): List<Trip>

    fun findByIdAndOwnerId(
        id: UUID,
        ownerId: UUID
    ): Trip?
}