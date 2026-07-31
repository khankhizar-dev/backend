package com.trippoint.backend.auth

import com.trippoint.backend.auth.dto.RegisterRequest
import com.trippoint.backend.auth.service.AuthService
import com.trippoint.backend.auth.service.PasswordService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var passwordService: PasswordService

    @Test
    fun `test password encoding and matching`() {
        val password = "TestPassword123!"
        val encoded = passwordService.encode(password)
        
        assert(passwordService.matches(password, encoded))
        assert(!passwordService.matches("WrongPassword", encoded))
    }

    @Test
    fun `test register user`() {
        val request = RegisterRequest(
            email = "test@example.com",
            password = "SecurePass123!",
            firstName = "John",
            lastName = "Doe"
        )

        val payload = authService.register(request)

        assertNotNull(payload.user)
        assertNotNull(payload.token)
        assertNotNull(payload.refreshToken)
        assertEquals("test@example.com", payload.user.email)
        assertEquals("John", payload.user.firstName)
        assertEquals("Doe", payload.user.lastName)
    }

    @Test
    fun `test login user`() {
        val email = "login@example.com"
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
        assertEquals(email, payload.user.email)
    }
}
