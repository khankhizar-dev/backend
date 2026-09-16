package com.trippoint.backend.config

import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.auth.service.JwtService
import com.trippoint.backend.auth.repository.UserRepository
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class JwtAuthenticationProvider(
    private val jwtService: JwtService,
    private val userRepository: UserRepository
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication {

        val token = authentication.credentials?.toString()
            ?: throw BadCredentialsException("Missing JWT token")

        if (!jwtService.validateToken(token)) {
            throw BadCredentialsException("Invalid or expired token")
        }

        val userId = jwtService.getUserIdFromToken(token)
            ?: throw BadCredentialsException("Invalid user token")

        val user = userRepository.findById(userId)
            .orElseThrow {
                BadCredentialsException("User not found")
            }

        if (!user.active) {
            throw BadCredentialsException("User account is inactive")
        }

        val issuedAt = jwtService.getTokenIssuedAt(token)

        if (
            user.tokensValidAfter != null &&
            (issuedAt == null || !issuedAt.isAfter(user.tokensValidAfter))
        ) {
            throw BadCredentialsException("Token is no longer valid")
        }

        val principal = UserPrincipal(
            userId = userId,
            email = user.email,
            isActive = user.active
        )

        return UsernamePasswordAuthenticationToken(
            principal,
            token,
            principal.authorities
        )
    }

    override fun supports(authentication: Class<*>): Boolean {
        return UsernamePasswordAuthenticationToken::class.java
            .isAssignableFrom(authentication)
    }
}