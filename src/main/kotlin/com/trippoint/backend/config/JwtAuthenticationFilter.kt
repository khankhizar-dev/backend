package com.trippoint.backend.config

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
    private val jwtService: JwtService
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
                    val authentication = UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        null,
                        emptyList()
                    )
                    SecurityContextHolder.getContext().authentication = authentication
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
