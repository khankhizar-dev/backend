package com.trippoint.backend.auth.service

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class PasswordService {

    private val encoder = BCryptPasswordEncoder()

    fun encode(password: String): String =
        encoder.encode(password)

    fun matches(password: String, hash: String): Boolean =
        encoder.matches(password, hash)
}