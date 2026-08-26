package com.trippoint.backend.trip.graphql

import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.trip.graphql.input.CreateTripInput
import com.trippoint.backend.trip.graphql.input.InviteTripMemberInput
import com.trippoint.backend.trip.graphql.input.TripFilterInput
import com.trippoint.backend.trip.graphql.input.UpdateTripInput
import com.trippoint.backend.trip.service.TripService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import java.util.UUID

@Controller
class TripGraphQLController(
    private val tripService: TripService
) {

    @QueryMapping
    fun trips(
        @Argument filter: TripFilterInput?,
        authentication: Authentication?
    ): List<TripResponse> {
        return tripService.getTrips(
            authenticatedUserId(authentication),
            filter
        )
    }

    @QueryMapping
    fun trip(
        @Argument id: UUID,
        authentication: Authentication?
    ): TripResponse {
        return tripService.getTrip(
            authenticatedUserId(authentication),
            id
        )
    }

    @MutationMapping
    fun createTrip(
        @Argument input: CreateTripInput,
        authentication: Authentication?
    ): TripResponse {
        return tripService.createTrip(
            authenticatedUserId(authentication),
            input
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

    @QueryMapping
    fun tripMembers(
        @Argument tripId: UUID,
        authentication: Authentication?
    ): List<TripMemberResponse> {

        return tripService.getTripMembers(
            authenticatedUserId(authentication),
            tripId
        )
    }

    @MutationMapping
    fun inviteTripMember(
        @Argument tripId: UUID,
        @Argument input: InviteTripMemberInput,
        authentication: Authentication?
    ): TripMemberResponse {

        return tripService.inviteMember(
            authenticatedUserId(authentication),
            tripId,
            input.email
        )
    }

    @MutationMapping
    fun acceptTripInvitation(
        @Argument tripId: UUID,
        authentication: Authentication?
    ): TripMemberResponse {

        return tripService.acceptTripInvitation(
            authenticatedUserId(authentication),
            tripId
        )
    }

    @MutationMapping
    fun declineTripInvitation(
        @Argument tripId: UUID,
        authentication: Authentication?
    ): TripMemberResponse {

        return tripService.declineTripInvitation(
            authenticatedUserId(authentication),
            tripId
        )
    }

    @QueryMapping
    fun myTripInvitations(
        authentication: Authentication?
    ): List<TripMemberResponse> {

        return tripService.getMyTripInvitations(
            authenticatedUserId(authentication)
        )
    }

    @MutationMapping
    fun updateTrip(
        @Argument tripId: UUID,
        @Argument input: UpdateTripInput,
        authentication: Authentication?
    ): TripResponse {

        return tripService.updateTrip(
            authenticatedUserId(authentication),
            tripId,
            input
        )
    }

    @MutationMapping
    fun archiveTrip(
        @Argument id: UUID,
        authentication: Authentication?
    ): Boolean =
        tripService.archiveTrip(
            authenticatedUserId(authentication),
            id
        )

    @MutationMapping
    fun restoreTrip(
        @Argument id: UUID,
        authentication: Authentication?
    ): TripResponse =
        tripService.restoreTrip(
            authenticatedUserId(authentication),
            id
        )

    @MutationMapping
    fun deleteTrip(
        @Argument id: UUID,
        authentication: Authentication?
    ): Boolean =
        tripService.deleteTrip(
            authenticatedUserId(authentication),
            id
        )
}