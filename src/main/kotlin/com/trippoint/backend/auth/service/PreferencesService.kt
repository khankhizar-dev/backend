package com.trippoint.backend.auth.service

import com.trippoint.backend.auth.entity.User
import com.trippoint.backend.auth.entity.UserPreferences
import com.trippoint.backend.auth.repository.UserPreferencesRepository
import com.trippoint.backend.auth.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PreferencesService(
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    @Transactional
    fun getPreferences(userId: UUID): UserPreferences {

        val existing = userPreferencesRepository.findByUserId(userId)

        if (existing != null) {
            return existing
        }

        val user = userRepository.findById(userId)
            .orElseThrow {
                IllegalArgumentException("User not found")
            }

        return createDefaultPreferences(user)
    }

    @Transactional
    fun createDefaultPreferences(user: User): UserPreferences {

        val existing = user.id?.let {
            userPreferencesRepository.findByUserId(it)
        }

        if (existing != null) {
            return existing
        }

        return userPreferencesRepository.save(
            UserPreferences(
                user = user,
                currency = "INR",
                language = "en",
                dateFormat = "DD/MM/YYYY",
                units = "METRIC",
                theme = "LIGHT"
            )
        )
    }

    @Transactional
    fun updatePreferences(
        userId: UUID,
        currency: String?,
        language: String?,
        dateFormat: String?,
        units: String?,
        theme: String?
    ): UserPreferences {

        val preferences = getPreferences(userId)

        currency?.let {
            preferences.currency = it.trim().uppercase()
        }

        language?.let {
            preferences.language = it.trim()
        }

        dateFormat?.let {
            preferences.dateFormat = it.trim()
        }

        units?.let {
            preferences.units = it.trim().uppercase()
        }

        theme?.let {
            preferences.theme = it.trim().uppercase()
        }

        return userPreferencesRepository.save(preferences)
    }
}