package com.trippoint.backend.auth.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "email_otp")
class EmailOtp(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,
    @Column(name = "otp_hash", nullable = false)
    var otpHash: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var purpose: OtpPurpose = OtpPurpose.EMAIL_VERIFICATION,
    @Column(name = "expires_at", nullable = false)
    var expiresAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(nullable = false)
    var attempts: Int = 0,
    @Column(name = "verified_at")
    var verifiedAt: OffsetDateTime? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()
)
