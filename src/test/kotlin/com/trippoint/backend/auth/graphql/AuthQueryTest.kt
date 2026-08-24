package com.trippoint.backend.auth.graphql

import com.trippoint.backend.auth.dto.UserDeviceResponse
import com.trippoint.backend.auth.dto.UserResponse
import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.auth.service.AuthService
import com.trippoint.backend.auth.service.PreferencesService
import com.trippoint.backend.auth.service.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class AuthQueryTest {
    private lateinit var userService: UserService
    private lateinit var service: AuthService
    private lateinit var query: AuthQuery
    @Mock
    lateinit var preferencesService: PreferencesService
    private val userId = UUID.randomUUID()
    private val authentication = UsernamePasswordAuthenticationToken(UserPrincipal(userId, "user@example.com"), null)

    @BeforeEach
    fun setup() {
        service = mock()
        userService = mock()

        query = AuthQuery(
            authService = service,
            userService = userService,
            preferencesService
        )
    }

    @AfterEach
    fun clearSecurityContext() = SecurityContextHolder.clearContext()

    @Test
    fun `me delegates using the authenticated principal`() {
        val user = UserResponse(
            userId,
            "user@example.com",
            "First",
            "Last"
        )

        whenever(userService.getProfile(userId))
            .thenReturn(user)

        assertEquals(
            user,
            query.me(authentication)
        )

        verify(userService).getProfile(userId)
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
