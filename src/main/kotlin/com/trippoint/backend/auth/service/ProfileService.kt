package com.trippoint.backend.auth.service

import com.trippoint.backend.auth.dto.UserResponse
import com.trippoint.backend.auth.entity.User
import com.trippoint.backend.auth.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class ProfileService(
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    fun getProfile(userId: UUID): UserResponse {

        val user = userRepository.findById(userId)
            .orElseThrow {
                IllegalArgumentException("User not found")
            }

        return toUserResponse(user)
    }

    @Transactional
    fun updateProfile(
        userId: UUID,
        firstName: String?,
        lastName: String?,
        phoneNumber: String?,
        dateOfBirth: LocalDate?,
        nationality: String?,
        profileImageUrl: String?
    ): UserResponse {

        val user = userRepository.findById(userId)
            .orElseThrow {
                IllegalArgumentException("User not found")
            }

        firstName?.let {
            user.firstName = it.trim()
        }

        lastName?.let {
            user.lastName = it.trim()
        }

        phoneNumber?.let {
            user.phoneNumber = it.trim()
        }

        dateOfBirth?.let {
            user.dateOfBirth = it
        }

        nationality?.let {
            user.nationality = it.trim()
        }

        profileImageUrl?.let {
            user.profileImageUrl = it.trim()
        }

        user.updatedAt = java.time.OffsetDateTime.now()

        val saved = userRepository.save(user)

        return toUserResponse(saved)
    }

    private fun toUserResponse(user: User): UserResponse {
        return UserResponse(
            id = user.id!!,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            phoneNumber = user.phoneNumber,
            dateOfBirth = user.dateOfBirth,
            nationality = user.nationality,
            profilePhotoUrl  = user.profileImageUrl
        )
    }
}