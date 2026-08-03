package com.trippoint.backend.auth.repository

import com.trippoint.backend.auth.entity.EmailOtp
import com.trippoint.backend.auth.entity.OtpPurpose
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EmailOtpRepository : JpaRepository<EmailOtp, UUID> {
    fun findTopByUser_IdAndPurposeOrderByCreatedAtDesc(userId: UUID, purpose: OtpPurpose): EmailOtp?
    fun findTopByUser_EmailAndPurposeOrderByCreatedAtDesc(email: String, purpose: OtpPurpose): EmailOtp?
}
