package com.trippoint.backend.activity.repository

import com.trippoint.backend.activity.entity.ActivityLog
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface ActivityLogRepository : JpaRepository<ActivityLog, UUID> {

    fun findAllByTripIdOrderByCreatedAtDescIdDesc(
        tripId: UUID,
        pageable: Pageable
    ): List<ActivityLog>

    @Query(
        """
        SELECT a
        FROM ActivityLog a
        WHERE a.tripId = :tripId
          AND (
              a.createdAt < :createdAt
              OR (
                  a.createdAt = :createdAt
                  AND a.id < :activityId
              )
          )
        ORDER BY a.createdAt DESC, a.id DESC
        """
    )
    fun findActivitiesBeforeCursor(
        @Param("tripId") tripId: UUID,
        @Param("createdAt") createdAt: LocalDateTime,
        @Param("activityId") activityId: UUID,
        pageable: Pageable
    ): List<ActivityLog>
}