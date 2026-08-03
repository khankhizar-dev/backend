package com.trippoint.backend.auth.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "token_blacklist")
class TokenBlacklist(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @Column(name = "token_id", nullable = false, unique = true)
    var tokenId: String = "",
    @Column(name = "user_id", nullable = false)
    var userId: UUID? = null,
    @Column(name = "expires_at", nullable = false)
    var expiresAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "blacklisted_at", nullable = false)
    var blacklistedAt: OffsetDateTime = OffsetDateTime.now()
)
