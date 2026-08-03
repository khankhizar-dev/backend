package com.trippoint.backend.auth.graphql

import com.trippoint.backend.auth.dto.UserDeviceResponse
import com.trippoint.backend.auth.dto.UserResponse
import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.auth.service.AuthService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID
import kotlin.test.assertEquals

class AuthQueryTest {
    private val service = mock<AuthService>()
    private val query = AuthQuery(service)
    private val userId = UUID.randomUUID()
    private val authentication = UsernamePasswordAuthenticationToken(UserPrincipal(userId, "user@example.com"), null)

    @AfterEach
    fun clearSecurityContext() = SecurityContextHolder.clearContext()

    @Test
    fun `me delegates using the authenticated principal`() {
        val user = UserResponse(userId, "user@example.com", "First", "Last")
        whenever(service.me(userId)).thenReturn(user)

        assertEquals(user, query.me(authentication))
        verify(service).me(userId)
    }

    @Test
    fun `user devices delegates using authentication from security context`() {
        val device = UserDeviceResponse(UUID.randomUUID(), "Pixel", "Android", "1", null, "now")
        whenever(service.userDevices(userId)).thenReturn(listOf(device))
        SecurityContextHolder.getContext().authentication = authentication

        assertEquals(listOf(device), query.userDevices(null))
        verify(service).userDevices(userId)
    }

    @Test
    fun `queries reject absent and invalid principals`() {
        assertThrows<IllegalArgumentException> { query.me(null) }
        val invalid = UsernamePasswordAuthenticationToken("not-a-user", null)
        assertThrows<IllegalArgumentException> { query.me(invalid) }
        assertThrows<IllegalArgumentException> { query.userDevices(invalid) }
    }
}
