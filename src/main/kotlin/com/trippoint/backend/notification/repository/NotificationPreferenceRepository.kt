package com.trippoint.backend.notification.repository

import com.trippoint.backend.notification.entity.NotificationPreference
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NotificationPreferenceRepository :
    JpaRepository<NotificationPreference, UUID> {

    fun findByUserId(userId: UUID): NotificationPreference?
}