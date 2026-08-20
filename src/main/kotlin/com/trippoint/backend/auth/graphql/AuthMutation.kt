package com.trippoint.backend.auth.graphql

import com.trippoint.backend.auth.dto.RegisterRequest
import com.trippoint.backend.auth.dto.AuthPayload
import com.trippoint.backend.auth.dto.RefreshTokenRequest
import com.trippoint.backend.auth.dto.RefreshTokenResponse
import com.trippoint.backend.auth.dto.VerifyEmailOtpRequest
import com.trippoint.backend.auth.dto.VerifyPasswordResetOtpRequest
import com.trippoint.backend.auth.dto.ResetPasswordRequest
import com.trippoint.backend.auth.dto.UpdateProfileInput
import com.trippoint.backend.auth.dto.UserResponse
import com.trippoint.backend.auth.service.AuthService
import com.trippoint.backend.auth.service.EmailOtpService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.auth.service.UserService
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Controller
class AuthMutation(
    private val authService: AuthService,
    private val emailOtpService: EmailOtpService,
    private val userService: UserService
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

    @MutationMapping
    fun verifyEmailOtp(@Argument input: VerifyEmailOtpRequest): Boolean =
        emailOtpService.verifyEmailOtp(input.email, input.otp)

    @MutationMapping
    fun resendEmailOtp(authentication: Authentication?): Boolean =
        emailOtpService.resendEmailVerificationOtp(authenticatedPrincipal(authentication).userId)

    @MutationMapping
    fun requestPasswordReset(@Argument email: String): Boolean = emailOtpService.requestPasswordReset(email)

    @MutationMapping
    fun verifyPasswordResetOtp(@Argument input: VerifyPasswordResetOtpRequest): Boolean =
        emailOtpService.verifyPasswordResetOtp(input.email, input.otp)

    @MutationMapping
    fun resetPassword(@Argument input: ResetPasswordRequest): Boolean =
        authService.resetPassword(input.email, input.otp, input.newPassword)

    private fun authenticatedPrincipal(authentication: Authentication?): UserPrincipal =
        (authentication ?: SecurityContextHolder.getContext().authentication)?.principal as? UserPrincipal
            ?: throw IllegalArgumentException("User not authenticated")

    @MutationMapping
    fun updateProfile(
        @Argument input: UpdateProfileInput,
        authentication: Authentication?
    ): UserResponse =
        userService.updateProfile(
            authenticatedPrincipal(authentication).userId,
            input
        )
}
