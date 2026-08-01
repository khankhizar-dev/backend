package com.trippoint.backend.auth.graphql

import com.trippoint.backend.auth.dto.AuthPayload
import com.trippoint.backend.auth.dto.RegisterRequest

import com.trippoint.backend.auth.dto.UserResponse
import com.trippoint.backend.auth.service.AuthService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DisplayName("AuthMutation Tests")
class AuthMutationTest {

    @Mock
    private lateinit var authService: AuthService

    private lateinit var authMutation: AuthMutation

    private val testUserId = UUID.randomUUID()
    private val testEmail = "test@example.com"
    private val testPassword = "SecurePass@123"
    private val testFirstName = "John"
    private val testLastName = "Doe"

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        authMutation = AuthMutation(authService)
    }

    @Test
    @DisplayName("login should return AuthPayload with valid credentials")
    fun `login returns auth payload on success`() {
        // Arrange
        val expectedUser = UserResponse(
            id = testUserId,
            email = testEmail,
            firstName = testFirstName,
            lastName = testLastName
        )
        val expectedPayload = AuthPayload(
            user = expectedUser,
            token = "eyJhbGciOiJIUzI1NiJ9.token",
            refreshToken = "eyJhbGciOiJIUzI1NiJ9.refreshToken"
        )

        whenever(authService.login(testEmail, testPassword))
            .thenReturn(expectedPayload)

        // Act
        val result = authMutation.login(testEmail, testPassword)

        // Assert
        assertNotNull(result)
        assertEquals(expectedPayload.token, result.token)
        assertEquals(expectedPayload.refreshToken, result.refreshToken)
        assertEquals(expectedPayload.user.email, result.user.email)
    }

    @Test
    @DisplayName("login should propagate exception for invalid email")
    fun `login throws exception on invalid email`() {
        // Arrange
        whenever(authService.login("nonexistent@example.com", testPassword))
            .thenThrow(IllegalArgumentException("User not found."))

        // Act & Assert
        try {
            authMutation.login("nonexistent@example.com", testPassword)
            assertTrue(false, "Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("User not found.", e.message)
        }
    }

    @Test
    @DisplayName("login should propagate exception for invalid password")
    fun `login throws exception on invalid password`() {
        // Arrange
        whenever(authService.login(testEmail, "WrongPassword"))
            .thenThrow(IllegalArgumentException("Invalid password."))

        // Act & Assert
        try {
            authMutation.login(testEmail, "WrongPassword")
            assertTrue(false, "Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Invalid password.", e.message)
        }
    }

    @Test
    @DisplayName("register should return AuthPayload with valid input")
    fun `register returns auth payload on success`() {
        // Arrange
        val registerInput = RegisterRequest(
            email = testEmail,
            password = testPassword,
            firstName = testFirstName,
            lastName = testLastName
        )
        val expectedUser = UserResponse(
            id = testUserId,
            email = testEmail,
            firstName = testFirstName,
            lastName = testLastName
        )
        val expectedPayload = AuthPayload(
            user = expectedUser,
            token = "eyJhbGciOiJIUzI1NiJ9.token",
            refreshToken = "eyJhbGciOiJIUzI1NiJ9.refreshToken"
        )

        whenever(authService.register(
            RegisterRequest(
                email = testEmail,
                password = testPassword,
                firstName = testFirstName,
                lastName = testLastName
            )
        )).thenReturn(expectedPayload)

        // Act
        val result = authMutation.register(registerInput)

        // Assert
        assertNotNull(result)
        assertEquals(expectedPayload.token, result.token)
        assertEquals(expectedPayload.refreshToken, result.refreshToken)
        assertEquals(expectedPayload.user.email, result.user.email)
    }

    @Test
    @DisplayName("register should propagate exception for duplicate email")
    fun `register throws exception on duplicate email`() {
        // Arrange
        val registerInput = RegisterRequest(
            email = "existing@example.com",
            password = testPassword,
            firstName = testFirstName,
            lastName = testLastName
        )

        whenever(authService.register(
            RegisterRequest(
                email = "existing@example.com",
                password = testPassword,
                firstName = testFirstName,
                lastName = testLastName
            )
        )).thenThrow(IllegalArgumentException("Email already registered."))

        // Act & Assert
        try {
            authMutation.register(registerInput)
            assertTrue(false, "Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Email already registered.", e.message)
        }
    }

    @Test
    @DisplayName("register input mapping should convert to RegisterRequest correctly")
    fun `register input is correctly mapped to request`() {
        // Arrange
        val registerInput = RegisterRequest(
            email = "mapping@example.com",
            password = "Pass@123",
            firstName = "Map",
            lastName = "Test"
        )
        val expectedPayload = AuthPayload(
            user = UserResponse(testUserId, "mapping@example.com", "Map", "Test"),
            token = "token",
            refreshToken = "refreshToken"
        )

        whenever(authService.register(
            RegisterRequest(
                email = "mapping@example.com",
                password = "Pass@123",
                firstName = "Map",
                lastName = "Test"
            )
        )).thenReturn(expectedPayload)

        // Act
        val result = authMutation.register(registerInput)

        // Assert
        assertNotNull(result)
        assertEquals("mapping@example.com", result.user.email)
        assertEquals("Map", result.user.firstName)
        assertEquals("Test", result.user.lastName)
    }

    @Test
    @DisplayName("login with empty email should throw exception")
    fun `login throws exception on empty email`() {
        // Arrange
        whenever(authService.login("", testPassword))
            .thenThrow(IllegalArgumentException("User not found."))

        // Act & Assert
        try {
            authMutation.login("", testPassword)
            assertTrue(false, "Should have thrown exception")
        } catch (e: IllegalArgumentException) {
            assertEquals("User not found.", e.message)
        }
    }

    @Test
    @DisplayName("login with empty password should throw exception")
    fun `login throws exception on empty password`() {
        // Arrange
        whenever(authService.login(testEmail, ""))
            .thenThrow(IllegalArgumentException("Invalid password."))

        // Act & Assert
        try {
            authMutation.login(testEmail, "")
            assertTrue(false, "Should have thrown exception")
        } catch (e: IllegalArgumentException) {
            assertEquals("Invalid password.", e.message)
        }
    }

    @Test
    @DisplayName("login returns token in correct JWT format")
    fun `login token format is valid JWT`() {
        // Arrange
        val jwtToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMTIzIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val expectedPayload = AuthPayload(
            user = UserResponse(testUserId, testEmail, testFirstName, testLastName),
            token = jwtToken,
            refreshToken = jwtToken
        )

        whenever(authService.login(testEmail, testPassword))
            .thenReturn(expectedPayload)

        // Act
        val result = authMutation.login(testEmail, testPassword)

        // Assert
        assertNotNull(result.token)
        assertTrue(result.token.contains("."), "Token should contain dots (JWT format)")
        val parts = result.token.split(".")
        assertEquals(3, parts.size, "JWT should have 3 parts (header.payload.signature)")
    }
}
