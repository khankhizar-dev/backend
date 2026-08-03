package com.trippoint.backend.auth.service

import com.trippoint.backend.auth.dto.RegisterRequest
import com.trippoint.backend.auth.dto.UserResponse
import com.trippoint.backend.auth.dto.AuthPayload
import com.trippoint.backend.auth.dto.RefreshTokenRequest
import com.trippoint.backend.auth.dto.RefreshTokenResponse
import com.trippoint.backend.auth.entity.RefreshToken
import com.trippoint.backend.auth.entity.User
import com.trippoint.backend.auth.repository.RefreshTokenRepository
import com.trippoint.backend.auth.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordService: PasswordService,
    private val jwtService: JwtService,
    private val hashService: HashService
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

            // Save refresh token
            refreshTokenRepository.save(
                RefreshToken(
                    user = saved,
                    tokenHash = hashService.sha256(refreshToken),
                    expiresAt = OffsetDateTime.now().plusDays(7)
                )
            )


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

            // Save refresh token
            refreshTokenRepository.save(
                RefreshToken(
                    user = user,
                    tokenHash = hashService.sha256(refreshToken),
                    expiresAt = OffsetDateTime.now().plusDays(7)
                )
            )

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

    fun refreshToken(request: RefreshTokenRequest): RefreshTokenResponse {

        if (!jwtService.isRefreshToken(request.refreshToken)) {
            throw IllegalArgumentException("Invalid refresh token")
        }

        val userId = jwtService.getUserIdFromToken(request.refreshToken)
            ?: throw IllegalArgumentException("Invalid refresh token")

        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        val storedToken = refreshTokenRepository
            .findAllByUser_Id(userId)
            .firstOrNull {
                hashService.sha256(request.refreshToken) == it.tokenHash
            }
            ?: throw IllegalArgumentException("Refresh token not found")

        if (storedToken.revokedAt != null) {
            throw IllegalArgumentException("Refresh token revoked")
        }

        if (storedToken.expiresAt.isBefore(OffsetDateTime.now())) {
            throw IllegalArgumentException("Refresh token expired")
        }

        // Revoke old token
        storedToken.revokedAt = OffsetDateTime.now()
        refreshTokenRepository.save(storedToken)

        // Generate new tokens
        val accessToken = jwtService.generateToken(user.id!!)
        val refreshToken = jwtService.generateRefreshToken(user.id!!)

        // Save new refresh token
        refreshTokenRepository.save(
            RefreshToken(
                user = user,
                tokenHash = hashService.sha256(refreshToken),
                expiresAt = OffsetDateTime.now().plusDays(7)
            )
        )

        return RefreshTokenResponse(
            token = accessToken,
            refreshToken = refreshToken
        )
    }

    fun me(userId: java.util.UUID): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow {
                IllegalArgumentException("User not found")
            }

        return UserResponse(
            id = user.id!!,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName
        )
    }
}