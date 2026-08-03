package com.trippoint.backend.auth.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import com.trippoint.backend.auth.repository.TokenBlacklistRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.UUID
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret:my-super-secret-key-that-is-at-least-256-bits-long-for-HS256-algo}")
    private val secret: String,

    @Value("\${jwt.expiration:86400000}")
    private val expiration: Long,

    @Value("\${jwt.refresh-expiration:604800000}")
    private val refreshExpiration: Long,

    private val tokenBlacklistRepository: TokenBlacklistRepository? = null
) {

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }

    fun generateToken(userId: UUID): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .subject(userId.toString())
            .id(UUID.randomUUID().toString())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    fun generateRefreshToken(userId: UUID): String {
        val now = Date()
        val expiryDate = Date(now.time + refreshExpiration)

        return Jwts.builder()
            .subject(userId.toString())
            .id(UUID.randomUUID().toString())
            .issuedAt(now)
            .expiration(expiryDate)
            .claim("type", "refresh")
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            val tokenClaims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
            !isTokenIdBlacklisted(tokenClaims.id)
        } catch (e: Exception) {
            false
        }
    }

    fun getUserIdFromToken(token: String): UUID? {
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
            UUID.fromString(claims.subject)
        } catch (e: Exception) {
            null
        }
    }

    fun isRefreshToken(token: String): Boolean {
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload

            claims["type"] == "refresh"
        } catch (e: Exception) {
            false
        }
    }

    fun getTokenId(token: String): String? = claims(token)?.id

    fun getTokenExpiration(token: String): OffsetDateTime? =
        claims(token)?.expiration?.toInstant()?.atOffset(ZoneOffset.UTC)

    fun getTokenIssuedAt(token: String): OffsetDateTime? =
        claims(token)?.issuedAt?.toInstant()?.atOffset(ZoneOffset.UTC)

    fun isTokenBlacklisted(token: String): Boolean = isTokenIdBlacklisted(getTokenId(token))

    private fun isTokenIdBlacklisted(tokenId: String?): Boolean =
        tokenId != null && tokenBlacklistRepository?.existsByTokenId(tokenId) == true

    private fun claims(token: String) = try {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
    } catch (_: Exception) {
        null
    }
}
