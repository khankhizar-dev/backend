package com.trippoint.backend.auth.mapper

import com.trippoint.backend.auth.dto.UserResponse
import com.trippoint.backend.auth.entity.User

object UserMapper {

    fun toResponse(user: User): UserResponse {
        return UserResponse(
            id = requireNotNull(user.id) {
                "User ID cannot be null"
            },
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            fullName = listOfNotNull(
                user.firstName,
                user.lastName
            )
                .joinToString(" ")
                .ifBlank { null },
            username = user.username,
            profilePhotoUrl = user.profileImageUrl,
            country = user.country,
            currency = user.currency,
            language = user.language,
            timezone = user.timezone,
            phoneNumber = user.phoneNumber,
            dateOfBirth = user.dateOfBirth,
            nationality = user.nationality
        )
    }
}