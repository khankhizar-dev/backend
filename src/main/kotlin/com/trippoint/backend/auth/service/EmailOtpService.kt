package com.trippoint.backend.auth.service

import com.trippoint.backend.auth.entity.EmailOtp
import com.trippoint.backend.auth.entity.OtpPurpose
import com.trippoint.backend.auth.entity.User
import com.trippoint.backend.auth.repository.EmailOtpRepository
import com.trippoint.backend.auth.repository.UserRepository
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.UUID

@Service
class EmailOtpService(
    private val emailOtpRepository: EmailOtpRepository,
    private val userRepository: UserRepository,
    private val hashService: HashService,
    private val emailSender: OtpEmailSender
) {
    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val EXPIRY_MINUTES = 10L
    }

    fun issueEmailVerificationOtp(user: User) = issueOtp(user, OtpPurpose.EMAIL_VERIFICATION)

    fun resendEmailVerificationOtp(userId: UUID): Boolean {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        require(!user.emailVerified) { "Email is already verified" }
        issueEmailVerificationOtp(user)
        return true
    }

    fun verifyEmailOtp(email: String, otp: String): Boolean {
        val record = getValidOtp(email, otp, OtpPurpose.EMAIL_VERIFICATION)
        val user = record.user ?: throw IllegalStateException("OTP user is missing")
        record.verifiedAt = OffsetDateTime.now()
        user.emailVerified = true
        user.updatedAt = OffsetDateTime.now()
        emailOtpRepository.save(record)
        userRepository.save(user)
        return true
    }

    fun requestPasswordReset(email: String): Boolean {
        val user = userRepository.findByEmail(email) ?: return true
        issueOtp(user, OtpPurpose.PASSWORD_RESET)
        return true
    }

    fun verifyPasswordResetOtp(email: String, otp: String): Boolean {
        getValidOtp(email, otp, OtpPurpose.PASSWORD_RESET)
        return true
    }

    private fun issueOtp(user: User, purpose: OtpPurpose) {
        val otp = SecureRandom().nextInt(1_000_000).toString().padStart(6, '0')
        emailOtpRepository.save(EmailOtp(
            user = user,
            otpHash = hashService.sha256(otp),
            purpose = purpose,
            expiresAt = OffsetDateTime.now().plusMinutes(EXPIRY_MINUTES)
        ))
        val action = if (purpose == OtpPurpose.EMAIL_VERIFICATION) "verify your email" else "reset your password"
        emailSender.send(user.email, "TripPoint verification code", "Use code $otp to $action. It expires in 10 minutes.")
    }

    private fun getValidOtp(email: String, otp: String, purpose: OtpPurpose): EmailOtp {
        val record = emailOtpRepository.findTopByUser_EmailAndPurposeOrderByCreatedAtDesc(email, purpose)
            ?: throw IllegalArgumentException("Invalid or expired OTP")
        require(record.verifiedAt == null && record.expiresAt.isAfter(OffsetDateTime.now()) && record.attempts < MAX_ATTEMPTS) {
            "Invalid or expired OTP"
        }
        if (record.otpHash != hashService.sha256(otp)) {
            record.attempts += 1
            emailOtpRepository.save(record)
            throw IllegalArgumentException("Invalid or expired OTP")
        }
        return record
    }
}
