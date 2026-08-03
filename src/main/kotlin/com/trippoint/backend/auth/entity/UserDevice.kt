package com.trippoint.backend.auth.entity

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "user_devices")
class UserDevice(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,
    @Column(name = "device_name")
    var deviceName: String? = null,
    var platform: String? = null,
    @Column(name = "app_version")
    var appVersion: String? = null,
    @Column(name = "last_login_at")
    var lastLoginAt: OffsetDateTime? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()
)
