package com.trippoint.backend.itinerary.graphql

import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.itinerary.graphql.input.CreateItineraryActivityInput
import com.trippoint.backend.itinerary.graphql.input.UpdateItineraryActivityInput
import com.trippoint.backend.itinerary.service.ItineraryActivityService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import java.util.UUID

@Controller
class ItineraryActivityGraphQLController(
    private val itineraryActivityService: ItineraryActivityService
) {

    @QueryMapping
    fun itineraryActivities(
        @Argument tripId: UUID,
        @Argument itineraryDayId: UUID,
        authentication: Authentication?
    ): List<ItineraryActivityResponse> {

        return itineraryActivityService.getActivities(
            authenticatedUserId(authentication),
            tripId,
            itineraryDayId
        )
    }

    @QueryMapping
    fun itineraryActivity(
        @Argument tripId: UUID,
        @Argument itineraryDayId: UUID,
        @Argument activityId: UUID,
        authentication: Authentication?
    ): ItineraryActivityResponse {

        return itineraryActivityService.getActivity(
            authenticatedUserId(authentication),
            tripId,
            itineraryDayId,
            activityId
        )
    }

    @MutationMapping
    fun createItineraryActivity(
        @Argument tripId: UUID,
        @Argument itineraryDayId: UUID,
        @Argument input: CreateItineraryActivityInput,
        authentication: Authentication?
    ): ItineraryActivityResponse {

        return itineraryActivityService.createActivity(
            authenticatedUserId(authentication),
            tripId,
            itineraryDayId,
            input
        )
    }

    @MutationMapping
    fun updateItineraryActivity(
        @Argument tripId: UUID,
        @Argument itineraryDayId: UUID,
        @Argument activityId: UUID,
        @Argument input: UpdateItineraryActivityInput,
        authentication: Authentication?
    ): ItineraryActivityResponse {

        return itineraryActivityService.updateActivity(
            authenticatedUserId(authentication),
            tripId,
            itineraryDayId,
            activityId,
            input
        )
    }

    @MutationMapping
    fun deleteItineraryActivity(
        @Argument tripId: UUID,
        @Argument itineraryDayId: UUID,
        @Argument activityId: UUID,
        authentication: Authentication?
    ): Boolean {

        return itineraryActivityService.deleteActivity(
            authenticatedUserId(authentication),
            tripId,
            itineraryDayId,
            activityId
        )
    }

    @MutationMapping
    fun markItineraryActivityCompleted(
        @Argument tripId: UUID,
        @Argument itineraryDayId: UUID,
        @Argument activityId: UUID,
        @Argument completed: Boolean,
        authentication: Authentication?
    ): ItineraryActivityResponse {

        return itineraryActivityService.markActivityCompleted(
            authenticatedUserId(authentication),
            tripId,
            itineraryDayId,
            activityId,
            completed
        )
    }

    private fun authenticatedUserId(
        authentication: Authentication?
    ): UUID {

        val auth = authentication
            ?: SecurityContextHolder.getContext().authentication
            ?: throw IllegalArgumentException("User not authenticated")

        val principal = auth.principal as? UserPrincipal
            ?: throw IllegalArgumentException("Invalid authentication principal")

        return principal.userId
    }
}