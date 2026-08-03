package com.trippoint.backend.auth.repository

import com.trippoint.backend.auth.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RefreshTokenRepository :
    JpaRepository<RefreshToken, UUID> {

    fun findAllByUser_Id(userId: UUID): List<RefreshToken>
    fun findAllByDevice_Id(deviceId: UUID): List<RefreshToken>
}
}
