package com.trippoint.backend.auth.graphql

import com.trippoint.backend.auth.dto.UserResponse
import com.trippoint.backend.auth.service.AuthService
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import java.util.UUID

@Controller
class AuthQuery(
    private val authService: AuthService
) {

    @QueryMapping
    fun me(): UserResponse {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw IllegalArgumentException("User not authenticated")

        val userId = try {
            UUID.fromString(authentication.name)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid user ID in token")
        }

        return authService.me(userId)
    }
}
