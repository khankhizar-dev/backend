package com.trippoint.backend.config

import org.springframework.graphql.server.support.AuthenticationExtractor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import reactor.core.publisher.Mono

class JwtAuthenticationExtractor : AuthenticationExtractor {

    override fun getAuthentication(
        payload: Map<String, Any>
    ): Mono<Authentication> {

        val authorization =
            payload["Authorization"] as? String
                ?: payload["authorization"] as? String
                ?: return Mono.empty()

        if (!authorization.startsWith("Bearer ")) {
            return Mono.empty()
        }

        val token = authorization
            .removePrefix("Bearer ")
            .trim()

        if (token.isBlank()) {
            return Mono.empty()
        }

        return Mono.just(
            UsernamePasswordAuthenticationToken(
                null,
                token
            )
        )
    }
}