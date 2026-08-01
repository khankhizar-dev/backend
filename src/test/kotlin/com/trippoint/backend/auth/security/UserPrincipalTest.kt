package com.trippoint.backend.auth.security

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.security.core.GrantedAuthority
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@DisplayName("UserPrincipal Tests")
class UserPrincipalTest {

    private val testUserId = UUID.randomUUID()
    private val testEmail = "test@example.com"

    @Test
    @DisplayName("UserPrincipal should store userId and email")
    fun `user principal stores user data`() {
        // Arrange & Act
        val principal = UserPrincipal(testUserId, testEmail)

        // Assert
        assertEquals(testUserId, principal.userId)
        assertEquals(testEmail, principal.username)
    }

    @Test
    @DisplayName("UserPrincipal username should return email")
    fun `username returns email`() {
        // Arrange & Act
        val principal = UserPrincipal(testUserId, testEmail)

        // Assert
        assertEquals(testEmail, principal.username)
    }

    @Test
    @DisplayName("UserPrincipal password should be empty")
    fun `password is empty`() {
        // Arrange & Act
        val principal = UserPrincipal(testUserId, testEmail)

        // Assert
        assertEquals("", principal.password)
    }

    @Test
    @DisplayName("UserPrincipal authorities should be empty collection")
    fun `authorities is empty`() {
        // Arrange & Act
        val principal = UserPrincipal(testUserId, testEmail)

        // Assert
        val authorities: Collection<GrantedAuthority> = principal.authorities
        assertTrue(authorities.isEmpty())
    }

    @Test
    @DisplayName("UserPrincipal account should be non-expired")
    fun `account is non-expired`() {
        // Arrange & Act
        val principal = UserPrincipal(testUserId, testEmail)

        // Assert
        assertTrue(principal.isAccountNonExpired)
    }

    @Test
    @DisplayName("UserPrincipal account should be non-locked")
    fun `account is non-locked`() {
        // Arrange & Act
        val principal = UserPrincipal(testUserId, testEmail)

        // Assert
        assertTrue(principal.isAccountNonLocked)
    }

    @Test
    @DisplayName("UserPrincipal credentials should be non-expired")
    fun `credentials are non-expired`() {
        // Arrange & Act
        val principal = UserPrincipal(testUserId, testEmail)

        // Assert
        assertTrue(principal.isCredentialsNonExpired)
    }

    @Test
    @DisplayName("UserPrincipal should be enabled")
    fun `principal is enabled`() {
        // Arrange & Act
        val principal = UserPrincipal(testUserId, testEmail)

        // Assert
        assertTrue(principal.isEnabled)
    }

    @Test
    @DisplayName("UserPrincipal with different IDs should create different instances")
    fun `different user IDs create different principals`() {
        // Arrange
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()

        // Act
        val principal1 = UserPrincipal(id1, testEmail)
        val principal2 = UserPrincipal(id2, testEmail)

        // Assert
        assertEquals(id1, principal1.userId)
        assertEquals(id2, principal2.userId)
        assertNotNull(principal1.userId)
        assertNotNull(principal2.userId)
    }

    @Test
    @DisplayName("UserPrincipal with different emails should create different instances")
    fun `different emails create different principals`() {
        // Arrange
        val email1 = "user1@example.com"
        val email2 = "user2@example.com"

        // Act
        val principal1 = UserPrincipal(testUserId, email1)
        val principal2 = UserPrincipal(testUserId, email2)

        // Assert
        assertEquals(email1, principal1.username)
        assertEquals(email2, principal2.username)
        assertEquals(email1, principal1.username)
        assertEquals(email2, principal2.username)
    }

    @Test
    @DisplayName("UserPrincipal should initialize with valid data")
    fun `principal initializes with valid data`() {
        // Arrange
        val email = "valid@example.com"

        // Act
        val principal = UserPrincipal(testUserId, email)

        // Assert
        assertNotNull(principal)
        assertNotNull(principal.userId)
        assertNotNull(principal.username)
        assertEquals(testUserId, principal.userId)
        assertEquals(email, principal.username)
    }
}
