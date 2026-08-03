package com.trippoint.backend.auth.graphql

import com.trippoint.backend.auth.dto.RegisterRequest
import com.trippoint.backend.auth.dto.AuthPayload
import com.trippoint.backend.auth.dto.RefreshTokenRequest
import com.trippoint.backend.auth.dto.RefreshTokenResponse
import com.trippoint.backend.auth.service.AuthService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller

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
}