package com.trippoint.backend.auth.service

import com.trippoint.backend.auth.entity.RefreshToken
import com.trippoint.backend.auth.entity.TokenBlacklist
import com.trippoint.backend.auth.entity.User
import com.trippoint.backend.auth.entity.UserDevice
import com.trippoint.backend.auth.repository.RefreshTokenRepository
import com.trippoint.backend.auth.repository.TokenBlacklistRepository
import com.trippoint.backend.auth.repository.UserDeviceRepository
import com.trippoint.backend.auth.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthServiceSessionTest {
    @Mock private lateinit var users: UserRepository
    @Mock private lateinit var refreshTokens: RefreshTokenRepository
    @Mock private lateinit var passwords: PasswordService
    @Mock private lateinit var jwt: JwtService
    @Mock private lateinit var hashes: HashService
    @Mock private lateinit var blacklist: TokenBlacklistRepository
    @Mock private lateinit var devices: UserDeviceRepository

    private lateinit var service: AuthService
    private val userId = UUID.randomUUID()
    private val user = User(id = userId, email = "user@example.com", passwordHash = "old-hash")

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = AuthService(users, refreshTokens, passwords, jwt, hashes, blacklist, devices)
    }

    @Test
    fun `logout blacklists an owned valid access token once`() {
        val expiration = OffsetDateTime.now().plusHours(1)
        whenever(jwt.getUserIdFromToken("access")).thenReturn(userId)
        whenever(jwt.getTokenId("access")).thenReturn("jti")
        whenever(jwt.getTokenExpiration("access")).thenReturn(expiration)
        whenever(blacklist.existsByTokenId("jti")).thenReturn(false)

        assertTrue(service.logout("access", userId))

        val captor = argumentCaptor<TokenBlacklist>()
        verify(blacklist).save(captor.capture())
        assertEquals("jti", captor.firstValue.tokenId)
        assertEquals(userId, captor.firstValue.userId)
        assertEquals(expiration, captor.firstValue.expiresAt)
    }

    @Test
    fun `logout does not insert an already blacklisted token`() {
        whenever(jwt.getUserIdFromToken("access")).thenReturn(userId)
        whenever(jwt.getTokenId("access")).thenReturn("jti")
        whenever(jwt.getTokenExpiration("access")).thenReturn(OffsetDateTime.now().plusHours(1))
        whenever(blacklist.existsByTokenId("jti")).thenReturn(true)

        assertTrue(service.logout("access", userId))
        verify(blacklist, never()).save(any())
    }

    @Test
    fun `logout rejects an invalid or another users token`() {
        whenever(jwt.getUserIdFromToken("invalid")).thenReturn(null)
        assertThrows<IllegalArgumentException> { service.logout("invalid", userId) }

        whenever(jwt.getUserIdFromToken("other")).thenReturn(UUID.randomUUID())
        val error = assertThrows<IllegalArgumentException> { service.logout("other", userId) }
        assertEquals("Token does not belong to the authenticated user", error.message)
    }

    @Test
    fun `logout rejects tokens without required claims and missing configuration`() {
        whenever(jwt.getUserIdFromToken("no-id")).thenReturn(userId)
        whenever(jwt.getTokenId("no-id")).thenReturn(null)
        assertThrows<IllegalArgumentException> { service.logout("no-id", userId) }

        whenever(jwt.getTokenId("no-expiry")).thenReturn("jti")
        whenever(jwt.getUserIdFromToken("no-expiry")).thenReturn(userId)
        whenever(jwt.getTokenExpiration("no-expiry")).thenReturn(null)
        assertThrows<IllegalArgumentException> { service.logout("no-expiry", userId) }

        val withoutBlacklist = AuthService(users, refreshTokens, passwords, jwt, hashes)
        whenever(jwt.getUserIdFromToken("no-repository")).thenReturn(userId)
        whenever(jwt.getTokenId("no-repository")).thenReturn("jti")
        whenever(jwt.getTokenExpiration("no-repository")).thenReturn(OffsetDateTime.now().plusHours(1))
        assertThrows<IllegalArgumentException> { withoutBlacklist.logout("no-repository", userId) }
    }

    @Test
    fun `logout all devices revokes active refresh tokens and invalidates access tokens`() {
        val active = RefreshToken(revokedAt = null)
        val revoked = RefreshToken(revokedAt = OffsetDateTime.now().minusDays(1))
        whenever(refreshTokens.findAllByUser_Id(userId)).thenReturn(listOf(active, revoked))
        whenever(users.findById(userId)).thenReturn(Optional.of(user))

        assertTrue(service.logoutAllDevices(userId))

        assertTrue(active.revokedAt != null)
        assertEquals(revoked.revokedAt, revoked.revokedAt)
        assertTrue(user.tokensValidAfter != null)
        verify(refreshTokens).saveAll(listOf(active))
        verify(users).save(user)
    }

    @Test
    fun `logout all devices fails for an unknown user`() {
        whenever(refreshTokens.findAllByUser_Id(userId)).thenReturn(emptyList())
        whenever(users.findById(userId)).thenReturn(Optional.empty())
        assertThrows<IllegalArgumentException> { service.logoutAllDevices(userId) }
        verify(refreshTokens).saveAll(emptyList())
    }

    @Test
    fun `change password updates password and revokes all active sessions`() {
        val active = RefreshToken(revokedAt = null)
        whenever(users.findById(userId)).thenReturn(Optional.of(user))
        whenever(passwords.matches("current", "old-hash")).thenReturn(true)
        whenever(passwords.encode("new-password")).thenReturn("new-hash")
        whenever(refreshTokens.findAllByUser_Id(userId)).thenReturn(listOf(active))

        assertTrue(service.changePassword(userId, "current", "new-password"))

        assertEquals("new-hash", user.passwordHash)
        assertTrue(active.revokedAt != null)
        assertTrue(user.tokensValidAfter != null)
        verify(users).save(user)
        verify(refreshTokens).saveAll(listOf(active))
    }

    @Test
    fun `change password rejects blank passwords bad credentials and unknown users`() {
        assertThrows<IllegalArgumentException> { service.changePassword(userId, "current", " ") }

        whenever(users.findById(userId)).thenReturn(Optional.of(user))
        whenever(passwords.matches("wrong", "old-hash")).thenReturn(false)
        assertThrows<IllegalArgumentException> { service.changePassword(userId, "wrong", "new") }

        whenever(users.findById(userId)).thenReturn(Optional.empty())
        assertThrows<IllegalArgumentException> { service.changePassword(userId, "current", "new") }
    }

    @Test
    fun `user devices maps persistent records to public responses`() {
        val now = OffsetDateTime.now()
        val device = UserDevice(UUID.randomUUID(), user, "Pixel 9", "Android", "1.0", now, now)
        whenever(devices.findAllByUser_IdOrderByLastLoginAtDesc(userId)).thenReturn(listOf(device))

        val result = service.userDevices(userId)

        assertEquals(1, result.size)
        assertEquals("Pixel 9", result.single().deviceName)
        assertEquals(now.toString(), result.single().lastLoginAt)
    }

    @Test
    fun `user devices requires device tracking configuration`() {
        val withoutDevices = AuthService(users, refreshTokens, passwords, jwt, hashes, blacklist)
        assertThrows<IllegalArgumentException> { withoutDevices.userDevices(userId) }
    }
}
