package com.trippoint.backend.auth.repository

import com.trippoint.backend.auth.entity.UserDevice
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserDeviceRepository : JpaRepository<UserDevice, UUID> {
    fun findAllByUser_IdOrderByLastLoginAtDesc(userId: UUID): List<UserDevice>
}
