package com.trippoint.backend.auth.graphql

import com.trippoint.backend.auth.dto.RegisterRequest
import com.trippoint.backend.auth.dto.AuthPayload
import com.trippoint.backend.auth.dto.RefreshTokenRequest
import com.trippoint.backend.auth.dto.RefreshTokenResponse
import com.trippoint.backend.auth.service.AuthService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import com.trippoint.backend.auth.security.UserPrincipal
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Controller
class AuthMutation(
    private val authService: AuthService
) {

    @MutationMapping
    fun register(
        @Argument input: RegisterRequest
    ): AuthPayload {
        return authService.register(input)
    }

    @MutationMapping
    fun login(
        @Argument email: String,
        @Argument password: String
    ): AuthPayload {
        return authService.login(email, password)
    }

    @MutationMapping
    fun refreshToken(
        @Argument input: RefreshTokenRequest
    ): RefreshTokenResponse {
        return authService.refreshToken(input)
    }

    @MutationMapping
    fun logout(authentication: Authentication?): Boolean {
        val principal = authenticatedPrincipal(authentication)
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        val token = request?.getHeader("Authorization")?.removePrefix("Bearer ")
            ?: throw IllegalArgumentException("No token provided")
        return authService.logout(token, principal.userId)
    }

    @MutationMapping
    fun logoutAllDevices(authentication: Authentication?): Boolean =
        authService.logoutAllDevices(authenticatedPrincipal(authentication).userId)

    @MutationMapping
    fun changePassword(
        @Argument currentPassword: String,
        @Argument newPassword: String,
        authentication: Authentication?
    ): Boolean = authService.changePassword(
        authenticatedPrincipal(authentication).userId, currentPassword, newPassword
    )

    private fun authenticatedPrincipal(authentication: Authentication?): UserPrincipal =
        (authentication ?: SecurityContextHolder.getContext().authentication)?.principal as? UserPrincipal
            ?: throw IllegalArgumentException("User not authenticated")
}
}
