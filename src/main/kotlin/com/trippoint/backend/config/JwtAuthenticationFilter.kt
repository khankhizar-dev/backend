package com.trippoint.backend.config

import com.trippoint.backend.auth.repository.UserRepository
import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.auth.service.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = extractToken(request)

            if (token != null && jwtService.validateToken(token)) {
                val userId = jwtService.getUserIdFromToken(token)

                if (userId != null) {
                    // Load user from database to verify they still exist and are active
                    val user = userRepository.findById(userId)
                        .orElse(null)

                    val issuedAt = jwtService.getTokenIssuedAt(token)
                    if (user != null && user.active &&
                        (user.tokensValidAfter == null || issuedAt?.isAfter(user.tokensValidAfter) == true)) {
                        // Build UserPrincipal from database (not just JWT)
                        val principal = UserPrincipal(
                            userId = userId,
                            email = user.email,
                            isActive = user.active
                        )

                        val authentication = UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.authorities
                        )

                        SecurityContextHolder.getContext().authentication = authentication
                    }
                }
            }
        } catch (e: Exception) {
            logger.debug("JWT authentication failed: ${e.message}")
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val authHeader = request.getHeader(AUTHORIZATION_HEADER)

        return if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            authHeader.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }
}
