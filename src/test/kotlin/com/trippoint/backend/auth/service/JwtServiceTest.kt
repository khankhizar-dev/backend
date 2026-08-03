package com.trippoint.backend.auth.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID
import com.trippoint.backend.auth.repository.TokenBlacklistRepository
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse

class JwtServiceTest {

    private lateinit var jwtService: JwtService

    @Mock
    private lateinit var tokenBlacklistRepository: TokenBlacklistRepository

    private val secret =
        "my-super-secret-key-that-is-at-least-256-bits-long-for-HS256-algo"

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        jwtService = JwtService(
            secret = secret,
            expiration = 60_000L,
            refreshExpiration = 600_000L
        )
    }

    @Test
    fun `should expose access token metadata`() {
        val userId = UUID.randomUUID()
        val token = jwtService.generateToken(userId)

        assertNotNull(jwtService.getTokenId(token))
        assertNotNull(jwtService.getTokenIssuedAt(token))
        assertNotNull(jwtService.getTokenExpiration(token))
        assertFalse(jwtService.isTokenBlacklisted(token))
    }

    @Test
    fun `should reject blacklisted tokens`() {
        val service = JwtService(secret, 60_000L, 600_000L, tokenBlacklistRepository)
        val token = service.generateToken(UUID.randomUUID())
        val tokenId = service.getTokenId(token)!!
        whenever(tokenBlacklistRepository.existsByTokenId(tokenId)).thenReturn(true)

        assertTrue(service.isTokenBlacklisted(token))
        assertFalse(service.validateToken(token))
    }

    @Test
    fun `should return null metadata for malformed tokens`() {
        assertNull(jwtService.getTokenId("invalid-token"))
        assertNull(jwtService.getTokenIssuedAt("invalid-token"))
        assertNull(jwtService.getTokenExpiration("invalid-token"))
        assertFalse(jwtService.isTokenBlacklisted("invalid-token"))
    }

    @Nested
    @DisplayName("Access Token Tests")
    inner class AccessTokenTests {

        @Test
        fun `should generate access token`() {

            val userId = UUID.randomUUID()

            val token = jwtService.generateToken(userId)

            assertNotNull(token)
            assertTrue(token.isNotBlank())
        }

        @Test
        fun `should validate generated access token`() {

            val token = jwtService.generateToken(UUID.randomUUID())

            assertTrue(jwtService.validateToken(token))
        }

        @Test
        fun `should extract user id from access token`() {

            val userId = UUID.randomUUID()

            val token = jwtService.generateToken(userId)

            val extracted = jwtService.getUserIdFromToken(token)

            assertEquals(userId, extracted)
        }
    }

    @Nested
    @DisplayName("Refresh Token Tests")
    inner class RefreshTokenTests {

        @Test
        fun `should generate refresh token`() {

            val token =
                jwtService.generateRefreshToken(UUID.randomUUID())

            assertNotNull(token)
            assertTrue(token.isNotBlank())
        }

        @Test
        fun `should identify refresh token`() {

            val token =
                jwtService.generateRefreshToken(UUID.randomUUID())

            assertTrue(jwtService.isRefreshToken(token))
        }

        @Test
        fun `should reject access token as refresh token`() {

            val token =
                jwtService.generateToken(UUID.randomUUID())

            assertFalse(jwtService.isRefreshToken(token))
        }

        @Test
        fun `should validate refresh token`() {

            val token =
                jwtService.generateRefreshToken(UUID.randomUUID())

            assertTrue(jwtService.validateToken(token))
        }

        @Test
        fun `should extract user id from refresh token`() {

            val userId = UUID.randomUUID()

            val token =
                jwtService.generateRefreshToken(userId)

            val extracted =
                jwtService.getUserIdFromToken(token)

            assertEquals(userId, extracted)
        }
    }

    @Nested
    @DisplayName("Invalid Token Tests")
    inner class InvalidTokenTests {

        @Test
        fun `should reject invalid token`() {

            assertFalse(
                jwtService.validateToken("invalid-token")
            )
        }

        @Test
        fun `should return null for invalid token`() {

            assertNull(
                jwtService.getUserIdFromToken("invalid-token")
            )
        }

        @Test
        fun `should reject empty token`() {

            assertFalse(
                jwtService.validateToken("")
            )
        }

        @Test
        fun `should reject malformed token`() {

            assertFalse(
                jwtService.validateToken("abc.xyz")
            )
        }
    }
}
}
