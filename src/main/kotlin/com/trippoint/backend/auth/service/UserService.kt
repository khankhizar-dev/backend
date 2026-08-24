package com.trippoint.backend.auth.service

import com.trippoint.backend.auth.dto.UpdateProfileInput
import com.trippoint.backend.auth.dto.UserResponse
import com.trippoint.backend.auth.mapper.UserMapper
import com.trippoint.backend.auth.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    fun getProfile(userId: UUID): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow {
                IllegalArgumentException("User not found")
            }

        return UserMapper.toResponse(user)
    }

    @Transactional
    fun updateProfile(
        userId: UUID,
        input: UpdateProfileInput
    ): UserResponse {

        val user = userRepository.findById(userId)
            .orElseThrow {
                IllegalArgumentException("User not found")
            }

        input.firstName?.let {
            user.firstName = it.trim()
        }

        input.lastName?.let {
            user.lastName = it.trim()
        }

        input.username?.let { username ->
            val normalizedUsername = username.trim().lowercase()

            require(normalizedUsername.isNotBlank()) {
                "Username cannot be blank"
            }

            userRepository.findByUsername(normalizedUsername)
                ?.let { existingUser ->
                    if (existingUser.id != user.id) {
                        throw IllegalArgumentException("Username already taken")
                    }
                }

            user.username = normalizedUsername
        }

        input.country?.let {
            user.country = it.trim()
        }

        input.phoneNumber?.let {
            user.phoneNumber = it.trim()
        }

        input.dateOfBirth?.let {
            user.dateOfBirth = LocalDate.parse(it)
        }

        input.nationality?.let {
            user.nationality = it.trim().uppercase()
        }

        input.profilePhotoUrl?.let {
            user.profileImageUrl = it.trim()
        }

        input.currency?.let {
            user.currency = it.trim().uppercase()
        }

        input.language?.let {
            user.language = it.trim().lowercase()
        }

        input.timezone?.let {
            user.timezone = it.trim()
        }

        return UserMapper.toResponse(
            userRepository.save(user)
        )
    }
}