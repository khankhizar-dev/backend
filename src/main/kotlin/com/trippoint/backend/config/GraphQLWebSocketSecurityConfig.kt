package com.trippoint.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.server.WebSocketGraphQlInterceptor
import org.springframework.graphql.server.webmvc.AuthenticationWebSocketInterceptor
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager

@Configuration
class GraphQLWebSocketSecurityConfig {

    @Bean
    fun graphqlWebSocketAuthenticationManager(
        jwtAuthenticationProvider: JwtAuthenticationProvider
    ): AuthenticationManager {
        return ProviderManager(
            jwtAuthenticationProvider
        )
    }

    @Bean
    fun graphqlWebSocketInterceptor(
        authenticationManager: AuthenticationManager
    ): WebSocketGraphQlInterceptor {

        return AuthenticationWebSocketInterceptor(
            JwtAuthenticationExtractor(),
            authenticationManager
        )
    }
}