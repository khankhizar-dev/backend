package com.trippoint.backend.trip.service

import com.trippoint.backend.auth.repository.UserRepository
import com.trippoint.backend.trip.entity.Trip
import com.trippoint.backend.trip.entity.TripMember
import com.trippoint.backend.trip.graphql.TripMemberResponse
import com.trippoint.backend.trip.graphql.TripResponse
import com.trippoint.backend.trip.graphql.input.CreateTripInput
import com.trippoint.backend.trip.graphql.input.TripFilterInput
import com.trippoint.backend.trip.graphql.input.UpdateTripInput
import com.trippoint.backend.trip.model.TripMemberRole
import com.trippoint.backend.trip.model.TripMemberStatus
import com.trippoint.backend.trip.model.TripStatus
import com.trippoint.backend.trip.repository.TripMemberRepository
import com.trippoint.backend.trip.repository.TripRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class TripService(
    private val tripRepository: TripRepository,
    private val tripMemberRepository: TripMemberRepository,
    private val userRepository: UserRepository,
    private val tripAccessService: TripAccessService
) {

    @Transactional
    fun createTrip(
        userId: UUID,
        input: CreateTripInput
    ): TripResponse {

        val name = input.name.trim()
        val destination = input.destination.trim()

        require(name.isNotBlank()) {
            "Trip name cannot be blank"
        }

        require(destination.isNotBlank()) {
            "Destination cannot be blank"
        }

        val startDate = LocalDate.parse(input.startDate)
        val endDate = LocalDate.parse(input.endDate)

        require(!endDate.isBefore(startDate)) {
            "End date cannot be before start date"
        }

        val trip = Trip(
            ownerId = userId,
            name = name,
            destination = destination,
            startDate = startDate,
            endDate = endDate,
            status = TripStatus.DRAFT
        )

        val savedTrip = tripRepository.save(trip)

        tripMemberRepository.save(
            TripMember(
                tripId = savedTrip.id,
                userId = userId,
                role = TripMemberRole.OWNER,
                status = TripMemberStatus.ACCEPTED,
                joinedAt = LocalDateTime.now()
            )
        )

        return TripResponse.from(savedTrip)
    }

    @Transactional(readOnly = true)
    fun getTrip(
        userId: UUID,
        tripId: UUID
    ): TripResponse {

        tripAccessService.requireMemberAccess(
            tripId = tripId,
            userId = userId
        )

        val trip = tripRepository.findById(tripId)
            .orElseThrow {
                IllegalArgumentException("Trip not found")
            }

        return TripResponse.from(trip)
    }

    @Transactional(readOnly = true)
    fun getTrips(
        userId: UUID,
        filter: TripFilterInput?
    ): List<TripResponse> {

        val trips = when {
            filter?.status != null ->
                tripRepository.findAllAccessibleTripsByStatus(
                    userId = userId,
                    memberStatus = TripMemberStatus.ACCEPTED,
                    tripStatus = filter.status
                )

            else ->
                tripRepository.findAllAccessibleTrips(
                    userId = userId,
                    status = TripMemberStatus.ACCEPTED
                )
        }

        val search = filter?.search
            ?.trim()
            ?.lowercase()

        return trips
            .filter {
                search.isNullOrBlank() ||
                        it.name.lowercase().contains(search) ||
                        it.destination.lowercase().contains(search)
            }
            .map(TripResponse::from)
    }

    @Transactional
    fun inviteMember(
        ownerId: UUID,
        tripId: UUID,
        email: String
    ): TripMemberResponse {

        val trip = tripRepository.findByIdAndOwnerId(
            tripId,
            ownerId
        ) ?: throw IllegalArgumentException("Trip not found")

        val normalizedEmail = email.trim().lowercase()

        require(normalizedEmail.isNotBlank()) {
            "Email cannot be blank"
        }

        val invitedUser = userRepository.findByEmail(normalizedEmail)
            ?: throw IllegalArgumentException("User not found")

        require(invitedUser.id != ownerId) {
            "Trip owner is already a member"
        }

        require(
            tripMemberRepository.findByTripIdAndUserId(
                trip.id,
                invitedUser.id ?: throw IllegalStateException("User ID cannot be null"),
            ) == null
        ) {
            "User is already a member of this trip"
        }

        val member = TripMember(
            tripId = trip.id,
            userId = invitedUser.id ?: throw IllegalStateException("User ID cannot be null"),
            role = TripMemberRole.MEMBER,
            status = TripMemberStatus.PENDING
        )

        return TripMemberResponse.from(
            tripMemberRepository.save(member)
        )
    }

    @Transactional(readOnly = true)
    fun getTripMembers(
        userId: UUID,
        tripId: UUID
    ): List<TripMemberResponse> {

        tripAccessService.requireMemberAccess(
            tripId = tripId,
            userId = userId
        )

        return tripMemberRepository
            .findAllByTripId(tripId)
            .map { TripMemberResponse.from(it) }
    }

    @Transactional
    fun acceptTripInvitation(
        userId: UUID,
        tripId: UUID
    ): TripMemberResponse {

        val member = tripMemberRepository.findByTripIdAndUserId(
            tripId,
            userId
        ) ?: throw IllegalArgumentException("Trip invitation not found")

        require(member.status == TripMemberStatus.PENDING) {
            "Trip invitation is no longer pending"
        }

        member.status = TripMemberStatus.ACCEPTED
        member.joinedAt = LocalDateTime.now()

        return TripMemberResponse.from(
            tripMemberRepository.save(member)
        )
    }

    @Transactional
    fun declineTripInvitation(
        userId: UUID,
        tripId: UUID
    ): TripMemberResponse {

        val member = tripMemberRepository.findByTripIdAndUserId(
            tripId,
            userId
        ) ?: throw IllegalArgumentException("Trip invitation not found")

        require(member.status == TripMemberStatus.PENDING) {
            "Trip invitation is no longer pending"
        }

        member.status = TripMemberStatus.DECLINED

        return TripMemberResponse.from(
            tripMemberRepository.save(member)
        )
    }

    @Transactional(readOnly = true)
    fun getMyTripInvitations(
        userId: UUID
    ): List<TripMemberResponse> {

        return tripMemberRepository
            .findAllByUserIdAndStatus(
                userId,
                TripMemberStatus.PENDING
            )
            .map(TripMemberResponse::from)
    }

    @Transactional
    fun updateTrip(
        userId: UUID,
        tripId: UUID,
        input: UpdateTripInput
    ): TripResponse {

        tripAccessService.requireOwnerAccess(
            tripId = tripId,
            userId = userId
        )

        val trip = tripRepository.findById(tripId)
            .orElseThrow {
                IllegalArgumentException("Trip not found")
            }

        input.name?.let {
            val name = it.trim()

            require(name.isNotBlank()) {
                "Trip name cannot be blank"
            }

            trip.name = name
        }

        input.destination?.let {
            val destination = it.trim()

            require(destination.isNotBlank()) {
                "Destination cannot be blank"
            }

            trip.destination = destination
        }

        input.startDate?.let {
            trip.startDate = LocalDate.parse(it)
        }

        input.endDate?.let {
            trip.endDate = LocalDate.parse(it)
        }

        require(!trip.endDate.isBefore(trip.startDate)) {
            "End date cannot be before start date"
        }

        input.status?.let {
            trip.status = it
        }

        return TripResponse.from(
            tripRepository.save(trip)
        )
    }

    @Transactional
    fun archiveTrip(
        userId: UUID,
        tripId: UUID
    ): Boolean {

        tripAccessService.requireOwnerAccess(
            tripId = tripId,
            userId = userId
        )

        val trip = tripRepository.findById(tripId)
            .orElseThrow {
                IllegalArgumentException("Trip not found")
            }

        require(trip.status != TripStatus.ARCHIVED) {
            "Trip is already archived"
        }

        trip.status = TripStatus.ARCHIVED

        tripRepository.save(trip)

        return true
    }

    @Transactional
    fun restoreTrip(
        userId: UUID,
        tripId: UUID
    ): TripResponse {

        tripAccessService.requireOwnerAccess(
            tripId = tripId,
            userId = userId
        )

        val trip = tripRepository.findById(tripId)
            .orElseThrow {
                IllegalArgumentException("Trip not found")
            }

        require(trip.status == TripStatus.ARCHIVED) {
            "Only archived trips can be restored"
        }

        trip.status = TripStatus.DRAFT

        return TripResponse.from(
            tripRepository.save(trip)
        )
    }

    @Transactional
    fun deleteTrip(
        userId: UUID,
        tripId: UUID
    ): Boolean {

        tripAccessService.requireOwnerAccess(
            tripId = tripId,
            userId = userId
        )

        val trip = tripRepository.findById(tripId)
            .orElseThrow {
                IllegalArgumentException("Trip not found")
            }

        tripMemberRepository.deleteAllByTripId(tripId)

        tripRepository.delete(trip)

        return true
    }

    @Transactional
    fun removeTripMember(
        tripId: UUID,
        memberUserId: UUID,
        currentUserId: UUID
    ): Boolean {

        tripAccessService.requireOwnerAccess(
            tripId,
            currentUserId
        )

        val member = tripMemberRepository
            .findByTripIdAndUserId(
                tripId,
                memberUserId
            )
            ?: throw IllegalArgumentException(
                "Trip member not found"
            )

        if (member.role == TripMemberRole.OWNER) {
            throw IllegalArgumentException(
                "Trip owner cannot be removed"
            )
        }

        tripMemberRepository.delete(member)

        return true
    }

    @Transactional
    fun leaveTrip(
        tripId: UUID,
        currentUserId: UUID
    ): Boolean {

        tripAccessService.requireMemberAccess(
            tripId,
            currentUserId
        )

        val member = tripMemberRepository
            .findByTripIdAndUserId(
                tripId,
                currentUserId
            )
            ?: throw IllegalArgumentException(
                "Trip member not found"
            )

        if (member.role == TripMemberRole.OWNER) {
            throw IllegalArgumentException(
                "Trip owner cannot leave the trip"
            )
        }

        tripMemberRepository.delete(member)

        return true
    }
}