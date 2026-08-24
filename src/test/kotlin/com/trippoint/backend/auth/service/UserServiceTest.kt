package com.trippoint.backend.auth.service

import com.trippoint.backend.auth.dto.UpdateProfileInput
import com.trippoint.backend.auth.entity.User
import com.trippoint.backend.auth.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.Optional
import java.util.UUID

class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userService: UserService

    private lateinit var userId: UUID
    private lateinit var user: User

    @BeforeEach
    fun setUp() {
        userRepository = mock()
        userService = UserService(userRepository)

        userId = UUID.randomUUID()

        user = User(
            id = userId,
            email = "khizar@example.com",
            passwordHash = "encoded-password",
            firstName = "Khizar",
            lastName = "Khan",
            username = "khizar",
            profileImageUrl = null,
            country = "IN",
            currency = "INR",
            language = "en",
            timezone = "Asia/Kolkata"
        )
    }

    @Test
    fun `should get user profile`() {
        whenever(userRepository.findById(userId))
            .thenReturn(Optional.of(user))

        val response = userService.getProfile(userId)

        assertEquals(userId, response.id)
        assertEquals("khizar@example.com", response.email)
        assertEquals("Khizar", response.firstName)
        assertEquals("Khan", response.lastName)
        assertEquals("Khizar Khan", response.fullName)
        assertEquals("khizar", response.username)
        assertEquals("IN", response.country)
        assertEquals("INR", response.currency)
        assertEquals("en", response.language)
        assertEquals("Asia/Kolkata", response.timezone)

        verify(userRepository).findById(userId)
    }

    @Test
    fun `should throw exception when user does not exist`() {
        whenever(userRepository.findById(userId))
            .thenReturn(Optional.empty())

        val exception = assertThrows<IllegalArgumentException> {
            userService.getProfile(userId)
        }

        assertEquals("User not found", exception.message)
    }

    @Test
    fun `should update user profile`() {
        whenever(userRepository.findById(userId))
            .thenReturn(Optional.of(user))

        whenever(userRepository.findByUsername("newusername"))
            .thenReturn(null)

        whenever(userRepository.save(any<User>()))
            .thenAnswer { it.arguments[0] as User }

        val input = UpdateProfileInput(
            firstName = "John",
            lastName = "Doe",
            username = "newusername",
            country = "US",
            currency = "USD",
            language = "en",
            timezone = "America/New_York",
            phoneNumber = "+919999999999",
            dateOfBirth = "1992-05-12",
            nationality = "Indian"
        )

        val response = userService.updateProfile(userId, input)

        assertEquals("John", response.firstName)
        assertEquals("Doe", response.lastName)
        assertEquals("John Doe", response.fullName)
        assertEquals("newusername", response.username)
        assertEquals("US", response.country)
        assertEquals("USD", response.currency)
        assertEquals("en", response.language)
        assertEquals("America/New_York", response.timezone)

        verify(userRepository).findById(userId)
        verify(userRepository).findByUsername("newusername")
        verify(userRepository).save(user)
    }

    @Test
    fun `should support partial profile update`() {
        whenever(userRepository.findById(userId))
            .thenReturn(Optional.of(user))

        whenever(userRepository.save(any<User>()))
            .thenAnswer { it.arguments[0] as User }

        val input = UpdateProfileInput(
            firstName = "John"
        )

        val response = userService.updateProfile(userId, input)

        assertEquals("John", response.firstName)

        // Existing values must remain unchanged
        assertEquals("Khan", response.lastName)
        assertEquals("khizar", response.username)
        assertEquals("IN", response.country)
        assertEquals("INR", response.currency)
        assertEquals("en", response.language)
        assertEquals("Asia/Kolkata", response.timezone)

        verify(userRepository).save(user)

        verify(userRepository, never())
            .findByUsername(any())
    }

    @Test
    fun `should normalize username`() {
        whenever(userRepository.findById(userId))
            .thenReturn(Optional.of(user))

        whenever(userRepository.findByUsername("john_doe"))
            .thenReturn(null)

        whenever(userRepository.save(any<User>()))
            .thenAnswer { it.arguments[0] as User }

        val input = UpdateProfileInput(
            username = "  JOHN_DOE  "
        )

        val response = userService.updateProfile(userId, input)

        assertEquals("john_doe", response.username)

        verify(userRepository)
            .findByUsername("john_doe")
    }

    @Test
    fun `should reject blank username`() {
        whenever(userRepository.findById(userId))
            .thenReturn(Optional.of(user))

        val exception = assertThrows<IllegalArgumentException> {
            userService.updateProfile(
                userId,
                UpdateProfileInput(
                    username = "   "
                )
            )
        }

        assertEquals(
            "Username cannot be blank",
            exception.message
        )

        verify(userRepository, never())
            .save(any<User>())
    }

    @Test
    fun `should reject duplicate username`() {
        val existingUser = User(
            id = UUID.randomUUID(),
            email = "other@example.com",
            passwordHash = "encoded-password",
            firstName = "Other",
            lastName = "User",
            username = "john"
        )

        whenever(userRepository.findById(userId))
            .thenReturn(Optional.of(user))

        whenever(userRepository.findByUsername("john"))
            .thenReturn(existingUser)

        val exception = assertThrows<IllegalArgumentException> {
            userService.updateProfile(
                userId,
                UpdateProfileInput(
                    username = "john"
                )
            )
        }

        assertEquals(
            "Username already taken",
            exception.message
        )

        verify(userRepository, never())
            .save(any<User>())
    }

    @Test
    fun `should allow user to keep their existing username`() {
        whenever(userRepository.findById(userId))
            .thenReturn(Optional.of(user))

        whenever(userRepository.findByUsername("khizar"))
            .thenReturn(user)

        whenever(userRepository.save(any<User>()))
            .thenAnswer { it.arguments[0] as User }

        val response = userService.updateProfile(
            userId,
            UpdateProfileInput(
                username = "khizar"
            )
        )

        assertEquals("khizar", response.username)

        verify(userRepository).save(user)
    }

    @Test
    fun `should normalize currency to uppercase`() {
        whenever(userRepository.findById(userId))
            .thenReturn(Optional.of(user))

        whenever(userRepository.save(any<User>()))
            .thenAnswer { it.arguments[0] as User }

        val response = userService.updateProfile(
            userId,
            UpdateProfileInput(
                currency = "usd"
            )
        )

        assertEquals("USD", response.currency)
    }

    @Test
    fun `should normalize language to lowercase`() {
        whenever(userRepository.findById(userId))
            .thenReturn(Optional.of(user))

        whenever(userRepository.save(any<User>()))
            .thenAnswer { it.arguments[0] as User }

        val response = userService.updateProfile(
            userId,
            UpdateProfileInput(
                language = "EN"
            )
        )

        assertEquals("en", response.language)
    }

    @Test
    fun `should throw exception when updating nonexistent user`() {
        whenever(userRepository.findById(userId))
            .thenReturn(Optional.empty())

        val exception = assertThrows<IllegalArgumentException> {
            userService.updateProfile(
                userId,
                UpdateProfileInput(
                    firstName = "John"
                )
            )
        }

        assertEquals(
            "User not found",
            exception.message
        )

        verify(userRepository, never())
            .save(any<User>())
    }
}