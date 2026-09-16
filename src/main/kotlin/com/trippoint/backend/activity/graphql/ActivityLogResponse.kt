package com.trippoint.backend.activity.graphql

import com.trippoint.backend.activity.entity.ActivityLog
import java.time.format.DateTimeFormatter
import java.util.UUID

data class ActivityLogResponse(
    val id: UUID,
    val tripId: UUID,
    val userId: UUID,
    val userName: String,
    val userPhotoUrl: String?,
    val action: String,
    val targetType: String,
    val targetName: String,
    val timestamp: String
) {

    companion object {

        private val formatter =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun from(
            activity: ActivityLog,
            userName: String,
            userPhotoUrl: String?
        ): ActivityLogResponse {

            return ActivityLogResponse(
                id = activity.id,
                tripId = activity.tripId,
                userId = activity.userId,
                userName = userName,
                userPhotoUrl = userPhotoUrl,
                action = activity.action,
                targetType = activity.targetType.name,
                targetName = activity.targetName,
                timestamp = activity.createdAt.format(formatter)
            )
        }
    }
}