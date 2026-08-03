package com.trippoint.backend.auth.repository

import com.trippoint.backend.auth.entity.TokenBlacklist
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime
import java.util.UUID

interface TokenBlacklistRepository : JpaRepository<TokenBlacklist, UUID> {
    fun existsByTokenId(tokenId: String): Boolean
    fun deleteByExpiresAtBefore(time: OffsetDateTime): Long
}
