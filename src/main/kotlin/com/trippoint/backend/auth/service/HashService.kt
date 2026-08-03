package com.trippoint.backend.auth.service

import org.springframework.stereotype.Service
import java.security.MessageDigest

@Service
class HashService {

    fun sha256(value: String): String {

        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray())

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }
}