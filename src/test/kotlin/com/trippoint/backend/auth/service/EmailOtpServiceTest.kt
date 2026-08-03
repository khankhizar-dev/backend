package com.trippoint.backend.auth.service

import com.trippoint.backend.auth.entity.EmailOtp
import com.trippoint.backend.auth.entity.OtpPurpose
import com.trippoint.backend.auth.entity.User
import com.trippoint.backend.auth.repository.EmailOtpRepository
import com.trippoint.backend.auth.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmailOtpServiceTest {
    @Mock private lateinit var otps: EmailOtpRepository
    @Mock private lateinit var users: UserRepository
    @Mock private lateinit var hashes: HashService
    @Mock private lateinit var sender: OtpEmailSender

    private lateinit var service: EmailOtpService
    private val user = User(id = UUID.randomUUID(), email = "user@example.com")

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = EmailOtpService(otps, users, hashes, sender)
        whenever(hashes.sha256(any())).thenAnswer { "hash-${it.getArgument<String>(0)}" }
    }

    @Test
    fun `issues a hashed six digit email verification OTP and sends it`() {
        service.issueEmailVerificationOtp(user)

        val otp = argumentCaptor<EmailOtp>()
        verify(otps).save(otp.capture())
        assertEquals(user, otp.firstValue.user)
        assertEquals(OtpPurpose.EMAIL_VERIFICATION, otp.firstValue.purpose)
        assertTrue(otp.firstValue.expiresAt.isAfter(OffsetDateTime.now().plusMinutes(9)))
        verify(sender).send(org.mockito.kotlin.eq(user.email), org.mockito.kotlin.any(), org.mockito.kotlin.any())
    }

    @Test
    fun `verifies a matching email OTP and marks the user verified`() {
        val record = EmailOtp(user = user, otpHash = "hash-123456", purpose = OtpPurpose.EMAIL_VERIFICATION,
            expiresAt = OffsetDateTime.now().plusMinutes(1))
        whenever(otps.findTopByUser_EmailAndPurposeOrderByCreatedAtDesc(user.email, OtpPurpose.EMAIL_VERIFICATION))
            .thenReturn(record)

        assertTrue(service.verifyEmailOtp(user.email, "123456"))

        assertTrue(user.emailVerified)
        assertTrue(record.verifiedAt != null)
        verify(users).save(user)
        verify(otps).save(record)
    }

    @Test
    fun `rejects expired and incorrect OTPs while tracking attempts`() {
        val expired = EmailOtp(user = user, otpHash = "hash-123456", expiresAt = OffsetDateTime.now().minusSeconds(1))
        whenever(otps.findTopByUser_EmailAndPurposeOrderByCreatedAtDesc(user.email, OtpPurpose.EMAIL_VERIFICATION))
            .thenReturn(expired)
        assertThrows<IllegalArgumentException> { service.verifyEmailOtp(user.email, "123456") }

        val incorrect = EmailOtp(user = user, otpHash = "hash-123456", expiresAt = OffsetDateTime.now().plusMinutes(1))
        whenever(otps.findTopByUser_EmailAndPurposeOrderByCreatedAtDesc(user.email, OtpPurpose.EMAIL_VERIFICATION))
            .thenReturn(incorrect)
        assertThrows<IllegalArgumentException> { service.verifyEmailOtp(user.email, "000000") }
        assertEquals(1, incorrect.attempts)
        verify(otps).save(incorrect)
    }

    @Test
    fun `resends only for an existing unverified user`() {
        whenever(users.findById(user.id!!)).thenReturn(Optional.of(user))
        assertTrue(service.resendEmailVerificationOtp(user.id!!))
        verify(sender).send(org.mockito.kotlin.eq(user.email), org.mockito.kotlin.any(), org.mockito.kotlin.any())

        user.emailVerified = true
        assertThrows<IllegalArgumentException> { service.resendEmailVerificationOtp(user.id!!) }
    }

    @Test
    fun `password reset does not disclose whether email exists and validates its own purpose`() {
        whenever(users.findByEmail("missing@example.com")).thenReturn(null)
        assertTrue(service.requestPasswordReset("missing@example.com"))
        verify(sender, never()).send(org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.any())

        val reset = EmailOtp(user = user, otpHash = "hash-123456", purpose = OtpPurpose.PASSWORD_RESET,
            expiresAt = OffsetDateTime.now().plusMinutes(1))
        whenever(otps.findTopByUser_EmailAndPurposeOrderByCreatedAtDesc(user.email, OtpPurpose.PASSWORD_RESET))
            .thenReturn(reset)
        assertTrue(service.verifyPasswordResetOtp(user.email, "123456"))
        assertFalse(user.emailVerified)
    }
}
