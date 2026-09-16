package com.trippoint.backend.activity.graphql

import com.trippoint.backend.activity.service.ActivityLogService
import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.auth.service.UserService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import java.util.UUID

@Controller
class ActivityGraphQLController(
    private val activityLogService: ActivityLogService,
    private val userService: UserService
) {

    @QueryMapping
    fun activityLogs(
        @Argument tripId: UUID,
        @Argument limit: Int?,
        @Argument beforeCursor: UUID?
    ): List<ActivityLogResponse> {

        val userId = currentUserId()

        val logs = activityLogService.getActivityLogs(
            userId = userId,
            tripId = tripId,
            limit = limit ?: 50,
            beforeCursor = beforeCursor
        )

        val users = userService.getProfiles(
            logs.map { it.userId }.toSet()
        )

        return logs.map { log ->

            val user = users[log.userId]
                ?: throw IllegalArgumentException(
                    "Activity user not found"
                )

            ActivityLogResponse.from(
                activity = log,
                userName = user.fullName
                    ?: listOfNotNull(
                        user.firstName,
                        user.lastName
                    ).joinToString(" "),
                userPhotoUrl = user.profilePhotoUrl
            )
        }
    }

    private fun currentUserId(): UUID {

        val authentication =
            SecurityContextHolder.getContext().authentication

        val principal = authentication?.principal

        require(principal is UserPrincipal) {
            "Authentication required"
        }

        return principal.userId
    }
}