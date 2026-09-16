package com.trippoint.backend.activity.event

import com.trippoint.backend.activity.entity.ActivityLog

data class ActivityLogCreatedEvent(
    val activity: ActivityLog
)
