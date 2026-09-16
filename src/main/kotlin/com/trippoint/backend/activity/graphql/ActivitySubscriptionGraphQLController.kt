package com.trippoint.backend.activity.graphql

import com.trippoint.backend.activity.entity.ActivityLog
import com.trippoint.backend.activity.service.ActivitySubscriptionService
import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.auth.service.UserService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import java.util.UUID

@Controller
class ActivitySubscriptionGraphQLController(
    private val activitySubscriptionService: ActivitySubscriptionService,
    private val userService: UserService
) {

    @SubscriptionMapping
    fun activityLogged(
        @Argument tripId: UUID,
        @AuthenticationPrincipal principal: UserPrincipal
    ): Flux<ActivityLogResponse> {

        return activitySubscriptionService
            .subscribe(
                userId = principal.userId,
                tripId = tripId
            )
            .map { activity ->
                toResponse(activity)
            }
    }

    private fun toResponse(
        activity: ActivityLog
    ): ActivityLogResponse {

        val user = userService.getProfiles(
            setOf(activity.userId)
        )[activity.userId]
            ?: throw IllegalArgumentException(
                "Activity user not found"
            )

        return ActivityLogResponse.from(
            activity = activity,
            userName = user.fullName
                ?: listOfNotNull(
                    user.firstName,
                    user.lastName
                ).joinToString(" "),
            userPhotoUrl = user.profilePhotoUrl
        )
    }
}