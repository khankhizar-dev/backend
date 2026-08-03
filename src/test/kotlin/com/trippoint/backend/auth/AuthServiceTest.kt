package com.trippoint.backend.auth

import com.trippoint.backend.auth.dto.RefreshTokenRequest
import com.trippoint.backend.auth.dto.RegisterRequest
import com.trippoint.backend.auth.repository.RefreshTokenRepository
import com.trippoint.backend.auth.repository.UserRepository
import com.trippoint.backend.auth.service.AuthService
import com.trippoint.backend.auth.service.JwtService
import com.trippoint.backend.auth.service.PasswordService
import jakarta.transaction.Transactional
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var passwordService: PasswordService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    private lateinit var jwtService: JwtService

    @BeforeEach
    fun cleanDatabase() {
        refreshTokenRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `test password encoding and matching`() {
        val password = "TestPassword123!"
        val encoded = passwordService.encode(password)
        
        assert(passwordService.matches(password, encoded))
        assert(!passwordService.matches("WrongPassword", encoded))
    }

    @Test
    fun `test register user`() {

        val email = "register-${UUID.randomUUID()}@example.com"

        try {

            val payload = authService.register(
                RegisterRequest(
                    email = email,
                    password = "SecurePass123!",
                    firstName = "John",
                    lastName = "Doe"
                )
            )

            println("SUCCESS")
            println(payload)

        } catch (e: Exception) {

            println("================================")
            println(e.message)
            e.printStackTrace()
            println("================================")

            throw e
        }
    }

    @Test
    fun `test login user`() {

        val email = "login-${UUID.randomUUID()}@example.com"
        val password = "LoginPass123!"

        authService.register(
            RegisterRequest(
                email = email,
                password = password,
                firstName = "Jane",
                lastName = "Smith"
            )
        )

        val payload = authService.login(email, password)

        assertNotNull(payload.user)
        assertNotNull(payload.token)
        assertNotNull(payload.refreshToken)

        assertEquals(email, payload.user.email)
    }

    @Test
    fun `should refresh token successfully`() {

        val email = "refresh-${UUID.randomUUID()}@test.com"

        val registerResponse = authService.register(
            RegisterRequest(
                email = email,
                password = "Password@123",
                firstName = "John",
                lastName = "Doe"
            )
        )

        assertTrue(
            jwtService.isRefreshToken(registerResponse.refreshToken)
        )

        val response = authService.refreshToken(
            RefreshTokenRequest(
                refreshToken = registerResponse.refreshToken
            )
        )

        assertNotNull(response.token)
        assertNotNull(response.refreshToken)

        assertTrue(jwtService.validateToken(response.token))
        assertTrue(jwtService.isRefreshToken(response.refreshToken))
    }

        @Test
        fun `should throw exception for invalid refresh token`() {

            val exception = assertThrows<IllegalArgumentException> {

                authService.refreshToken(
                    RefreshTokenRequest(
                        refreshToken = "invalid-token"
                    )
                )
            }

            assertEquals(
                "Invalid refresh token",
                exception.message
            )
        }

        @Test
        fun `should reject access token as refresh token`() {

            val registerResponse = authService.register(
                RegisterRequest(
                    email = "access@test.com",
                    password = "Password@123",
                    firstName = "John",
                    lastName = "Doe"
                )
            )

            val exception = assertThrows<IllegalArgumentException> {

                authService.refreshToken(
                    RefreshTokenRequest(
                        refreshToken = registerResponse.token
                    )
                )
            }

            assertEquals(
                "Invalid refresh token",
                exception.message
            )
        }

        @Test
        fun `should reject revoked refresh token`() {

            val register = authService.register(
                RegisterRequest(
                    email = "rotation@test.com",
                    password = "Password@123",
                    firstName = "John",
                    lastName = "Doe"
                )
            )

            authService.refreshToken(
                RefreshTokenRequest(register.refreshToken)
            )

            val exception = assertThrows<IllegalArgumentException> {

                authService.refreshToken(
                    RefreshTokenRequest(register.refreshToken)
                )
            }

            assertEquals(
                "Refresh token revoked",
                exception.message
            )
        }
    }
