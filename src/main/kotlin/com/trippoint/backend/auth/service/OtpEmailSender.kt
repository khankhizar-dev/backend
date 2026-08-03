package com.trippoint.backend.auth.service

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

interface OtpEmailSender {
    fun send(to: String, subject: String, body: String)
}

@Component
@ConditionalOnProperty(prefix = "app.mail", name = ["enabled"], havingValue = "true")
class SmtpOtpEmailSender(private val mailSender: JavaMailSender) : OtpEmailSender {
    override fun send(to: String, subject: String, body: String) {
        mailSender.send(SimpleMailMessage().apply {
            setTo(to)
            this.subject = subject
            text = body
        })
    }
}

@Component
@ConditionalOnProperty(prefix = "app.mail", name = ["enabled"], havingValue = "false", matchIfMissing = true)
class LoggingOtpEmailSender : OtpEmailSender {
    private val logger = LoggerFactory.getLogger(javaClass)
    override fun send(to: String, subject: String, body: String) {
        logger.warn("SMTP delivery is disabled; OTP email for {} was not sent. Subject: {}", to, subject)
    }
}
