package com.trippoint.backend.auth.service

import com.trippoint.backend.auth.dto.RegisterRequest
import com.trippoint.backend.auth.entity.User
import com.trippoint.backend.auth.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DisplayName("AuthService Login Tests")
class AuthServiceLoginTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordService: PasswordService

    @Mock
    private lateinit var jwtService: JwtService

    private lateinit var authService: AuthService

    private val testUserId = UUID.randomUUID()
    private val testEmail = "test@example.com"
    private val testPassword = "SecurePass@123"
    private val testPasswordHash = "\$2a\$10\$abcdefghijklmnopqrstuvwxyz"
    private val testFirstName = "John"
    private val testLastName = "Doe"

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        authService = AuthService(userRepository, passwordService, jwtService)
    }

    @Nested
    @DisplayName("Login Happy Path Tests")
    inner class LoginHappyPathTests {

        @Test
        @DisplayName("login should return AuthPayload with valid email and password")
        fun `login returns auth payload on success`() {
            // Arrange
            val testUser = User(
                id = testUserId,
                email = testEmail,
                passwordHash = testPasswordHash,
                firstName = testFirstName,
                lastName = testLastName,
                active = true
            )
            val testJwtToken = "eyJhbGciOiJIUzI1NiJ9.token"
            val testRefreshToken = "eyJhbGciOiJIUzI1NiJ9.refreshToken"

            whenever(userRepository.findByEmail(testEmail))
                .thenReturn(testUser)

            whenever(passwordService.matches(testPassword, testPasswordHash))
                .thenReturn(true)

            whenever(jwtService.generateToken(testUserId))
                .thenReturn(testJwtToken)

            whenever(jwtService.generateRefreshToken(testUserId))
                .thenReturn(testRefreshToken)

            // Act
            val result = authService.login(testEmail, testPassword)

            // Assert
            assertNotNull(result)
            assertEquals(testEmail, result.user.email)
            assertEquals(testFirstName, result.user.firstName)
            assertEquals(testLastName, result.user.lastName)
            assertEquals(testJwtToken, result.token)
            assertEquals(testRefreshToken, result.refreshToken)
        }

        @Test
        @DisplayName("login should return correct user data in response")
        fun `login returns correct user data`() {
            // Arrange
            val testUser = User(
                id = testUserId,
                email = testEmail,
                passwordHash = testPasswordHash,
                firstName = testFirstName,
                lastName = testLastName,
                active = true
            )

            whenever(userRepository.findByEmail(testEmail))
                .thenReturn(testUser)

            whenever(passwordService.matches(testPassword, testPasswordHash))
                .thenReturn(true)

            whenever(jwtService.generateToken(testUserId))
                .thenReturn("token")

            whenever(jwtService.generateRefreshToken(testUserId))
                .thenReturn("refreshToken")

            // Act
            val result = authService.login(testEmail, testPassword)

            // Assert
            assertEquals(testUserId, result.user.id)
            assertEquals(testEmail, result.user.email)
            assertEquals(testFirstName, result.user.firstName)
            assertEquals(testLastName, result.user.lastName)
        }

        @Test
        @DisplayName("login should return both access and refresh tokens")
        fun `login returns both tokens`() {
            // Arrange
            val testUser = User(
                id = testUserId,
                email = testEmail,
                passwordHash = testPasswordHash,
                firstName = testFirstName,
                lastName = testLastName,
                active = true
            )
            val accessToken = "access.token.here"
            val refreshToken = "refresh.token.here"

            whenever(userRepository.findByEmail(testEmail))
                .thenReturn(testUser)

            whenever(passwordService.matches(testPassword, testPasswordHash))
                .thenReturn(true)

            whenever(jwtService.generateToken(testUserId))
                .thenReturn(accessToken)

            whenever(jwtService.generateRefreshToken(testUserId))
                .thenReturn(refreshToken)

            // Act
            val result = authService.login(testEmail, testPassword)

            // Assert
            assertNotNull(result.token)
            assertNotNull(result.refreshToken)
            assertEquals(accessToken, result.token)
            assertEquals(refreshToken, result.refreshToken)
            assertTrue(result.token != result.refreshToken)
        }
    }

    @Nested
    @DisplayName("Login Error Cases")
    inner class LoginErrorCases {

        @Test
        @DisplayName("login should throw exception for non-existent user")
        fun `login throws exception for non-existent email`() {
            // Arrange
            whenever(userRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(null)

            // Act & Assert
            try {
                authService.login("nonexistent@example.com", testPassword)
                assertTrue(false, "Should have thrown exception")
            } catch (e: IllegalArgumentException) {
                assertEquals("User not found.", e.message)
            }
        }

        @Test
        @DisplayName("login should throw exception for invalid password")
        fun `login throws exception for wrong password`() {
            // Arrange
            val testUser = User(
                id = testUserId,
                email = testEmail,
                passwordHash = testPasswordHash,
                firstName = testFirstName,
                lastName = testLastName,
                active = true
            )

            whenever(userRepository.findByEmail(testEmail))
                .thenReturn(testUser)

            whenever(passwordService.matches("WrongPassword", testPasswordHash))
                .thenReturn(false)

            // Act & Assert
            try {
                authService.login(testEmail, "WrongPassword")
                assertTrue(false, "Should have thrown exception")
            } catch (e: IllegalArgumentException) {
                assertEquals("Invalid password.", e.message)
            }
        }

        @Test
        @DisplayName("login should throw exception for empty email")
        fun `login throws exception for empty email`() {
            // Arrange
            whenever(userRepository.findByEmail(""))
                .thenReturn(null)

            // Act & Assert
            try {
                authService.login("", testPassword)
                assertTrue(false, "Should have thrown exception")
            } catch (e: IllegalArgumentException) {
                assertEquals("User not found.", e.message)
            }
        }

        @Test
        @DisplayName("login should throw exception for empty password")
        fun `login throws exception for empty password`() {
            // Arrange
            val testUser = User(
                id = testUserId,
                email = testEmail,
                passwordHash = testPasswordHash,
                firstName = testFirstName,
                lastName = testLastName,
                active = true
            )

            whenever(userRepository.findByEmail(testEmail))
                .thenReturn(testUser)

            whenever(passwordService.matches("", testPasswordHash))
                .thenReturn(false)

            // Act & Assert
            try {
                authService.login(testEmail, "")
                assertTrue(false, "Should have thrown exception")
            } catch (e: IllegalArgumentException) {
                assertEquals("Invalid password.", e.message)
            }
        }

        @Test
        @DisplayName("login should throw exception when user ID is null")
        fun `login throws exception when user ID is null`() {
            // Arrange
            val testUser = User(
                id = null,
                email = testEmail,
                passwordHash = testPasswordHash,
                firstName = testFirstName,
                lastName = testLastName,
                active = true
            )

            whenever(userRepository.findByEmail(testEmail))
                .thenReturn(testUser)

            whenever(passwordService.matches(testPassword, testPasswordHash))
                .thenReturn(true)

            // Act & Assert
            try {
                authService.login(testEmail, testPassword)
                assertTrue(false, "Should have thrown exception")
            } catch (e: IllegalStateException) {
                assertEquals("Failed to get user ID", e.message)
            }
        }
    }

    @Nested
    @DisplayName("Me Query Tests")
    inner class MeQueryTests {

        @Test
        @DisplayName("me should return user profile for valid user ID")
        fun `me returns user profile`() {
            // Arrange
            val testUser = User(
                id = testUserId,
                email = testEmail,
                passwordHash = testPasswordHash,
                firstName = testFirstName,
                lastName = testLastName,
                active = true
            )

            whenever(userRepository.findById(testUserId))
                .thenReturn(Optional.of(testUser))

            // Act
            val result = authService.me(testUserId)

            // Assert
            assertNotNull(result)
            assertEquals(testUserId, result.id)
            assertEquals(testEmail, result.email)
            assertEquals(testFirstName, result.firstName)
            assertEquals(testLastName, result.lastName)
        }

        @Test
        @DisplayName("me should throw exception for non-existent user")
        fun `me throws exception for non-existent user`() {
            // Arrange
            val nonExistentId = UUID.randomUUID()

            whenever(userRepository.findById(nonExistentId))
                .thenReturn(Optional.empty())

            // Act & Assert
            try {
                authService.me(nonExistentId)
                assertTrue(false, "Should have thrown exception")
            } catch (e: IllegalArgumentException) {
                assertEquals("User not found", e.message)
            }
        }

        @Test
        @DisplayName("me should handle user with null first name")
        fun `me handles null first name`() {
            // Arrange
            val testUser = User(
                id = testUserId,
                email = testEmail,
                passwordHash = testPasswordHash,
                firstName = null,
                lastName = testLastName,
                active = true
            )

            whenever(userRepository.findById(testUserId))
                .thenReturn(Optional.of(testUser))

            // Act
            val result = authService.me(testUserId)

            // Assert
            assertNotNull(result)
            assertEquals(testUserId, result.id)
            assertEquals(null, result.firstName)
            assertEquals(testLastName, result.lastName)
        }

        @Test
        @DisplayName("me should handle user with null last name")
        fun `me handles null last name`() {
            // Arrange
            val testUser = User(
                id = testUserId,
                email = testEmail,
                passwordHash = testPasswordHash,
                firstName = testFirstName,
                lastName = null,
                active = true
            )

            whenever(userRepository.findById(testUserId))
                .thenReturn(Optional.of(testUser))

            // Act
            val result = authService.me(testUserId)

            // Assert
            assertNotNull(result)
            assertEquals(testUserId, result.id)
            assertEquals(testFirstName, result.firstName)
            assertEquals(null, result.lastName)
        }
    }

    @Nested
    @DisplayName("Register Tests")
    inner class RegisterTests {

        @Test
        @DisplayName("register should throw exception for existing email")
        fun `register throws exception for duplicate email`() {
            // Arrange
            whenever(userRepository.existsByEmail(testEmail))
                .thenReturn(true)

            val request = RegisterRequest(
                email = testEmail,
                password = testPassword,
                firstName = testFirstName,
                lastName = testLastName
            )

            // Act & Assert
            try {
                authService.register(request)
                assertTrue(false, "Should have thrown exception")
            } catch (e: IllegalArgumentException) {
                assertEquals("Email already registered.", e.message)
            }
        }
    }
}
