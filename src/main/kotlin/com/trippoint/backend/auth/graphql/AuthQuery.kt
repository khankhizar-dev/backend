package com.trippoint.backend.auth.graphql

import com.trippoint.backend.auth.dto.UserResponse
import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.auth.service.AuthService
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import org.springframework.graphql.data.method.annotation.SchemaMapping
import com.trippoint.backend.auth.dto.UserDeviceResponse
import com.trippoint.backend.auth.service.UserService

@Controller
class AuthQuery(
    private val authService: AuthService,
    private val userService: UserService
) {

    @QueryMapping
    fun me(authentication: Authentication?): UserResponse {
        val auth = authentication ?: SecurityContextHolder.getContext().authentication
        ?: throw IllegalArgumentException("User not authenticated")

        val principal = auth.principal as? UserPrincipal
            ?: throw IllegalArgumentException("Invalid authentication principal")

        return userService.getProfile(principal.userId)
    }

    @QueryMapping
    fun userDevices(authentication: Authentication?): List<UserDeviceResponse> {
        val auth = authentication ?: SecurityContextHolder.getContext().authentication
            ?: throw IllegalArgumentException("User not authenticated")
        val principal = auth.principal as? UserPrincipal
            ?: throw IllegalArgumentException("Invalid authentication principal")
        return authService.userDevices(principal.userId)
    }
}
