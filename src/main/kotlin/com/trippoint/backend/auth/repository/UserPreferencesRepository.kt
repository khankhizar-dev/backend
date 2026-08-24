package com.trippoint.backend.auth.repository

import com.trippoint.backend.auth.entity.UserPreferences
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserPreferencesRepository : JpaRepository<UserPreferences, UUID> {

    fun findByUserId(userId: UUID): UserPreferences?
}