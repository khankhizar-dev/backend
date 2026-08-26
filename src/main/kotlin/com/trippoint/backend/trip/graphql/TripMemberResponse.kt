package com.trippoint.backend.trip.graphql

import com.trippoint.backend.trip.entity.TripMember
import com.trippoint.backend.trip.model.TripMemberRole
import com.trippoint.backend.trip.model.TripMemberStatus

data class TripMemberResponse(
    val id: String,
    val tripId: String,
    val userId: String,
    val role: TripMemberRole,
    val status: TripMemberStatus,
    val invitedAt: String,
    val joinedAt: String?
) {
    companion object {
        fun from(member: TripMember): TripMemberResponse =
            TripMemberResponse(
                id = member.id.toString(),
                tripId = member.tripId.toString(),
                userId = member.userId.toString(),
                role = member.role,
                status = member.status,
                invitedAt = member.invitedAt.toString(),
                joinedAt = member.joinedAt?.toString()
            )
    }
}
