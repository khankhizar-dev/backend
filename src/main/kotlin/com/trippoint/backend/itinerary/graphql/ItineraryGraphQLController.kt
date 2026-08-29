package com.trippoint.backend.itinerary.graphql

import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.itinerary.graphql.input.CreateItineraryDayInput
import com.trippoint.backend.itinerary.graphql.input.UpdateItineraryDayInput
import com.trippoint.backend.itinerary.service.ItineraryDayService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import java.util.UUID

@Controller
class ItineraryGraphQLController(
    private val itineraryDayService: ItineraryDayService
) {

    @QueryMapping
    fun itineraryDays(
        @Argument tripId: UUID,
        authentication: Authentication?
    ): List<ItineraryDayResponse> {

        return itineraryDayService.getDays(
            authenticatedUserId(authentication),
            tripId
        )
    }

    @QueryMapping
    fun itineraryDay(
        @Argument tripId: UUID,
        @Argument dayNumber: Int,
        authentication: Authentication?
    ): ItineraryDayResponse {

        return itineraryDayService.getDay(
            authenticatedUserId(authentication),
            tripId,
            dayNumber
        )
    }

    @MutationMapping
    fun createItineraryDay(
        @Argument tripId: UUID,
        @Argument input: CreateItineraryDayInput,
        authentication: Authentication?
    ): ItineraryDayResponse {

        return itineraryDayService.createDay(
            authenticatedUserId(authentication),
            tripId,
            input
        )
    }

    @MutationMapping
    fun updateItineraryDay(
        @Argument tripId: UUID,
        @Argument dayNumber: Int,
        @Argument input: UpdateItineraryDayInput,
        authentication: Authentication?
    ): ItineraryDayResponse {

        return itineraryDayService.updateDay(
            authenticatedUserId(authentication),
            tripId,
            dayNumber,
            input
        )
    }

    @MutationMapping
    fun deleteItineraryDay(
        @Argument tripId: UUID,
        @Argument dayNumber: Int,
        authentication: Authentication?
    ): Boolean {

        return itineraryDayService.deleteDay(
            authenticatedUserId(authentication),
            tripId,
            dayNumber
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