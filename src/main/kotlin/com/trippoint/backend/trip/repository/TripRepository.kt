package com.trippoint.backend.trip.repository

import com.trippoint.backend.trip.entity.Trip
import com.trippoint.backend.trip.model.TripMemberStatus
import com.trippoint.backend.trip.model.TripStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
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

    @Query(
        """
    SELECT DISTINCT t
    FROM Trip t
    LEFT JOIN TripMember tm
        ON tm.tripId = t.id
        AND tm.userId = :userId
        AND tm.status = :status
    WHERE t.ownerId = :userId
       OR tm.id IS NOT NULL
    ORDER BY t.startDate ASC
    """
    )
    fun findAllAccessibleTrips(
        @Param("userId") userId: UUID,
        @Param("status") status: TripMemberStatus
    ): List<Trip>

    @Query(
        """
    SELECT DISTINCT t
    FROM Trip t
    LEFT JOIN TripMember tm
        ON tm.tripId = t.id
        AND tm.userId = :userId
        AND tm.status = :memberStatus
    WHERE (t.ownerId = :userId OR tm.id IS NOT NULL)
      AND t.status = :tripStatus
    ORDER BY t.startDate ASC
    """
    )
    fun findAllAccessibleTripsByStatus(
        @Param("userId") userId: UUID,
        @Param("memberStatus") memberStatus: TripMemberStatus,
        @Param("tripStatus") tripStatus: TripStatus
    ): List<Trip>
}