package com.trippoint.backend.config

import com.trippoint.backend.auth.entity.User
import com.trippoint.backend.auth.repository.UserRepository
import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.auth.service.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional
import java.util.UUID
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private lateinit var jwtService: JwtService

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var request: HttpServletRequest

    @Mock
    private lateinit var response: HttpServletResponse

    @Mock
    private lateinit var filterChain: FilterChain

    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    private val testUserId = UUID.randomUUID()
    private val testEmail = "test@example.com"
    private val validToken = "eyJhbGciOiJIUzI1NiJ9.validToken"

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        jwtAuthenticationFilter = JwtAuthenticationFilter(jwtService, userRepository)
        SecurityContextHolder.clearContext()
    }

    @Nested
    @DisplayName("Filter with Valid JWT Token")
    inner class ValidTokenTests {

        @Test
        @DisplayName("filter should set authentication for valid token")
        fun `filter sets authentication for valid token`() {
            // Arrange
            val testUser = User(
                id = testUserId,
                email = testEmail,
                passwordHash = "hash",
                firstName = "John",
                lastName = "Doe",
                active = true
            )

            whenever(request.getHeader("Authorization"))
                .thenReturn("Bearer $validToken")

            whenever(jwtService.validateToken(validToken))
                .thenReturn(true)

            whenever(jwtService.getUserIdFromToken(validToken))
                .thenReturn(testUserId)

            whenever(userRepository.findById(testUserId))
                .thenReturn(Optional.of(testUser))

            // Act
            jwtAuthenticationFilter.doFilter(request, response, filterChain)

            // Assert
            val authentication = SecurityContextHolder.getContext().authentication
            assertNotNull(authentication)
            assertTrue(authentication.isAuthenticated)
            val principal = authentication.principal as UserPrincipal
            assertEquals(testUserId, principal.userId)
            assertEquals(testEmail, principal.username)
        }

        @Test
        @DisplayName("filter should not set authentication if user is inactive")
        fun `filter does not authenticate inactive user`() {
            // Arrange
            val inactiveUser = User(
                id = testUserId,
                email = testEmail,
                passwordHash = "hash",
                firstName = "John",
                lastName = "Doe",
                active = false
            )

            whenever(request.getHeader("Authorization"))
                .thenReturn("Bearer $validToken")

            whenever(jwtService.validateToken(validToken))
                .thenReturn(true)

            whenever(jwtService.getUserIdFromToken(validToken))
                .thenReturn(testUserId)

            whenever(userRepository.findById(testUserId))
                .thenReturn(Optional.of(inactiveUser))

            // Act
            jwtAuthenticationFilter.doFilter(request, response, filterChain)

            // Assert
            val authentication = SecurityContextHolder.getContext().authentication
            assertNull(authentication)
        }

        @Test
        @DisplayName("filter should not set authentication if user not found")
        fun `filter does not authenticate if user not found`() {
            // Arrange
            whenever(request.getHeader("Authorization"))
                .thenReturn("Bearer $validToken")

            whenever(jwtService.validateToken(validToken))
                .thenReturn(true)

            whenever(jwtService.getUserIdFromToken(validToken))
                .thenReturn(testUserId)

            whenever(userRepository.findById(testUserId))
                .thenReturn(Optional.empty())

            // Act
            jwtAuthenticationFilter.doFilter(request, response, filterChain)

            // Assert
            val authentication = SecurityContextHolder.getContext().authentication
            assertNull(authentication)
        }
    }

    @Nested
    @DisplayName("Filter with Invalid/Missing JWT Token")
    inner class InvalidTokenTests {

        @Test
        @DisplayName("filter should not set authentication without authorization header")
        fun `filter does not authenticate without auth header`() {
            // Arrange
            whenever(request.getHeader("Authorization"))
                .thenReturn(null)

            // Act
            jwtAuthenticationFilter.doFilter(request, response, filterChain)

            // Assert
            val authentication = SecurityContextHolder.getContext().authentication
            assertNull(authentication)
        }

        @Test
        @DisplayName("filter should not set authentication with malformed bearer token")
        fun `filter does not authenticate malformed bearer token`() {
            // Arrange
            whenever(request.getHeader("Authorization"))
                .thenReturn("Malformed token")

            // Act
            jwtAuthenticationFilter.doFilter(request, response, filterChain)

            // Assert
            val authentication = SecurityContextHolder.getContext().authentication
            assertNull(authentication)
        }

        @Test
        @DisplayName("filter should not set authentication with invalid jwt")
        fun `filter does not authenticate invalid jwt`() {
            // Arrange
            whenever(request.getHeader("Authorization"))
                .thenReturn("Bearer invalidToken")

            whenever(jwtService.validateToken("invalidToken"))
                .thenReturn(false)

            // Act
            jwtAuthenticationFilter.doFilter(request, response, filterChain)

            // Assert
            val authentication = SecurityContextHolder.getContext().authentication
            assertNull(authentication)
        }

        @Test
        @DisplayName("filter should not set authentication with empty bearer token")
        fun `filter does not authenticate empty bearer token`() {
            // Arrange
            whenever(request.getHeader("Authorization"))
                .thenReturn("Bearer ")

            whenever(jwtService.validateToken(""))
                .thenReturn(false)

            // Act
            jwtAuthenticationFilter.doFilter(request, response, filterChain)

            // Assert
            val authentication = SecurityContextHolder.getContext().authentication
            assertNull(authentication)
        }
    }

    @Nested
    @DisplayName("Filter Chain Continuation")
    inner class FilterChainTests {

        @Test
        @DisplayName("filter should continue chain regardless of authentication")
        fun `filter continues chain on valid token`() {
            // Arrange
            val testUser = User(
                id = testUserId,
                email = testEmail,
                passwordHash = "hash",
                firstName = "John",
                lastName = "Doe",
                active = true
            )

            whenever(request.getHeader("Authorization"))
                .thenReturn("Bearer $validToken")

            whenever(jwtService.getUserIdFromToken(validToken))
                .thenReturn(testUserId)

            whenever(userRepository.findById(testUserId))
                .thenReturn(Optional.of(testUser))

            // Act
            jwtAuthenticationFilter.doFilter(request, response, filterChain)

            // Assert
            verify(filterChain).doFilter(request, response)
        }

        @Test
        @DisplayName("filter should continue chain without authentication")
        fun `filter continues chain without auth header`() {
            // Arrange
            whenever(request.getHeader("Authorization"))
                .thenReturn(null)

            // Act
            jwtAuthenticationFilter.doFilter(request, response, filterChain)

            // Assert
            verify(filterChain).doFilter(request, response)
        }
    }

    @Nested
    @DisplayName("Bearer Token Extraction")
    inner class BearerTokenExtractionTests {

        @Test
        @DisplayName("filter should correctly extract bearer token")
        fun `filter extracts bearer token correctly`() {

            val testUser = User(
                id = testUserId,
                email = testEmail,
                passwordHash = "hash",
                firstName = "John",
                lastName = "Doe",
                active = true
            )

            val token = "eyJhbGciOiJIUzI1NiJ9.token123"

            whenever(request.getHeader("Authorization"))
                .thenReturn("Bearer $token")

            whenever(jwtService.validateToken(token))
                .thenReturn(true)

            whenever(jwtService.getUserIdFromToken(token))
                .thenReturn(testUserId)

            whenever(userRepository.findById(testUserId))
                .thenReturn(Optional.of(testUser))

            jwtAuthenticationFilter.doFilter(request, response, filterChain)

            verify(jwtService).validateToken(token)
            verify(jwtService).getUserIdFromToken(token)
        }

        @Test
        @DisplayName("filter should handle bearer token with extra spaces")
        fun `filter handles bearer prefix correctly`() {
            // Arrange
            whenever(request.getHeader("Authorization"))
                .thenReturn("Bearer ")

            whenever(jwtService.validateToken(""))
                .thenReturn(false)

            // Act
            jwtAuthenticationFilter.doFilter(request, response, filterChain)

            // Assert
            val authentication = SecurityContextHolder.getContext().authentication
            assertNull(authentication)
        }
    }

    @Nested
    @DisplayName("User Data in Principal")
    inner class UserDataTests {

        @Test
        @DisplayName("filter should populate principal with user email")
        fun `filter sets user email in principal`() {

            val userEmail = "custom@example.com"

            val testUser = User(
                id = testUserId,
                email = userEmail,
                passwordHash = "hash",
                firstName = "John",
                lastName = "Doe",
                active = true
            )

            whenever(request.getHeader("Authorization"))
                .thenReturn("Bearer $validToken")

            whenever(jwtService.validateToken(validToken))
                .thenReturn(true)

            whenever(jwtService.getUserIdFromToken(validToken))
                .thenReturn(testUserId)

            whenever(userRepository.findById(testUserId))
                .thenReturn(Optional.of(testUser))

            jwtAuthenticationFilter.doFilter(request, response, filterChain)

            val authentication = SecurityContextHolder.getContext().authentication

            assertNotNull(authentication)

            val principal = authentication.principal as UserPrincipal

            assertEquals(userEmail, principal.username)
        }

        @Test
        @DisplayName("filter should populate principal with user ID")
        fun `filter sets user ID in principal`() {

            val testUser = User(
                id = testUserId,
                email = testEmail,
                passwordHash = "hash",
                firstName = "John",
                lastName = "Doe",
                active = true
            )

            whenever(request.getHeader("Authorization"))
                .thenReturn("Bearer $validToken")

            whenever(jwtService.validateToken(validToken))
                .thenReturn(true)

            whenever(jwtService.getUserIdFromToken(validToken))
                .thenReturn(testUserId)

            whenever(userRepository.findById(testUserId))
                .thenReturn(Optional.of(testUser))

            jwtAuthenticationFilter.doFilter(request, response, filterChain)

            val authentication = SecurityContextHolder.getContext().authentication

            assertNotNull(authentication)

            val principal = authentication.principal as UserPrincipal

            assertEquals(testUserId, principal.userId)
        }
    }
}
