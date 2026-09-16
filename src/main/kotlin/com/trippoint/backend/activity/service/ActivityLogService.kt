package com.trippoint.backend.activity.service

import com.trippoint.backend.activity.entity.ActivityLog
import com.trippoint.backend.activity.event.ActivityLogCreatedEvent
import com.trippoint.backend.activity.event.ActivityLogEventPublisher
import com.trippoint.backend.activity.model.ActivityTarget
import com.trippoint.backend.activity.repository.ActivityLogRepository
import com.trippoint.backend.trip.service.TripAccessService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ActivityLogService(
    private val activityLogRepository: ActivityLogRepository,
    private val tripAccessService: TripAccessService,
    private val activityLogEventPublisher: ActivityLogEventPublisher
) {

    @Transactional
    fun log(
        tripId: UUID,
        userId: UUID,
        action: String,
        targetType: ActivityTarget,
        targetName: String
    ): ActivityLog {

        require(action.isNotBlank()) {
            "Activity action cannot be blank"
        }

        require(targetName.isNotBlank()) {
            "Activity target name cannot be blank"
        }

        val activity = activityLogRepository.save(
            ActivityLog(
                tripId = tripId,
                userId = userId,
                action = action.trim(),
                targetType = targetType,
                targetName = targetName.trim()
            )
        )

        activityLogEventPublisher.publish(
            ActivityLogCreatedEvent(activity)
        )

        return activity
    }

    @Transactional(readOnly = true)
    fun getActivityLogs(
        userId: UUID,
        tripId: UUID,
        limit: Int,
        beforeCursor: UUID?
    ): List<ActivityLog> {

        tripAccessService.requireMemberAccess(
            tripId = tripId,
            userId = userId
        )

        require(limit > 0) {
            "Limit must be greater than zero"
        }

        val pageSize = limit.coerceAtMost(100)

        val logs = if (beforeCursor == null) {

            activityLogRepository
                .findAllByTripIdOrderByCreatedAtDescIdDesc(
                    tripId = tripId,
                    pageable = PageRequest.of(0, pageSize)
                )

        } else {

            val cursor = activityLogRepository.findById(
                beforeCursor
            ).orElseThrow {
                IllegalArgumentException(
                    "Activity cursor not found"
                )
            }

            require(cursor.tripId == tripId) {
                "Activity cursor does not belong to this trip"
            }

            activityLogRepository.findActivitiesBeforeCursor(
                tripId = tripId,
                createdAt = cursor.createdAt,
                activityId = cursor.id,
                pageable = PageRequest.of(0, pageSize)
            )
        }

        return logs.asReversed()
    }
}