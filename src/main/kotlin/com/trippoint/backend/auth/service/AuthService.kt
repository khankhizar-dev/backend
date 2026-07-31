package com.trippoint.backend.auth.service

import com.trippoint.backend.auth.dto.RegisterRequest
import com.trippoint.backend.auth.dto.UserResponse
import com.trippoint.backend.auth.dto.AuthPayload
import com.trippoint.backend.auth.entity.User
import com.trippoint.backend.auth.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordService: PasswordService,
    private val jwtService: JwtService
) {

    fun register(request: RegisterRequest): AuthPayload {
        require(!userRepository.existsByEmail(request.email)) {
            "Email already registered."
        }

        val user = User(
            email = request.email,
            passwordHash = passwordService.encode(request.password),
            firstName = request.firstName,
            lastName = request.lastName
        )

        val saved = userRepository.save(user)

        return saved.id?.let { userId ->
            val token = jwtService.generateToken(userId)
            val refreshToken = jwtService.generateRefreshToken(userId)

            AuthPayload(
                user = UserResponse(
                    id = userId,
                    email = saved.email,
                    firstName = saved.firstName,
                    lastName = saved.lastName
                ),
                token = token,
                refreshToken = refreshToken
            )
        } ?: throw IllegalStateException("Failed to generate user ID")
    }

    fun login(email: String, password: String): AuthPayload {
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("User not found.")

        require(passwordService.matches(password, user.passwordHash)) {
            "Invalid password."
        }

        return user.id?.let { userId ->
            val token = jwtService.generateToken(userId)
            val refreshToken = jwtService.generateRefreshToken(userId)

            AuthPayload(
                user = UserResponse(
                    id = userId,
                    email = user.email,
                    firstName = user.firstName,
                    lastName = user.lastName
                ),
                token = token,
                refreshToken = refreshToken
            )
        } ?: throw IllegalStateException("Failed to get user ID")
    }
}